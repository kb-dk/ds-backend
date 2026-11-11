package dk.kb.datahandler.preservica;

import dk.kb.datahandler.model.v1.PreservicaSearchDto;
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
        private final List<PreservicaApiClient.Hit> hits;
        int searches = 0;

        FakeSource(List<PreservicaApiClient.Hit> hits) {
            super(unusedSearchConfig());
            this.hits = hits;
        }

        private static PreservicaSearchDto unusedSearchConfig() {
            PreservicaSearchDto search = new PreservicaSearchDto();
            search.setApiUrl("http://unused");
            search.setQuery("{}");
            return search;
        }

        @Override
        public PreservicaApiClient.SearchResult search(Instant from, int max) {
            searches++;
            List<PreservicaApiClient.Hit> matching = hits.stream()
                    .filter(hit -> from == null || !hit.created().isBefore(from))
                    .toList();
            return new PreservicaApiClient.SearchResult(matching.subList(0, Math.min(max, matching.size())),
                                                        matching.size());
        }
    }

    /**
     * @param createdMillis xip.created per hit; the ids are h0, h1, ...
     */
    private static List<PreservicaApiClient.Hit> hits(long... createdMillis) {
        List<PreservicaApiClient.Hit> hits = new ArrayList<>();
        for (int i = 0; i < createdMillis.length; i++) {
            hits.add(new PreservicaApiClient.Hit("h" + i, Instant.ofEpochMilli(createdMillis[i])));
        }
        return hits;
    }

    private static List<String> drain(PreservicaSearchPager pager) {
        List<String> ids = new ArrayList<>();
        for (List<PreservicaApiClient.Hit> page = pager.next(); !page.isEmpty(); page = pager.next()) {
            page.forEach(hit -> ids.add(hit.id()));
        }
        return ids;
    }

    private static List<String> ids(List<PreservicaApiClient.Hit> hits) {
        return hits.stream().map(PreservicaApiClient.Hit::id).toList();
    }

    /**
     * Drains the pager and checks that every hit is returned in order, with only counted duplicates.
     */
    private static List<String> assertAllHitsInOrder(List<PreservicaApiClient.Hit> hits, PreservicaSearchPager pager) {
        List<String> returned = drain(pager);
        assertEquals(ids(hits), returned.stream().distinct().toList());
        assertEquals(hits.size(), returned.size() - pager.getDuplicates(), "returned: " + returned);
        return returned;
    }

    @Test
    public void next_whenDistinctCreated_thenLastHitOfEachPageRepeated() {
        List<PreservicaApiClient.Hit> hits = hits(1, 2, 3, 4, 5, 6, 7);
        PreservicaSearchPager pager = new PreservicaSearchPager(new FakeSource(hits), 3, null);

        assertEquals(List.of("h0", "h1", "h2", "h2", "h3", "h4", "h4", "h5", "h6", "h6"), assertAllHitsInOrder(hits, pager));
        assertEquals(3, pager.getDuplicates());
        assertEquals(7, pager.getTotalHits());
    }

    @Test
    public void next_whenTieAcrossPageBoundary_thenNoGaps() {
        List<PreservicaApiClient.Hit> hits = hits(1, 2, 3, 3, 4, 5);

        assertAllHitsInOrder(hits, new PreservicaSearchPager(new FakeSource(hits), 3, null));
    }

    @Test
    public void next_whenTieFillsWholePage_thenFails() {
        List<PreservicaApiClient.Hit> hits = hits(1, 2, 2, 2, 3);
        PreservicaSearchPager pager = new PreservicaSearchPager(new FakeSource(hits), 3, null);

        assertEquals(List.of("h0", "h1", "h2"), ids(pager.next()));
        assertThrows(IllegalStateException.class, pager::next);
    }

    @Test
    public void next_whenLastPageIsExactlyFull_thenTerminates() {
        List<PreservicaApiClient.Hit> hits = hits(1, 2, 3, 4, 5, 6);
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
        List<PreservicaApiClient.Hit> hits = hits(1, 2, 3, 3, 4, 5);

        PreservicaSearchPager pager = new PreservicaSearchPager(new FakeSource(hits), 3, Instant.ofEpochMilli(3));

        assertAllHitsInOrder(hits.subList(2, hits.size()), pager);
    }
}
