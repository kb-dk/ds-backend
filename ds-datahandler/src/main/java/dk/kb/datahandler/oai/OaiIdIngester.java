package dk.kb.datahandler.oai;

import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Ingests records by OAI identifier: fetches them in parallel with GetRecord and adds them to ds-storage through the
 * target's {@link OaiResponseFilter}, in the order of the given identifiers. The filter is only called from the
 * calling thread.
 * <p>
 * Not thread safe. Close it to stop the fetch threads.
 */
public class OaiIdIngester implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(OaiIdIngester.class);

    private final OaiHarvestClient oaiClient;
    private final OaiResponseFilter filter;
    private final ExecutorService executor;
    private int notFound = 0;

    public OaiIdIngester(OaiHarvestClient oaiClient, OaiResponseFilter filter, int parallel) {
        this.oaiClient = oaiClient;
        this.filter = filter;
        this.executor = Executors.newFixedThreadPool(parallel);
    }

    /**
     * Fetch and store the records. Returns when all records have been handled.
     *
     * @param identifiers OAI identifiers, e.g. {@code oai:io:<uuid>} for Preservica.
     * @throws ServiceException if a record could not be fetched or stored. Records before it in the list may have
     *                          been stored.
     */
    public void ingest(List<String> identifiers) throws ServiceException {
        List<Future<OaiRecord>> futures = new ArrayList<>(identifiers.size());
        for (String identifier : identifiers) {
            futures.add(executor.submit(() -> oaiClient.getRecord(identifier)));
        }

        try {
            for (int i = 0; i < futures.size(); i++) {
                OaiRecord record = futures.get(i).get();
                if (record == null) {
                    notFound++;
                    log.info("OAI record '{}' does not exist, skipping it", identifiers.get(i));
                    continue;
                }
                //Maybe run the store part in parallel too
                store(record);
            }
        } catch (ExecutionException e) {
            futures.forEach(future -> future.cancel(true));
            if (e.getCause() instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new InternalServiceException("Fetching an OAI record failed", e.getCause());
        } catch (InterruptedException e) {
            futures.forEach(future -> future.cancel(true));
            Thread.currentThread().interrupt();
            throw new InternalServiceException("Interrupted while fetching OAI records", e);
        } catch (RuntimeException e) {
            futures.forEach(future -> future.cancel(true));
            throw e;
        }
    }

    // The target's filtering is in addToStorage(OaiResponse), so each record is wrapped in a response
    private void store(OaiRecord record) throws ServiceException {
        OaiResponse response = new OaiResponse();
        response.setRecords(new ArrayList<>(List.of(record)));
        filter.addToStorage(response);
    }

    /**
     * @return the number of records processed by the filter, including records it filtered out.
     */
    public int getProcessed() {
        return filter.getProcessed();
    }

    /**
     * @return the number of identifiers the OAI server did not know.
     */
    public int getNotFound() {
        return notFound;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
