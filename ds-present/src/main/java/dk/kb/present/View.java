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
     * {@link #MANIFESTATION5} <br/>
     */
    enum Strategy {
        /**
         * Default strategy, used when the metadata to transform does not depend on manifestation metadata.
         */
        NONE,
        /**
         * Manifestation strategy, used when the metadata to transform is dependent on one or more manifestations. This
         * strategy is oriented towards metadata from preservica 5 and expects the metadata to conform to the preservica 5
         * datamodel, where data is divided between DeliverableUnits and Manifestations. The DeliverableUnit contains
         * the metadata, while a manifestation contains metadata on a single representation of the resource described in
         * the DeliverableUnit.
         * This strategy injects a manifestation into the XSLT, which is then used as part of the transformation.
         * The manifestation is needed to create streaming_urls for resources.
         */
        MANIFESTATION5,
        /**
         *  Manifestation strategy, used when the metadata to transform is dependent on one MANIFESTATION. This
         *  strategy is oriented towards metadata from preservica 7 and expects the metadata to conform to the preservica 7
         *  datamodel, where metadata delivered from OAI-PMH doesn't contain information on manifestations.
         *  Manifestations for records have been extracted from the Preservica 7 REST API and are presented as
         *  {@code referenceId}s in {@link DsRecordDto}s.
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
                updateMetadataMapWithPreservica7Manifestation(record, metadata);
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
     * Update the map of metadata with manifestation from the input {@link DsRecordDto}s referenceId.
     * @param record with a referenceId.
     * @param metadata map that values from the record is extracted to.
     */
    private void updateMetadataMapWithPreservica7Manifestation(DsRecordDto record, Map<String, String> metadata) {
        String manifestationName = record.getReferenceId();
        if (!(manifestationName == null) && !manifestationName.isEmpty()){
            metadata.put("manifestation", manifestationName);
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

}
