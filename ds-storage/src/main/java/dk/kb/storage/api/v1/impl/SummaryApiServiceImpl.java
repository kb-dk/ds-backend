package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.SummaryApi;
import dk.kb.storage.facade.SummaryFacade;
import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.SummaryRequestDto;
import dk.kb.storage.model.v1.SummaryResponseDto;
import dk.kb.util.webservice.ImplBase;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.apache.cxf.interceptor.InInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ds-storage
 */
@InInterceptors(interceptors = "dk.kb.storage.webservice.KBAuthorizationInterceptor")
public class SummaryApiServiceImpl extends ImplBase implements SummaryApi {
  private final Logger log = LoggerFactory.getLogger(SummaryApiServiceImpl.class);

  /**
   * Save list of SummaryRequestDto in summaries table, update mtime in ds_records table and return
   * number of rows inserted or updated in summaries table.
   *
   * @param summaryRequestDtoList:
   */
  @Override
  public RecordsCountDto updateSummaries(@Valid List<SummaryRequestDto> summaryRequestDtoList) {
    try {
      log.info("Updating summaries table");
      return SummaryFacade.updateSummaries(summaryRequestDtoList);
    } catch (Exception exception) {
      throw handleException(exception);
    }
  }

  /**
   * Return a SummaryResponseDto from fileId. If no match was found, it returns a HTTP code 404.
   *
   * @param fileId
   */
  @Override
  public SummaryResponseDto getSummaryByFileId(UUID fileId) {
    try {
      return SummaryFacade.getSummaryByFileId(fileId);
    } catch (Exception exception) {
      throw handleException(exception);
    }
  }

  /**
   * Return latest created datetime from summaries table.
   */
  @Override
  public CreatedDto latestCreatedSummary() {
    try {
      return SummaryFacade.latestCreatedSummary();
    } catch (Exception exception) {
      throw handleException(exception);
    }
  }
}
