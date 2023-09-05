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


import java.util.stream.Stream;

import dk.kb.storage.model.v1.DsRecordDto;

/**
 * Provides access to records.
 *
 * Note that no methods uses checked Exceptions. Implementations are aimed towards web services and should throw
 * appropriate {@link dk.kb.util.webservice.exception.ServiceException}s instead.
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
     */
    String getRecord(String id);

    /**
     * Return the record as a ds-storage record. This is "best effort", as some element such as
     * {@link DsRecordDto#getcTime()} might not be available.
     * @param id the ID for a record.
     * @return the record with the given ID, if available.
     */
    DsRecordDto getDSRecord(String id);

    /**
     * Return records in mTime order, where all record.mTimes are > the given mTime.
     * @param origin optional (can be null) origin.
     * @param mTime point in time (epoch * 1000) for the records to deliver, exclusive.
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @return a stream of records after the given mTime.
     */
    Stream<DsRecordDto> getDSRecords(String origin, long mTime, long maxRecords);

}
