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
package dk.kb.util.yaml;

import dk.kb.util.Resolver;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.AccessDeniedException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Wrapper for SnakeYAML output for easier access to the YAML elements.
 * <p>
 * Primary use case is for configuration files, using either of
 * <ul>
 *     <li>{@link #resolveLayeredConfigs(String...)} which treats multiple files as separate YAMLs, where the content
 *     of the YAMLs is merged and atomic values are overwritten with the latest file</li>
 *     <li>{@link #resolveMultiConfig(String...)} which treats multiple files as a single YAML allowing for
 *     cross referencing inside of the YAML parts</li>
 * </ul>
 * <p>
 * For standard use, the {@link #resolveLayeredConfigs(String...)} is recommended as it makes it simple to layer the
 * configurations:
 * <ol>
 *     <li>myapp_behaviour.yaml (base behaviour settings, goes into the main repo)</li>
 *     <li>myapp_environment.yaml (servers, usernames, passwords..., is controlled by Operations)</li>
 *     <li>myapp_local_overrides.yaml (local overrides, used for developing and testing)</li>
 * </ol>
 *
 * System properties can be used in the YAML values with the syntax {@code ${environment.variable}}
 * if it has been activated with {@link #setExtrapolate(boolean)}. The default is to extrapolate.
 * <p>
 * If extrapolation is enabled, system property values can be used as YAML values, such as
 * {@code greeting: "Running with {myapp.threads} threads"}
 * User controlled properties can be specified for the application from command line
 * {@code java -Dmyapp.threads=4 -jar myapp.jar}, container configuration or similar.
 * <p>
 * A list of locally available system properties can be obtained with {@code System.getProperties().list(System.out)}
 * but only some of them are guaranteed to be set.
 * See <a href="https://howtodoinjava.com/java/basics/java-system-properties/">java-system-properties</a>
 * Commonly used properties are {@code user.name} and {@code user.home}.
 * <p>
 * Due to limitations of the Generics implementation in Java, using system properties as list values
 * involves fixed conversions: Integral numbers are treated as Integers, floating point numbers as Doubles.
 * <p>
 * Note: Besides system environment, it is possible to use other substitutions, such as environment variables using
 * the syntax {@code ${env:USERNAME}}. See the JavaDoc for {@link StringSubstitutor} for examples. Where possible,
 * use system environment as that is the least unstable across platforms.
 * <p>
 * Note 2: Nested fallbacks is somewhat possible, but works poorly with prefixed lookups. As {@code sys:} is implicit
 * a working "provide the value either as system properties or environment variables" definition can be written as
 * {@code ${env:USERNAME:-${user.name:-igiveup}}} where switching {@code env:USERNAME} and {@code user.name} will not
 * work.
 * <p>
 * The getter-methods uses {@code yPath}s as input. {@code yPath} stands for YAML-path is a subset of the paths
 * used for extracting values from JSON structures using <a href="https://github.com/jqlang/jq">jq</a>.
 * The path elements are dot-separated ({@code .} and supports
 * <ul>
 * <li>{@code key} for direct traversal, e.g. "foo" or "foo.bar"</li>
 * <li>{@code key[index]} for a specific element in a list, e.g. "foo.[2]" or "foo.[2].bar"</li>
 * <li>{@code key.[last]} for the last element in a list, e.g. "foo.[last]" or "foo.bar.[last]"</li>
 * <li>{@code key.[subkey=value]} for the map elements in a list where their value for the subkey matches, e.g. "foo.[bar=baz]"</li>
 * <li>{@code key.[subkey!=value]} for the map elements in a list where their value for the subkey does not match, e.g. "foo.[bar!=baz]"</li>
 * <li>{@code key.[*].zoo} for all values of zoo in a map</li>
 * <li>{@code key.*.zoo} for all values of zoo, one level into the current YAML. Matches key.foo.zoo and key.bar.zoo.</li>
 * <li>{@code key.**.zoo} for all value in the structure with the key zoo.</li>
 * </ul>
 * Dots {@code .} in YAML keys can be escaped with quotes: {@code foo.'a.b.c' -> [foo, a.b.c]}.
 */
public class YAML extends LinkedHashMap<String, Object> {

    private static final Logger log = LoggerFactory.getLogger(YAML.class);

    private static final long serialVersionUID = -5211961549015821195L;


    public static final Pattern ARRAY_ELEMENT = Pattern.compile("^([^\\[]*)\\[([^]]*)]$");
    private static final Pattern ARRAY_CONDITIONAL = Pattern.compile(" *([^!=]+) *(!=|=) *(.*)"); // foo=bar or foo!=bar
    private static final YAML EMPTY = new YAML();

    boolean extrapolateSystemProperties = false; // All constructors set this explicitly
    private List<StringSubstitutor> substitutors = null;

    /**
     * Creates an empty YAML.
     */
    public YAML() {
        super();
    }

    /**
     * Resolves one or more resources using globbing and returns YAML based on the concatenated resources.
     * <p>
     * Note: This method merges the YAML configs as-is: Any key-collisions are handled implicitly by keeping the latest
     * key-value pair in the stated configurations. Sub-entries are not merged on key collisions.
     * Use {@link #resolveLayeredConfigs} for treating YAML files as overlays when loading.
     * <p>
     * Extrapolation of values is enabled with this constructor. Use {@link YAML(Boolean, String...)} to control this.
     * @param resourceNames globs for YAML files.
     * @throws IOException if the files could not be loaded or parsed.
     * @see #resolveLayeredConfigs(String...)
     * @see #resolveLayeredConfigs(MERGE_ACTION, MERGE_ACTION, String...)
     */
    public YAML(String... resourceNames) throws IOException {
        this(true, resourceNames);
    }

    /**
     * Resolves one or more resources using globbing and returns YAML based on the concatenated resources.
     * <p>
     * Note: This method merges the YAML configs as-is: Any key-collisions are handled implicitly by keeping the latest
     * key-value pair in the stated configurations. Sub-entries are not merged on key collisions.
     * Use {@link #resolveLayeredConfigs} for treating YAML files as overlays when loading.
     * <p>
     * @param extrapolateSystemProperties whether system properties should be extrapolated in values.
     *                                    Note that extrapolation cannot be turned off once enabled.
     * @param resourceNames globs for YAML files.
     * @throws IOException if the files could not be loaded or parsed.
     * @see #resolveLayeredConfigs(String...)
     * @see #resolveLayeredConfigs(MERGE_ACTION, MERGE_ACTION, String...)
     */
    public YAML(boolean extrapolateSystemProperties, String... resourceNames) throws IOException {
        putAll(YAML.resolveMultiConfig(resourceNames));
        setExtrapolate(extrapolateSystemProperties);
    }

    /**
     * Creates a YAML wrapper around the given map.
     * The content of {@code map} is shallow copied: Changes to values in the original {@code map} will be reflected
     * in this YAML.
     * <p>
     * Extrapolation of values is enabled with this constructor. Use {@link YAML(Map, Boolean)} to control this.
     * @param map a map presumable delivered by SnakeYAML.
     */
    public YAML(Map<String, Object> map) {
        this(map, true);
    }

    /**
     * Creates a YAML wrapper around the given map.
     * The content of {@code map} is shallow copied: Changes to values in the original {@code map} will be reflected
     * in this YAML.
     *
     * @param map a map presumable delivered by SnakeYAML.
     * @param extrapolateSystemProperties whether system properties should be extrapolated in values.
     *                                    Note that extrapolation cannot be turned off once enabled.
     */
    public YAML(Map<String, Object> map, boolean extrapolateSystemProperties) {
        putAll(map);
        setExtrapolate(extrapolateSystemProperties);
    }

    /**
     * Creates a YAML wrapper around the given map.
     * The content of {@code map} is shallow copied: Changes to values in the original {@code map} will be reflected
     * in this YAML.
     *
     * @param map a map presumable delivered by SnakeYAML.
     * @param extrapolateSystemProperties should system properties be extrapolated in values
     *                                    Note that extrapolation cannot be turned off once enabled.
     * @param substitutors explicit specification of substitutors. Typically used for creating submaps.
     */
    YAML(Map<String, Object> map, boolean extrapolateSystemProperties, List<StringSubstitutor> substitutors) {
        this(map, extrapolateSystemProperties, false, substitutors);
    }

    /**
     * Internal optimized constructor. Creates a YAML wrapper around the given map.
     * The content of {@code map} is shallow copied: Changes to values in the original {@code map} will be reflected
     * in this YAML.
     *
     * @param map a map presumable delivered by SnakeYAML.
     * @param extrapolateSystemProperties should system properties be extrapolated in values
     *                                    Note that extrapolation cannot be turned off once enabled.
     * @param alreadyExtrapolated         if true, extrapolation has already been performed on values,
     *                                    so no extra traversal for extrapolation is needed.
     * @param substitutors explicit specification of substitutors. Typically used for creating submaps.
     */
    YAML(Map<String, Object> map, boolean extrapolateSystemProperties, boolean alreadyExtrapolated,
         List<StringSubstitutor> substitutors) {
        this.putAll(map);
        this.substitutors = substitutors;
        if (extrapolateSystemProperties && alreadyExtrapolated) {
            // Skip the traversal as we know the map has already been extrapolated
            this.extrapolateSystemProperties = true;
        } else {
            setExtrapolate(extrapolateSystemProperties);
        }
    }

    /**
     * Resolves the YAML sub map at the given path in the YAML. Supports {@code .} for path separation,
     * Sample path: {@code foo.bar}
     * <p>
     * Note: Dots {@code .} in YAML keys can be escaped with quotes: {@code foo.'a.b.c' -> [foo, a.b.c]}.
     * <p>
     * This method is equal to {@link #getSubMap(String)}. {@code getYAML} is preferred due to clearer semantics.
     * @param path path for the sub map.
     * @return the map at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value cannot be parsed as a List of YAMLs
     * @throws NullPointerException if the path is null
     */
    public YAML getYAML(String path) throws NotFoundException, InvalidTypeException, NullPointerException {
        return getSubMap(path, false);
    }

    /**
     * Resolves the YAML sub map at the given path in the YAML. Supports {@code .} for path separation,
     * Sample path: {@code foo.bar}
     * <p>
     * Note: Dots {@code .} in YAML keys can be escaped with quotes: {@code foo.'a.b.c' -> [foo, a.b.c]}.
     *
     * @param path path for the sub map.
     * @return the map at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value cannot be parsed as a List of YAMLs
     * @throws NullPointerException if the path is null
     */
    @NotNull
    public YAML getSubMap(String path) throws NotFoundException, InvalidTypeException, NullPointerException {
        return getSubMap(path, false);
    }

    /**
     * Resolves the YAML sub map at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: {@code foo.bar}
     * Note: Dots {@code .} in YAML keys can be escaped with quotes: {@code foo.'a.b.c' -> [foo, a.b.c]}.
     *
     * @param path         path for the sub map.
     * @param maintainKeys preserve the path prefix for the keys in the result
     * @return the map at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value cannot be parsed as a subMap
     * @throws NullPointerException if the path is null
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public YAML getSubMap(String path, boolean maintainKeys)
            throws NotFoundException, InvalidTypeException, NullPointerException {
        Object found = get(path);

        if (found == null) {
            log.debug("Value for subMap is null, therefore an empty YAML structure is returned.");
            return EMPTY;
        }

        if (!(found instanceof Map)) {
            throw new InvalidTypeException(
                    "Expected a Map for path but got '" + found.getClass().getName() + "'", path);
        }
        Map<String, Object> result;
        try {
            result = (Map<String, Object>) found;
        } catch (ClassCastException e) {
            throw new InvalidTypeException("Exception casting '" + found + "' to Map<String, Object>", path,
                    e);
        }

        if (maintainKeys) {
            result = result.entrySet().stream().collect(Collectors.toMap(
                    entry -> path + "." + entry.getKey(),
                    Map.Entry::getValue
            ));
        }

        // Note: getSubstitutors() is used to ensure that substitutors are created for the full YAML.
        //       This is needed for path substitution
        return new YAML(result, extrapolateSystemProperties, extrapolateSystemProperties, getSubstitutors());
    }

    /**
     * Resolves the list at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: {@code foo.bar}
     * Note: Dots {@code .} in YAML keys can be escaped with quotes: {@code foo.'a.b.c' -> [foo, a.b.c]}.
     * @param path path for the list.
     * @param <T> the type of elements in the list.
     *           Valid types are atomics ({@code Integer}, {@code Boolean} etc), {@code String} and {@code YAML}.
     * @return the list at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value cannot be parsed as a List
     * @throws NullPointerException if the path is null
     */
    @SuppressWarnings({"unchecked", "unused"})
    @NotNull
    public <T> List<T> getList(String path) throws NotFoundException, InvalidTypeException {
        try {
            // The cast to test checks if T is YAML, in which case the designated getYAMLList is called
            // It is ugly, but no better test for T == YAML is known. See e.g.
            // https://stackoverflow.com/questions/182636/how-to-determine-the-class-of-a-generic-type
            // https://stackoverflow.com/questions/73982858/java-generics-reflection-get-classt-from-generic-returns-typevariableimpl-ins
            T test = (T)EMPTY;
            return (List<T>)getYAMLList(path);
        } catch (ClassCastException | NullPointerException e) {
            // Do nothing as this just mean that T is not YAML or contains elements that are not directly YAML, such as null.
        }

        Object found = get(path);
        if (found == null) {
            // This message should probably be: "Requested key does not exist in YAML."
            throw new NotFoundException("Path gives a null value", path);
        }

        if (!(found instanceof List)) {
            throw new InvalidTypeException(
                    "Expected a List for path but got '" + found.getClass().getName() + "'", path);
        }
        try {
            List<T> foundList = (List<T>) found;

            return foundList.stream()
                    .map(value -> (T) Objects.requireNonNullElseGet(value, YAML::new))
                    .collect(Collectors.toList());

        } catch (ClassCastException e) {
            throw new InvalidTypeException("Exception casting '" + found + "' to List<T>", path, e);
        }
    }

    /**
     * Resolves the list at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path        path for the list.
     * @param <T> the type of elements in the list
     * @param defaultList if the path cannot be resolved, return this value.
     * @return the list at the path or defaultList if it could not be located.
     */
    public <T> List<T> getList(String path, List<T> defaultList) {
        try {
            return getList(path);
        } catch (NotFoundException | InvalidPathException e) {
            return defaultList;
        }
    }

    /**
     * Resolves the list of sub YAMLs at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     * <p>
     * Note: If the result is assigned to a generified List, this is equivalent to
     * {@code List<YAML> yamls = superyaml.getList("foo")}.
     * @param path path for the list.
     * @return the list of sub YAMLs at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value cannot be parsed as a List of YAMLs
     * @throws InvalidTypeException if the path was invalid (i.e. if treated a value as a map)
     * @throws NullPointerException if the path is null
     * @see #getList(String yPath)
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public List<YAML> getYAMLList(String path) throws NotFoundException, InvalidTypeException, NullPointerException {
        Object found = get(path);
        if (found == null) {
            return List.of(new YAML());
            //throw new NotFoundException("Path gives a null value", path);
        }

        if (!(found instanceof List)) {
            throw new InvalidTypeException(
                    "Expected a List for path but got '" + found.getClass().getName() + "'", path);
        }

        List<Map<String, Object>> hmList;
        try {
            hmList = (List<Map<String, Object>>) found;
        } catch (ClassCastException e) {
            throw new InvalidTypeException(
                    "Exception casting '" + found + "' to List<Map<String, Object>>", path, e);
        }
        // Note: getSubstitutors() is used to ensure that substitutors are created for the full YAML.
        //       This is needed for path substitution
        return hmList.stream()
                .map(map -> new YAML(map, extrapolateSystemProperties, extrapolateSystemProperties, getSubstitutors()))
                .collect(Collectors.toList());
    }

    /**
     * Resolves the Short at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path path for the Short.
     * @return the Short at the path or null if it could not be located.
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value is not a valid Short
     * @throws InvalidTypeException if the path was invalid (i.e. if treated a value as a map)
     * @throws NullPointerException if the path is null
     */
    public Short getShort(String path) {
        Object o = get(path);
        try {
            return Short.valueOf(o.toString());
        } catch (NumberFormatException e) {
            throw new InvalidTypeException("Exception casting '" + o + "' to Short", path, e);
        }
    }

    /**
     * Resolves the Short at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path         path for the Short.
     * @param defaultValue if the path cannot be resolved, return this value.
     * @return the Short at the path or defaultValue if it could not be located.
     * @throws NullPointerException if the path is null
     */
    public Short getShort(String path, Short defaultValue) {
        try {
            return getShort(path);
        } catch (NotFoundException | InvalidTypeException e) {
            log.debug("No value has been found for '{}', using the default: '{}' instead.", path, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Resolves the integer at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path path for the integer.
     * @return the integer at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value is not a valid Integer
     * @throws InvalidTypeException if the path was invalid (i.e. if treated a value as a map)
     * @throws NullPointerException if the path is null
     */
    @NotNull
    public Integer getInteger(String path) throws NotFoundException, InvalidTypeException, NullPointerException {
        Object o = get(path);
        try {
            return Integer.valueOf(o.toString());
        } catch (ClassCastException e) {
            throw new InvalidTypeException("Exception casting '" + o + "' to Integer", path, e);
        }
    }

    /**
     * Resolves the Integer at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path         path for the integer.
     * @param defaultValue if the path cannot be resolved, return this value.
     * @return the integer at the path or defaultValue if it could not be located or parsed as an Integer.
     * @throws NullPointerException if the path is null
     */
    @NotNull
    public Integer getInteger(String path, Integer defaultValue) throws NullPointerException {
        Object o;
        try {
            o = get(path);
        } catch (NotFoundException | InvalidTypeException  e) {
            log.debug("No value has been found for '{}', using the default: '{}' instead.", path, defaultValue);
            return defaultValue;
        }
        try {
            return Integer.valueOf(o.toString());
        } catch (NumberFormatException e) {
            log.debug("Unable to parse '" + o.toString() + "' as Integer", o);
            return defaultValue;
        } catch (NullPointerException e){
            log.debug("No value has been found for '{}', using the default: '{}' instead.", path, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Resolves the Long at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path path for the Long.
     * @return the Long at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value is not a valid Long
     * @throws InvalidTypeException if the path was invalid (i.e. if treated a value as a map)
     * @throws NullPointerException if the path is null
     */
    public Long getLong(String path) {
        Object o = get(path);
        try {
            return Long.valueOf(o.toString());
        } catch (ClassCastException e) {
            throw new InvalidTypeException("Exception casting '" + o + "' to Long", path, e);
        }
    }

    /**
     * Resolves the Long at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path         path for the Long.
     * @param defaultValue if the path cannot be resolved, return this value.
     * @return the Long at the path or defaultValue if it could not be located.
     * @throws NullPointerException if the path is null
     */
    public Long getLong(String path, Long defaultValue) {
        try {
            return getLong(path);
        } catch (NotFoundException | InvalidTypeException e) {
            log.debug("No value has been found for '{}', using the default: '{}' instead.", path, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Resolves the double at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path path for the double.
     * @return the Double at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value is not a valid Double
     * @throws InvalidTypeException if the path was invalid (i.e. if treated a value as a map)
     * @throws NullPointerException if the path is null
     */
    public Double getDouble(String path) {
        Object o = get(path);
        try {
            return Double.valueOf(o.toString());
        } catch (ClassCastException e) {
            throw new InvalidTypeException("Exception casting '" + o + "' to Double", path, e);
        }
    }

    /**
     * Resolves the double at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path         path for the double.
     * @param defaultValue if the path cannot be resolved, return this value.
     * @return the Double at the path or defaultValue if it could not be located.
     * @throws NullPointerException if the path is null
     */
    public Double getDouble(String path, Double defaultValue) {
        try {
            return getDouble(path);
        } catch (NotFoundException | InvalidTypeException e) {
            log.debug("No value has been found for '{}', using the default: '{}' instead.", path, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Resolves the float at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path path for the Float.
     * @return the Float at the path or null if it could not be located.
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the value is not a valid Float
     * @throws InvalidTypeException if the path was invalid (i.e. if treated a value as a map)
     * @throws NullPointerException if the path is null
     */
    public Float getFloat(String path) {
        Object o = get(path);
        try {
            return Float.valueOf(o.toString());
        } catch (ClassCastException e) {
            throw new InvalidTypeException("Exception casting '" + o + "' to Float", path, e);
        }
    }

    /**
     * Resolves the float at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path         path for the Float.
     * @param defaultValue if the path cannot be resolved, return this value.
     * @return the Float at the path or defaultValue if it could not be located.
     */
    public Float getFloat(String path, Float defaultValue) {
        try {
            return getFloat(path);
        } catch (NotFoundException | InvalidTypeException e) {
            log.debug("No value has been found for '{}', using the default: '{}' instead.", path, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Resolves the boolean at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path path for the boolean.
     * @return the boolean at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the path was invalid (i.e. if treated a value as a map)
     * @throws NullPointerException if the path is null
     */
    @NotNull
    public Boolean getBoolean(String path) throws NotFoundException, InvalidTypeException, NullPointerException {
        Object o = get(path);
        return Boolean.valueOf(o.toString());
    }

    /**
     * Resolves the boolean at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path         path for the boolean.
     * @param defaultValue if the path cannot be resolved, return this value.
     * @return the boolean at the path or defaultValue if it could not be located.
     * @throws NullPointerException if the path is null
     */
    @NotNull
    public Boolean getBoolean(String path, Boolean defaultValue) throws NullPointerException {
        try {
            return getBoolean(path);
        } catch (NotFoundException | InvalidTypeException | NullPointerException e) {
            log.debug("No value has been found for '{}', using the default: '{}' instead.", path, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Resolves the string at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     * Note: Object.toString is used to provide the String value, so this is safe to call for most YAML content.
     *
     * @param path path for the string.
     * @return the String at the path
     * @throws NotFoundException    if the path could not be found
     * @throws InvalidTypeException if the path was invalid (i.e. if treated a value as a map)
     * @throws NullPointerException if the path is null
     */
    @NotNull
    public String getString(String path) throws NotFoundException, InvalidTypeException, NullPointerException {
        return get(path).toString();
    }

    /**
     * Resolves the string at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar
     *
     * @param path         path for the Object.
     * @param defaultValue if the path cannot be resolved, return this value.
     * @return the String at the path or defaultValue if it could not be located.
     * @throws NullPointerException if the path is null
     */
    @NotNull
    public String getString(String path, String defaultValue) throws NullPointerException {
        try {
            return getString(path);
        } catch (NotFoundException | InvalidTypeException e) {
            log.debug("No value has been found for '{}', using the default: '{}' instead.", path, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Resolves all values related to a given key, from a part of a YAML structure. All values that are children of the
     * input {@code yamlPath} and are specified with the input {@code key} are returned as a list.
     * @deprecated
     * This is no longer the best method to visit values. Use {@link #visit(String, YAML, YAMLVisitor)} instead.
     * @param yamlPath the specific part of a YAML file, that is to be queried for values.
     * @param key all values that are associated with this key are added to the returned list.
     * @return a list of all scalar values that are associated with the input key.
     */
    @Deprecated
    public List<Object> getMultipleFromSubYaml(String yamlPath, String key){
        String combinedPath = yamlPath + "[*].*." + key;

        MultipleValuesVisitor visitor = new MultipleValuesVisitor();
        visit(combinedPath, this, visitor);
        return visitor.extractedValues;
    }

    /* **************************** Path-supporting overrides ************************************ */

    /**
     * Checks if a key is present at the given path in the YAML. See {@link #get(Object)} for path syntax.
     * Sample path: foo.bar.
     *
     * @param path path for the Object.
     * @return true is an Object exists for the given path.
     * @throws NullPointerException if the path is null
     */
    @Override
    public boolean containsKey(Object path) throws NullPointerException {
        try {
            get(path);
            return true;
        } catch (NotFoundException | InvalidTypeException e) {
            return false;
        }
    }

    /**
     * Resolve the Object at the given path in this YAML.
     * @param yPath path for the entry. See the class Javadoc for {@code yPath} syntax.
     *              If {@code yPath} is empty, the full YAML is returned.
     * @return the Object. Will never return null, will rather throw exceptions.
     * @throws NotFoundException     if {@code yPath} cannot be found.
     * @throws InvalidTypeException  if {@code yPath} was invalid (i.e. if treated a value as a map).
     * @throws NullPointerException  if {@code yPath} is null.
     * @throws InvalidTypeException  if {@code yPath} was invalid (i.e. if treated a value as a map).
     * @throws IllegalStateException if {@code yPath} matches more than 1 value.
     * @see #getMultiple(String)
     * @see #getMultiple(String, YAML)
     */
    @Override
    @NotNull
    public Object get(Object yPath) throws NotFoundException, InvalidTypeException, NullPointerException {
        MultipleValuesVisitor visitor = new MultipleValuesVisitor();
        visit(yPath.toString(), this, visitor);

        if (visitor.extractedValues.isEmpty()) {
            throw new NotFoundException("Cannot find object at path '", yPath + "'");
        }
        if (visitor.extractedValues.size() > 1) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "get('%s') found %d values when expecting 1. " +
                            "If the first value out of multiple is wanted, the getMultiple method can be used",
                    yPath, visitor.extractedValues.size()));
        }
        return visitor.extractedValues.get(0);
    }

    /**
     * Resolves Objects which match the given path in this YAML.
     * @param yPath path to the entries in this YAML. See the class Javadoc for {@code yPath} syntax.
     * @return a list of matching objects. If there are no matches, the empty list will be returned.
     * @see #get(Object yPath)
     * @see #getMultiple(String, YAML)
     * @see #visit(String, YAML, YAMLVisitor)
     */
    public List<Object> getMultiple(String yPath) {
        return getMultiple(yPath, this);
    }

    /**
     * Resolves Objects which match the given path in the YAML.
     * @param yPath path to the entries in the {@code yaml}. See the class Javadoc for {@code yPath} syntax.
     * @param yaml which is traversed for {@code yPath}.
     * @return a list of matching objects. If there are no matches, the empty list will be returned.
     * @see #get(Object yPath)
     * @see #getMultiple(String)
     * @see #visit(String, YAML, YAMLVisitor)
     */
    public List<Object> getMultiple(String yPath, YAML yaml) {
        if (yPath == null) {
            throw new NullPointerException("Failed to query config for null path");
        }
        String path = yPath.trim();
        if (path.startsWith(".")) {
            path = path.substring(1);
        }

        MultipleValuesVisitor visitor = new MultipleValuesVisitor();
        yaml.visit(path, this, visitor);
        return visitor.extractedValues;
    }

    // The real implementation of get(path), made flexible so that the entry YAML can be specified

    /**
     * Follows the <a href="https://en.wikipedia.org/wiki/Visitor_pattern">Visitor Pattern</a>, performing a callback
     * with the node value to {@code visitor} for all nodes matching {@code yPath} in {@code yaml}.
     * @param yPath path to the entries in the {@code yaml}. See the class Javadoc for {@code yPath} syntax.
     * @param yaml which is traversed for {@code yPath}.
     * @param visitor {@link YAMLVisitor#visit(Object element)} will be called for all elements in {@code yaml}
     *        matching {@code yPath}
     * @see #getMultiple(String)
     * @see #visit(String, YAML, YAMLVisitor)
     */
    public void visit(String yPath, YAML yaml, YAMLVisitor visitor) throws NotFoundException, InvalidTypeException, NullPointerException {
        if (yPath == null) {
            throw new NullPointerException("Failed to query config for null path");
        }
        String path = yPath.trim();
        if (path.startsWith(".")) {
            path = path.substring(1);
        }
        YPath yPathParsed = new YPath(path);

        if (!yPathParsed.isEmpty()){
            traverse(yPathParsed, yaml, visitor);
        }

        // TODO: Investigate how JQ handles paths as: test.tuplesequence[*].*.name
    }

    /**
     * Recursively traverse a YAML file for entries that match the keys from {@code yPath}.
     * @param yPath path in YAML file. The path is split into a list for each level/part of the path.
     * @param yaml a YAML object containing the full YAML which is to be traversed.
     * @param visitor which visits entries that match the input path.
     */
    private void traverse(YPath yPath, Object yaml, YAMLVisitor visitor) {
        if (yaml instanceof Map) {
            traverseMap(yPath, yaml, visitor);
        } else if (yaml instanceof List) {
            traverseList(yPath, yaml, visitor);
        }
    }

    /**
     * Traverse a YAML List recursively and collect matching paths to the input visitor.
     * The implementation supports retrieval of multiple matching results.
     * <p>
     * It supports placeholders such as foo[] and foo[*], which both of them returns all elements in this list.
     * This can be used in conjunction with further traversal such as foo[].bar. The implementation also supports the
     * syntax foo[last], which will return the last element in the list. Standard index based lookup such as foo[2] is
     * also supported. The implementation supports dot-array syntax as well. This means that the queries foo[bar] and
     * foo.[bar] returns the same value.
     * <p>
     * The implementation supports conditional lookups as well. This means that a query for zoo.[foo=bar].baz returns
     * all values for baz, that are a child of zoo and has the sibling-key foo with the value bar.
     * @param yPath a list of path elements. This list contains all parts of a specified path, which is most likely
     *              delivered through the {@link #visit(String, YAML, YAMLVisitor)}-method.
     * @param yaml the current place in the YAML file being traversed. This should be an instance of a List.
     * @param visitor used to visit values that match the given path.
     */
    @SuppressWarnings("unchecked")
    private void traverseList(YPath yPath, Object yaml, YAMLVisitor visitor) {
        List<Object> list = (List<Object>) yaml;

        Matcher matcher = ARRAY_ELEMENT.matcher(yPath.getFirst());
        final String arrayElementIndex;
        if (matcher.matches()) { // foo.bar[2]
            arrayElementIndex = matcher.group(2);
        } else {
            arrayElementIndex = null;
        }

        if (arrayElementIndex != null){
            switch (arrayElementIndex) {
                case "*":
                case "":
                    // Set yPath entry to * as [] and [*] are to be treated equal.
                    YPath asterixYPath = yPath.replaceFirst("*");
                    convertListToMapAndTraverse(asterixYPath, visitor, list);
                    break;
                case "last":
                    // Set yPath entry to the last index of the list.
                    YPath lastYPath = yPath.replaceFirst(String.valueOf(list.size() - 1));
                    convertListToMapAndTraverse(lastYPath, visitor, list);
                    break;
                default:
                    // Set yPath entry as index number
                    YPath indexYPath = yPath.replaceFirst(arrayElementIndex);
                    convertListToMapAndTraverse(indexYPath, visitor, list);
            }
        } else {
            convertListToMapAndTraverse(yPath, visitor, list);
        }
    }

    /**
     * Traverse a YAML List recursively and collect matching paths to the input visitor.
     * The implementation supports retrieval of multiple matching results.
     * <p> <br>
     * YAML maps can be queried in multiple ways and with different placeholders as well. The implementation supports
     * the syntax foo.*.bar, which returns all matches for 'bar' a single level below 'foo'. Eg: this would match paths
     * such as foo.zoo.bar and foo.fooz.bar, but not a path such as foo.fooz.zoo.bar
     * <p>
     * The implementation also supports the syntax foo.**.bar, which returns all bar's that are below 'foo' in the hierarchy.
     * This path would match all the following values: 'foo.zoo.bar', 'foo.fooz.zoo.bar' and 'foo.anything.bar'.
     * <p>
     * The implementation supports conditional lookups as well. This means that a query for zoo.[foo=bar].baz returns
     * all values for baz, that are a child of zoo and has the sibling-key foo with the value bar.
     * @param yPath a list of path elements. This list contains all parts of a specified path, which is most likely
     *              delivered through the {@link #visit(String, YAML, YAMLVisitor)}-method.
     * @param yaml the current place in the YAML file being traversed. This should be an instance of a Map.
     * @param visitor used to visit matching paths.
     */
    private void traverseMap(YPath yPath, Object yaml, YAMLVisitor visitor) {
        if (yPath.isEmpty()) {
            return;
        }

        // Quick fix cleaning entries as [foo=bar] to foo=bar.
        YPath cleanedYPath = YPath.removeBracketsFromPathElement(yPath);

        Map<?, ?> map = (Map<?, ?>) yaml;
        YPath shortenedPath = cleanedYPath.removeFirst();

        Matcher conditionalMatch = ARRAY_CONDITIONAL.matcher(cleanedYPath.getFirst());

        if (cleanedYPath.firstEquals("**") && !cleanedYPath.isLast()){
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                traverse(cleanedYPath, entry.getValue(), visitor);
                traverse(shortenedPath, entry.getValue(), visitor);
            }
        } else if (cleanedYPath.firstEquals("**") && cleanedYPath.isLast()) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map || entry.getValue() instanceof List){
                    traverse(cleanedYPath, entry.getValue(), visitor);
                } else {
                    visitor.visit(entry.getValue());
                }
            }
        }
        else if (cleanedYPath.firstEquals("*")) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (cleanedYPath.size() > 1) {
                    traverse(shortenedPath, entry.getValue(), visitor);
                } else {
                    visitor.visit(entry.getValue());
                }
            }
        } else if (conditionalMatch.matches()) {
            conditionalTraverse(cleanedYPath, visitor, conditionalMatch, map, shortenedPath);
        } else {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey().toString();
                Object value = entry.getValue();

                if (cleanedYPath.size() == 1){
                    if (key.equals(cleanedYPath.getFirst())){
                        visitor.visit(Objects.requireNonNullElseGet(value, YAML::new));
                    }
                } else {
                    if (key.equals(cleanedYPath.getFirst())){
                        traverse(shortenedPath, value, visitor);
                    }
                }
            }
        }
    }

    private void conditionalTraverse(YPath yPath, YAMLVisitor visitor, Matcher conditionalMatch, Map<?, ?> map, YPath shortenedPath) {
        String key = conditionalMatch.group(1);
        boolean mustMatch = conditionalMatch.group(2).equals("="); // The Pattern ensures only "!=" or "=" is in the group
        String value = conditionalMatch.group(3);

        List<Object> matchingObjects = new ArrayList<>();
        for (Map.Entry<?,?> entry : map.entrySet()) {
            matchingObjects.add(conditionalGet(entry.getValue(), key, value, mustMatch));
        }

        matchingObjects = matchingObjects.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (yPath.isLast()){
            for (Object matchingEntry: matchingObjects) {
                visitor.visit(matchingEntry);
            }
        } else {
            for (Object matchingEntry: matchingObjects) {
                traverse(shortenedPath, matchingEntry, visitor);
            }
        }
    }


    /**
     * Convert a YAML list to a YAML map containing all elements, where the key is the index and continue the traversal
     * of the YAML structure through the {@link #traverse(YPath, Object, YAMLVisitor)}-method.
     * @param yPath a list of path elements. This list contains all parts of a specified path, which is most likely
     *              delivered through the {@link #visit(String, YAML, YAMLVisitor)}-method.
     * @param visitor used to visit values that match the given path.
     * @param list the YAML list which is to be converted to a YAML map.
     */
    private void convertListToMapAndTraverse(YPath yPath, YAMLVisitor visitor, List<Object> list) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(list.size());
        Object yaml;
        for (int j = 0; j < list.size(); j++) {
            map.put(j + "", list.get(j));
        }
        yaml = map;
        traverse(yPath, yaml, visitor);
    }

    /**
     * Checks if the conditional is satisfied for the given element and if so returns it.
     * If the element is not a map, an exception will be thrown.
     * @param map the map to use for lookup.
     * @param key the key to look up.
     * @param value the value to match.
     * @param mustMatch if true, the result of lookup for the key must match the value.
     *                  If false, the result of a lookup for the key must not match the value.
     * @return the element if the conditional is satisfied.
     */
    @SuppressWarnings("unchecked")
    private Object conditionalGet(Object map, String key, String value, boolean mustMatch) {
        if (!(map instanceof Map)) {
            throw new IllegalArgumentException("Conditional index lookup requires sub-elements to be Maps, " +
                    "but the current element was a " + map.getClass().getSimpleName());
        }
        // Note: getSubstitutors() is used to ensure that substitutors are created for the full YAML.
        //       This is needed for path substitution
        YAML subYAML = new YAML((Map<String, Object>)map, extrapolateSystemProperties, extrapolateSystemProperties,
                getSubstitutors());

        // Check at the outer level for flat map style
        Object keyValue;
        try {
            keyValue = subYAML.get(key);
        } catch (NotFoundException e) {
            keyValue = null;
        }

        if (keyValue == null) {
            // Check at the inner level for nested map style (commonly used @ kb.dk)
            keyValue = ((Map<String, Object>)map).values().stream().
                    filter(element -> element instanceof Map).
                    map(element -> (Map<?, ?>)element).
                    filter(elementMap -> evaluateConditional(value, elementMap.get(key), mustMatch)).
                    findFirst().
                    orElse(null);

            // Return nested map if one of its elements satisfies the condition
            if (keyValue != null) {
                return keyValue;
            }
        }

        // Flat map style
        return evaluateConditional(value, keyValue, mustMatch) ? subYAML : null;
    }

    private boolean evaluateConditional(String expected, Object value, boolean mustMatch) {
        return mustMatch ?
                value != null && value.toString().equals(expected) :
                value == null || !value.toString().equals(expected);
    }

    /**
     * Attempts to guess the type of atomic elements: {@code 123} is int, {@code true} is boolean, {@code 1.2} is double
     * and String is the fallback.
     * @param sub an Object that is to be substituted.
     * @return the sub with environment variables substituted.
     */
    private Object extrapolateGuessType(Object sub) {
        if (sub == null || !isExtrapolating()){
            return sub;
        }
        if (sub instanceof String) {
            String any = substitute((String) sub);
            if (INTEGRAL_MATCHER.matcher(any).matches()) {
                return Integer.valueOf(any);
            }
            if (FLOAT_MATCHER.matcher(any).matches()) {
                return Double.valueOf(any);
            }
            if (BOOLEAN_MATCHER.matcher(any).matches()) {
                return Boolean.valueOf(any);
            }
            return any; // Fallback to String
        }
        if (sub instanceof List<?>) {
            List<?> objects = (List<?>) sub;
            return objects.stream().map(this::extrapolateGuessType).collect(Collectors.toList());
        }
        return sub;
    }
    private final Pattern INTEGRAL_MATCHER = Pattern.compile("[0-9]+");
    private final Pattern FLOAT_MATCHER = Pattern.compile("[0-9]*[.][0-9]+"); // Leading digit optional: .2 is ok
    private final Pattern BOOLEAN_MATCHER = Pattern.compile("true|false");

    /* **************************** Fetching YAML ************************************ */

    /**
     * Resolve the given YAML configuration.
     * <p>
     * Note: The resolver supports globbing so {@code /home/someone/myapp-conf/*.yaml} expands to all YAML-files
     * in the {@code myapp} folder. When globbing is used, the matching files are iterated in alphanumerical order
     * so that subsequent YAML definitions overwrites previous ones. See also {@link #resolveMultiConfig(String...)}.
     *
     * @param configName the name, path or glob for the configuration file.
     * @return the configuration parsed up as a tree represented as Map and wrapped as YAML.
     * @throws IOException                    if the configuration could not be fetched.
     * @throws FileNotFoundException  if the config name does not refer to a file
     * @throws MalformedURLException if the resource location could not be converted to an URL.
     * @throws InvalidPathException           if the configName is not valid as a path
     * @throws NullPointerException           if the configName is null
     * @throws IllegalArgumentException       if the config cannot be parsed as YAML
     * @deprecated use {@link #resolveLayeredConfigs(String...)} or {@link #resolveMultiConfig(String...)} instead.
     */
    @NotNull
    @Deprecated
    public static YAML resolveConfig(String configName) throws IOException, NullPointerException, InvalidPathException {
        return resolveConfig(configName, null);
    }

    /**
     * Resolve the given YAML configuration.
     * <p>
     * Note: The resolver supports globbing so {@code /home/someone/myapp-conf/*.yaml} expands to all YAML-files
     * in the {@code myapp-conf} folder. When globbing is used, the matching files are iterated in alphanumerical order
     * so that subsequent YAML definitions overwrites previous ones. See also {@link #resolveMultiConfig(String...)}.
     *
     * @param configName the path, name or glob of the configuration file.
     * @param confRoot   the root element in the configuration or null if the full configuration is to be returned.
     * @return the configuration parsed up as a tree represented as Map and wrapped as YAML.
     * @throws IOException                    if the configuration could not be fetched.
     * @throws FileNotFoundException  if the config name does not refer to a file
     * @throws MalformedURLException if the resource location could not be converted to an URL.
     * @throws InvalidPathException           if the configName is not valid as a path
     * @throws NullPointerException           if the configName is null
     * @throws IllegalArgumentException       if the config cannot be parsed as YAML
     * @throws NotFoundException              if the confRoot is not null and is not found in the YAML document
     * @throws InvalidTypeException           if the confRoot was not null and is invalid (i.e. if treated a value as a map)
     * @deprecated use {@link #resolveLayeredConfigs(String...)} or {@link #resolveMultiConfig(String...)} instead.
     */
    @Deprecated
    public static YAML resolveConfig(String configName, String confRoot) throws IOException {
        YAML rootMap = resolveMultiConfig(configName);

        if (confRoot == null) {
            return rootMap;
        }

        if (!rootMap.containsKey(confRoot)) {
            throw new NotFoundException("YAML configuration must contain the '" + confRoot + "' element", confRoot);
        } else {
            return rootMap.getSubMap(confRoot);
        }
    }

    /**
     * Parse the given configStream as a single YAML.
     * <p>
     * Note: This method merges the YAML config as-is: Any key-collisions are handled implicitly by keeping the latest
     * key-value pair. Sub-entries are not merged on key collisions, meaning that key-collisions at the root level
     * replaces the full tree under the key. References are supported with this method.
     * <p>
     * Extrapolation of values is enabled with this method. Use {@link #parse(InputStream, boolean)} to control this.
     * @param yamlStream YAML.
     * @return a YAML based on the given stream.
     */
    public static YAML parse(InputStream yamlStream) {
        return parse(yamlStream, true);
    }

    /**
     * Parse the given configStream as a single YAML.
     * <p>
     * Note: This method merges the YAML config as-is: Any key-collisions are handled implicitly by keeping the latest
     * key-value pair. Sub-entries are not merged on key collisions, meaning that key-collisions at the root level
     * replaces the full tree under the key. References are supported with this method.
     * @param yamlStream YAML.
     * @param extrapolateSystemProperties whether system properties should be extrapolated in values.
     *                                    Note that extrapolation cannot be turned off once enabled.
     * @return a YAML based on the given stream.
     */
    @SuppressWarnings({"StatementWithEmptyBody", "unused"})
    public static YAML parse(InputStream yamlStream, boolean extrapolateSystemProperties) {
        Object raw = new Yaml().load(yamlStream);
        if (raw instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>) raw;
            // Get a classcast exception here, and not someplace later https://stackoverflow.com/a/509288
            for (String s : map.keySet());
            for (Object o : map.values());

            YAML rootMap = new YAML(map, false);
            log.trace("Parsed YAML config stream with extrapolateSystemProperties={}", extrapolateSystemProperties);
            rootMap.setExtrapolate(extrapolateSystemProperties);
            return rootMap;
        } else {
            throw new IllegalArgumentException("The config resource does not evaluate to a valid YAML configuration.");
        }
    }

    /**
     * Parse the given Paths as a single YAML, effectively concatenating all paths.
     * It is possible to cross-reference between the individual paths.
     * Duplicate keys across files are handled by last-wins, with no merging of sub-entries.
     * <p>
     * Use {@link #resolveLayeredConfigs} for treating multiple YAML files as overlays.
     * <p>
     * Extrapolation of values is enabled with this method. Use {@link #parse(boolean, Path...)} to control this.
     *
     * @param yamlPaths paths to YAML Files.
     * @return a YAML based on the given paths.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if a yamlFile could not be located.
     * @throws AccessDeniedException if a resource at a yamlPath could not be read.
     */
    public static YAML parse(Path... yamlPaths) throws IOException {
        return parse(true, yamlPaths);
    }

    /**
     * Parse the given Paths as a single YAML, effectively concatenating all paths.
     * It is possible to cross-reference between the individual paths.
     * Duplicate keys across files are handled by last-wins, with no merging of sub-entries.
     * <p>
     * Use {@link #resolveLayeredConfigs} for treating multiple YAML files as overlays.
     *
     * @param extrapolateSystemProperties whether system properties should be extrapolated in values.
     *                                    Note that extrapolation cannot be turned off once enabled.
     * @param yamlPaths paths to YAML Files.
     * @return a YAML based on the given paths.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if a yamlFile could not be located.
     * @throws AccessDeniedException if a resource at a yamlPath could not be read.
     */
    public static YAML parse(boolean extrapolateSystemProperties, Path... yamlPaths) throws IOException {
        return parse(extrapolateSystemProperties, Arrays.stream(yamlPaths).map(Path::toFile).toArray(File[]::new));
    }

    /**
     * Parse the given Files as a single YAML, effectively concatenating all files.
     * It is possible to use cross references between the individual files.
     * Duplicate keys across files are handled by last-wins, with no merging of sub-entries.
     * <p>
     * Use {@link #resolveLayeredConfigs} for treating multiple YAML files as overlays.
     * <p>
     * Extrapolation of values is enabled with this method. Use {@link #parse(boolean, File...)} to control this.
     *
     * @param yamlFiles path to YAML Files.
     * @return a YAML based on the given stream.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if a yamlFile could not be located.
     * @throws AccessDeniedException if a resource at a yamlFile could not be read.
     */
    public static YAML parse(File... yamlFiles) throws IOException {
        return parse(true, yamlFiles);
    }

    /**
     * Parse the given Files as a single YAML, effectively concatenating all files.
     * It is possible to use cross references between the individual files.
     * Duplicate keys across files are handled by last-wins, with no merging of sub-entries.
     * <p>
     * Use {@link #resolveLayeredConfigs} for treating multiple YAML files as overlays.
     *
     * @param extrapolateSystemProperties whether system properties should be extrapolated in values.
     *                                    Note that extrapolation cannot be turned off once enabled.
     * @param yamlFiles path to YAML Files.
     * @return a YAML based on the given stream.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if a yamlFile could not be located.
     * @throws AccessDeniedException if a resource at a yamlFile could not be read.
     */
    public static YAML parse(boolean extrapolateSystemProperties, File... yamlFiles) throws IOException {
        // Check is files can be read
        for (File yamlFile: yamlFiles) {
            if (!yamlFile.exists()) {
                throw new FileNotFoundException("The file '" + yamlFile + "' could not be found");
            }
            if (!yamlFile.canRead()) {
                throw new AccessDeniedException("The file '" + yamlFile + "' could not be read (check permissions)");
            }
        }

        List<InputStream> configs = null;
        try {
            // Convert to InputStreams
            configs = Arrays.stream(yamlFiles)
                    .map(YAML::openStream)
                    .collect(Collectors.toList());

            // Concatenate all InputStreams
            InputStream yamlStream = null;
            for (InputStream config : configs) {
                yamlStream = yamlStream == null ? config : new SequenceInputStream(yamlStream, config);
            }

            // Perform a single parse of the content
            return parse(yamlStream, extrapolateSystemProperties);
        } finally {
            if (configs != null) {
                for (InputStream config : configs) {
                    config.close();
                }
            }
        }
    }

    /**
     * Resolve the given YAML configurations and present a merged YAML from that.
     * <p>
     * Note: This method merges the YAML configs as-is: Any key-collisions are handled implicitly by keeping the latest
     * key-value pair in the stated configurations. Sub-entries are not merged on key collisions.
     * This means that references across configResources is possible.
     * Use {@link #resolveLayeredConfigs} for treating multiple YAML files as overlays.
     * <p>
     * Note 2: The resolver supports globbing so {@code /home/someone/myapp-conf/*.yaml} expands to all YAML-files
     * in the {@code myapp} folder. When globbing is used, the matching files in each glob are parsed in alphanumerical
     * order for that glob. The overall order is the given array of configResources
     * <p>
     * Extrapolation of values is enabled with this method.
     * Use {@link #resolveMultiConfig(boolean, String...)} to control this.
     *
     * @param configResources the names, paths or globs of the configuration files.
     * @return the configurations merged and parsed up as a tree represented as Map and wrapped as YAML.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if none of the given configResources could be resolved.
     * @throws AccessDeniedException if any of the resolved config files could not be read.
     * @see #resolveLayeredConfigs for alternative.
     */
    public static YAML resolveMultiConfig(String... configResources) throws IOException {
        return resolveMultiConfig(true, configResources);
    }

    /**
     * Resolve the given YAML configurations and present a merged YAML from that.
     * <p>
     * Note: This method merges the YAML configs as-is: Any key-collisions are handled implicitly by keeping the latest
     * key-value pair in the stated configurations. Sub-entries are not merged on key collisions.
     * This means that references across configResources is possible.
     * Use {@link #resolveLayeredConfigs} for treating multiple YAML files as overlays.
     * <p>
     * Note 2: The resolver supports globbing so {@code /home/someone/myapp-conf/*.yaml} expands to all YAML-files
     * in the {@code myapp} folder. When globbing is used, the matching files in each glob are parsed in alphanumerical
     * order for that glob. The overall order is the given array of configResources
     *
     * @param extrapolateSystemProperties whether system properties should be extrapolated in values.
     *                                    Note that extrapolation cannot be turned off once enabled.
     * @param configResources the names, paths or globs of the configuration files.
     * @return the configurations merged and parsed up as a tree represented as Map and wrapped as YAML.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if none of the given configResources could be resolved.
     * @throws AccessDeniedException if any of the resolved config files could not be read.
     * @see #resolveLayeredConfigs for alternative.
     */
    public static YAML resolveMultiConfig(boolean extrapolateSystemProperties, String... configResources) throws IOException {
        Path[] configPaths = Arrays.stream(configResources)
                .map(Resolver::resolveGlob).flatMap(Collection::stream)
                .toArray(Path[]::new);
        if (configPaths.length == 0) {
            throw new FileNotFoundException("No paths resolved from " + Arrays.toString(configResources));
        }
        return parse(extrapolateSystemProperties, configPaths);
    }

    /**
     * Resolve the given YAML configurations, merging key-value pairs from subsequent configs into the first one.
     * This is typically used to support easy overwriting of specific parts of a major configuration file.
     * This is shorthand for {@code resolveLayeredConfig(MERGE_ACTION.union, MERGE_ACTION.kee_extra, configResources}:
     * The values for duplicate keys in YAMLs are merged, lists and atomic values are overwritten with the values
     * from extra.
     * <p>
     * Note: As opposed to {@link #resolveMultiConfig(String...)} this approach does not allow for references
     * across configResources.
     * <p>
     * Note 2: The resolver supports globbing so {@code /home/someone/myapp-conf/*.yaml} expands to all YAML-files
     * in the {@code myapp} folder. When globbing is used, the matching files in each glob are parsed in alphanumerical
     * order for that glob. The overall order is the given array of configResources
     * <p>
     * Extrapolation of values is enabled with this method.
     * Use {@link #resolveLayeredConfigs(boolean, String...)} to control this.
     *
     * @param configResources the names, paths or globs of the configuration files.
     * @return the configurations merged and parsed up as a tree represented as Map and wrapped as YAML.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if none of the given configResources could be resolved.
     * @throws AccessDeniedException if any of the resolved config files could not be read.
     * @see #resolveMultiConfig for alternative.
     */
    public static YAML resolveLayeredConfigs(String... configResources) throws IOException {
        return resolveLayeredConfigs(true, configResources);
    }

    /**
     * Resolve the given YAML configurations, merging key-value pairs from subsequent configs into the first one.
     * This is typically used to support easy overwriting of specific parts of a major configuration file.
     * This is shorthand for {@code resolveLayeredConfig(MERGE_ACTION.union, MERGE_ACTION.kee_extra, configResources}:
     * The values for duplicate keys in YAMLs are merged, lists and atomic values are overwritten with the values
     * from extra.
     * <p>
     * Note: As opposed to {@link #resolveMultiConfig(String...)} this approach does not allow for references
     * across configResources.
     * <p>
     * Note 2: The resolver supports globbing so {@code /home/someone/myapp-conf/*.yaml} expands to all YAML-files
     * in the {@code myapp} folder. When globbing is used, the matching files in each glob are parsed in alphanumerical
     * order for that glob. The overall order is the given array of configResources
     * <p>
     * Note 3: System property extrapolation is not enabled by default. Call {@link #extrapolate(boolean)} for that.
     *
     * @param extrapolateSystemProperties whether system properties should be extrapolated in values.
     *                                    Note that extrapolation cannot be turned off once enabled.
     * @param configResources the names, paths or globs of the configuration files.
     * @return the configurations merged and parsed up as a tree represented as Map and wrapped as YAML.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if none of the given configResources could be resolved.
     * @throws AccessDeniedException if any of the resolved config files could not be read.
     * @see #resolveMultiConfig for alternative.
     */
    public static YAML resolveLayeredConfigs(boolean extrapolateSystemProperties, String... configResources)
            throws IOException {
        return resolveLayeredConfigs(
                MERGE_ACTION.union, MERGE_ACTION.keep_extra, extrapolateSystemProperties, configResources);
    }

    /**
     * Resolve the given YAML configurations, merging key-value pairs from subsequent configs into the first one.
     * This is typically used to support easy overwriting of specific parts of a major configuration file.
     * <p>
     * Note: As opposed to {@link #resolveMultiConfig(String...)} this approach does not allow for references
     * across configResources.
     * <p>
     * Note 2: The resolver supports globbing so {@code /home/someone/myapp-conf/*.yaml} expands to all YAML-files
     * in the {@code myapp} folder. When globbing is used, the matching files in each glob are parsed in alphanumerical
     * order for that glob. The overall order is the given array of configResources
     * <p>
     * Extrapolation of values is enabled with this method.
     * Use {@link #resolveLayeredConfigs(MERGE_ACTION, MERGE_ACTION, boolean, String...)} to control this.
     *
     * @param configResources the names, paths or globs of the configuration files.
     * @return the configurations merged and parsed up as a tree represented as Map and wrapped as YAML.
     * @param defaultMA the general action to take when a key collision is encountered. Also used for maps (YAMLs).
     *                  Typically this will be {@link MERGE_ACTION#union}.
     * @param listMA    the action to take when a key collision for a list is encountered.
     *                  Typically this will be {@link MERGE_ACTION#union}.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if none of the given configResources could be resolved.
     * @throws AccessDeniedException if any of the resolved config files could not be read.
     * @see #resolveMultiConfig for alternative.
     */
    public static YAML resolveLayeredConfigs(MERGE_ACTION defaultMA, MERGE_ACTION listMA, String... configResources)
            throws IOException {
        return resolveLayeredConfigs(defaultMA, listMA, true, configResources);
    }

    /**
     * Resolve the given YAML configurations, merging key-value pairs from subsequent configs into the first one.
     * This is typically used to support easy overwriting of specific parts of a major configuration file.
     * <p>
     * Note: As opposed to {@link #resolveMultiConfig(String...)} this approach does not allow for references
     * across configResources.
     * <p>
     * Note 2: The resolver supports globbing so {@code /home/someone/myapp-conf/*.yaml} expands to all YAML-files
     * in the {@code myapp} folder. When globbing is used, the matching files in each glob are parsed in alphanumerical
     * order for that glob. The overall order is the given array of configResources
     *
     * @return the configurations merged and parsed up as a tree represented as Map and wrapped as YAML.
     * @param defaultMA the general action to take when a key collision is encountered. Also used for maps (YAMLs).
     *                  Typically this will be {@link MERGE_ACTION#union}.
     * @param listMA    the action to take when a key collision for a list is encountered.
     *                  Typically this will be {@link MERGE_ACTION#union}.
     * @param extrapolateSystemProperties whether system properties should be extrapolated in values.
     *                                    Note that extrapolation cannot be turned off once enabled.
     * @param configResources the names, paths or globs of the configuration files.
     * @throws IOException if a configuration could not be fetched.
     * @throws FileNotFoundException if none of the given configResources could be resolved.
     * @throws AccessDeniedException if any of the resolved config files could not be read.
     * @see #resolveMultiConfig for alternative.
     */
    public static YAML resolveLayeredConfigs(MERGE_ACTION defaultMA, MERGE_ACTION listMA,
                                             boolean extrapolateSystemProperties, String... configResources)
            throws IOException {
        List<Path> configPaths = Arrays.stream(configResources)
                .map(Resolver::resolveGlob).flatMap(Collection::stream)
                .collect(Collectors.toList());
        if (configPaths.isEmpty()) {
            throw new FileNotFoundException("No paths resolved from " + Arrays.toString(configResources));
        }
        YAML compound = new YAML();
        for (Path configPath: configPaths) {
            // Important to wait with extrapolation af paths might cross files
            compound = compound.merge(YAML.parse(false, configPath), defaultMA, listMA);
        }
        compound.setExtrapolate(extrapolateSystemProperties);
        return compound;
    }

    private static InputStream openStream(File file) {
        try {
            return file.toPath().toUri().toURL().openStream();
        } catch (IOException e) {
            throw new RuntimeException("IOException opening stream for '" + file + "'", e);
        }
    }

    /**
     * Merges the extra YAML into this YAML.
     * <p>
     * The merge uses {@link MERGE_ACTION#union} aka extra-wins: The values for duplicate keys in YAMLs are merged,
     * lists and atomic values are overwritten with the values from extra.
     * <p>
     * Shallow copying is used when possible, so updates to extra after the merge is strongly discouraged.
     * @param extra the YAML that will be added to this.
     * @return this YAML, updated with the values from extra.
     */
    public YAML merge(YAML extra) {
        return merge(this, extra, MERGE_ACTION.union, MERGE_ACTION.keep_extra);
    }

    /**
     * Merges the extra YAML into this YAML. In case of key collisions, the stated merge actions are taken.
     * <p>
     * Shallow copying is used when possible, so updates to extra after the merge is strongly discouraged.
     * @param extra the YAML that will be added to this.
     * @param defaultMA the general action to take when a key collision is encountered. Also used for maps (YAMLs)
     *                  Typically this will be {@link MERGE_ACTION#union}.
     * @param listMA    the action to take when a key collision for a list is encountered.
     *                  Typically this will be {@link MERGE_ACTION#union}.
     * @return this YAML, udpated with the values from extra.
     */
    public YAML merge(YAML extra, MERGE_ACTION defaultMA, MERGE_ACTION listMA) {
        return merge(this, extra, defaultMA, listMA);
    }

    /**
     * Merges the extra YAML into the base YAML.
     * The merge uses union/extra-wins: The values for duplicate keys in YAMLs are merged, lists and atomic values are
     * overwritten with the values from extra.
     * <p>
     * Shallow copying is used when possible, so updates to extra after the merge is strongly discouraged.
     * @param base the YAML that will be updated with the content from extra.
     * @param extra the YAML that will be added to base.
     * @return the updated base YAML.
     */
    public static YAML merge(YAML base, YAML extra) {
        return merge(base, extra, MERGE_ACTION.union, MERGE_ACTION.keep_extra);
    }

    /**
     * Merges the extra YAML into the base YAML. In case of key collisions, the stated merge actions are taken.
     * <p>
     * Shallow copying is used when possible, so updates to extra after the merge is strongly discouraged.
     * <p>
     * The available MERGE_ACTIONs are<br>
     * union: Duplicate maps are merged, lists are concatenated, atomics are overwritten by last entry.<br>
     * keep_base: Duplicate maps, lists and atomics are ignored.<br>
     * keep_extra: Duplicate maps, lists and atomics are overwrittes, so that the last encounterd key-value pair wins.<br>
     * fail: Duplicate maps, lists and atomics throws an exception.<br>
     *
     * @param base the YAML that will be updated with the content from extra.
     * @param extra the YAML that will be added to base.
     * @param defaultMA the general action to take when a key collision is encountered. Also used for maps (YAMLs)
     *                  Typically this will be {@link MERGE_ACTION#union}.
     * @param listMA    the action to take when a key collision for a list is encountered.
     *                  Typically this will be {@link MERGE_ACTION#union}.
     * @return the updated base YAML.
     */
    public static YAML merge(YAML base, YAML extra, MERGE_ACTION defaultMA, MERGE_ACTION listMA) {
        base.substitutors = null; // Clear existing substitutors. They will be re-created for the merged YAML
        YAML merged = (YAML)mergeEntry("", base, extra, defaultMA, listMA);
        return base.isExtrapolating() ? merged.extrapolateAll() : merged;
    }

    @SuppressWarnings("unchecked")
    private static Object mergeEntry(
            String path, Object base, Object extra, MERGE_ACTION defaultMA, MERGE_ACTION listMA) {
        final String pre = "Configuration incompatibility at " + path;

        if (extra instanceof LinkedHashMap) { // YAML is a LinkedHashMap
            if (!(base instanceof LinkedHashMap)) {
                throw new IllegalArgumentException(
                        pre + ": Attempting to merge value type " + extra.getClass() + " to type " + base.getClass());
            }

            LinkedHashMap<Object, Object> bYAML = (LinkedHashMap<Object, Object>) base;
            LinkedHashMap<Object, Object> eYAML = (LinkedHashMap<Object, Object>) extra;
            eYAML.forEach((key, value) -> {
                mergeValueToYAML(path, bYAML, key, value, defaultMA, listMA);
            });
            return base;
        }

        if (extra instanceof List) {
            if (!(base instanceof List)) {
                throw new IllegalArgumentException(
                        pre + ": Attempting to merge value type " + List.class + " to type " + base.getClass());
            }
            switch (listMA) {
                case fail: throw new IllegalArgumentException(
                        pre + ": Duplicate keys with list merge action " + MERGE_ACTION.fail);
                case keep_base: return base;
                case keep_extra: return extra;
                case union:
                    ((List<Object>)base).addAll((List<Object>)extra);
                    break;
                default: throw new UnsupportedOperationException("Unknown merge action for list '" + defaultMA + "'");
            }
            return base;
        }

        // When it's not a map or a list we don't care about type
        switch (defaultMA) {
            case fail: throw new IllegalArgumentException(
                    pre + ": Duplicate keys with merge action " + MERGE_ACTION.fail);
            case keep_base: return base;
            case keep_extra: return extra;
            case union: return extra; // TODO: Should we do something else here? Make a type-aware merger? Fail?
            default: throw new UnsupportedOperationException("Unknown merge action '" + defaultMA + "'");
        }
    }

    private static void mergeValueToYAML(String path, LinkedHashMap<Object, Object> base, Object key, Object value,
                                         MERGE_ACTION defaultMA, MERGE_ACTION listMA) {
        if (!base.containsKey(key)) {
            base.put(key, value);
            return;
        }
        base.put(key, mergeEntry(path + "." + key, base.get(key), value, defaultMA, listMA));
    }

    /**
     * Iterates the full YAML, performing extrapolation on all leafs.
     * See the Javadoc for {@link YAML} for a description of extrapolation.
     * <p>
     * Once enabled, extrapolation cannot be undone: Setting this to {@code false} after it has been set to {@code true}
     * will throw an exception.
     * @return this YAML, not a copy.
     */
    public YAML extrapolate(boolean extrapolateSystemProperties) {
        if (this.extrapolateSystemProperties && !extrapolateSystemProperties) {
            throw new IllegalArgumentException(
                    "Attempted to set extrapolateSystemProperties to false when it has already been set to true");
        }
        this.extrapolateSystemProperties = extrapolateSystemProperties;
        if (extrapolateSystemProperties) {
            extrapolateAll();
        }
        return this;
    }

    /**
     * Iterate the full YAML structure and perform extrapolation on all leaf elements,
     * if {@link #extrapolateSystemProperties} is {@code true}.
     * <p>
     * This is a destructive process as it replaces the Strings.
     * <p>
     * See the {@link YAML} documentation for the effect of extrapolation.
     * @return this YAML, not a copy.
     */
    private YAML extrapolateAll() {
        if (!extrapolateSystemProperties) {
            log.info("extrapolateAll called with extrapolateSystemProperties == false. " +
                    "No extrapolation will be performed");
            return this;
        }
        // This logging has been disabled as the YAML class has been rewritten to a recursive structure, which enables this by default.
        // log.info("Extrapolating all values with paths and system properties");
        extrapolateAll(this);
        return this;
    }

    /**
     * Iterate the full given {@code yaml} structure and perform extrapolation on all leaf elements.
     * <p>
     * Warning: This is a destructive process as it replaces the Strings that uses substitution.
     * <p>
     * See the {@link YAML} documentation for the effect of extrapolation.
     * @param yamlObject the starting point of the extrapolation.
     * @return the extrapolated element.                   
     */
    @SuppressWarnings("unchecked")
    private Object extrapolateAll(Object yamlObject) {
        // This does not use the visit method as it needs to modify the structure underway

        if (yamlObject instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>)yamlObject;
            map.replaceAll((key, value) -> extrapolateAll(value));
            return map;
        }

        if (yamlObject instanceof List) {
            List<Object> list = (List<Object>)yamlObject;
            list.replaceAll(this::extrapolateAll);
            return list;
        }

        if (yamlObject instanceof String) {
            return extrapolateGuessType(yamlObject);
        }

        // Only possible entries are maps, lists, null and scalars
        // Only String scalars are substituted
        return yamlObject;
    }

    /**
     * If the YAML extrapolates the current values of System.getProperties() in the values returned.
     * @return true if extrapolation is enabled.
     */
    public boolean isExtrapolating() {
        return extrapolateSystemProperties;
    }

    /**
     * Iterates the full YAML, performing extrapolation on all leafs.
     * See the Javadoc for {@link YAML} for a description of extrapolation.
     * <p>
     * Once enabled, extrapolation cannot be undone: Setting this to {@code false} after it has been set to {@code true}
     * will throw an exception.
     */
    public void setExtrapolate(boolean extrapolateSystemProperties) {
        extrapolate(extrapolateSystemProperties);
    }

    @Override
    public String toString(){
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndentWithIndicator(true);
        dumperOptions.setIndicatorIndent(2);
        dumperOptions.setProcessComments(true);
        // dumperOptions.setPrettyFlow(true);
        return new Yaml(dumperOptions).dumpAs(this, null, DumperOptions.FlowStyle.BLOCK);
    }
    public enum MERGE_ACTION {
        /**
         * Duplicate maps are merged, lists are concatenated, atomics are overwritten by last entry
         */
        union,
        /**
         * Duplicate maps, lists and atomics are ignored.
         */
        keep_base,
        /**
         * Duplicate maps, lists and atomics are overwrittes, so that the last encounterd key-value pair wins.
         */
        keep_extra,
        /**
         * Duplicate maps, lists and atomics throws an exception.
         */
        fail};

    /**
     * Extrapolates the given string, using lazy created {@link StringSubstitutor}s.
     * @param s the String to substitute.
     * @return s substituted.
     */
    private String substitute(String s) {
        String last = null;
        String current = s;
        int iterations = 0;
        // When a path-substitution return a value, that value must also be substituted etc.
        while (current != null && !current.equals(last)) {
            last = current;
            for (StringSubstitutor substitutor : getSubstitutors()) {
                current = current == null ? null : substitutor.replace(current);
            }
            if (++iterations == 10) {
                throw new IllegalStateException("Circular substitution chain detected for input value '" + s +
                        ". Value after 10 iterations is '" + current + "'");
            }
        }
        return current;
    }

    public synchronized List<StringSubstitutor> getSubstitutors() {
        if (substitutors == null) {
            substitutors = List.of(
                    // General prefix based
                    StringSubstitutor.createInterpolator(),
                    // Default to system property lookup
                    PathSubstitutor.createInterpolator(this),
                    new StringSubstitutor(StringLookupFactory.INSTANCE.systemPropertyStringLookup()).
                            setEnableUndefinedVariableException(true));
        }
        return substitutors;
    }

    /**
     * Substitutor that takes paths in the current YAML.
     * This substitutor only accepts paths to scalars and cannot be used as generally as YAML's references.
     * See {@link PathLookup} for details on syntax and use.
     */
    static class PathSubstitutor {
        /**
         * Create a path based substitutor backed by the given YAML.
         * @param yaml the YAML used for resolving paths.
         * @return a path substitutor.
         */
        public static StringSubstitutor createInterpolator(YAML yaml) {
            return new StringSubstitutor(new PathLookup(yaml)).
                    setVariablePrefix("${path"); // Default suffix is '}'
        }
    }

    /**
     * A path based StringLookup that operates on a given YAML.
     * The path is used directly with {@link YAML#get(Object)}.
     */
    private static class PathLookup implements StringLookup {
        private final YAML yaml;

        /**
         * @param yaml the YAML used for resolving paths.
         */
        public PathLookup(YAML yaml) {
            this.yaml = yaml;
        }

        @Override
        public String lookup(String key) {
            // For some reason keys starts with the delimiter character ':'
            key = key.substring(1);
            try {
                return yaml.get(key).toString();
            } catch (NotFoundException|NullPointerException e) {
                return null; // The framework will handle it if action needs to be taken
            }
        }
    }

}
