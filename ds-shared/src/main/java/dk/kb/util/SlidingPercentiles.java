package dk.kb.util;

import dk.kb.util.reader.CircularIntBuffer;

import java.util.Arrays;

/**
 * Sliding window percentile calculator (this includes arithmetic mean) with min and max.
 *
 * A window size is defined and the calculator is fed a stream of values. It will automatically remove older entries.
 * Care has been taken to optimize performance by using int[], binary search and array copy to maintain the internal
 * structure. The intended use case is for windows that are well within level 2/3-cache.
 *
 * This implementation is not thread safe.
 */
// TODO: Running calculation of average and deviation
public class SlidingPercentiles {

    public static final double MEDIAN = 0.5d;

    /**
     * The maximum size of the windows that holds collected data.
     */
    private final int maxWindow;
    /**
     * The current size of the window.
     */
    private int windowSize = 0;

    /**
     * If {@link #continuousSort} this contains at all times the collected values, sorted by natural order.
     * If continuousSort is false, this acts as a buffer when requesting percentiles.
     */
    private final int[] sortedValues;

    private final boolean continuousSort;
    private static final boolean DEFAULT_CONTINUOUS_SORT = true;


    /**
     * The collected values, ordered as delivered.
     */
    private final CircularIntBuffer values;
    private boolean sortedDirty = false;

    private long sum = 0;

    /**
     * Shorthand for {@code SlidingPercentiles(windowSize, true)}. The slider is thus optimized for frequent
     * polling for percentiles.
     * @param windowSize the maximum amount of values that are remembered.
     */
    public SlidingPercentiles(int windowSize) {
        this(windowSize, DEFAULT_CONTINUOUS_SORT);
    }

    /**
     * @param windowSize     the maximum amount of values that are remembered.
     * @param continuousSort if true, the structure is optimized for frequent polling of percentiles.
     *                       If false, calculation of percentiles is more costly but updates are cheaper.
     */
    public SlidingPercentiles(int windowSize, boolean continuousSort) {
        this.maxWindow = windowSize;
        sortedValues = new int[windowSize];
        values = new CircularIntBuffer(windowSize, windowSize);
        this.continuousSort = continuousSort;
    }

    /**
     * Add the given value to the window, maintaining internal invariants. If the maximum size for the window has been
     * reached, this involves an eviction of the oldest value.
     *
     * If {@link #continuousSort} is true, insertion time is O(n); if false, insertion time is O(1).
     * {@code System#arraycopy} is used for shifting values when continuousSort is true.
     * @param value will be added to the sliding window.
     */
    public void add(int value) {
        sum += value;
        if (size() >= maxWindow) {
            sum -= pop();
        }
        if (continuousSort) {
            int insertionPoint = Arrays.binarySearch(sortedValues, 0, windowSize, value);
            insertionPoint = insertionPoint >= 0 ? insertionPoint : -1 * (insertionPoint +1);
            System.arraycopy(sortedValues, insertionPoint, sortedValues, insertionPoint+1, windowSize-insertionPoint);
            sortedValues[insertionPoint] = value;
        } else {
            sortedDirty = true;
        }
        values.put(value);
        windowSize++;
    }

    /**
     * Removed the oldest received value from the window.
     *
     * If {@link #continuousSort} is true, removal time is O(n); if false, removal time is O(1).
     * {@code System#arraycopy} is used for shifting values when continuousSort is true.
     * @return the oldest value in the window.
     */
    private int pop() {
        final int value = values.take();
        if (continuousSort) {
            int removalPoint = Arrays.binarySearch(sortedValues, 0, windowSize, value);
            if (removalPoint < 0) {
                throw new IllegalStateException("The value " + value + " did not exist in the window");
            }
            if (removalPoint != size()-1) {
                System.arraycopy(
                        sortedValues, removalPoint+1, sortedValues, removalPoint, sortedValues.length-removalPoint);
            }
        } else {
            sortedDirty = true;
        }
        windowSize--;
        return value;
    }

    public int size() {
        return windowSize;
    }

    public void clear() {
        windowSize = 0;
        values.clear();
        sortedDirty = false;
    }

    /**
     * @param reuse if not null and if reuse.length == size(), the sorted values will be copied into reuse and
     *              returned. Else a new int[] is created.
     * @return a copy of the values in the window, sorted by natural order.
     *         Response time is O(n) if {@link #continuousSort} is true, else O(n*log(n))
     */
    public int[] getSortedValues(int[] reuse) {
        if (!continuousSort && sortedDirty) {
            updateSortedValues();
        }
        final int[] result = reuse != null && reuse.length == windowSize ? reuse : new int[windowSize];
        System.arraycopy(sortedValues, 0, result, 0, windowSize);
        return result;
    }

    /**
     * Shorthand for {@code getPercentile(SlidingPercentiles.MEDIAN)};
     * @return the median for the collected values.
     */
    public double getMedian() {
        return getPercentile(MEDIAN);
    }
    /**
     * Returns the given percentile, interpolated between two values if the percentile is not a perfect split.
     * @param percent the wanted percentile as a number from 0 to 1, both inclusive.
     * @return the calculated percentile in O(1) time if {@link #continuousSort} is true, else O(n*log(n)).
     */
    public double getPercentile(double percent) {
        if (values.length() == 0) {
            return 0;
        }
        if (!continuousSort && sortedDirty) {
            updateSortedValues();
        }
        double pos = values.length() * percent - 1;
        if (pos < 0) {
            return sortedValues[0];
        }
        if (pos >= values.length()-1) {
            return sortedValues[values.length()-1];
        }
        int valLeft = sortedValues[(int)pos];
        int valRight = sortedValues[((int)pos)+1];
        double fraction = pos - ((int)pos);
        return valLeft + (valRight - valLeft) * fraction;
    }

    void updateSortedValues() {
        values.copy(sortedValues);
        Arrays.sort(sortedValues, 0, values.size());
        sortedDirty = false;
    }

    /**
     * @return {code sum/count} aka the unweighted mean or 0 if there are no values. Response time is O(1).
     */
    public double getAverage() {
        return values.length() == 0 ? 0 : ((double)sum)/values.length();
    }

    /**
     * @return the underlying sorted value array. Might contain unassigned entry points.
     * Might be unsorted; call {@link #updateSortedValues()} to sort. Expert use only.
     */
    int[] getSortedValuesRaw() {
        values.read();
        return sortedValues;
    }
}
