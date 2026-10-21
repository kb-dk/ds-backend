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
 * Configuration for the {@code folder} backend type, used by {@link dk.kb.present.storage.FileStorageFactory}.
 */
public class FolderBackendConfig implements BackendConfig {
    private final String root;
    private final String extension;
    private final boolean stripPrefix;
    private final List<String> whitelist;
    private final List<String> blacklist;

    /**
     * @param root the folder to serve files from. Mandatory.
     * @param extension file extension appended to incoming IDs, or {@code ""} for none.
     * @param stripPrefix whether the origin prefix should be stripped from incoming IDs before resolving the file.
     * @param whitelist regular expression patterns (as plain strings); if non-null, only files matching at least
     *                  one of these are served.
     * @param blacklist regular expression patterns (as plain strings); if non-null, files matching any of these
     *                  are never served.
     */
    public FolderBackendConfig(String root, String extension, boolean stripPrefix,
                                List<String> whitelist, List<String> blacklist) {
        this.root = root;
        this.extension = extension;
        this.stripPrefix = stripPrefix;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }

    @Override
    public String getType() {
        return "folder";
    }

    public String getRoot() {
        return root;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isStripPrefix() {
        return stripPrefix;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public List<String> getBlacklist() {
        return blacklist;
    }

    @Override
    public String toString() {
        return "FolderBackendConfig(root='" + root + "', extension='" + extension + "', stripPrefix=" + stripPrefix +
               ", whitelist=" + whitelist + ", blacklist=" + blacklist + ')';
    }
}
