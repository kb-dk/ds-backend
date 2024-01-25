package dk.kb.license;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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

}
