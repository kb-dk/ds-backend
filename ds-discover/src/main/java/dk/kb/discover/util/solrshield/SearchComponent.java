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
package dk.kb.discover.util.solrshield;

import dk.kb.util.yaml.YAML;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Representation of a Solr Search component.
 */
public class SearchComponent extends Component {

    public SearchComponent(YAML config) {
        super(config);

    }

    @Override
    public SearchComponent deepCopy() {
        SearchComponent clone;
        try {
            clone = (SearchComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(
                    "Got CloneNotSupportedException with super class Object. This should not happen", e);
        }
        deepCopyNonAtomicAttributes(clone);
        return clone;
    }


}
