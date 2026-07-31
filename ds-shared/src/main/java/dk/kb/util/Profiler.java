/* $Id: Profiler.java,v 1.7 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.7 $
 * $Date: 2007/12/04 13:22:01 $
 * $Author: mke $
 *
 * The SB Util Library.
 * Copyright (C) 2005-2007  The State and University Library of Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.kb.util;


import de.thetaphi.forbiddenapis.SuppressForbidden;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.Locale;


/**
 * The State and University Library of Denmark.
 * User: te
 * Date: Dec 8, 2005
 * Time: 8:32:37 AM
 * CVS:  $Id: Profiler.java,v 1.7 2007/12/04 13:22:01 mke Exp $
 */

public class Profiler {
// TODO: Make the useCurrentSpeed handle situations where beat was called
// with a number larger than 1. This can be done by storing the current
// count along with the timestamps in the queue.

// TODO: Generic implementation of a ringbuffer and use that instead of the internal

    private long startTime = 0;
    private long beats = 0;
    private long expectedTotal = 0;
    private int bpsSpan = 10;

    /**
     * We use an array instead of a LinkedList because of speed - we might have several thousand updates / second.
     */
    private long[] queue = new long[10];
    private int queueStart = -1;
    private int queueEnd = -1;

    private boolean paused = false;
    private long pauseTime = 0; // The timestamp for pausing

    /**
     * Create a Profiler and set the internal timestamp to now.
     * The Profiler will start measuring immediately.
     */
    public Profiler() {
        reset();
    }

    /**
     * Create a Profiler and set the internal timestamp to now.
     * The Profiler will start measuring immediately.
     *
     * @param expectedTotal the expected total, used for ETA-calculations.
     */
    public Profiler(int expectedTotal) {
        reset();
        setExpectedTotal(expectedTotal);
    }

    /**
     * Create a Profiler and set the internal timestamp to now.
     * The Profiler will start measuring immediately.
     *
     * @param expectedTotal the expected total, used for ETA-calculations.
     * @param bpsSpan       the number of stored beat-timestamps.
     *                      See {@link #setBpsSpan(int)} for explanation.
     */
    public Profiler(int expectedTotal, int bpsSpan) {
        reset();
        setExpectedTotal(expectedTotal);
        setBpsSpan(bpsSpan);
    }

    /** Mutators */

    /**
     * Get the number of beats.
     *
     * @return the total number of beats
     */
    public long getBeats() {
        return beats;
    }

    /**
     * Sets the number of beats. Any negative value is converted to 0.
     *
     * @param value The number of beats in the queue
     */
    public synchronized void setBeats(long value) {
        beats = Math.max(0, value);
    }

    /**
     * Get the expected total number of beats.
     *
     * @return the expected total number of beats
     */
    public long getExpectedTotal() {
        return expectedTotal;
    }

    /**
     * Set the expected total number of beats. Values belov 0 are converted to 0.
     *
     * @param value the expected total number of beats.
     */
    public synchronized void setExpectedTotal(long value) {
        expectedTotal = Math.max(0, value);
    }

    /**
     * The bpsSpan is used for calculating the beats per second. The span is the maximum number of
     * beats that are remembered. A value of 0 disables bps updating.
     *
     * @return the bpsSpan
     */
    public int getBpsSpan() {
        return bpsSpan;
    }

    /**
     * The bpsSpan is used for calculating the beats per second. The span is the maximum number of
     * beats that are remembered. A value of 0 disables bps updating. A value of 1 is changed to 2,
     * as bps cannot be calculated from a list of length 1.
     *
     * @param value the bpsSpan
     */
    public synchronized void setBpsSpan(int value) {
        bpsSpan = Math.max(0, value);
        if (bpsSpan == 1) {
            bpsSpan = 2;
        }
        queue = new long[bpsSpan];
        queueStart = -1;
        queueEnd = -1;
        paused = false;
    }

    /**
     * Reset the ProgressFeedback, setting the internal timestamp to now, clearing the beats and the queue.
     */
    public synchronized void reset() {
        startTime = System.currentTimeMillis();
        beats = 0;
        queueStart = -1;
        queueEnd = -1;
        paused = false;
    }


