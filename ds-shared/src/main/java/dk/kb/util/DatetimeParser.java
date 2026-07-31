package dk.kb.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Datetime Parser used to parse wrongly formatted data from observed formats from DOMS. Examples of formats, that are
 * fixed by this class can be seen in the test class {@code DatetimeParserTest}.
 */
public class DatetimeParser {
    private static final Logger log = LoggerFactory.getLogger(DatetimeParser.class);



    /**
     * This method parses a String, datetime, to a ZonedDateTime object with pattern of String, format.
     * Ex. parseStringToZonedDateTime("2024-02-23T08:55:00+0100", "yyyy-MM-dd'T'HH:mm:ssXXXX")
     *
     * @param datetime the datetime String to parse to a ZoneDatetime object
     * @param format   the pattern of the datetime String, used to create a {@link DateTimeFormatter} object
     * @return {@link ZonedDateTime} object
     * @see ZonedDateTime
     * @see DateTimeFormatter
     */
    public static ZonedDateTime parseStringToZonedDateTime(String datetime, String format) throws MalformedIOException {
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format, Locale.ROOT);
            return ZonedDateTime.parse(datetime, dtf);
        } catch (DateTimeParseException e) {
            return tryRepairZonedDateTime(datetime, format);
        }
    }

    /**
     * Tries to convert a time that don't fullfills the official datetimeformatter pattern
     *
     * @param datetime time to be converted
     * @param format   The datetimeformatter pattern
     * @return Returns the converted datetime. If it fails a DateTimeParseException is thrown
     * which then will be logged in the error file
     */
    private static ZonedDateTime tryRepairZonedDateTime(String datetime, String format) throws MalformedIOException {
        try {
            // Remove spaces
            datetime = datetime.replace(" ", "");
            // Remove any brackets
            datetime = datetime.replace("[", "").replace("]", "");
            // Insert the missing "T" between date and time components
            String formattedDateTimeString = datetime.replaceAll("(\\d{4}-\\d{2}-\\d{2})(\\d{2}:\\d{2}:\\d{2})", "$1T$2");
            // Add seconds to timestamp if missing.
            String datetimeWithCorrectSeconds = getDateTimeWithCorrectSeconds(formattedDateTimeString);
            // Remove extra zeros from the time zone offset
            String trimmedDateTimeString = datetimeWithCorrectSeconds.replaceAll("(\\+|-)(\\d{2})(\\d{2})(\\d{0,2})$", "$1$2$3");

            // Parse it.
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format, Locale.ROOT);
            return ZonedDateTime.parse(trimmedDateTimeString, dtf);
        } catch (DateTimeParseException e) {
            return tryRepairStrangeTZ(datetime, format);
        }
    }

    /**
     * Validate that the timestamp in a datetime string has the correct size and format.
     * The string should be 8 characters long and contain exactly two ':' chars.
     * @param datetime to validate
     * @return an updated datetime string. Where seconds have been added to the timestamp if they were missing.
     * Otherwise, return the original datetime-string.
     */
    private static String getDateTimeWithCorrectSeconds(String datetime) {
        String timestamp = getTimestamp(datetime);
        if (timestamp.length() == 8) {
            return datetime;
        } else {
            if (!containsColonTwice(timestamp)) {
                String timestampWithSeconds = timestamp + ":00";
                return datetime.replace(timestamp, timestampWithSeconds);
            } else {
                throw new RuntimeException("The timestamp contains two ':' but the length is: " + timestamp.length());
            }
        }
    }

    /**
     * get the timestamp from a datetime by extracting everything between T and either Z or +.
     * @param datetimeString to return timestamp from.
     * @return the timestamp from the input datetime.
     */
    private static String getTimestamp(String datetimeString) {
        // matches on everything after T and before either + or Z
        String timestampPattern = "T(.*?)([Z|\\+])";
        Pattern pattern = Pattern.compile(timestampPattern);

        // Match against input datetime string
        Matcher matcher1 = pattern.matcher(datetimeString);
        if (matcher1.find()) {
            return matcher1.group(1);
        } else {
            throw new RuntimeException("Could not extract timezone from datetime string: " + datetimeString);
        }
    }

    /**
     * Tries to convert a date with wrong number of zeroes in the timezone
     *
     * @param datetime time to be converted
     * @param format   The datetimeformatter pattern
     * @return Returns the converted datetime. If it fails a DateTimeParseException is thrown
     * which then will be logged in the error file
     */
    private static ZonedDateTime tryRepairStrangeTZ(String datetime, String format) throws MalformedIOException {
        try {
            // Find index where the character is
            int index = datetime.indexOf('+');

            if (index == -1 && datetime.endsWith("Z")){
                log.debug("Datetime '{}' is in UTC time", datetime);
                return ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_INSTANT);
            }

            // Extract the content after the '+' e.g. the timezone part
            String timeZoneOffset = datetime.substring(index + 1);
            // remove all zeros, so only the zimezone value remains
            String tzValue = timeZoneOffset.replace("0", "");
            // Extract the part without timezone
            String dateTimeWithoutTZ = datetime.substring(0, datetime.indexOf('+'));
            // Create Timezone in correct format
            String prefix = "+0";
            if (tzValue.length() != 1) {
                prefix = "+";
            }
            // Create the date in correct format
            datetime = dateTimeWithoutTZ + prefix + tzValue + "00";
            // Parse the date-time string
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format, Locale.ROOT);
            return ZonedDateTime.parse(datetime, formatter);
        } catch (RuntimeException e) {
            throw new MalformedIOException("Could not parse/repair date: " + datetime);
        }
    }


    /**
     * Validate that a string, often a timestamp, contains two colons.
     * @param input to validate.
     * @return true if the input string contains two colons. Otherwise, return false.
     */
    public static boolean containsColonTwice(String input) {
        // Find the first occurrence of ':'
        int firstColonIndex = input.indexOf(':');
        // If no ':' is found, return false
        if (firstColonIndex == -1) {
            return false;
        }
        // Find the second occurrence of ':', starting after the first one
        int secondColonIndex = input.indexOf(':', firstColonIndex + 1);
        // If the second ':' is found, return true; otherwise, return false
        return secondColonIndex != -1;
    }
}

