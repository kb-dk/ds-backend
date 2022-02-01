package dk.kb.present.transform;

import org.apache.commons.collections4.FluentIterable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
class TransformerControllerTest {

    @Test
    void testDiscovery() {
        List<String> EXPECTED_IDS = Arrays.asList(IdentityTransformer.ID, XSLTTransformer.ID);
        Set<String> supported = TransformerController.getSupportedTransformerIDs();

        for (String transformerID : EXPECTED_IDS) {
            assertTrue(supported.contains(transformerID),
                       "The Transformercontroller should discover the " + transformerID + " transformer factory. " +
                       "Supported transformers: " + supported);
        }
    }
}