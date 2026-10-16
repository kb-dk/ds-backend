package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.TranscriptionApi;
import dk.kb.storage.facade.TranscriptionFacade;
import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.util.webservice.ImplBase;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.apache.cxf.interceptor.InInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ds-storage
 */
@InInterceptors(interceptors = "dk.kb.storage.webservice.KBAuthorizationInterceptor")
public class TranscriptionApiServiceImpl extends ImplBase implements TranscriptionApi {
  private final Logger log = LoggerFactory.getLogger(TranscriptionApiServiceImpl.class);

  /**
   * Return a transcription by fileId.
   *
   * @param fileId FileId for the stream, this is the stream filename.
   * @return TranscriptionDto
   */
  @Override
  public TranscriptionDto getTranscriptionByFileId(@NotNull String fileId) {
    try {
      return TranscriptionFacade.getTranscriptionByFileId(fileId);
    } catch (Exception exception) {
      throw handleException(exception);
    }
  }

  /**
   * Create a new transcription or update an existing. Primary key is fileId.
   *
   * @param transcriptionDto
   */
  @Override
  public void createOrUpdateTranscription(@Valid TranscriptionDto transcriptionDto) {
    try {
      TranscriptionFacade.createOrUpdateTranscription(transcriptionDto);
    } catch (Exception exception) {
      throw handleException(exception);
    }
  }
}
