/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Structure for timing-instrumentation of other code. Intended for always-enabled use as all methods are
 * sought to be lightweight.
 * <p>
 * Usage: Create a root instance and optionally add children with {@link #getChild}.
 * <p>
 * Mixed thread safety: Methods are thread safe, unless the JavaDoc says otherwise.
 */
// TODO: Consider adding a toJSON
public class Timing {
    public enum STATS {
        name, subject, ms, ns, updates, ms_updates, ns_updates, updates_s, min_ms, min_ns, max_ms, max_ns,
        last_ms, last_ns, utilization,
    }
    public static final STATS[] MS_STATS = new STATS[]{
            STATS.name, STATS.subject, STATS.ms, STATS.updates, STATS.ms_updates, STATS.updates_s,
            STATS.min_ms, STATS.max_ms, STATS.utilization
    };
    public static final STATS[] MS_STATS_SIMPLE = new STATS[]{
            STATS.name, STATS.subject, STATS.ms, STATS.updates, STATS.ms_updates, STATS.updates_s,
            STATS.max_ms
    };
    public static final STATS[] NS_STATS = new STATS[]{
            STATS.name, STATS.subject, STATS.ns, STATS.updates, STATS.ns_updates, STATS.updates_s,
            STATS.min_ns, STATS.max_ns, STATS.utilization
    };

    private final String name;
    private final String subject;
    private final String unit;
    private STATS[] showStats;
    private final long objectCreation = System.nanoTime();

    private long lastStart = System.nanoTime();
    private final AtomicLong lastNS = new AtomicLong(0);
    private final AtomicLong minNS = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxNS = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong spendNS = new AtomicLong(0);
    private AtomicLong updateCount = new AtomicLong(0);
    private Map<String, Timing> children = null;

    /**
     * Create a root timer with the given name.
     * @param name timer designation. Typically a method name or a similar code-path description.
     */
    public Timing(String name) {
        this(name, null);
    }

    /**
     * Create a root timer with the given name and subject.
     * @param name timer designation. Typically a method name or a similar code-path description.
     * @param subject specific subject. Typically a document ID or similar workload-specific identifier.
     */
    public Timing(String name, String subject) {
        this(name, subject, null);
    }

    /**
     * @param name    timer designation. Typically a method name or a similar code-path description.
     * @param subject specific subject. Typically a document ID or similar workload-specific identifier.
     * @param unit    the unit to use for average speed in toString. If null, the unit will be set to {@code upd}.
     */
    public Timing(String name, String subject, String unit) {
        this(name, subject, unit, MS_STATS);
    }

    /**
     * @param name    timer designation. Typically a method name or a similar code-path description.
     * @param subject specific subject. Typically a document ID or similar workload-specific identifier.
     * @param unit    the unit to use for average speed in toString. If null, the unit will be set to {@code upd}.
     * @param showStats the stats to show on calls to {@link #toString}.
     */
    public Timing(String name, String subject, String unit, STATS[] showStats) {
        this.name = name;
        this.subject = subject;
        this.unit = unit == null ? "upd" : unit;
        this.showStats = showStats == null ? MS_STATS : showStats;
    }

    public Timing(String name, long spendNS) {
        this(name);
        this.spendNS.set(spendNS);
    }

    public Timing(String name, String subject, long spendNS) {
        this(name, subject);
        this.spendNS.set(spendNS);
    }

    public Timing(String name, String subject, String unit, long spendNS) {
        this(name, subject, unit);
        this.spendNS.set(spendNS);
    }

    public STATS[] getShowStats() {
        return showStats;
    }

    /**
     * Specify the stats to show on {@code toString}.
     * @param showStats the stats to show.
     * @return this Timing for further chaining.
     */
    public Timing setShowStats(STATS[] showStats) {
        this.showStats = showStats == null ? MS_STATS : showStats;
        return this;
    }

    /**
     * If a child with the given name already exists, it will be returned.
     * If a child does not exist, it will be created.
     * <p>
     * Note: If the child already exists, {@link #start()} WILL NOT be called automatically.
     *       Consider chaining with {@code Timing myTiming = parent.getChild(...).start();}
     * <p>
     * Note 2: If a child is created it will inherit {@link #showStats} from the parent.
     * @param name child Timing designation. Typically a method name or a similar code-path description.
     * @return the re-used or newly created child.
     */
    public Timing getChild(String name) {
        return getChild(name, null);
    }

    /**
     * If a child with the given name already exists, it will be returned.
     * If a child does not exist, it will be created.
     * <p>
     * Note: If the child already exists, {@link #start()} WILL NOT be called automatically.
     *       Consider chaining with {@code Timing myTiming = parent.getChild(...).start();}
     * <p>
     * Note 2: If a child is created it will inherit {@link #showStats} from the parent.
     * @param name    child Timing designation. Typically a method name or a similar code-path description.
     * @param subject specific child subject. Typically a document ID or similar workload-specific identifier.
     * @return the re-used or newly created child.
     */
    public Timing getChild(String name, String subject) {
        return getChild(name, subject, unit);
    }

    /**
     * If a child with the given name already exists, it will be returned.
     * If a child does not exist, it will be created. It will use the default {@link #MS_STATS}.
     * <p>
     * Note: If the child already exists, {@link #start()} WILL NOT be called automatically.
     *       Consider chaining with {@code Timing myTiming = parent.getChild(...).start();}
     * <p>
     * Note 2: If a child is created it will inherit {@link #showStats} from the parent.
     * @param name    child Timing designation. Typically a method name or a similar code-path description.
     * @param subject specific child subject. Typically a document ID or similar workload-specific identifier.
     * @param unit    the unit to use for average speed in toString. If null, the unit will be set to {@code upd}.
     * @return the re-used or newly created child.
     */
    public Timing getChild(String name, String subject, String unit) {
        return getChild(name, subject, unit, null);
    }

    /**
     * If a child with the given name already exists, it will be returned.
     * If a child does not exist, it will be created.
     * <p>
     * Note: If the child already exists, {@link #start()} WILL NOT be called automatically.
     *       Consider chaining with {@code Timing myTiming = parent.getChild(...).start();}
     * @param name    child Timing designation. Typically a method name or a similar code-path description.
     * @param subject specific child subject. Typically a document ID or similar workload-specific identifier.
     * @param unit    the unit to use for average speed in toString. If null, the unit will be set to {@code upd}.
     * @param showStats the stats to show on calls to {@link #toString}.
     * @return the re-used or newly created child.
     */
    @SuppressWarnings("SameParameterValue")
    public synchronized Timing getChild(String name, String subject, String unit, STATS[] showStats) {
        if (children == null) {
            children = new LinkedHashMap<>();
        }
        Timing child = children.get(name);
        if (child == null) {
            child = new Timing(name, subject, unit, showStats == null ? this.showStats : showStats);
            children.put(name, child);
        }
        return child;
    }

    /**
     * Perform 1 call to {@link Runnable#run()}, measuring the time and adding that to the current Timing.
     * <p>
     * This is a thread-safe equivalent of
     * <pre>
     *     myTiming.start();
     *     runnable.run();
     *     myTiming.stop();
     * </pre>
     * <p>
     * A common pattern is to measure with a child-Timing:
     * <pre>
     *     parent.getChild("xslt").measure(() ->
     *         collector.setResult(xsltProcessor.transform(myInput));
     *     );
     * </pre>
     * <p>
     * If the {@code runnable} throws an Exception, the time used up to that Exception is still added, as well as
     * an increment of {@link #updateCount}.
     * @param runnable any runnable action.
     * @return this Timing for further chaining.
     */
    public Timing measure(Runnable runnable) {
        long startNS = System.nanoTime();
        try {
            runnable.run();
        } finally {
            addNS(System.nanoTime()-startNS);
        }
        return this;
    }

    /**
     * Perform 1 call to {@link Supplier#get()}, measuring the time and adding that to the current Timing before
     * returning the result of the call. The supplier will be called exactly once.
     * <p>
     * This is a thread-safe equivalent of
     * <pre>
     *     myTiming.start();
     *     myResult = supplier.get();
     *     myTiming.stop();
     * </pre>
     * <p>
     * A common pattern is to measure with a child-Timing:
     * <pre>
     *     myResult = parent.getChild("xslt").measure(() ->
     *         xsltProcessor.transform(myInput);
     *     );
     * </pre>
     * <p>
     * If the {@code supplier} throws an Exception, the time used up to that Exception is still added, as well as
     * an increment of {@link #updateCount}.
     * @param supplier delivers the result.
     * @return the result from activating the supplier.
     * @see #wrap(Supplier)
     */
    public <T> T measure(Supplier<T> supplier) {
        long startNS = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            addNS(System.nanoTime()-startNS);
        }
    }

    /**
     * Wrap the provided {@link Runnable} lambda in the measure function.
     * @param runnable any runnable to be measured.
     * @return the runnable with the side effect of collecting statistics.
     * @see #measure(Runnable)
     */
    public Runnable wrap(Runnable runnable) {
        return () -> measure(runnable);
    }

    /**
     * Wrap the provided {@link Supplier} lambda in the measure function.
     * @param supplier any supplier to be measured.
     * @return the supplier with the side effect of collecting statistics.
     * @see #measure(Supplier) 
     */
    public <T> Supplier<T> wrap(Supplier<T> supplier) {
        return () -> measure(supplier);
    }

    /**
     * Wrap the provided function in the measure function for use with streaming.
     * Example:
     * <pre>
     *     Function<Integer, String> myFunction = num -> Integer.toString(num);
     *     Function<Integer, String> wrappedFunction = myTimer.wrap(myFunction);
     *     return Stream.of(1, 2, 3).map(wrappedFunction).collect(Collectors.toList());
     * </pre>
     * will measure invocation time and count of {@code myFunction} during the streaming processing.
     * @param function any function to be measured.
     * @return the function with the side effect of collecting statistics.
     */
    public <S, T> Function<S, T> wrap(Function<S, T> function) {
        return s -> measure(() -> function.apply(s));
    }

    /**
     * Wrap the provided predicate in the measure function for use with streaming.
     * Example:
     * <pre>
     *     Function<Integer> isEven = num -> (num & 1) == 0;
     *     Function<Integer, String> wrappedPredicate = myTimer.wrap(mypredicate);
     *     return Stream.of(1, 2, 3).filter(wrappedpredicate).collect(Collectors.toList());
     * </pre>
     * will measure invocation time and count of {@code myPredicate} during the streaming processing.
     * @param predicate any predicate to be measured.
     * @return the predicate with the side effect of collecting statistics.
     */
    public <T> Predicate<T> wrap(Predicate<T> predicate) {
        return t -> measure(() -> predicate.test(t));
    }

    /**
     * Wrap the provided Consumer in the measure function for use with streaming.
     * Example:
     * <pre>
     *     List<Integer> myNumbers = new ArrayList<>();
     *     Consumer<Integer> myConsumer = num -> myNumbers.add(num);
     *     Consumer<Integer> wrappedConsumer = myTimer.wrap(myConsumer);
     *     Stream.of(1, 2, 3).forEach(wrappedConsumer);
     *     assertEquals(3, myNumbers.size());
     * </pre>
     * will measure invocation time and count of {@code myConsumer} during the streaming processing.
     * @param Consumer any Consumer to be measured.
     * @return the Consumer with the side effect of collecting statistics.
     */
    public <T> Consumer<T> wrap(Consumer<T> Consumer) {
        return t -> measure(() -> Consumer.accept(t));
    }

    // No wrapping of collector as it is unclear what to do with count

    /**
     * Not a high-performance method as the list is created on each call from a HashMap.
     * Note that children may have sub-children.
     * @return A list of all children. If there are no children, the empty list will be returned.
     */
    public synchronized List<Timing> getAllChildren() {
        return children == null ?
                Collections.emptyList() :
                new ArrayList<>(children.values());
    }

    /**
     * @return the number of children.
     */
    public synchronized int getChildCount() {
        return children == null ? 0 : children.size();
    }

    /**
     * Resets start time to current nanoTime.
     * <p>
     * Note: Start is automatically called during construction of this Timing instance.
     * <p>
     * Note 2: The use of start() and {@link #stop()} is not thread-safe by nature.
     * @return this Timing for further chaining.
     */
    public Timing start() {
        lastStart = System.nanoTime();
        return this;
    }

    /**
     * Adds now-lastStart to spendNS, increments updateCount with 1 and sets lastStart to now.
     * <p>
     * Note: The use of @{link #start()} and stop() is not thread-safe by nature.
     * @return now-lastStart.
     */
    public long stop() {
        return stop(updateCount.get()+1);
    }

    /**
     * Adds now-lastStart to spendNS, sets updateCount to the given updates and sets lastStart to now.
     * This is used when a process has handled an amount of entities and the average time spend on each
     * entity should be part of the report.
     * <p>
     * Note: The use of @{link #start()} and stop() is not thread-safe by nature.
     * @param updates the number of updates that happened since start.
     * @return now-lastStart.
     */
    public long stop(long updates) {
        long now = System.nanoTime();
        long spend = now-lastStart;
        updateMinMax(spend);
        lastNS.set(spend);
        spendNS.addAndGet(spend);
        updateCount.set(updates);
        lastStart = now;
        return spend;
    }

    private void updateMinMax(long spend) {
        long min = minNS.get();
        while (min > spend) {
            if (minNS.compareAndSet(min, spend)) {
                break;
            }
            min = minNS.get();
        }

        long max = maxNS.get();
        while (max < spend) {
            if (maxNS.compareAndSet(max, spend)) {
                break;
            }
            max = maxNS.get();
        }
    }

    /**
     * Add ns to spendNS and increments updateCount.
     * @param ns nano seconds to add.
     * @return spendNS.
     */
    public long addNS(long ns) {
        updateMinMax(ns);
        lastNS.set(ns);
        spendNS.addAndGet(ns);
        updateCount.incrementAndGet();
        return getNS();
    }

    /**
     * Add ns to spendNS and increments updateCount.
     * Min and max will be updated with ns/updates for approximation.
     * @param ns nano seconds to add.
     * @param updates the number of updates that the ns represents.
     * @return total spend NS.
     */
    public long addNS(long ns, long updates) {
        if (updates == 1) {
            updateMinMax(ns);
        } else if (updates > 1) {
            updateMinMax(ns/updates);
        }
        lastNS.set(ns);
        spendNS.addAndGet(ns);
        updateCount.addAndGet(updates);
        return getNS();
    }

    /**
     * Add time to spendNS and increments updateCount.
     * @param ms milli seconds to add.
     * @return total spend MS.
     */
    public long addMS(long ms) {
        addNS(ms*1000000);
        return getMS();
    }

    /**
     * Add time to spendNS and increments updateCount.
     * @param ms milli seconds to add.
     * @param updates the number of updates that the ns represents.
     * @return total spend MS.
     */
    public long addMS(long ms, long updates) {
        addNS(ms*1000000, updates);
        return getMS();
    }

    /**
     * Increment the update count with 1.
     * @return update count after incrementing.
     */
    public long update() {
        return updateCount.incrementAndGet();
    }

    /**
     * Adds the given number to the update counter.
     * @param count the amount to add.
     * @return the new total number of updates.
     */
    public long addUpdates(int count) {
        return updateCount.addAndGet(count);
    }

    /**
     * Set the update count to the specific number.
     * Note that calling {@link #stop()} auto-increments the updateCount with 1.
     * @param updateCount the number of updated for the timing.
     * @return this Timing for further chaining.
     */
    public Timing setUpdates(int updateCount) {
        this.updateCount.set(updateCount);
        return this;
    }

    /**
     * @return spendNS if updateCount &gt; 0 else now-lastStart.
     */
    public long getNS() {
        return updateCount.get() > 0 ? spendNS.get() : System.nanoTime()-lastStart;
    }

    /**
     * @return spendNS if updateCount &gt; 0 else now-lastStart, divided by 1000000.
     */
    public long getMS() {
        return (updateCount.get() > 0 ? spendNS.get() : System.nanoTime()-lastStart)/1000000;
    }

    public long getUpdates() {
        return updateCount.get();
    }

    /**
     * @return average based on {@link #getNS()} and updateCount.
     */
    public long getAverageNS() {
        final long count = updateCount.get();
        return count == 0 ? 0 : getNS()/count;
    }

    /**
     * @return average based on {@link #getMS()} and updateCount.
     */
    public long getAverageMS() {
        final long count = updateCount.get();
        return count == 0 ? 0 : getNS()/count/1000000;
    }

    public Timing clear() {
        updateCount.set(0);
        lastNS.set(0);
        spendNS.set(0);
        start();
        return this;
    }

    /**
     * @return recursive timing information using the existing {@link #showStats} setup.
     */
    public String toString() {
        return toString((STATS[])null, false);
    }

    /**
     * @param ns if true, nano-seconds are returned, else milli-seconds.
     * @return recursive timing information in nano- or milli-seconds.
     */
    public String toString(boolean ns) {
        StringBuilder sb = new StringBuilder();
        toString(sb, ns);
        return sb.toString();
    }

    /**
     * String serialization using fixed stat elements.
     * @param ns if true, stats with nano-seconds are returned using {@link #NS_STATS},
     *          else milli-seconds are returned using {@link #MS_STATS}.
     * @param indent if true, the result is rendered multi-line and indented.
     * @return recursive timing information in nano- or milli-seconds.
     */
    public String toString(boolean ns, boolean indent) {
        return toString(ns ? NS_STATS : MS_STATS, indent);
    }

    /**
     * String serialization using fixed stat elements.
     * @param sb will receive the serialized stats.
     * @param ns if true, stats with nano-seconds are returned using {@link #NS_STATS},
     *          else milli-seconds are returned using {@link #MS_STATS}.
     */
    public void toString(StringBuilder sb, boolean ns) {
        toString(sb, ns, false);
    }

    /**
     * String serialization using fixed stat elements.
     * @param sb will receive the serialized stats.
     * @param ns if true, stats with nano-seconds are returned using {@link #NS_STATS},
     *          else milli-seconds are returned using {@link #MS_STATS}.
     * @param indent if true, the result is rendered multi-line and indented.
     */
    synchronized void toString(StringBuilder sb, boolean ns, boolean indent) {
        toString(sb, ns ? NS_STATS : MS_STATS, indent, "");
    }

    /**
     * String serialization using explictly stated {@link STATS}. Note that {@code showStats} will be used transitively
     * for all {@code Timing} elements in the tree. State {@code null} to serialize using the {@link STATS} already
     * defined for each element.
     * @param showStats the stats to output. Pre-defined collections are {@link #MS_STATS} and {@link #NS_STATS}.
     * @return recursive timing information.
     */
    public String toString(STATS[] showStats) {
        return toString(showStats, false);
    }

    /**
     * String serialization using explictly stated {@link STATS}. Note that {@code showStats} will be used transitively
     * for all {@code Timing} elements in the tree. State {@code null} to serialize using the {@link STATS} already
     * defined for each element.
     * @param showStats the stats to output. Pre-defined collections are {@link #MS_STATS} and {@link #NS_STATS}.
     * @param indent if true, the result is rendered multi-line and indented.
     * @return recursive timing information.
     */
    public String toString(STATS[] showStats, boolean indent) {
        StringBuilder sb = new StringBuilder();
        toString(sb, showStats, indent, "");
        return sb.toString();
    }

    private synchronized void toString(StringBuilder sb, STATS[] showStats, boolean indent, String spaces) {
        sb.append(spaces);
        final STATS[] localStats = showStats == null ? this.showStats : showStats;
        for (STATS stat: localStats) {
            if (stat == STATS.name) {
                sb.append(name);
                break;
            }
        }
        sb.append("(");
        boolean empty = true;
        for (STATS stat: localStats) {
            if (stat == STATS.name || (stat == STATS.subject && subject == null)) {
                continue;
            }
            if (empty) {
                empty = false;
            } else {
                sb.append(", ");
            }
            switch (stat) {
                case subject:
                    sb.append("subj='").append(subject).append("'");
                    break;
                case ms:
                    sb.append(getMS()).append("ms");
                    break;
                case ns:
                    sb.append(getMS()).append("ns");
                    break;
                case updates:
                    sb.append(updateCount).append(unit);
                    break;
                case ms_updates:
                    sb.append(getAverageMS()).append("ms/").append(unit);
                    break;
                case ns_updates:
                    sb.append(getAverageNS()).append("ns/").append(unit);
                    break;
                case updates_s:
                    sb.append(getAverageUpdatesPerSecond()).append(unit).append("/s");
                    break;
                case min_ms:
                    sb.append("min=").append(getMinMS()).append("ms");
                    break;
                case min_ns:
                    sb.append("min=").append(getMinNS()).append("ns");
                    break;
                case max_ms:
                    sb.append("max=").append(getMaxMS()).append("ms");
                    break;
                case max_ns:
                    sb.append("max=").append(getMaxNS()).append("ns");
                    break;
                case last_ms:
                    sb.append("last").append(lastNS.get()/1000000).append("ms");
                    break;
                case last_ns:
                    sb.append("last").append(lastNS.get()).append("ns");
                    break;
                case utilization:
                    sb.append(String.format(Locale.ROOT, "util=%.1f%%", 100.0*getNS()/(System.nanoTime()-objectCreation)));
                    break;
                default: throw new UnsupportedOperationException("The stat '" + stat + "' is not supported yet");
            }
        }
        if (children != null && !children.isEmpty()) {
            sb.append(indent ? ", [\n" : ", [");
            boolean first = true;
            for (Timing child : children.values()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(indent ? ",\n" : ", ");
                }
                child.toString(sb, showStats, indent, indent ? spaces + "  " : "");
            }
            sb.append(indent ? "\n" + spaces + "]" : "]");
        }
        sb.append(")");
    }

    public long getMinNS() {
        final long min = minNS.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxNS() {
        final long max = maxNS.get();
        return max == Long.MIN_VALUE ? 0 : max;
    }

    public long getMinMS() {
        final long min = minNS.get();
        return min == Long.MAX_VALUE ? 0 : min/1000000;
    }

    public long getMaxMS() {
        final long max = maxNS.get();
        return max == Long.MIN_VALUE ? 0 : max/1000000;
    }

    public long getAverageUpdatesPerSecond() {
        final long count = updateCount.get();
        return count == 0 ? 0 : count*1000000*1000/spendNS.get();
    }

}
