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

import dk.kb.present.transform.DSTransformer;
import dk.kb.present.transform.TransformerController;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * A view is at the core a list of {@link dk.kb.present.transform.DSTransformer}s.
 * It takes one value of {@code DsRecordDto} and return transformed recordContent.
 */
public class View extends ArrayList<DSTransformer> implements Function<DsRecordDto, String> {
    private static final Logger log = LoggerFactory.getLogger(View.class);

    private static final String MIME_KEY = "mime";
    private static final String TRANSFORMERS_KEY = "transformers";
    private static final String STRATEGY_KEY = "strategy";
    /**
     * Preservica manifestations can be of different types. Presentation manifestations are of type 2 and are the ones
     * we want to extract through this pattern.
     */
    private static final Pattern PRESENTATION_MANIFESTATION_PATTERN = Pattern.compile(
            "<ManifestationRelRef>2</ManifestationRelRef>");

    private final String id;
    private final String origin;
    private final MediaType mime;
    private final Strategy strategy;

    /**
     * Defines the strategy used to construct the wanted view of the resource.
     * Strategy can be one of the following: <br/>
     * {@link #NONE} <br/>
     * {@link #MANIFESTATION} <br/>
     */
    enum Strategy {
        /**
         * Default strategy, used when the metadata to transform does not depend on manifestation metadata.
         */
        NONE,
        /**
         * Manifestation strategy, used when the metadata to transform is dependent on one or more manifestations. This
         * strategy is oriented towards metadata from preservica and expects the metadata to conform to the preservica
         * datamodel, where data is divided between DeliverableUnits and Manifestations. The DeliverableUnit contains
         * the metadata, while a manifestation contains metadata on a single representation of the resource described in
         * the DeliverableUnit.
         * This strategy injects a manifestation into the XSLT,which is then used as part of the transformation.
         * The manifestation is needed to create streaming_urls for resources.
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
            case MANIFESTATION:
                updateMetadataMapWithPreservicaManifestation(record, metadata);
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
     * Extract metadata which should always be present to a metadata map.
     * @param record to extract basic metadata from.
     */
    private Map<String,String> createBasicMetadataMap(DsRecordDto record) {

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("recordID", record.getId());
        metadata.put("origin", origin);
        metadata.put("mTime", record.getmTime().toString());
        metadata.put("mTimeHuman", getSolrDate(record.getmTimeHuman()));
        //TODO: Update placeholder when actual value is in place
        metadata.put("conditionsOfAccess", "TODO: placeholderCondition");
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
     * Update the map of metadata with manifestation record from the deliverable unit contained in the input record.
     * @param record with an expanded tree containing manifestation childs.
     * @param metadata map that values from the record is extracted to.
     */
    private void updateMetadataMapWithPreservicaManifestation(DsRecordDto record, Map<String, String> metadata) {
        String child = getFirstPresentationManifestation(record);
        if (!child.isEmpty()){
            metadata.put("manifestation", child);
        }
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

    /**
     * If record has children, the first presentation manifestation is returned.
     * If record have not got children an empty string is returned.
     * @param record to extract the newest presentation manifestation from.
     * @return the data from the first presentation manifestation related to the input record.
     */
    private String getFirstPresentationManifestation(DsRecordDto record) {
        // TODO: Figure how to choose correct manifestation for record, if more than one is present
        // Return first child record of manifestation type = 2. If there are multiple presentation manifestations,
        // the rest are currently not added to the transformation
        List<String> presentationManifestations = record.getChildren() == null ? Collections.singletonList("") :
                record.getChildren().stream()
                        .map(this::getNonNullChild)
                        //.filter(this::isPresentationManifestation) // Filter not needed for Preservica 7
                        .collect(Collectors.toList());

        return returnPresentationManifestationFromList(presentationManifestations, record.getId());
    }

    /**
     * Determine if the input preservica manifestation is a presentation manifestation by looking for the XML tag
     * {@code ManifestationRelRef} with a value of 2. This filter should only be used for legacy Preservica 5
     * @param preservicaManifestation content from a ds-storage record, which is a child of the current DeliverableUnit
     *                                being processed.
     * @return true if the given manifestation is a presentation manifestation, otherwise return false.
     */
    private boolean isPresentationManifestation(String preservicaManifestation) {
        Matcher m = PRESENTATION_MANIFESTATION_PATTERN.matcher(preservicaManifestation);
        return m.find();
    }

    /**
     * Returns the first presentation manifestation for a record. If there are more than one presentation manifestation
     * for the record a warning will be logged.
     * @param presentationManifestations list of all presentation manifestations for a record
     * @return the first presentation manifestation for a record.
     */
    private String returnPresentationManifestationFromList(List<String> presentationManifestations, String recordId) {
        if (presentationManifestations == null || presentationManifestations.isEmpty()){
            log.warn("No presentation manifestations were delivered from DS-storage as children for record: '{}'", recordId);
            return "";
        }

        if (presentationManifestations.size() > 1) {
            log.warn("Multiple presentation manifestations were present for record with id: '{}'. " +
                    "Only the first has been returned", recordId);
            return presentationManifestations.get(0);
        }

        return presentationManifestations.get(0);
    }

    /**
     * Check for the data of the child being null.
     * @param child record to check for data in.
     * @return the data from child if child is not null. Otherwise, return an empty string.
     */
    private String getNonNullChild(DsRecordDto child) {
        if (child.getData() == null){
            return "";
        }
        return child.getData();
    }
}
