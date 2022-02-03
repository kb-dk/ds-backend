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

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.model.v1.CollectionDto;
import dk.kb.present.model.v1.ViewDto;
import dk.kb.present.webservice.exception.NotFoundServiceException;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class PresentFacade {
    private static final Logger log = LoggerFactory.getLogger(PresentFacade.class);

    private static CollectionHandler collectionHandler;

    /**
     * Optional warmUp (initialization) for fail early.
     */
    public static void warmUp() {
        getCollectionHandler();
    }

    private static CollectionHandler getCollectionHandler() {
        if (collectionHandler == null) {
            collectionHandler = new CollectionHandler(ServiceConfig.getConfig());
        }
        return collectionHandler;
    }

    // TODO: What about setting the MIME type?

    /**
     * Derived a collection from the recordID and requests the record from that, with the specified format.
     * @param recordID an ID for a record in any known collection.
     * @param format the wanted format (collection specific).
     * @return the record in the given format.
     * @throws NotFoundServiceException if the record or the format was unknown.
     */
    public static String getRecord(String recordID, String format) {
        return collectionHandler.getRecord(recordID, format);
    }

    /**
     * @param id ID for a collection.
     * @return the collection with the given ID.
     * @throws NotFoundServiceException if the collection was not known.
     */
    public static CollectionDto getCollection(String id) {
        DSCollection collection = collectionHandler.getCollection(id);
        if (collection == null) {
            throw new NotFoundServiceException("A collection with the id '" + id + "' could not be located");
        }
        return toDto(collection);
    }

    /**
     * @return all known collections.
     */
    public static List<CollectionDto> getCollections() {
        return collectionHandler.getCollections().stream()
                .map(PresentFacade::toDto)
                .collect(Collectors.toList());
    }

    // TODO: storage is not returned as that is internal information. With elevated privileges this might be added?
    private static CollectionDto toDto(DSCollection collection) {
        return new CollectionDto()
                .id(collection.getId())
                .description(collection.getDescription())
                .views(collection.getViews().values().stream()
                               .map(PresentFacade::toDto)
                               .collect(Collectors.toList()));
    }

    private static ViewDto toDto(View view) {
        return new ViewDto().id(view.getId());
    }
}
