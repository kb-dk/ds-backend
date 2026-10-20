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

import io.smallrye.config.SmallRyeConfig;

import java.io.Closeable;
import java.io.IOException;

/**
 * Helper class for temporarily changing the application config.
 * When the test code has finished, the configuration is restored to its previous state.
 * Use the auto-closing try-catch mechanism around the test code using the temporary config:
 * <pre>
 * try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
 *      ...testcode...
 * }
 * </pre>
 * <p>
 * Previously (under kb-util {@code YAML}/{@code AutoYAML}), this worked by swapping whole {@code ServiceConfig}
 * instances via {@code ServiceConfig.getInstance()}/{@code setInstance(...)}. Since {@code ServiceConfig} is now
 * a purely static singleton backed by SmallRye Config (matching every other ds-backend module), this instead
 * captures and restores the single backing {@link SmallRyeConfig} object directly, via the package-private
 * {@code ServiceConfig.getRawConfig()}/{@code setRawConfig(...)} escape hatch.
 */
public class ConfigAdjuster implements Closeable {
    private static SmallRyeConfig oldConfig;

    public ConfigAdjuster(String temporaryConfigSource) {
        try {
            oldConfig = ServiceConfig.getRawConfig();
            ServiceConfig.initialize(temporaryConfigSource);
        } catch (IOException e) {
            throw new RuntimeException("Exception creating temporary ServiceConfig", e);
        }
    }

    @Override
    public void close() {
        ServiceConfig.setRawConfig(oldConfig);
    }
}
