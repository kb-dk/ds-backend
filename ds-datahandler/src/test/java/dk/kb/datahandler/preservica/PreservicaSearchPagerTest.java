package dk.kb.datahandler.preservica;

import dk.kb.datahandler.model.v1.OaiTargetSearchDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreservicaSearchPagerTest {

    /**
     * Search over a fixed list of hits, sorted on created and then id, like the order Preservica returns ties in.
     */
    private static class FakeSource extends PreservicaApiClient {
        private final List<PreservicaSearchPager.Hit> hits;
        int searches = 0;

        FakeSource(List<PreservicaSearchPager.Hit> hits) {
            super(unusedSearchConfig());
            this.hits = hits;
        }

        private static OaiTargetSearchDto unusedSearchConfig() {
            OaiTargetSearchDto search = new OaiTargetSearchDto();
            search.setApiUrl("http://unused");
            search.setQuery("{}");
            return search;
        }

        @Override
        public PreservicaSearchPager.Page search(Instant from, int max) {
            searches++;
            List<PreservicaSearchPager.Hit> matching = hits.stream()
                    .filter(hit -> from == null || !hit.created().isBefore(from))
                    .toList();
            return new PreservicaSearchPager.Page(matching.subList(0, Math.min(max, matching.size())), matching.size());
        }
    }

    /**
     * @param createdMillis xip.created per hit; the ids are h0, h1, ...
     */
    private static List<PreservicaSearchPager.Hit> hits(long... createdMillis) {
        List<PreservicaSearchPager.Hit> hits = new ArrayList<>();
        for (int i = 0; i < createdMillis.length; i++) {
            hits.add(new PreservicaSearchPager.Hit("h" + i, Instant.ofEpochMilli(createdMillis[i])));
        }
        return hits;
    }

    private static List<String> drain(PreservicaSearchPager pager) {
        List<String> ids = new ArrayList<>();
        for (List<PreservicaSearchPager.Hit> page = pager.next(); !page.isEmpty(); page = pager.next()) {
            page.forEach(hit -> ids.add(hit.id()));
        }
        return ids;
    }

    private static List<String> ids(List<PreservicaSearchPager.Hit> hits) {
        return hits.stream().map(PreservicaSearchPager.Hit::id).toList();
    }

    /**
     * Drains the pager and checks that every hit is returned in order, with only counted duplicates.
     */
    private static List<String> assertAllHitsInOrder(List<PreservicaSearchPager.Hit> hits, PreservicaSearchPager pager) {
        List<String> returned = drain(pager);
        assertEquals(ids(hits), returned.stream().distinct().toList());
        assertEquals(hits.size(), returned.size() - pager.getDuplicates(), "returned: " + returned);
        return returned;
    }

    @Test
    public void next_whenDistinctCreated_thenLastHitOfEachPageRepeated() {
        List<PreservicaSearchPager.Hit> hits = hits(1, 2, 3, 4, 5, 6, 7);
        PreservicaSearchPager pager = new PreservicaSearchPager(new FakeSource(hits), 3, null);

        assertEquals(List.of("h0", "h1", "h2", "h2", "h3", "h4", "h4", "h5", "h6", "h6"), assertAllHitsInOrder(hits, pager));
        assertEquals(3, pager.getDuplicates());
        assertEquals(7, pager.getTotalHits());
    }

    @Test
    public void next_whenTieAcrossPageBoundary_thenNoGaps() {
        List<PreservicaSearchPager.Hit> hits = hits(1, 2, 3, 3, 4, 5);

        assertAllHitsInOrder(hits, new PreservicaSearchPager(new FakeSource(hits), 3, null));
    }

    @Test
    public void next_whenTieFillsWholePage_thenFails() {
        List<PreservicaSearchPager.Hit> hits = hits(1, 2, 2, 2, 3);
        PreservicaSearchPager pager = new PreservicaSearchPager(new FakeSource(hits), 3, null);

        assertEquals(List.of("h0", "h1", "h2"), ids(pager.next()));
        assertThrows(IllegalStateException.class, pager::next);
    }

    @Test
    public void next_whenLastPageIsExactlyFull_thenTerminates() {
        List<PreservicaSearchPager.Hit> hits = hits(1, 2, 3, 4, 5, 6);
        FakeSource source = new FakeSource(hits);

        assertAllHitsInOrder(hits, new PreservicaSearchPager(source, 3, null));
        assertTrue(source.searches <= 4, "searches: " + source.searches);
    }

    @Test
    public void next_whenNoHits_thenEmpty() {
        PreservicaSearchPager pager = new PreservicaSearchPager(new FakeSource(List.of()), 3, null);

        assertTrue(pager.next().isEmpty());
        assertTrue(pager.next().isEmpty());
        assertEquals(0, pager.getTotalHits());
    }

    @Test
    public void next_whenFrom_thenStartsInclusiveAtFrom() {
        List<PreservicaSearchPager.Hit> hits = hits(1, 2, 3, 3, 4, 5);

        PreservicaSearchPager pager = new PreservicaSearchPager(new FakeSource(hits), 3, Instant.ofEpochMilli(3));

        assertAllHitsInOrder(hits.subList(2, hits.size()), pager);
    }

    @Test
    public void iso_whenWholeSecond_thenMillisecondsIncluded() {
        assertEquals("2018-07-20T08:49:24.000Z", PreservicaSearchPager.iso(Instant.parse("2018-07-20T08:49:24Z")));
        assertEquals("2018-07-20T08:49:24.174Z", PreservicaSearchPager.iso(Instant.ofEpochMilli(1532076564174L)));
    }
}
