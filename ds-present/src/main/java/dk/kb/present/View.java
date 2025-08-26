/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.present;

import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RightsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationOutputDto;
import dk.kb.license.util.DsLicenseClient;
import dk.kb.present.config.ServiceConfig;
import dk.kb.present.transform.DSTransformer;
import dk.kb.present.transform.TransformerController;
import dk.kb.present.util.ExtractedPreservicaValues;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.yaml.YAML;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;


/**
 * A view is at the core a list of {@link dk.kb.present.transform.DSTransformer}s.
 * It takes one value of {@code DsRecordDto} and return transformed recordContent.
 */
public class View extends ArrayList<DSTransformer> implements Function<DsRecordDto, String> {
    private static final Logger log = LoggerFactory.getLogger(View.class);

    private static final String MIME_KEY = "mime";
    private static final String TRANSFORMERS_KEY = "transformers";
    private static final String STRATEGY_KEY = "strategy";

    private final String id;
    private final String origin;
    private final MediaType mime;
    private final Strategy strategy;

    /**
     * Defines the strategy used to construct the wanted view of the resource.
     * Strategy can be one of the following: <br/>
     * {@link #NONE} <br/>
     * {@link #DR} <br/>
     * {@link #MANIFESTATION} <br/>
     */
    enum Strategy {
        /**
         * Default strategy, used when the metadata to transform does not depend on manifestation metadata.
         */
        NONE,
        /**
         * DR strategy used when DR holdback is to be applied to the metadata records. This also applies the features
         * from the MANIFESTATION strategy afterward.
         */
        DR,
        /**
         *  Manifestation strategy, used when the metadata to transform is dependent on one MANIFESTATION. This
         *  strategy is oriented towards metadata from preservica 7 and expects the metadata to conform to the preservica 7.6
         *  datamodel, where metadata delivered from OAI-PMH does contain information on manifestations in the transcoding status XML fragment.
         */
        MANIFESTATION
    }

    /**
     * Creates a view from the given YAML. Expects the YAML to contain a single entry,
     * where the key is the ID for the view and the value is the configuration of the view.
     * @param conf the configuration for this specific view.
     * @param origin the origin of the collection specified in the CONF yaml.
     */
    public View(YAML conf, String origin) {
        super();
        if (conf.size() != 1) {
            throw new IllegalArgumentException
                    ("Expected a single entry in the configuration but there was " + conf.size() +
                     ". Maybe indenting was not correct in the config file?");
        }
        id = conf.keySet().stream().findFirst().orElseThrow();
        this.origin = origin;
        conf = conf.getSubMap(id);
        String[] mimeTokens = conf.getString(MIME_KEY).split("/", 2);
        mime = new MediaType(mimeTokens[0], mimeTokens[1]);
        strategy = Strategy.valueOf(conf.getString(STRATEGY_KEY, "NONE"));
        if (conf.isEmpty()) {
            throw new IllegalArgumentException("No transformer specified for view '" + id + "'");
        }
        for (YAML transformerConf: conf.getYAMLList(TRANSFORMERS_KEY)) {
            try {
                add(TransformerController.createTransformer(transformerConf));
            } catch (Exception e) {
                throw new RuntimeException(e); // Wrap for stream use
            }
        }

        log.info("Created " + this);
    }

    public String getId() {
        return id;
    }

    public MediaType getMime() {
        return mime;
    }

    @Override
    public String apply(DsRecordDto record) {
        final Map<String, String> metadata = createBasicMetadataMap(record);
        String content = record.getData();

        switch (strategy) {
            case DR:
                applyDrStrategy(record, content, metadata);
                break;
            case MANIFESTATION:
                applyManifestationStrategy(record, content, metadata);
                break;
            case NONE:
                break;
            default:
                throw new UnsupportedOperationException("Strategy: '" + strategy + "' is not allowed. " +
                        "Allowed strategies are: '" + Arrays.toString(Strategy.values()) + "'.");
        }


        for (DSTransformer transformer: this) {
            try {
                content = transformer.apply(content, metadata);
            } catch (Exception e) {
                String message = String.format(
                        Locale.ROOT, "Exception in View '%s' while calling %s with recordID '%s' and metadata %s",
                        getId(), transformer, record.getId(), metadata);
                log.warn(message, e);
                throw new InternalServiceException(message);
            }
        }
        return content;
    }

