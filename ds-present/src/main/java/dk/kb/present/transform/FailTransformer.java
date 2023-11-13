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
package dk.kb.present.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The fail transformer always fails. Used to signal unavailable views.
 */
public class FailTransformer implements DSTransformer {
    private static final Logger log = LoggerFactory.getLogger(FailTransformer.class);
    public static final String ID = "fail";

    private final String message;

    /**
     * Construct a transformer that always fails with the given message.
     * @param message the message to throw in a {@link RuntimeException} when {@link #apply(String, Map)} is called.
     */
    public FailTransformer(String message) {
        this.message = message;
        log.debug("Constructed " + this);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String apply(String s, Map<String, String> metadata) {
        throw new RuntimeException(message);
    }

    @Override
    public String toString() {
        return "FailTransformer(" +
               "message='" + message + '\'' +
               ')';
    }
}
