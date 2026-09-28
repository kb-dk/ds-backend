package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.TranscriptionApi;
import dk.kb.storage.facade.TranscriptionFacade;
import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.util.webservice.ImplBase;
import org.apache.cxf.interceptor.InInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * ds-storage
 */
@InInterceptors(interceptors = "dk.kb.storage.webservice.KBAuthorizationInterceptor")
public class TranscriptionApiServiceImpl extends ImplBase implements TranscriptionApi {
    private Logger log = LoggerFactory.getLogger(TranscriptionApiServiceImpl.class);

    /**
     * Load full transcription for a stream
     *
     * @param fileId FileId for the stream, this is the stream filename.
     * @return TranscriptionDto Will return empty transcriptionDto if no transcription is found
     */
    @Override
    public TranscriptionDto getTranscription(@NotNull String fileId) {
        return TranscriptionFacade.getTranscription(fileId);
    }

    /**
     * Create a new transcription or update an existing. Primary key is fileId.
     *
     * @param transcriptionDto
     */
    @Override
    public void createOrUpdateTranscription(@Valid TranscriptionDto transcriptionDto) {
        TranscriptionFacade.createOrUpdateTranscription(transcriptionDto);
    }
}