    /**
     * Apply the operations required, when working with DR material. This method does the following:
     * <ul>
     *     <li>Extract values from preservica record. (Dates, values for holdback calculation and own-production).</li>
     *     <li>Clean start- and end-date and add them to the metadata map.</li>
     *     <li>Create metadata map entries for own production.</li>
     *     <li>Calculate holdback for the record in hand and add these values to the metadata map.</li>
     * </ul>
     * @param record to apply the strategy to.
     * @param content of the record.
     * @param metadata map containing values that are to be used in the XSLT transformation.
     */
    private void applyDrStrategy(DsRecordDto record, String content, Map<String, String> metadata) {
        ExtractedPreservicaValues extractedValues;
        try {
            extractedValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(content, record.getId());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Error extracting values from Preservica content. recordId: '{}'.", record.getId(), e);
            throw new InternalServiceException("Error extracting values from Preservica content for record:" + record.getId(), e);
        }

        String url = ServiceConfig.getConfig().getString("licensemodule.url");
        DsLicenseClient licenseClient = new DsLicenseClient(url);

        PlatformEnumDto platform = PlatformEnumDto.DRARKIV;
        RightsCalculationInputDto rightsInputDto = extractedValues.asRightsCalculationInputDto(platform, record.getOrigin());
        RightsCalculationOutputDto rightsOutput = licenseClient.calculateRights(rightsInputDto);

        extractStartAndEndDatesToMetadataMap(metadata, extractedValues);
        // The following three methods are all related to holdback and ownproduction calculations.
        updateMetadataMapWithFormAndContent(metadata, extractedValues);
        updateMetadataMapWithProductionCodeDr(metadata, extractedValues.getOrigin(), rightsOutput);
        updateMetadataMapWithDrHoldback(metadata, rightsOutput);

        if (StringUtils.isNotEmpty(extractedValues.getProductionId())){
            metadata.put("productionId", extractedValues.getProductionId());
            // Check if production ID is restricted from DR.
            log.debug("Performing productionID lookup for id: '{}' in DR restricted ID list.", extractedValues.getProductionId());
            metadata.put("productionIdRestrictedDr", String.valueOf(rightsOutput.getDr().getDrIdRestricted()));
        }

        metadata.put("dsIdRestricted", String.valueOf(rightsOutput.getDr().getDsIdRestricted()));
        metadata.put("titleRestricted", String.valueOf(rightsOutput.getDr().getTitleRestricted()));
    }

    /**
     * Apply the operations required, when working with Preservica material. This method does the following:
     * <ul>
     *     <li>Extract values from preservica record. (Dates, values for holdback calculation and own-production).</li>
     *     <li>Clean start- and end-date and add them to the metadata map.</li>
     * </ul>
     * @param record to apply the strategy to.
     * @param content of the record.
     * @param metadata map containing values that are to be used in the XSLT transformation.
     */
    private void applyManifestationStrategy(DsRecordDto record, String content, Map<String, String> metadata) {
        ExtractedPreservicaValues extractedValues;
        try {
            extractedValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(content, record.getId());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Error extracting values from Preservica content. recordId:'{}'.",record.getId(),e);
            throw new InternalServiceException("Error extracting values from Preservica content for record:" + record.getId(), e);
        }
        extractStartAndEndDatesToMetadataMap(metadata, extractedValues);
    }

    /**
     * Updates the provided metadata map with the production code information based on the given parameters.
     *
     * @param metadataMap the map to be updated with production code information.
     * @param productionCode the production code to be validated and used for updating the metadata map.
     * @param rightsOutput the {@link RightsCalculationOutputDto} containing rights information related to the production code.
     */
    private void updateMetadataMapWithProductionCodeDr(Map<String, String> metadataMap, String productionCode, RightsCalculationOutputDto rightsOutput){
        if (log.isDebugEnabled()  && origin.equals("ds.tv")) {
            if (StringUtils.isEmpty(productionCode) || productionCode.length() != 4) {
                log.debug("Nielsen/Gallup origin did not have length 4. Production code allowance will not be calculated correctly. Origin is: '{}'", productionCode);
            }
        }

        if (StringUtils.isNotEmpty(productionCode)) {
            boolean allowedProductionCode = rightsOutput.getDr().getProductionCodeAllowed();
            metadataMap.put("productionCodeAllowed", Boolean.toString(allowedProductionCode));
            metadataMap.put("productionCodeValue", productionCode);
        } else if (origin.equals("ds.radio")){
            metadataMap.put("productionCodeAllowed", "true");
        } else if (origin.equals("ds.tv")){
            log.debug("Record {} is tv record with no production code");
            metadataMap.put("productionCodeAllowed", "false");
        }
    }

