package dk.kb.util.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.ws.rs.ext.ContextResolver;
import java.io.File;
import java.io.IOException;


public class JSON implements ContextResolver<ObjectMapper> {
    private ObjectMapper mapper;
    
    /**
     * Create a new JSON converter
     */
    public JSON() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        
        //Fixes errors like
        // java.lang.RuntimeException: com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
        //  Java 8 date/time type `java.time.LocalDateTime` not supported by default: add Module
        //  "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
        // When trying to serialize or deserialize java 8 dates
        mapper.registerModule(new JavaTimeModule());
        
        StdDateFormat fmt = new StdDateFormat();
        fmt.withColonInTimeZone(true);
        mapper.setDateFormat(fmt);
    }
    
    /**
     * Serialise the given java object as json. This is equivalent to calling #toJson(object,true)
     * @param object the java object to serialise
     * @return as json
     * @see #toJson(Object, boolean)
     */
    public static String toJson(Object object) {
        return toJson(object, true);
    }
    
    /**
     * Serialise the given java object as json.
     * @param object the java object to serialise
     * @param indent if true, the resulting string will include linebreaks and indents. If false, the resulting
     *               string will be just one line, as suitable for jsonLines documents
     * @param <T> the type of object
     * @return the object serialised as a string
     */
    public static <T> String toJson(T object, boolean indent) {
        
        if (object == null) {
            return "";
        }
        JSON json = new JSON();
        ObjectMapper mapper = json.getContext(object.getClass());
        
        
        if (indent) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        } else {
            mapper.disable(SerializationFeature.INDENT_OUTPUT);
        }
        
        
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Create java object from json string
     * @param jsonString the json string
     * @param type the Class of java object to create
     * @param <T> the type of java object to create
     * @return a java object of type Type
     */
    public static <T> T fromJson(String jsonString, Class<T> type) {
        JSON json = new JSON();
        ObjectMapper mapper = json.getContext(type);
        
        try {
            return mapper.readValue(jsonString, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Read json file and inflate it as a java object
     * @param file the file to read
     * @param type the class of the object
     * @param <T> the class of the object
     * @return the object
     */
    public static <T> T fromJson(File file, Class<T> type) {
        JSON json = new JSON();
        ObjectMapper mapper = json.getContext(type);
        
        try {
            return mapper.readValue(file, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
   
    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
    
   
}
