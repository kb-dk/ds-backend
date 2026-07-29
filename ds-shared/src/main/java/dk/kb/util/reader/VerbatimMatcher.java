/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.kb.util.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For bulk matching of many static verbatim keys against many String sources.
 * Counts matches and performs a callback for each match.
 */
public abstract class VerbatimMatcher<P> {
    private Node tree = new Node();
    private long matchCount = 0;
    private int lastMatchLength = -1;

    public enum MATCH_MODE {all, shortest, longest};
    public final MATCH_MODE DEFAULT_MATCH_MODE = MATCH_MODE.all;

    private MATCH_MODE matchMode = DEFAULT_MATCH_MODE;
    private boolean skipMatching = false;

    /**
     * If not 0, this character must be present just before matches, or the match must be at position 0 of the input.
     * Not used with {@link #findMatches(String, Pattern)}.
     */
    private char leading = 0;

    /**
     * If not 0, this character must be present immediately after matches, or the match must be at the very end
     * of the input.
     */
    private char following = 0;

    /**
     * This will be called for each match.
     * @param match the matching verbatim.
     * @param payload optional payload for the matching Node. Might be null.
     */
    public abstract void callback(String match, P payload);
    private void indirectCallback(String match, P payload) {
        matchCount++;
        lastMatchLength = match.length();
        callback(match, payload);
    };

    /**
     * Find matches in the given source and call {@link #callback(String, Object)} for each match.
     * @param source a String which will be searched for verbatims.
     * @return the number of matches.
     */
    public int findMatches(String source) {
        return findMatches(source, matchMode, skipMatching);
    }

    /**
     * Find matches in the given source, using the given mode to handle multiple matches.
     * @param source a String which will be searched for verbatims.
     * @param mode   how to handle multiple matches.
     * @param skipMatching if true, the sliding window matcher is moved to the position immediately after the last
     *                     match, when a match is made. If false, it is moved a single character after each match
     *                     attempt. Normally used with {@link MATCH_MODE#shortest} or {@link MATCH_MODE#longest} as
     *                     those ensures a single match.
     * @return the number of matches.
     */
    public int findMatches(String source, MATCH_MODE mode, boolean skipMatching) {
        return findMatches(source, mode, skipMatching, leading, following);
    }

    /**
     * Find matches in the given source, using the given mode to handle multiple matches.
     * @param source a String which will be searched for verbatims.
     * @param mode   how to handle multiple matches.
     * @param skipMatching if true, the sliding window matcher is moved to the position immediately after the last
     *                     match, when a match is made. If false, it is moved a single character after each match
     *                     attempt. Normally used with {@link MATCH_MODE#shortest} or {@link MATCH_MODE#longest} as
     *                     those ensures a single match.
     * @param leading If not 0, this character must be present just before matches,
     *                or the match must be at position 0 of the input.
     * @param following If not 0, this character must be present immediately after matches,
     *                  or the match must be at the very end of the input.
     * @return the number of matches.
     */
    public int findMatches(String source, MATCH_MODE mode, boolean skipMatching, final char leading, char following) {
        int matches = 0;
        int lastMatches = matches;
        int i = 0;
        while (i < source.length()) {
            // char before current pos must be == leading
            if (i != 0 && leading != 0 && source.charAt(i-1) != leading) {
                i++;
                continue;
            }
            matches += tree.findMatches(source, i, mode, following);
            i += (skipMatching && matches != lastMatches) ? lastMatchLength : 1;
            lastMatches = matches;
        }
        return matches;
    }

    public int findMatches(String source, Pattern delimiter) {
        int matches = 0;
        Matcher delimitMatcher = delimiter.matcher(source);
        boolean first = true;
        while (delimitMatcher.find()) {
            if (first && !(delimitMatcher.start() == 0)) {
                matches += tree.findMatches(source, 0, matchMode);
            }
            first = false;
            matches += tree.findMatches(source, delimitMatcher.end(), matchMode);
        }
        return matches;
    }

    public void addRule(String verbatim) {
        tree.addRule(verbatim);
    }

    public void addRule(String verbatim, P payload) {
        tree.addRule(verbatim, payload);
    }

    public void addRules(String... verbatims) {
        for (String verbatim: verbatims) {
            tree.addRule(verbatim);
        }
    }

    public void setMatchMode(MATCH_MODE matchMode) {
        this.matchMode = matchMode;
    }

    public void setSkipMatching(boolean skipMatching) {
        this.skipMatching = skipMatching;
    }

