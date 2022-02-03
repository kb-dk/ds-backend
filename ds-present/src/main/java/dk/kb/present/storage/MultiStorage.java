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

import dk.kb.present.webservice.exception.NotFoundServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Encapsulation of multiple storages, exposed as a single storage.
 *
 * This storage is used internally by {@link StorageController} and cannot be specified in the configuration.
 */
public class MultiStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(MultiStorage.class);

    public static final String TYPE = "multi";

    public enum ORDER {sequential, parallel;

        public static ORDER getDefault() {
            return sequential;
        }
    };

    public final String id;
    public final ORDER order;
    public final Collection<Storage> subStorages;
    private final boolean isDefault;

    public MultiStorage(String id, Collection<Storage> subStorages, ORDER order, boolean isDefault) {
        this.id = id;
        this.order = order;
        this.subStorages = subStorages;
        this.isDefault = isDefault;
        log.info("Created " + this);
    }

    @Override
    public String getRecord(String id) throws IOException {
        subStorages.stream().forEach(s -> System.out.println("Available_: " + s));
        switch (order) {
            case sequential: return getRecord(subStorages.stream(), id);
            case parallel: return getRecord(subStorages.parallelStream(), id);
            default: throw new UnsupportedOperationException("The order '" + order + "' is not supported");
        }
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Iterate the storageStream and attempt to retrieve a record with the given ID.
     * @param storageStream stream of storages to query. If a parallel stream is given,
     *                      the requests will be done in parallel.
     * @param id a record ID.
     * @return the content of the record.
     * @throws IOException if the record could not be located.
     */
    private String getRecord(Stream<Storage> storageStream, String id) throws IOException {
        Optional<String> record = storageStream
                .map(subStorage -> safeGetRecord(subStorage, id))
                .filter(Objects::nonNull)
                .findFirst();
        if (record.isPresent()) {
            return record.get();
        }
        throw new IOException("Unable to locate record with id '" + id + "'");
    }

    /**
     * Retrieve the record from the storage, returning either the content or null, instead of throwing an IOException.
     * @param storage the storage to use for retrieval.
     * @param id the ID of the record.
     * @return the content for the record or null.
     */
    private String safeGetRecord(Storage storage, String id) {
        try {
            System.out.println("Requesting from "+ storage);
            return storage.getRecord(id);
        } catch (IOException|NotFoundServiceException e) {
            log.trace("Unable to retrieve record '" + id + "' from storage " + storage.getType() +
                      ". Trying another storage");
            return null;
        }
    }

    @Override
    public String toString() {
        return "MultiStorage(" +
               "id='" + id + '\'' +
               ", isDefault=" + isDefault +
               ", order=" + order +
               ", subStorages=" + subStorages +
               ')';
    }
}
