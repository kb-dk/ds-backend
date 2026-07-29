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
package dk.kb.util.string;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sub-strings matching a given regular expression are delivered to a callback that returns the replacement string.
 */
// Implementation note: The replacer takes String as an input. It would be highly usable to switch to streaming
// based replacement, taking a Reader og InputStream as input, but this does not work with Java's build in Pattern +
// Matcher. Maybe because it is quite hard to make a streaming regexp engine?
// It would be possible to take a CharSequence (e.g. StringBuffer is a CharSequence) as input, but that optimization
// seems premature as there are no known calls for it.
public class CallbackReplacer implements Function<String, String> {
    private final Pattern pattern;
    private final Function<String, String> callback;
    /**
     * If true and {@link #pattern} has 1 capturing group, the content of the capturing group will be used as input
     * for the {@link #callback} but the full capture will be replaced with the result.
     * <p>
     * If false and {@link #pattern} has 1 capturing group, the content of the capturing group will be used as input
     * for the {@link #callback} and only content of the capturing group will be replaced.
     * <p>
     * If {@link #pattern} has no capturing group, {@code replaceFull} is ignored.
     * <p>
     * Default is false.
     */
    private final boolean replaceFull;

    /**
     * @param pattern the pattern to look for.
     *                If the pattern contains no groups, the full match is passed to the callback.
     *                If it contains a single capturing group, the content of that group is passed to the callback.
     *                More than 1 group is not supported.
     * @param callback optionally adjusts the part matching the pattern. Returning null is the same as the empty String.
     */
    public CallbackReplacer(String pattern, Function<String, String> callback) {
        this(Pattern.compile(pattern), callback);
    }

    /**
     * @param pattern the pattern to look for.
     *                If the pattern contains no groups, the full match is passed to the callback.
     *                If it contains a single capturing group, the content of that group is passed to the callback.
     *                More than 1 group is not supported.
     * @param callback optionally adjusts the part matching the pattern. Returning null is the same as the empty String.
     */
    public CallbackReplacer(Pattern pattern, Function<String, String> callback) {
        this(pattern, callback, false);
    }

    /**
     * @param pattern the pattern to look for.
     *                If the pattern contains no groups, the full match is passed to the callback.
     *                f it contains a single capturing group, the content of that group is passed to the callback.
     *                More than 1 group is not supported.
     * @param callback optionally adjusts the part matching the pattern. Returning null is the same as the empty String.
     * @param replaceFull if true and {@code pattern} has 1 capturing group, the content of the capturing group will be
     *                    used as input for the {@code callback} but the full capture will be replaced with the result.
     *                    <p>
     *                    If false and {@code #attern} has 1 capturing group, the content of the capturing group will
     *                    be used as input for the {@code callback} and only content of the capturing group will be
     *                    replaced.
     *                    <p>
     *                    If {@code pattern} has no capturing group, {@code replaceFull} is ignored.
     */
    public CallbackReplacer(Pattern pattern, Function<String, String> callback, boolean replaceFull) {
        this.pattern = pattern;
        this.callback = callback;
        this.replaceFull = replaceFull;
        final int groups = pattern.matcher("").groupCount();
        if (groups > 1) {
            throw new UnsupportedOperationException(String.format(
                    Locale.ROOT,  "The Pattern '%s' has %d capturing groups, while only 0 or 1 group is supported",
                    pattern.pattern(), groups));
        }
    }

    /**
     * Replaces {@link #pattern} matches in s with the result of calling {@link #callback} with the match.
     * @param s an input string for replacements.
     * @return thre result of replacing all matches in s.
     */
    @Override
    public String apply(String s) {
        Writer out = new StringWriter();
        try {
            apply(s, out);
        } catch (Exception e) {
            throw new RuntimeException("Exception during replacement", e);
        }                       
        return out.toString();
    }

    /**
     * Replaces {@link #pattern} matches in s with the result of calling {@link #callback} with the match.
     * @param in an input string for replacements.
     * @param out the result is written here. {@link Writer#flush()} and {@link Writer#flush()} are not called.
     */
    public void apply(String in, Writer out) throws IOException {
        Matcher matcher = pattern.matcher(in);
        int begin = 0;
        while (matcher.find()) {
            out.write(in.substring(begin, matcher.start())); // Copy up to the beginning of the match

            String replacement;
            // If there is a group in the Pattern, use the content of that one, else use the full match
            if (matcher.groupCount() == 0) { // No group
                replacement = callback.apply(matcher.group());
                out.write(replacement == null ? "" : replacement);
            } else if (matcher.groupCount() == 1) {
                replacement = callback.apply(matcher.group(1));
                if (replaceFull) { // Replace the full capture
                    out.write(replacement == null ? "" : replacement);
                } else { // Replace only the group capture
                    out.write(in.substring(matcher.start(), matcher.start(1)));
                    out.write(replacement == null ? "" : replacement);
                    out.write(in.substring(matcher.end(1), matcher.end()));
                }
            } else {
                throw new IllegalStateException(
                        "More that 1 capturing group is not supported. Pattern: '" + pattern.pattern() + "'");
            }
            begin = matcher.end();
        }
        out.write(in.substring(begin)); // Copy from the end of last match to the end of the input
    }

    @Override
    public String toString() {
        return "CallbackReplacer(pattern='" + pattern.pattern() + ", replaceFull=" + replaceFull + "')";
    }
}
