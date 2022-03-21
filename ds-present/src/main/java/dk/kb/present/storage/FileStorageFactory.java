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

import dk.kb.util.yaml.YAML;
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

    public static final String FOLDER_KEY = "root";

    public static final String EXTENSION_KEY = "extension";
    public static final String EXTENSION_DEFAULT = ""; // All extensions

    public static final String STRIP_PREFIX_KEY = "stripprefix";
    public static final boolean STRIP_PREFIX_DEFAULT = true;

    public static final String WHITELIST_KEY = "whitelist";
    public static final String BLACKLIST_KEY = "blacklist";

    @Override
    public String getStorageType() {
        return FileStorage.TYPE;
    }

    @Override
    public Storage createStorage(String id, YAML conf, boolean isDefault) throws Exception {
        String folderStr = conf.getString(FOLDER_KEY);
        if (folderStr == null) {
            throw new NullPointerException(
                    "The root folder was not specified under the key '" + FOLDER_KEY + "' for storage '" + id + "'");
        }
        Path folder = Path.of(folderStr);
        String extension = conf.getString(EXTENSION_KEY, EXTENSION_DEFAULT);
        boolean stripPrefix = conf.getBoolean(STRIP_PREFIX_KEY, STRIP_PREFIX_DEFAULT);

        List<String> whitelistStr = conf.getList(WHITELIST_KEY, null);
        List<Pattern> whitelist = whitelistStr == null ? null :
                whitelistStr.stream().map(Pattern::compile).collect(Collectors.toList());

        List<String> blacklistStr = conf.getList(BLACKLIST_KEY, null);
        List<Pattern> blacklist = blacklistStr == null ? null :
                blacklistStr.stream().map(Pattern::compile).collect(Collectors.toList());
        

        return new FileStorage(id, folder, extension, stripPrefix, whitelist, blacklist, isDefault);
    }
}
