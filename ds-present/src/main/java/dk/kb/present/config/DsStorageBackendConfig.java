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

/**
 * Configuration for the {@code ds-storage} backend type, used by {@link dk.kb.present.storage.DSStorageFactory}.
 */
public class DsStorageBackendConfig implements BackendConfig {
    private final String origin;
    private final String url;
    private final int batchCount;

    /**
     * @param origin the ds-storage origin to request records for, or {@code null} (calls to retrieve records will
     *               then fail, but the storage can still be constructed - matches pre-existing lenient behaviour).
     * @param url the URL of the backing ds-storage instance. Mandatory.
     * @param batchCount number of records to request per batch call to ds-storage.
     */
    public DsStorageBackendConfig(String origin, String url, int batchCount) {
        this.origin = origin;
        this.url = url;
        this.batchCount = batchCount;
    }

    @Override
    public String getType() {
        return "ds-storage";
    }

    public String getOrigin() {
        return origin;
    }

    public String getUrl() {
        return url;
    }

    public int getBatchCount() {
        return batchCount;
    }

    @Override
    public String toString() {
        return "DsStorageBackendConfig(origin='" + origin + "', url='" + url + "', batchCount=" + batchCount + ')';
    }
}
