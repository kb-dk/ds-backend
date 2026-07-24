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
package dk.kb.present.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper class for combining streams, iterators and collections.
 */
public class Combiner {
    private static final Logger log = LoggerFactory.getLogger(Combiner.class);

    /**
     * Merge multiple ordered streams into a single ordered stream.
     * The input ordering must match the order of the comparator.
     * This implementation uses streaming processing and has minimal memory overhead.
     * @param streams zero or more streams where the order of the delivered elements matches comparator.
     * @param comparator a comparator that matches the order in the given streams.
     */
    public static <T> Stream<T> mergeStreams(Collection<Stream<T>> streams, Comparator<T> comparator) {
        Collection<Iterator<T>> iterators = streams.stream()
                .map(BaseStream::iterator)
                .collect(Collectors.toList());
        Iterator<T> iterator = mergeIterators(iterators, comparator);

        return StreamSupport.stream(((Iterable<T>) () -> iterator).spliterator(), false);
    }

    /**
     * Merge multiple ordered collections into a single ordered list.
     * The input ordering must match the order of the comparator.
     * This collection delivers the result as a List which implies a memory overhead which is the sum of all the
     * elements from the given origins.
     * See {@link #mergeStreams(Collection, Comparator)} or {@link #mergeIterators(Collection, Comparator)} for
     * low-memory merging of pre-ordered origins.
     *
     * @param origins    zero or more origins where the order of the elements matches comparator.
     * @param comparator a comparator that matches the order in the given origins.
     */
    public static <T> List<T> mergeCollections(Collection<Collection<T>> origins, Comparator<T> comparator) {
        Collection<Iterator<T>> iterators = origins.stream()
                .map(Collection::iterator)
                .collect(Collectors.toList());
        Iterator<T> iterator = mergeIterators(iterators, comparator);

        return StreamSupport.stream(((Iterable<T>) () -> iterator).spliterator(), false).collect(Collectors.toList());
    }

    /**
     * Merge multiple ordered iterators into a single ordered iterator.
     * The input ordering must match the order of the comparator.
     * This implementation uses streaming processing and has minimal memory overhead.
     * @param iterators zero or more iterators where the order of the delivered elements matches comparator.
     * @param comparator a comparator that matches the order of the elements delivered by the given iterators.
     */
    public static <T> Iterator<T> mergeIterators(Collection<Iterator<T>> iterators, Comparator<T> comparator) {
        final PriorityQueue<Source<T>> pq = new PriorityQueue<>(new SourceComparator<>(comparator));
        iterators.stream()
                .map(Source::new)
                .filter(Source::hasValue)
                .forEach(pq::add);

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !pq.isEmpty();
            }

            @Override
            public T next() {
                Source<T> source = pq.poll();
                if (source == null) {
                    throw new NullPointerException("No more values");
                }
                T value = source.value;
                if (source.moveToNext()) {
                    // Source is not empty so we put it back
                    pq.add(source);
                }
                return value;
            }
        };
    }

    private static class SourceComparator<T> implements Comparator<Source<T>> {
        final Comparator<T> valueComparator;

        public SourceComparator(Comparator<T> valueComparator) {
            this.valueComparator = valueComparator;
        }

        @Override
        public int compare(Source<T> s1, Source<T> s2) {
            return valueComparator.compare(s1.getValue(), s2.getValue());
        }
    }

    /**
     * Peekable wrapper for iterators.
     */
    private static class Source<T> {
        final Iterator<T> iterator;
        T value;
        boolean hasValue = true;

        public Source(Iterator<T> iterator) {
            this.iterator = iterator;
            moveToNext();
        }

        public boolean moveToNext() {
            if (!iterator.hasNext()) {
                return hasValue = false;
            }
            value = iterator.next();
            return true;
        }

        public boolean hasValue() {
            return hasValue;
        }

        public T getValue() {
            return value;
        }
    }
}
