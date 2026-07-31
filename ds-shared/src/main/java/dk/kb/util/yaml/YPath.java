package dk.kb.util.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YPath helper for YAML traversal.
 * Handles getting, setting and removing elements from the YPath.
 */
public class YPath {
    private static final Logger log = LoggerFactory.getLogger(YPath.class);

    /**
     * The list containing the YPath elements
     */
    private List<String> yPath;

    /**
     * Construct a {@code YPath} from a string. The string will be split by dots.
     * @param path to create YPat from eg: foo.bar.zoo
     */
    public YPath(String path){
        if (path.startsWith(".")) {
            path = path.substring(1);
        }
        this.yPath = splitPath(path);
    }

    /**
     * Construct YPath from an already existing YPath.
     * @param original YPath which the new YPath is a copy of.
     */
    public YPath(YPath original){
        this.yPath = new ArrayList<>(original.yPath);
    }

    /**
     * Get the first element in the YPath.
     */
    public String getFirst(){
        return yPath.get(0);
    }

    /**
     * Remove the first entry from a YPath
     */
    public YPath removeFirst(){
        if (yPath.isEmpty()){
            return this;
        } else {
            YPath shortenedPath = new YPath(this);
            shortenedPath.yPath = shortenedPath.yPath.subList(1, yPath.size());
            return shortenedPath;
        }
    }

    /**
     * Replace the first entry in a YPath.
     * @param replacement the value, which is to replace the first value in the {@code YPath}.
     * @return a new YPath object, with the first string replaced.
     */
    public YPath replaceFirst(String replacement){
        YPath replacedYPath = new YPath(this);
        replacedYPath.yPath.set(0, replacement);

        return replacedYPath;
    }

    /**
     * Check if the internal YPath list is empty.
     */
    public boolean isEmpty(){
        return yPath.isEmpty();
    }



    /**
     * Splits the given path on {@code .}, with support for quoting with single {@code '} and double {@code "} quotes.
     * {@code foo.bar."baz.zoo".'eni.meni' -> [foo, bar, baz.zoo, eni.meni]}.
     * @param path a YAML path with dots {@code .} as dividers.
     * @return the path split on {@code .}.
     */
    private List<String> splitPath(String path) {
        List<String> tokens = new ArrayList<>();
        // Ensure all path elements are separated by a singular dot
        path = path.replaceAll("([^.])\\[", "$1.[");
        Matcher matcher = QUOTE_DOT_SPLIT.matcher(path);
        while (matcher.find()) {
            // Getting group(0) would not remove quote characters so a check for capturing group is needed
            tokens.add(matcher.group(1) == null ? matcher.group(2) : matcher.group(1));
        }
        return tokens;
    }
    private final Pattern QUOTE_DOT_SPLIT = Pattern.compile("[\"']([^\"']*)[\"']|([^.]+)");

    /**
     * If the first element in a list starts with '[' and ends with ']' remove the brackets from the string. The brackets
     * are used to describe a conditional lookup in a YAML map and needs to be removed before the traversal can be
     * continued.
     * @param yPath list of path elements, where the first element is the current element of the YAML traversal, which
     *              is checked for '[' and ']'.
     */
    public static YPath removeBracketsFromPathElement(YPath yPath) {
        Matcher arrayMatcher = YAML.ARRAY_ELEMENT.matcher(yPath.getFirst());

        if (arrayMatcher.matches()){
            String cleaned = arrayMatcher.group(2);
            YPath replacedYPath = yPath.replaceFirst(cleaned);

            return replacedYPath;
        }

        return yPath;

    }

    /**
     * Evaluate if the input {@code value} is equal to the first entry in YPath.
     * @param value the value which is compared to the first entry in the YPath
     * @return true if equal, false if not.
     */
    public boolean firstEquals(String value){
        return this.getFirst().equals(value);
    }

    /**
     * Evaluate if the current YPath object only contains a single value.
     * This happens a lot during traversal to see if a match is found.
     * @return true if the YPath contains a single element. Otherwise return false
     */
    public boolean isLast(){
        return this.yPath.size() == 1;
    }

    /**
     * Get the size of the internal YPath element.
     */
    public int size() {
        return yPath.size();
    }

    @Override
    public String toString() {
        return "YPath(" + yPath + ')';
    }
}
