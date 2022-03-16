package dk.kb.present.util;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

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
class CombinerTest {

    @Test
    void iterateFromStream() {
        Stream<Integer> foreverStream = Stream.generate(() -> new Random().nextInt());
        Iterator<Integer> foreverIterator = foreverStream.iterator();
        System.out.println("First: " + foreverIterator.next());
        System.out.println("Second: " + foreverIterator.next());
    }

    @Test
    void testStream() {
        List<Stream<Integer>> streams = Arrays.asList(
                Stream.of(1, 3, 5),
                Stream.of(2, 4),
                Stream.of());
        Stream<Integer> merged = Combiner.mergeStreams(streams, Integer::compare);
        assertEquals("[1, 2, 3, 4, 5]", merged.collect(Collectors.toList()).toString());
    }

    @Test
    void testCollections() {
        List<Collection<Integer>> collections = List.of(
                List.of(2, 4),
                List.of(1, 3, 5),
                List.of());
        Collection<Integer> merged = Combiner.mergeCollections(collections, Integer::compare);
        assertEquals("[1, 2, 3, 4, 5]", merged.toString());
    }

    @Test
    void testEmpty() {
        List<Collection<Integer>> collections = List.of();
        Collection<Integer> merged = Combiner.mergeCollections(collections, Integer::compare);
        assertEquals("[]", merged.toString());
    }

    @Test
    void testEmptyMulti() {
        List<Collection<Integer>> collections = List.of(
                List.of(),
                List.of());
        Collection<Integer> merged = Combiner.mergeCollections(collections, Integer::compare);
        assertEquals("[]", merged.toString());
    }
}