    public Node getNode(String verbatim) {
        return getNode(verbatim, false);
    }
    public Node getNode(String verbatim, boolean autoCreate) {
        Node node = tree.getNode(verbatim, -1);
        if (node != null || !autoCreate) {
            return node;
        }
        addRule(verbatim);
        node = tree.getNode(verbatim);
        if (node == null) {
            throw new IllegalStateException(
                    "Logic error: Just added node for '" + verbatim + "' but could not extract it");
        }
        return node;
    }

    public long getMatchCount() {
        return matchCount;
    }

    public char getLeading() {
        return leading;
    }

    public void setLeading(char leading) {
        this.leading = leading;
    }

    public char getFollowing() {
        return following;
    }

    public void setFollowing(char following) {
        this.following = following;
    }

    /**
     * Single char per node tree.
     */
    public class Node {
        private final char c;
        private boolean endpoint = false;
        private List<Node> children = new ArrayList<Node>();
        private P payload = null;

        /**
         * Constructor for the super-Node.
         */
        public Node() {
            c = 0;
        }

        public Node(String s, final int index, P payload) {
            c = s.charAt(index);
            if (index+1 == s.length()) {
                endpoint = true;
                this.payload = payload;
            } else {
                children.add(new Node(s, index+1, payload));
            }
        }

        public void addRule(String key) {
            addRule(key, null);
        }
        public void addRule(String key, P payload) {
            addChild(key, -1, payload);
        }
        private void addChild(String s, final int index, P payload) {
            // End reached, mark as end point
            if (index+1 == s.length()) {
                endpoint = true;
                this.payload = payload;
                return;
            }

            {   // Check for existing matching children
                Node child = getChild(s.charAt(index + 1));
                if (child != null) {
                    child.addChild(s, index + 1, payload);
                    return;
                }
            }

            // Add new child
            children.add(new Node(s, index+1, payload));
        }

        public Node getNode(String verbatim) {
            return getNode(verbatim, -1);
        }

        private Node getNode(String s, final int index) {
            if (index+1 == s.length()) {
                return endpoint ? this : null;
            }

            Node child = getChild(s.charAt(index + 1));
            return child == null ? null : child.getNode(s, index+1);
        }

        public int findMatches(CharSequence buffer) {
            return findMatches(buffer, 0, matchMode);
        }

        public int findMatches(CharSequence buffer, final int start, MATCH_MODE matchMode) {
            return findMatches(buffer, start, matchMode, following);
        }
        public int findMatches(CharSequence buffer, final int start, MATCH_MODE matchMode, char following) {
            MatchCallback mc;
            switch (matchMode) {
                case all:
                    mc = new MatchCallback();
                    break;
                case shortest:
                case longest:
                    mc = new MatchCallbackSorted(matchMode == MATCH_MODE.shortest);
                    break;
                default: throw new UnsupportedOperationException("The MATCH_MODE " + matchMode + " is unsupported");
            }
            findAll(buffer, start, start-1, mc, following);
            mc.close();
            return mc.matchCount;
        }

        private void findAll(
                CharSequence buffer, final int start, final int index, MatchCallback matchCallback, char following) {
            if (endpoint) {
                if (following == 0 || index == buffer.length()-1 || buffer.charAt(index+1) == following) {
                    matchCallback.callback(buffer.subSequence(start, index + 1).toString(), payload);
                }
            }

            if (index+1 >= buffer.length()) {
                return;
            }

            Node child = getChild(buffer.charAt(index+1));
            if (child != null) {
                child.findAll(buffer, start, index + 1, matchCallback, following);
            }
        }

        public char getC() {
            return c;
        }

        public boolean isEndpoint() {
            return endpoint;
        }

        public P getPayload() {
            return payload;
        }

        public void setPayload(P payload) {
            this.payload = payload;
        }

        private Node getChild(char c) {
            for (Node child : children) {
                if (child.c == c) {
                    return child;
                }
            }
            return null;
        }

        public class MatchCallback {
            public int matchCount = 0;
            public void callback(String match, P payload) {
                VerbatimMatcher.this.indirectCallback(match, payload);
                matchCount++;
            }
            public void close() {
                // Default is no-op
            }
        }

        public class MatchCallbackSorted extends MatchCallback {
            private final boolean shortest;
            public String lastMatch = null;
            public P lastPayload = null;

            public MatchCallbackSorted(boolean shortest) {
                this.shortest = shortest;
            }

            @Override
            public void callback(String match, P payload) {
                if (lastMatch == null ||
                    (shortest && match.length() < lastMatch.length()) ||
                    (!shortest && match.length() > lastMatch.length())) {
                    lastMatch = match;
                    lastPayload = payload;
                    matchCount = 1;
                }
            }
            @Override
            public void close() {
                if (lastMatch != null) {
                    VerbatimMatcher.this.indirectCallback(lastMatch, lastPayload);
                }
            }
        }
    }
}
