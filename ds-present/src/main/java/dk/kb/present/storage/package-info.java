/**
 * The storage package is used for providing access to a backing storage, capable of delivering records.
 * <br>
 * The storage package uses a ServiceLoader to discover and control storages:
 * New storages are added by creating an {@link dk.kb.present.storage.Storage} with a corresponding
 * {@link dk.kb.present.storage.StorageFactory} and registering the factory in the file
 * {@code src/main/resources/META-INF/services/dk.kb.present.storage.StorageFactory}.
 * <br>
 * Storage instances are created by {@link dk.kb.present.storage.StorageController}.
 */
package dk.kb.present.storage;