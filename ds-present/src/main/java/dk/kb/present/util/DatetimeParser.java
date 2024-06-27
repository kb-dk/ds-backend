package dk.kb.present.util;

import dk.kb.present.MalformedIOException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * TODO: Move to kb-util
 */
public class DatetimeParser {


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
            // Remove extra zeros from the time zone offset
            String trimmedDateTimeString = formattedDateTimeString.replaceAll("(\\+|-)(\\d{2})(\\d{2})(\\d{0,2})$", "$1$2$3");
            // Parse it.
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format, Locale.ROOT);
            return ZonedDateTime.parse(trimmedDateTimeString, dtf);
        } catch (DateTimeParseException e) {
            return tryRepairStrangeTZ(datetime, format);
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
            // Find index where the charater is
            int index = datetime.indexOf('+');
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
}
