package dk.kb.present.util;

import java.lang.reflect.Field;

/**
 * Reflection utilities
 * The methods of this class are necessary for testing. Earlier some UnitTests depended on an internal Mockito method,
 * which isn't available in newer versions of mockito.
 * {@link dk.kb.present.api.v1.impl.DsPresentApiServiceImplTest} makes use of this class.
 * <p/>
 * {@see  <a href="https://www.javadoc.io/doc/org.mockito/mockito-core/1.10.19/org/mockito/internal/util/reflection/FieldSetter.html">Mockitos old internal FieldSetter.</a>} <br/>
 * {@see  <a href="https://stackoverflow.com/a/72557617/12400491">Stackoverflow answer, which this class originates from.</a>}
 */
public class ReflectUtils {
    private ReflectUtils() {}

    public static void setField(Object object, String fieldName, Object value) {
        try {
            var field = object.getClass().getDeclaredField(fieldName);
            field.trySetAccessible();
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set " + fieldName + " of object", e);
        }
    }

    public static void setField(Object object, Field fld, Object value) {
        try {
            fld.trySetAccessible();
            fld.set(object, value);
        } catch (IllegalAccessException e) {
            String fieldName = null == fld ? "n/a" : fld.getName();
            throw new RuntimeException("Failed to set " + fieldName + " of object", e);
        }
    }
}
