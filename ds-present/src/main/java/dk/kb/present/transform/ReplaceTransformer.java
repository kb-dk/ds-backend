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

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Perform regexp-based search & replace on the given content.
 */
public class ReplaceTransformer extends DSTransformer {
    private static final Logger log = LoggerFactory.getLogger(ReplaceTransformer.class);
    public static final String ID = "replace";

    private final Pattern pattern;
    private final String replacement;
    private final boolean replaceAll;

    /**
     * Construct a transformer that performs rexexp-based replacement on input.
     * <p>
     * The replacer uses {@link Pattern} with {@link Pattern#DOTALL} enabled for multiline support.
     * <p>
     * Sample usage: {@code regexp="id=\"([0-9]+)-([a-z]+)\"", replacement="id=\"$2-$1\""} will change the input
     * {@code <mystructure id="123-foo">...} to {@code <mystructure id="foo-123">...}.
     * @param regexp the regular expression to match. See {@link Pattern} for syntax.
     * @param replacement the replacement. See {@link Matcher#replaceAll(String)} for syntax.
     * @param replaceAll if true, all matches will be replaced. If false, only first match will be replaced.
     */
    public ReplaceTransformer(String regexp, String replacement, boolean replaceAll) {
        this.pattern = Pattern.compile(regexp, Pattern.DOTALL);
        this.replacement = replacement;
        this.replaceAll = replaceAll;

        log.debug("Constructed " + this);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getStylesheet() {
        return null;
    }

    @Override
    public String apply(String s, Map<String, String> metadata) {
        Matcher m = pattern.matcher(s);
        return replaceAll ? m.replaceAll(replacement) : m.replaceFirst(replacement);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "ReplaceTransformer(regexp='%s', replacement='%s', replaceAll=%b)",
                pattern.pattern(), replacement, replaceAll);
    }

}
