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

import dk.kb.present.config.BackendConfig;
import dk.kb.present.config.FolderBackendConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Constructs {@link FileStorage}s.
 */
public class FileStorageFactory implements StorageFactory {
    private static final Logger log = LoggerFactory.getLogger(FileStorageFactory.class);

    @Override
    public String getStorageType() {
        return FileStorage.TYPE;
    }

    @Override
    public Storage createStorage(String id, BackendConfig conf, boolean isDefault) throws Exception {
        FolderBackendConfig c = (FolderBackendConfig) conf;
        if (c.getRoot() == null) {
            throw new NullPointerException("The root folder was not specified for storage '" + id + "'");
        }
        Path folder = Path.of(c.getRoot());

        List<Pattern> whitelist = c.getWhitelist() == null ? null :
                c.getWhitelist().stream().map(Pattern::compile).collect(Collectors.toList());
        List<Pattern> blacklist = c.getBlacklist() == null ? null :
                c.getBlacklist().stream().map(Pattern::compile).collect(Collectors.toList());

        return new FileStorage(id, folder, c.getExtension(), c.isStripPrefix(), whitelist, blacklist, isDefault);
    }
}