    /**
     * Extract start and end date from the record and ensure that they are in a valid format.
     * @param metadataMap containing values given to the transformer creating the view.
     * @param extractedPreservicaValues containing values that have been extracted from the metadata record.
     */
    private static void extractStartAndEndDatesToMetadataMap(Map<String, String> metadataMap, ExtractedPreservicaValues extractedPreservicaValues) {
        metadataMap.put("startTime", extractedPreservicaValues.getStartTime());
        metadataMap.put("endTime", extractedPreservicaValues.getEndTime());
    }

    /**
     * Extract metadata which should always be present to a metadata map.
     * @param record to extract basic metadata from.
     */
    private Map<String,String> createBasicMetadataMap(DsRecordDto record) {

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("recordID", record.getId());
        metadata.put("origin", origin);
        metadata.put("mTime", record.getmTime().toString());
        metadata.put("mTimeHuman", getSolrDate(record.getmTimeHuman()));

        if (record.getKalturaId() != null){
            metadata.put("kalturaID", record.getKalturaId());
        }

        return metadata;
    }

    /**
     * Ensure formatting of date from ds-storage is indexable in solr and correctly formatted for schema.org representation
     * by converting from format {@code yyyy-MM-dd HH:mm:ssZ} to {@code yyyy-MM-ddTHH:mm:ssZ}.
     * @param dateTime a string representation of a dateTime in the format {@code yyyy-MM-dd HH:mm:ssZ}.
     * @return a solr and schema.org compliant string in the format {@code yyyy-MM-ddTHH:mm:ssZ}
     * converted with the {@link DateTimeFormatter#ISO_INSTANT}.
     */
    private static String getSolrDate(String dateTime) {
        // Define the formatter for the input date string. This is almost already the expected format of ISO 8601.
        // However, a small conversion needs to be make to change the space in the input pattern to a T.
        DateTimeFormatter dsStoragePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ", Locale.ROOT);
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(Objects.requireNonNull(dateTime), dsStoragePattern);
        Instant instant = offsetDateTime.toInstant();
        // Convert the datetime string to the ISO INSTANT format, which is in use in both SCHEMA.ORG and SOLR
        // https://schema.org/DateTime and https://solr.apache.org/guide/6_6/working-with-dates.html#WorkingwithDates-DateFormatting
        DateTimeFormatter isoInstantFormatter = DateTimeFormatter.ISO_INSTANT;
        return instant.atOffset(offsetDateTime.getOffset()).format(isoInstantFormatter);
    }

    /**
     * Updates the provided metadata map with holdback information from the rights output DTO.
     *
     * @param metadata the map to be updated with holdback information.
     * @param rightsOutputDto the {@link RightsCalculationOutputDto} containing holdback details.
     */
    private void updateMetadataMapWithDrHoldback(Map<String, String> metadata, RightsCalculationOutputDto rightsOutputDto){
        metadata.put("holdbackDate", rightsOutputDto.getDr().getHoldbackExpiredDate());

        String holdbackName = rightsOutputDto.getDr().getHoldbackName();

        if (holdbackName == null || holdbackName.isEmpty()){
            metadata.put("holdbackPurposeName", "");
        } else {
            metadata.put("holdbackPurposeName", rightsOutputDto.getDr().getHoldbackName());

        }
    }

    /**
     * Add form and content values used for holdback calculation to the XSLT metadata map.
     * @param metadata map to add values to.
     * @param extractedValues to retrieve form and content values from.
     */
    private void updateMetadataMapWithFormAndContent(Map<String, String> metadata, ExtractedPreservicaValues extractedValues) {
        metadata.put("holdbackFormValue", extractedValues.getFormValue());
        metadata.put("holdbackContentValue", extractedValues.getContent());
    }

    @Override
    public String toString() {
        return "View(" +
               "id='" + id + '\'' +
               ", mime=" + mime +
               ", strategy=" + strategy +
               ", origin=" + origin +
               ", transformers=" + super.toString() +
               ')';
    }

}