    /** Updators */

    /**
     * Update ProgressFeedback with a single beat.
     *
     * @return the total number of beats
     */
    public long beat() {
        return beat(1);
    }

    /**
     * Update ProgressFeedback with a number of beats.
     *
     * @param increase the number of beats to add
     * @return the total number of beats
     */
    private synchronized long beat(long increase) {
        unpause();
        beats += increase;
        if (bpsSpan > 0) {
            queueEnd = ++queueEnd % bpsSpan;                                 // Move the end
            if (queueEnd == queueStart) {
                queueStart = ++queueStart % bpsSpan; // Move the start, if the end reached it
            }
            if (queueStart == -1) {
                queueStart = 0;                            // Set the start, if this is the first beat
            }
            queue[queueEnd] = System.currentTimeMillis();
        }
        return beats;
    }

    /**
     * Internal calculations.
     *
     * @return the size of the queue holding the most recent beats
     */
    public int queueSize() {
        if (queueStart == -1) {
            return 0;
        }
        if (queueStart == queueEnd) {
            return 1;
        }
        if (queueStart < queueEnd) {
            return queueEnd - queueStart + 1;
        }
        return bpsSpan + queueEnd - queueStart + 1;
    }

    /** Queries */

    /**
     * Convenience method for getBps(false).
     *
     * @return the number of beats per second averaged over the whole run.
     */
    public double getBps() {
        return getBps(false);
    }

    /**
     * Bps is the number of beats per second.
     *
     * @param useCurrentSpeed use the last bpsSpan beats to calculate the bps, thus giving a current speed estimate
     * @return the number of beats per second for the last bpsSpan, NaN if it is incalculable
     */
    public synchronized double getBps(boolean useCurrentSpeed) {
        if (useCurrentSpeed) {
            /* If there's no span, bps doesn't make sense */
            if (bpsSpan <= 0) {
                return Double.NaN;
            }

            int size = queueSize();
            /* If there's 1 or 0 timestamps in the queue, we can't calculate
             * a mean time */
            if (size <= 1) {
                return Double.NaN;
            }

            double seconds = (queue[queueEnd] - queue[queueStart]) / 1000.0;
            /* Remember to subtract 1 from the queue size, as we're looking
             * at the time spend between each beat */
            return (size - 1) / seconds;
        } else {
            if (beats == 0) {
                return Double.NaN;
            }
            return 1000.0 * beats / getSpendMilliseconds();
        }
    }

    /**
     * The amount of time since creation or reset.
     *
     * @return the number of milliseconds this ProgressFeedback has been running
     */
    public long getSpendMilliseconds() {
        return paused ? pauseTime - startTime :
               System.currentTimeMillis() - startTime;
    }

    /**
     * The amount of time since creation or reset.
     *
     * @return the spend time as a string
     */
    public String getSpendTime() {
        return millisecondsToString(getSpendMilliseconds());
    }

    /**
     * Helper method for providing human-readable time.
     *
     * @param ms milliseconds
     * @return human-readable time
     */
    public static String millisecondsToString(long ms) {
        int years = (int) Math.floor((double) ms / (365.0 * 24 * 60 * 60 * 1000));
        ms = (long) (ms % (365.0 * 24 * 60 * 60 * 1000.0));
        int days = (int) Math.floor((double) ms / (24 * 60 * 60 * 1000));
        ms = (long) (ms % (24 * 60 * 60 * 1000.0));
        int hours = (int) Math.floor((double) ms / (60 * 60 * 1000));
        ms = (long) (ms % (60 * 60 * 1000.0));
        int minutes = (int) Math.floor((double) ms / (60 * 1000));
        ms = (long) (ms % (60 * 1000.0));
        int seconds = (int) Math.floor((double) ms / 1000);
        int milliseconds = (int) (ms % 1000.0);

        StringWriter sw = new StringWriter();
        if (years > 0) {
            sw.write(Integer.toString(years));
            sw.write(years == 1 ? " year, " : " years, ");
        }
        if (years > 0 || days > 0) {
            sw.write(Integer.toString(days));
            sw.write(days == 1 ? " day, " : " days, ");
        }
        if (years > 0 || days > 0 || hours > 0) {
            sw.write(Integer.toString(hours));
            sw.write(hours == 1 ? " hour, " : " hours, ");
        }
        if (years > 0 || days > 0 || hours > 0 || minutes > 0) {
            sw.write(Integer.toString(minutes));
            sw.write(minutes == 1 ? " minute, " : " minutes, ");
        }
        if (years > 0 || days > 0 || hours > 0 || minutes > 0 || seconds > 0) {
            sw.write(Integer.toString(seconds));
            sw.write(seconds == 1 ? " second, " : " seconds, ");
        }
        sw.write(Integer.toString(milliseconds));
        sw.write(" ms");

        return sw.toString();
    }

