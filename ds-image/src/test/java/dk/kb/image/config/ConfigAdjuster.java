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
package dk.kb.image.config;

import java.io.Closeable;
import java.io.IOException;

/**
 * Helper class for temporarily changing the application config.
 * When the test code has finished, the configuration is restored to its previous state.
 * <p>
 * Use the auto-closing try-catch mechanism around the test code using the temporary config:
 * <pre>
 * try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
 *      ...testcode...
 * }
 * </pre>
 */
public class ConfigAdjuster implements Closeable {
    private static ServiceConfig oldConfig;

    public ConfigAdjuster(String temporaryConfigSource) {
        try {
            oldConfig = ServiceConfig.getInstance();
            ServiceConfig tempConf = new ServiceConfig();
            tempConf.initialize(temporaryConfigSource);
            ServiceConfig.setInstance(tempConf);
        } catch (IOException e) {
            throw new RuntimeException("Exception creating temporary ServiceConfig", e);
        }
    }

    @Override
    public void close() {
        ServiceConfig.setInstance(oldConfig);
    }
}
