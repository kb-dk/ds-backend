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
package dk.kb.present.config;

import java.util.List;

/**
 * Configuration for a single entry under {@code origins}, consumed by {@link dk.kb.present.DSOrigin} and
 * {@link dk.kb.present.OriginHandler}.
 */
public class OriginConfig {
    private final String id;
    private final String prefix;
    private final String origin;
    private final String description;
    private final String recordRequestType;
    private final String storage;
    private final List<ViewConfig> views;

    /**
     * @param id the origin ID, primarily used for debugging and configuration.
     * @param prefix the prefix recordIDs for this origin must start with.
     * @param origin the ds-storage origin backing this ds-present origin.
     * @param description human-readable description, or {@code null}.
     * @param recordRequestType one of {@code dk.kb.storage.model.v1.RecordTypeDto}, as a raw (not-yet-parsed)
     *                          string.
     * @param storage the ID of the storage to use, or {@code null} for the default storage.
     * @param views the views (formats) available for this origin.
     */
    public OriginConfig(String id, String prefix, String origin, String description, String recordRequestType,
                         String storage, List<ViewConfig> views) {
        this.id = id;
        this.prefix = prefix;
        this.origin = origin;
        this.description = description;
        this.recordRequestType = recordRequestType;
        this.storage = storage;
        this.views = views;
    }

    public String getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDescription() {
        return description;
    }

    public String getRecordRequestType() {
        return recordRequestType;
    }

    public String getStorage() {
        return storage;
    }

    public List<ViewConfig> getViews() {
        return views;
    }

    @Override
    public String toString() {
        return "OriginConfig(id='" + id + "', prefix='" + prefix + "', origin='" + origin + "', description='" +
               description + "', recordRequestType='" + recordRequestType + "', storage='" + storage +
               "', views=" + views + ')';
    }
}
