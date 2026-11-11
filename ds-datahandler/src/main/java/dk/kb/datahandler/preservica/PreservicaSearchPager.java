package dk.kb.datahandler.preservica;

import dk.kb.datahandler.preservica.PreservicaApiClient.Hit;
import java.time.Instant;
import java.util.List;

/**
 * Pages through search hits in {@code xip.created} order using key-based paging: each page is searched with
 * {@code xip.created >= lastKey} and start 0, so offsets never grow.
 * <p>
 * Several objects can share a {@code xip.created} value, so each page starts with the hits at the boundary key that
 * the previous page ended with. They are returned again, which is harmless as storing a record is an upsert, and
 * counted in {@link #getDuplicates()}. A full page with a single {@code xip.created} value cannot be paged past and
 * fails the harvest.
 * <p>
 * Not thread safe.
 */
public class PreservicaSearchPager {
    private final PreservicaApiClient preservicaApiClient;
    private final int pageSize;
    // Inclusive xip.created the next search starts from: the last xip.created of the previous page
    private Instant fromCreated;
    // Number of hits at fromCreated that have already been returned
    private int returnedAtFromCreated = 0;
    private long duplicates = 0;
    private boolean done = false;
    private long totalHits = -1;

    /**
     * @param from inclusive {@code xip.created} to start from, or null to start from the beginning.
     */
    public PreservicaSearchPager(PreservicaApiClient preservicaApiClient, int pageSize, Instant from) {
        if (pageSize < 2) {
            throw new IllegalArgumentException("pageSize must be at least 2, was " + pageSize);
        }
        this.preservicaApiClient = preservicaApiClient;
        this.pageSize = pageSize;
        this.fromCreated = from;
    }

    /**
     * @return the next hits in {@code xip.created} order. Empty when all hits have been returned.
     */
    public List<Hit> next() {
        if (done) {
            return List.of();
        }
        PreservicaApiClient.SearchResult result = preservicaApiClient.search(fromCreated, pageSize);
        if (totalHits < 0) {
            totalHits = result.totalHits();
        }
        List<Hit> hits = result.hits();
        done = hits.size() < pageSize;
        if (hits.isEmpty()) {
            return hits;
        }

        Instant last = hits.get(hits.size() - 1).created();
        if (!done && hits.get(0).created().equals(last)) {
            throw new IllegalStateException("A full page of " + pageSize + " hits all have xip.created " + last +
                                            ", which key-based paging cannot step past. " +
                                            "Increase preservicaSearch.pageSize");
        }
        // fromCreated is still the start of this search, so hits at it were partly returned by the previous page
        duplicates += Math.min(returnedAtFromCreated, countCreatedAt(hits, fromCreated));
        fromCreated = last;
        returnedAtFromCreated = countCreatedAt(hits, last);
        return hits;
    }

    /**
     * @return the number of hits reported by the first search, or -1 before the first call to {@link #next()}.
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

    private static int countCreatedAt(List<Hit> hits, Instant created) {
        return (int) hits.stream().filter(hit -> hit.created().equals(created)).count();
    }
}
