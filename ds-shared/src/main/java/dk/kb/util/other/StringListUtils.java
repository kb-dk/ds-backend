package dk.kb.util.other;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StringListUtils {
    
    
    public static final Predicate<String> notNull = Predicate.not(Objects::isNull);
    public static final Predicate<String> notEmptyString = Predicate.not(String::isEmpty);
    public static final Predicate<String> isNotEmpty = notNull.and(notEmptyString);
    
    /**
     * Turn an iterator into a stream
     *
     * @param iterator the iterator
     * @param <T>      the the type of elements
     * @return A stream of the iterator elements in the same order
     */
    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false);
    }
    
    /**
     * Return the string or "" if null
     *
     * @param string the string
     * @return same or "" if null
     */
    public static String notNull(String string) {
        return Optional.ofNullable(string).orElse("");
    }
    
    /**
     * Return the first entry in the list or defaultValue if the list is empty
     *
     * @param list         the list
     * @param defaultValue the value if the list is empty
     * @return first value or default value if list is empty
     */
    public static String getFirst(List<String> list, String defaultValue) {
        if (list == null || list.isEmpty()) {
            return defaultValue;
        } else {
            return list.get(0);
        }
    }
    
    /**
     * If param is not null return that. Otherwise return the value from props[propKey]
     *
     * @param param   the value to return if notNull
     * @param props   the properties to get default from
     * @param propKey the key in props to get the default
     * @return param or props[propKey]
     */
    public static String getParamOrProps(String param, Properties props, String propKey) {
        if (param == null || param.isBlank()) {
            
            if (!props.contains(propKey)) {
                param = props.getProperty(propKey);
            } else {
                param = null;
            }
        }
        return param;
    }
    
    /**
     * Remove empty elements from the list and distinct the list.
     * <p>
     * The order of the list will be preserved, but elements will be removed.
     * <p>
     * if list is null, return an empty list.
     *
     * @param list the list to filter
     * @return a list without any empty elements
     */
    public static List<String> removeEmpties(List<String> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                   .filter(Objects::nonNull)
                   .filter(element -> !element.trim().isEmpty())
                   .distinct()
                   .collect(Collectors.toList());
    }
    
    /**
     * Remove empty elements from the list and distinct the list.
     * <p>
     * The order of the list will be preserved, but elements will be removed.
     * <p>
     * if list is null, return an empty list.
     *
     * @param list the list to filter
     * @return a list without any empty elements
     */
    public static List<String> removeEmpties(String... list) {
        return Arrays.stream(list)
                     .filter(Objects::nonNull)
                     .filter(number -> !number.trim().isEmpty())
                     .distinct()
                     .collect(Collectors.toList());
    }
    
    
    /**
     * Remove any entries in the list that are contained in any other entries in the list
     *
     * @param list the list to clean
     * @return a list without any elements that was a substring of any other element in the list
     */
    public static List<String> removeSubstrings(List<String> list) {
        List<String> coll = toModifiableList(list);
        
        Iterator<String> firstIterator = coll.iterator();
        while (firstIterator.hasNext()) {
            String s = firstIterator.next();
            for (String t : list) {
                if (s.equals(t)) {
                    continue;
                }
                if (t.contains(s)) {
                    firstIterator.remove();
                    break;
                }
            }
        }
        
        return coll;
    }
    
    
    /**
     * If value is null or blank, return defaultValue. Otherwise return value
     *
     * @param value        the value
     * @param defaultValue the default value
     * @return see above
     *         This is a specialisation of #orDefault(Object, Object), where we check not just null but also blank
     */
    public static String useDefaultIfNullOrEmpty(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        } else {
            return value;
        }
    }
    
    /**
     * If value is null, return defaultValue. Otherwise return value
     *
     * @param value        the value
     * @param defaultValue the default value
     * @param <T>          the type
     * @return see above
     */
    public static <T> T useDefaultIfNull(T value, T defaultValue) {
        return Optional.ofNullable(value).orElse(defaultValue);
    }
    
    
    /**
     * Return the elements as a MODIFIABLE list
     *
     * @param elements the elements
     * @param <T>      the type of elements
     * @return the elements as a ArrayList
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> toModifiableList(T... elements) {
        if (elements == null) {
            return new ArrayList<>();
        } else {
            return new ArrayList<T>(Arrays.asList(elements));
        }
    }
    
    private static final Set<Class<?>> definitelyMutableLists = Set.of(ArrayList.class,
                                                                       LinkedList.class);
    
    /**
     * Return a modifiable list with the same content, or the same list if it is already modifiable
     * <p>
     * This method is threadsafe,  IFF the input list is not modified by another thread during this method.
     * <p>
     * So if you have full control of your input list, this method is thread-safe. Therefore, ensure that
     * no other thread is accessing/modifying the list while we make it modifiable.
     * <p>
     * Please note that you are NOT guaranteed to get the same subtype of List back from this method. But
     * you cannot trust that you do NOT get the same type back
     *
     * @param list the list to get as modifiable
     * @param <E>  the type
     * @return the same list, if it is modifiable or a new Arraylist with the same content
     */
    public static <L extends List<E>, E> List<E> toModifiableList(@NotNull final L list) {
        if (list == null) {
            return null;
        }
        
        //If this list is one of the types we KNOW are mutable, just return it
        if (definitelyMutableLists.contains(list.getClass())) {
            return list;
        }
        
        //Size beforehand
        int orig_size = list.size();
        try {
            //First we check
            list.addAll(Collections.emptyList());
            //Not all immutable lists trigger on this... Collections.emptyList() is one do NOT fail on addAll
            
            boolean added;
            if (!list.isEmpty()) {
                //If the list is not empty, try to add the first element as a new last element
                added = list.add(list.get(0));
                //Remove the newly added element (orig_size is length = lastIndex+1 = index of newly added element)
                if (added) { //added is NOT always true, see org.apache.commons.collections4.list.SetUniqueList#add
                    list.remove(orig_size);
                }
            }
            
            //Otherwise just add null
            //Some collections do not like null, so better to try something else beforehand
            added = list.add(null);
            //We cannot just add whatever because we do not know the type of the list
            //Remove the newly added element (orig_size is length = lastIndex+1 = index of newly added element)
            if (added) { //added is NOT always true, see org.apache.commons.collections4.list.SetUniqueList#add
                list.remove(orig_size);
            }
            
            
            return list;
        } catch (Exception e) { //Is ANYTHING went wrong in the heuristics, make a new list
            //Use sublist to ensure that we do not include any elements added in heuristics
            //Sublist is (should be) cheap
            return new ArrayList<>(list.subList(0, orig_size));
        }
    }
    
    /**
     * Substring that allows for negative indexes and indexes beyound string length
     *
     * @param string     the string
     * @param startIndex the start index. If negative, count backwards from the end of the string.
     *                   If longer than the string, cap at string length.
     * @param endIndex   the end index. If negative, count backwards from the end of the string.
     *                   If longer than the string, cap at string length.
     * @return the substring
     */
    public static String substring(String string, int startIndex, int endIndex) {
        string = notNull(string);
        
        while (startIndex < 0) {
            if (string.length() > 0) {
                //Negative index so subtract it from the length of the string
                //This allows you to use a negative index to start a number of chars from the END of the string
                startIndex = startIndex + string.length();
            } else {
                //String is length 0 so break the loop
                startIndex = 0;
            }
        }
        
        while (endIndex < 0) {
            if (string.length() > 0) {
                //Negative index so subtract it from the length of the string
                //This allows you to use a negative index to start a number of chars from the END of the string
                endIndex = endIndex + string.length();
            } else {
                //String is length 0 so break the loop
                endIndex = 0;
            }
        }
        
        //If index is beyound the string length, set it to the the string length
        startIndex = Math.min(startIndex, string.length());
        endIndex   = Math.min(endIndex, string.length());
        
        if (startIndex > endIndex) {
            //If start is beyound end, substring as two strings
            //First part is from the start to the end_of_string
            String first = string.substring(startIndex);
            //Second part is from the start_of_string to endIndex
            String second = string.substring(0, endIndex);
            return first + second;
        } else {
            //StartIndex is no larger than endIndex so do a normal boring substring
            return string.substring(startIndex, endIndex);
        }
    }
    
    /**
     * Remove the middle part of the string, such that the result is no longer than maxLength
     *
     * @param string    the string to cut
     * @param maxLength the max length of the output
     * @return a string with the middle part replaced with ...
     */
    public static String truncateMiddle(String string, int maxLength) {
        
        string = notNull(string);
        if (string.length() <= maxLength) {
            return string;
        }
        String truncateString = "...";
        maxLength = maxLength - truncateString.length();
        
        int startStringLength = maxLength / 2;
        int endStringLength = Math.floorDiv(maxLength, 2);
        String startString = substring(string, 0, startStringLength);
        String endString = substring(string, string.length() - 1 - endStringLength, string.length());
        
        int numberRemovedChars = string.length() - maxLength;
        return startString + truncateString + endString;
        
    }
    
    /**
     * Cut the end of the string such that the result is no longer than maxLength
     *
     * @param string    the string
     * @param maxLength the max length
     * @return the string with the end replaced with ... if nessesary
     */
    public static String truncateEnd(String string, int maxLength) {
        string = notNull(string);
        if (string.length() <= maxLength) {
            return string;
        }
        String truncateString = "...";
        maxLength = maxLength - truncateString.length();
        
        String startString = substring(string, 0, maxLength);
        
        return startString + truncateString;
        
    }
}