    /**
     * The estimated number of milliseconds, before the task is completed.
     * Requires expectedTotal to be more than 0.
     *
     * @param useCurrentSpeed use the bpsSpan for the estimate, thus basing it on current speed
     * @return milliseconds before the task should be completed. -1 if it cannot be calculated
     */
    public long getTimeLeft(boolean useCurrentSpeed) {
        double bps = getBps(useCurrentSpeed);

//        System.out.println("Expected: " + expectedTotal + " beats: " + beats + " bps: " + bps);

        if (expectedTotal == 0) {
            return -1;
        }
        if (expectedTotal <= beats) {
            return 0;
        }
        return (int) ((expectedTotal - beats) / bps * 1000.0);
    }

    /**
     * Request the time left using GetTimeLeft and format the result using millisecondstoString.
     * This is a convenience method.
     *
     * @param useCurrentSpeed use the bpsSpan for the estimate, thus basing it on current speed
     * @return human-readable time left before the task should be completed. "N/A" if incalculable, "None" the task should be completed at call-time
     */
    public String getTimeLeftAsString(boolean useCurrentSpeed) {
        long timeLeft = getTimeLeft(useCurrentSpeed);
        if (timeLeft == -1) {
            return "N/A";
        }
        if (timeLeft == 0) {
            return "None";
        }
        return millisecondsToString(timeLeft);
    }

    /**
     * Calculate the estimated time of arrival.
     *
     * @param useCurrentSpeed use the bpsSpan for the estimate, thus basing it on current speed
     * @return the extimated time of arrival, null if it is incalculable
     */
    @SuppressForbidden
    public Calendar getETA(boolean useCurrentSpeed) {
        long timeLeft = getTimeLeft(useCurrentSpeed);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        if (timeLeft == -1) {
            return null;
        }
        if (timeLeft == 0) {
            return calendar;
        }

        calendar.setTimeInMillis(calendar.getTimeInMillis() + timeLeft);
        return calendar;
    }

    /**
     * Request the ETA using getETA and format the result as YYYY-MM-DD HH:mm:SS.
     *
     * @param useCurrentSpeed use the bpsSpan for the estimate, thus basing it on current speed
     * @return the extimated time of arrival, "N/A" if it is incalculable
     */
    public String getETAAsString(boolean useCurrentSpeed) {
        Calendar eta = getETA(useCurrentSpeed);
        if (eta == null) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%1$tF %1$tT", eta);
    }

    /**
     * Pause the Profiler. Call {@link #unpause} in order to continue.
     * Calling {@link #beat} will automatically unpause.
     *
     * Note: The larger the {@link #bpsSpan}, the longer it takes to unpause.
     */
    public synchronized void pause() {
        if (paused) {
            return;
        }
        pauseTime = System.currentTimeMillis();
        paused = true;
    }

    public synchronized void unpause() {
        if (!paused) {
            return;
        }
        long inactiveTime = System.currentTimeMillis() - pauseTime;
        if (inactiveTime > 0) {
            for (int i = 0; i < queue.length; i++) {
                queue[i] += inactiveTime;
            }
            startTime += inactiveTime;
        }
        paused = false;
    }

}