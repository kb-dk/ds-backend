package dk.kb.datahandler.preservica;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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
    private static final DateTimeFormatter ISO_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);

    public record Hit(String id, Instant created) {
    }

    public record Page(List<Hit> hits, long totalHits) {
    }

    private final PreservicaApiClient source;
    private final int pageSize;
    private Instant key;
    // Number of hits at key that have already been returned
    private int returnedAtKey = 0;
    private long duplicates = 0;
    private boolean done = false;
    private long totalHits = -1;

    /**
     * @param from inclusive {@code xip.created} to start from, or null to start from the beginning.
     */
    public PreservicaSearchPager(PreservicaApiClient source, int pageSize, Instant from) {
        if (pageSize < 2) {
            throw new IllegalArgumentException("pageSize must be at least 2, was " + pageSize);
        }
        this.source = source;
        this.pageSize = pageSize;
        this.key = from;
    }

    /**
     * @return the next hits in {@code xip.created} order. Empty when all hits have been returned.
     */
    public List<Hit> next() {
        if (done) {
            return List.of();
        }
        Page page = source.search(key, pageSize);
        if (totalHits < 0) {
            totalHits = page.totalHits();
        }
        List<Hit> hits = page.hits();
        done = hits.size() < pageSize;
        if (hits.isEmpty()) {
            return hits;
        }

        Instant last = hits.get(hits.size() - 1).created();
        if (!done && hits.get(0).created().equals(last)) {
            throw new IllegalStateException("A full page of " + pageSize + " hits all have xip.created " + iso(last) +
                                            ", which key-based paging cannot step past. Increase search.pageSize");
        }
        duplicates += Math.min(returnedAtKey, countCreatedAt(hits, key));
        key = last;
        returnedAtKey = countCreatedAt(hits, last);
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

    public static String iso(Instant instant) {
        return ISO_MILLIS.format(instant);
    }

    private static int countCreatedAt(List<Hit> hits, Instant created) {
        return (int) hits.stream().filter(hit -> hit.created().equals(created)).count();
    }
}
