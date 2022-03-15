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
package dk.kb.present.storage;

import dk.kb.present.backend.model.v1.DsRecordDto;
import dk.kb.util.yaml.YAML;

import java.io.IOException;
import java.util.function.Function;

/**
 * Provides access to records.
 */
public interface Storage {

    /**
     * @return the ID for the storage, as defined in the configuration, e.g. {@code imagecollection}.
     */
    String getID();

    /**
     * @return the type of storage, e.g. {@code ds-storage}.
     */
    String getType();

    /**
     * @return true if this storage is stated as being default, else false.
     */
    boolean isDefault();

    /**
     * @param id the ID for a record.
     * @return the record with the given ID, if available.
     * @throws IOException if the record could not be retrieved.
     */
    String getRecord(String id) throws IOException;

    /**
     * Return the record as a ds-storage record. This is "best effort", as some element such as
     * {@link DsRecordDto#getcTime()} might not be available.
     * @param id the ID for a record.
     * @return the record with the given ID, if available.
     * @throws IOException if the record could not be retrieved.
     */
    DsRecordDto getDSRecord(String id) throws IOException;

}
