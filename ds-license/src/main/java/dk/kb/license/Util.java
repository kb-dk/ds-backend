package dk.kb.license;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import dk.kb.license.model.v1.RestrictedIdInputDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.storage.LicenseContent;


/**
 * Util class for various small methods used in ds-license. A bit messy and lots of different use-cases.
 * Some of the methods here are also called from the JSP page.
 */

public class Util {

	private static final Logger log = LoggerFactory.getLogger(Util.class);


	/**
	 * Defines a DateFormatter that is thread-safe 
	 * 
	 */
	private static final ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy" ,Locale.ROOT);		    
			df.setLenient(false);
			return df;
		}
	};

	// Used from JSP	
	/**
	 * Validate if a given group (by name) is in a list of {@link LicenseContent}s. 
	 * 
	 * @param groups The list of LicensContent each having a group name.
	 * @param groupName The group name to find
	 * @return True/False depending on if the groupName was matched
	 */
	public static boolean groupsContainGroupName(ArrayList<LicenseContent> groups, String groupName) {
		for (int i = 0; i < groups.size(); i++) {
			if (groups.get(i).getName().equalsIgnoreCase(groupName)) {
				return true;
			}
		}
		return false;
	}

 
	/**
	 * Validate if a list of {@link LicenseContent}s has a specific group
	 * and {@link dk.kb.license.storage.PresentationType} allowed
	 * <p>
	 * Method also used from JSP application.
	 * 
	 * @param groups The list of licenseContent that will be tested against.
	 * @param groupKey The group key that must match.
	 * @param presentationTypeKey The presentionTypeKey that must match (and also match the groupKey)
	 *  
	 * @return  True/False depending on if the groupName and presentationType was matched 
	 */	
	public static boolean groupsContainsGroupWithLicense(ArrayList<LicenseContent> groups, String groupKey, String presentationTypeKey) {
		for (int i = 0; i < groups.size(); i++) {
			if (groups.get(i).getName().equalsIgnoreCase(groupKey)) {
				for (int j = 0; j < groups.get(i).getPresentations().size(); j++) {
					if (groups.get(i).getPresentations().get(j).getKey().equalsIgnoreCase(presentationTypeKey)) {
						return true;
					}
				}
				return false;
			}
		}
		return false;
	}

	/**
	 * Validate that a date is of the format: dd-MM-yyyy.
	 * 
	 * @param dateFormat The string to validate is of form 'dd-MM-yyyy'.
	 * @return True or false depending if the date format is valid 
	 */
	public static boolean validateDateFormat(String dateFormat) {
		try {
			Date date = formatter.get().parse(dateFormat);
			String formated = formatter.get().format(date);
			if (!dateFormat.equals(formated)) { // Self check. Due to bug that dateformat accept 2013xxx or 2xxxx as year etc.
				return false;
			}
			return true;
		} catch (ParseException e) {
			log.info("Invalid dateformat entered:" + dateFormat);
			return false;
		}

	}
   
	/**
    * Format a date of form 'dd-MM-yyyy"' to milliseconds.
    * <p>
    * @param dateFormat The date to format.
    * @return The date in millis.
    * @throws IllegalArgumentException If date is not of the format 'dd-MM-yyyy'
    */
	public static long convertDateFormatToLong(String dateFormat) {
		boolean valid = validateDateFormat(dateFormat);
		if (!valid) {
			throw new IllegalArgumentException("Not valid date:" + dateFormat);
		}
		try {
			return formatter.get().parse(dateFormat).getTime();
		} catch (ParseException e) {
			throw new IllegalArgumentException("Could not format date:" + dateFormat);
		}
	}


	
	/**
	 * Called by JSP to color each new row differently (zebra).
	 * @return String Color value that is defined in the CSS.
	 */
	public static String getStyle(int row) {
		if (row % 2 == 0) {
			return "success";
		} else {
			return "error";
		}
	}

	/**
	 * Validates that the given object and all its fields are non-null.
	 *<p>
	 * This method checks if the provided object is null. If it is, the method throws an error.
	 * It then iterates through all declared fields of the object's class, checking each field's value.
	 * If any field's value is null, the method throws an error. If a field is an object (not a primitive),
	 * the method recursively validates that object's fields as well.
	 *
	 * @param object the object to validate for non-null fields.
	 */
	public static void validateNoNullFields(Object object) {
		AtomicReference<String> errorMessage = new AtomicReference<>("");
        boolean result;

		try {
            result = hasNoNullFields(object, errorMessage);
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            throw new InternalServiceException("An error occurred while checking object for null fields: ", e);
        }

        if (!result) {
			throw new InvalidArgumentServiceException(errorMessage.get());
		}
	}

	/**
	 * Checks if the specified object has no null fields.
	 * <p>
	 * This method serves as an entry point for validating that the given object
	 * and all its fields (including nested objects and collections) are non-null.
	 *
	 * @param obj the object to validate for null fields.
	 * @param errorMessage an {@link AtomicReference} to hold an error message if a null field is found.
	 * @return true if the object has no null fields; false otherwise.
	 */
	public static boolean hasNoNullFields(Object obj, AtomicReference<String> errorMessage)
			throws IntrospectionException, InvocationTargetException, IllegalAccessException {
		return hasNoNullFields(obj, new HashSet<>(), errorMessage);
	}

	/**
	 * Recursively checks if the specified object and its fields have no null values.
	 * <p>
	 * This private method performs a deep validation of the given object, checking
	 * each field for null values. It handles collections and maps, ensuring that
	 * all elements and entries are also validated. The method avoids cycles by
	 * keeping track of visited objects. If a null field is found, an appropriate
	 * error message is set in the provided {@link AtomicReference}.
	 *
	 * @param object the object to validate for null fields.
	 * @param visited a set of visited objects to avoid cycles during validation.
	 * @param errorMessage an {@link AtomicReference} to hold an error message if a null field is found.
	 * @return true if the object and all its fields are non-null; false otherwise.
	 */
	private static boolean hasNoNullFields(Object object, Set<Object> visited, AtomicReference<String> errorMessage)
			throws IntrospectionException, InvocationTargetException, IllegalAccessException {
		if (object == null) {
			errorMessage.set("Input object is null");
			return false;
		}

		// Avoid cycles
		if (visited.contains(object)) {
			return true;
		}
		visited.add(object);

		Class<?> objectClass = object.getClass();

		// Handle collections
		if (object instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) object;
            for (Object item : collection) {
				if (!hasNoNullFields(item, visited, errorMessage)){
					return false;
				}
			}
			return true;
		}

		// Handle maps
		if (object instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) object;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (!hasNoNullFields(entry.getKey(), visited, errorMessage) || !hasNoNullFields(entry.getValue(), visited, errorMessage)) {
					return false;
				}
			}
			return true;
		}

		// Handle primitives and their wrappers
		if (objectClass.isPrimitive() || objectClass.getName().startsWith("java.")) {
			return true;
		}

		// Handle nested objects without using forbidden reelection APIs
		for (PropertyDescriptor pd : Introspector.getBeanInfo(objectClass, Object.class).getPropertyDescriptors()) {
			var readMethod = pd.getReadMethod();
			if (readMethod != null) {
				Object value = readMethod.invoke(object);
				if (value == null) {
					errorMessage.set("Field '" + pd.getName() + "' in class " + objectClass.getName() + " is null.");
					return false;
				}

				if (!hasNoNullFields(value, visited, errorMessage)) {
					return false;
				}
			}
		}

		return true;
	}

    /**
     * Validates and formats the production ID in the given {@link RestrictedIdInputDto}.
     * <p>
     * This method removes leading zeros from the production ID and checks if the ID is
     * already in the correct format. If the production ID is 10 digits long and ends with
     * two zeros, it is considered valid and is set back on the input DTO. If not, a zero
     * is appended to the production ID before updating the input DTO.
     *
     * @param productionId the {@link RestrictedIdInputDto} containing the production ID to be validated.
     */
    public static void validateDrProductionIdFormat(String productionId) {

		if (!productionId.matches("\\d+")) {
			throw new InvalidArgumentServiceException("The input production ID: '" + productionId + "' should only contain digigts'");
		}

		if (productionId.length() <= 7){
			throw new InvalidArgumentServiceException("The input production ID: '" + productionId + "' should be at least 8 digigts");
		}

    }
}
