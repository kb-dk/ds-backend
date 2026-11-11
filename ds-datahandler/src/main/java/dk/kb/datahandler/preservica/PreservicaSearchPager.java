package dk.kb.datahandler.preservica;

import dk.kb.datahandler.preservica.PreservicaApiClient.Hit;
import java.time.Instant;
import java.util.List;

/**
 * Pages through search hits in {@code xip.updated} order using key-based paging: each page is searched with
 * {@code xip.updated >= lastKey} and start 0, so offsets never grow. An object that changes during the run gets a
 * later {@code xip.updated}, so it moves ahead of the paging and is returned again in its new state.
 * <p>
 * Several objects can share a {@code xip.updated} value, so each page starts with the hits at the boundary key that
 * the previous page ended with. They are returned again, which is harmless as storing a record is an upsert, and
 * counted in {@link #getDuplicates()}. A full page with a single {@code xip.updated} value cannot be paged past and
 * fails the harvest.
 * <p>
 * Not thread safe.
 */
public class PreservicaSearchPager {
    private final PreservicaApiClient preservicaApiClient;
    private final int pageSize;
    // Inclusive xip.updated the next search starts from: the last xip.updated of the previous page
    private Instant fromUpdated;
    // Number of hits at fromUpdated that have already been returned
    private int returnedAtFromUpdated = 0;
    private long duplicates = 0;
    private boolean done = false;
    private long totalHits = -1;

    /**
     * @param from inclusive {@code xip.updated} to start from, or null to start from the beginning.
     */
    public PreservicaSearchPager(PreservicaApiClient preservicaApiClient, int pageSize, Instant from) {
        if (pageSize < 2) {
            throw new IllegalArgumentException("pageSize must be at least 2, was " + pageSize);
        }
        this.preservicaApiClient = preservicaApiClient;
        this.pageSize = pageSize;
        this.fromUpdated = from;
    }

    /**
     * @return the next hits in {@code xip.updated} order. Empty when all hits have been returned.
     */
    public List<Hit> next() {
        if (done) {
            return List.of();
        }
        PreservicaApiClient.SearchResult result = preservicaApiClient.search(fromUpdated, pageSize);
        if (totalHits < 0) {
            totalHits = result.totalHits();
        }
        List<Hit> hits = result.hits();
        done = hits.size() < pageSize;
        if (hits.isEmpty()) {
            return hits;
        }

        Instant last = hits.get(hits.size() - 1).updated();
        if (!done && hits.get(0).updated().equals(last)) {
            throw new IllegalStateException("A full page of " + pageSize + " hits all have xip.updated " + last +
                                            ", which key-based paging cannot step past. " +
                                            "Increase preservicaSearch.pageSize");
        }
        // fromUpdated is still the start of this search, so hits at it were partly returned by the previous page
        duplicates += Math.min(returnedAtFromUpdated, countUpdatedAt(hits, fromUpdated));
        fromUpdated = last;
        returnedAtFromUpdated = countUpdatedAt(hits, last);
        return hits;
    }

    /**
     * @return the number of hits reported by the first search, an estimate of the run's size: objects that change
     *         during the run are returned again. -1 before the first call to {@link #next()}.
     */
    public long getTotalHits() {
        return totalHits;
    }

    /**
     * @return the number of hits returned more than once, so returned hits minus duplicates is the number of objects.
     */
    public long getDuplicates() {
        return duplicates;
    }

    private static int countUpdatedAt(List<Hit> hits, Instant updated) {
        return (int) hits.stream().filter(hit -> hit.updated().equals(updated)).count();
    }
}
