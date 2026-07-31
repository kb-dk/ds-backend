/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The SB Util Library.
 * Copyright (C) 2005-2007  The State and University Library of Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.kb.util.reader;


import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for creating Text-oriented replacers. A replacer will be selected
 * and instantiated based on the given rules.
 *
 * It is highly recommended to use this factory instead of directly creating
 * replacers, as it selects the optimal replacer based on the rules.
 *
 * @see CharArrayReplacer
 * @see CharReplacer
 * @see StringReplacer
 * @see ReplaceReader
 */
public class ReplaceFactory {

    private ReplaceReader replacer;

    /**
     * Creates a factory that generates a replacer for the given rules.
     * Changes to the rules between calls to {@link #getReplacer()} are not
     * guaranteed to take effect: Create a new Factory instead.
     *
     * After a factory for a given set of rules is created, getting a replacer
     * for the rules is very cheap as internal mappings are shared between the
     * replacer instances.
     *
     * @param rules what to replace in the form of target-replacement pairs.
     */
    public ReplaceFactory(Map<String, String> rules) {
        replacer = getReplacer(rules);
    }

    /**
     * Creates a new replacer which uses the rules given in the constructor.
     * This is fast and with little memory-overhead. This is a recommended
     * method for getting distinct replacers for the same rules.
     *
     * @return a replacer based on the rules given in the constructor.
     */
    public ReplaceReader getReplacer() {
        return (ReplaceReader) replacer.clone();
    }

    /**
     * Creates a new replacer which uses the rules given in the constructor.
     * This is fast and with little memory-overhead. This is a recommended
     * method for getting distinct replacers for the same rules.
     *
     * @param in the input character stream in which to replace substrings
     * @return a replacer based on the rules given in the constructor.
     */
    public ReplaceReader getReplacer(Reader in) {
        ReplaceReader reader = getReplacer();
        reader.setSource(in);
        return reader;
    }

    /**
     * Creates a replacer from the given rules reading character data from
     * {@code in}.
     *
     * Note that you can reuse the replace reader with the same rules by calling
     * {@link ReplaceReader#setSource(java.io.Reader)}.
     *
     * The factory will detect
     * the optimal strategy for character replacement based on {@code rules}
     * and return a {@link CharArrayReplacer}, {@link CharReplacer}, or
     * {@link StringReplacer} accordingly.
     *
     * @param in    the input character stream in which to replace substrings
     * @param rules what to replace in the form of target-replacement pairs.
     * @return a replacer made from the rules.
     */
    public static ReplaceReader getReplacer(Reader in,
                                            Map<String, String> rules) {
        boolean allTargetsSingleChars = true;
        boolean allReplacementsSingleChars = true;
        for (Map.Entry<String, String> entry : rules.entrySet()) {
            allTargetsSingleChars &= entry.getKey().length() == 1;
            allReplacementsSingleChars &= entry.getValue().length() == 1;
        }
        if (allTargetsSingleChars && allReplacementsSingleChars) {
            return new CharReplacer(in, rules);
        }
        if (allTargetsSingleChars) {
            return new CharArrayReplacer(in, rules);
        }
        return new StringReplacer(in, rules);
    }

    /**
     * Creates a replacer from the given rules with an empty input stream.
     * You will need to set the data source of the returned reader by calling
     * {@link ReplaceReader#setSource(java.io.Reader)}.
     *
     * Note that you can reuse the replace reader with the same rules by calling
     * {@link ReplaceReader#setSource(java.io.Reader)}.
     *
     * The factory will detect
     * the optimal strategy for character replacement based on {@code rules}
     * and return a {@link CharArrayReplacer}, {@link CharReplacer}, or
     * {@link StringReplacer} accordingly.
     *
     * @param rules what to replace in the form of target-replacement pairs.
     * @return a replacer made from the rules.
     */
    public static ReplaceReader getReplacer(Map<String, String> rules) {
        return getReplacer(new StringReader(""), rules);
    }

    /**
     * Get a new {@code ReplaceReader} on {@code in} by dynamically building
     * a rule map where {@code rules[i]} maps to {@code rules[++i]}.
     *
     * You can resue the returned reader by calling
     * {@link ReplaceReader#setSource}
     *
     * @param in    the input character stream in which to replace substrings
     * @param rules what to replace in the form of target-replacement pairs.
     * @return a replacer made from the rules
     * @throws IllegalArgumentException if passed an uneven number of arguments
     */
    public static ReplaceReader getReplacer(Reader in, String... rules) {
        if (rules.length % 2 != 0) {
            throw new IllegalArgumentException("Uneven number of arguments");
        }

        Map<String, String> ruleMap = new HashMap<String, String>(rules.length,
                                                                  1F);

        for (int i = 0; i < rules.length - 1; i++) {
            ruleMap.put(rules[i], rules[++i]);
        }

        return getReplacer(in, ruleMap);
    }

    /**
     * Get a new {@code ReplaceReader} with an empty input stream. The
     * replacement rules are
     * dynamically build by mapping {@code rules[i]} to {@code rules[++i]}.
     *
     * Before reading from the returned reader you must
     * call {@link ReplaceReader#setSource} on it to set the underlying stream
     * to replace substrings from.
     *
     * You can reuse the returned reader by calling
     * {@link ReplaceReader#setSource}.
     *
     * @param rules what to replace in the form of target-replacement pairs.
     * @return a replacer made from the rules
     * @throws IllegalArgumentException if passed an uneven number of arguments
     */
    public static ReplaceReader getReplacer(String... rules) {
        return getReplacer(new StringReader(""), rules);
    }
}
