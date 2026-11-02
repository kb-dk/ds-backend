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
 * Configuration for the {@code replace} transformer type, used by {@link dk.kb.present.transform.ReplaceFactory}.
 */
public class ReplaceConfig implements TransformerConfig {
    private final String regexp;
    private final String replacement;
    private final boolean replaceAll;

    public ReplaceConfig(String regexp, String replacement, boolean replaceAll) {
        this.regexp = regexp;
        this.replacement = replacement;
        this.replaceAll = replaceAll;
    }

    @Override
    public String getType() {
        return "replace";
    }

    public String getRegexp() {
        return regexp;
    }

    public String getReplacement() {
        return replacement;
    }

    public boolean isReplaceAll() {
        return replaceAll;
    }

    @Override
    public String toString() {
        return "ReplaceConfig(regexp='" + regexp + "', replacement='" + replacement + "', replaceAll=" + replaceAll + ')';
    }
}
