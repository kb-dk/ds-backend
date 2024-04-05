package dk.kb.discover.util;

import dk.kb.discover.config.ServiceConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
class SolrParamMergerTest {

    @BeforeAll
    public static void setup() throws IOException {
        ServiceConfig.initialize("solrparammerger-test.yaml");
    }

    @Test
    public void testDefaultsNoExtra() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        assertEquals("foo", merger.get("fq").get(0));
    }

    @Test
    public void testPutPutSingle() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        merger.put("foo", "bar");
        merger.put("boom", "baz");
    }

    @Test
    public void testAddSingle() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        assertFalse(merger.isFrozen());
        merger.add("foo", "bar");
        merger.add("foo", "baz");
        assertEquals("[bar, baz]", merger.get("foo").toString());
    }

    @Test
    public void testAddMulti() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        assertFalse(merger.isFrozen());
        merger.add("foo", "bar");
        merger.add("foo", List.of("baz", "zoo"));
        assertEquals("[bar, baz, zoo]", merger.get("foo").toString());
    }

    @Test
    public void testPutNothing() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        merger.put("foo", "bar");
        merger.put("foo", (String)null);
        merger.put("foo", Collections.emptyList());
        assertEquals("[bar]", merger.get("foo").toString());
    }

    @Test
    public void testPutInteger() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        merger.put("foo", 10);
        assertEquals("[10]", merger.get("foo").toString());
    }

    @Test
    public void testPutPutList() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        merger.put("foo", Collections.singletonList("bar"));
        merger.put("boom", Collections.singletonList("baz"));
        assertEquals("[bar]", merger.get("foo").toString());
        assertEquals("[baz]", merger.get("boom").toString());
    }

    @Test
    public void testDefaultsOverride() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        merger.put("fq", "zoo");
        assertEquals("zoo", merger.get("fq").get(0));
    }

    @Test
    public void testForceNoExtra() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select2").createMerger();
        assertEquals("bar", merger.get("fq").get(0));
    }

    @Test
    public void testForceAdd() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select2").createMerger();
        merger.put("fq", "zoo");
        assertEquals("[zoo, bar]", merger.get("fq").toString());
    }

    @Test
    public void testDefaultAndForceNoExtra() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select3").createMerger();
        assertEquals("[foo, bar]", merger.get("fq").toString());
    }

    @Test
    public void testDefaultAndForceAdd() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select3").createMerger();
        merger.put("fq", "zoo");
        assertEquals("[zoo, bar]", merger.get("fq").toString());
    }

    @Test
    public void testForceSimpleAdd() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select3").createMerger();
        merger.put("rows", "20");
        assertEquals("[10]", merger.get("rows").toString());
    }

    @Test
    public void testIllegalState() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        merger.put("fq", "zoo");
        merger.get("fq");
        assertThrows(IllegalStateException.class, () -> merger.put("fq", "baz"));
    }

    @Test
    public void testClear() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        merger.put("fq", "zoo");
        merger.get("fq");
        assertTrue(merger.containsKey("maxCollationRetries"),
                    "Before clear there should be a maxCollationRetries");
        merger.clear();
        merger.put("fq", "baz"); // Throws without clear in testIllegalState
        assertFalse(merger.containsKey("maxCollationRetries"),
                    "After clear there should be no maxCollationRetries");
    }

    @Test
    public void testClearAdd() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        assertTrue(merger.containsKey("maxCollationRetries"),
                    "Before clear(true) there should be a maxCollationRetries");
        merger.clear(true);
        assertTrue(merger.containsKey("maxCollationRetries"),
                    "After clear(true) there should be a maxCollationRetries");
    }

    @Test
    public void testAddMixedMap() {
        SolrParamMerger merger = new SolrParamMerger.Factory("select1").createMerger();
        merger.addAll(Map.of("fq", "bar"));
        merger.addAll(Map.of("fq", new Integer[]{1, 2}));
        //merger.addAll(Map.of("fq", new int[]{3, 4})); // Not supported (yet)
        merger.addAll(Map.of("fq", List.of(true, false)));
        assertEquals("[foo, bar, 1, 2, true, false]", merger.get("fq").toString());
    }

}