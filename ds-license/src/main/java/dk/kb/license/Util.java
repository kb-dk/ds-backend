package dk.kb.license;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import dk.kb.license.model.v1.RightsCalculationInputDto;
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
	 * @throws IllegalAccessException if the method cannot access a field due to its access modifier.
	 */
	public static void validateNonNull(Object object) throws IllegalAccessException {
		if (object == null) {
			throw new InvalidArgumentServiceException("Object is null");
		}

		Class<?> objClass = object.getClass();
		for (Field field : objClass.getDeclaredFields()) {
			field.setAccessible(true); // Access private fields
			Object value = field.get(object);

			if (value == null) {
				throw new InvalidArgumentServiceException("Field " + field.getName() + " is null");
			}

			// Validate that enums contains values.
			if (field.getType() == RightsCalculationInputDto.PlatformEnum.class){
				if (value.toString().isEmpty()){
					throw new InvalidArgumentServiceException("Field " + field.getName() + " is null");
				}
			}

			// If the field is an object, validate it as well. Except if it is a PlatformEnum as enums creates stack overflow errors.
			if (!isPrimitiveOrWrapper(field.getType()) && field.getType() != RightsCalculationInputDto.PlatformEnum.class) {
				validateNonNull(value);
			}
		}
	}

	/**
	 * Checks if the given class is a primitive type or its corresponding wrapper class.
	 *
	 * This method determines whether the specified class is a primitive type (e.g., int, boolean)
	 * or one of its corresponding wrapper classes (e.g., Integer, Boolean).
	 *
	 * @param clazz the class to check.
	 * @return true if the class is a primitive type or a wrapper class; false otherwise.
	 */
	private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		return clazz.isPrimitive() ||
				clazz == Boolean.class ||
				clazz == Character.class ||
				clazz == Byte.class ||
				clazz == Short.class ||
				clazz == Integer.class ||
				clazz == Long.class ||
				clazz == Float.class ||
				clazz == Double.class;
	}

}
