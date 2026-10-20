package dk.kb.storage.facade;

import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.SummaryRequestDto;
import dk.kb.storage.model.v1.SummaryResponseDto;
import dk.kb.storage.storage.BaseModuleStorage;
import dk.kb.storage.storage.SummaryStorage;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryFacade {

  private static final Logger log = LoggerFactory.getLogger(SummaryFacade.class);

  /**
   * Save list of SummaryRequestDto in summaries table, update mtime in ds_records table and return
   * number of rows inserted or updated in summaries table.
   *
   * @param summaryRequestDtoList:
   */
  public static RecordsCountDto updateSummaries(List<SummaryRequestDto> summaryRequestDtoList) {
    return BaseModuleStorage.performStorageAction("updateSummaries()", SummaryStorage.class,
        storage -> {
          RecordsCountDto recordsCountDto =
              ((SummaryStorage) storage).updateSummaries(summaryRequestDtoList);

          return recordsCountDto;
        });
  }

  /**
   * Return a SummaryResponseDto from fileId. If no match was found, it returns a HTTP code 404.
   *
   * @param fileId
   */
  public static SummaryResponseDto getSummaryByFileId(UUID fileId) {
    SummaryResponseDto summaryResponseDto =
        BaseModuleStorage.performStorageAction("getSummaryByFileId(" + fileId + ")",
            SummaryStorage.class, storage -> {
              return ((SummaryStorage) storage).getSummaryByFileId(fileId);
            });

    if (summaryResponseDto == null) {
      throw new NotFoundServiceException("No summary found for fileId '" + fileId + "'");
    }

    return summaryResponseDto;
  }

  /**
   * Return latest created datetime from summaries table.
   */
  public static CreatedDto latestCreatedSummary() {
    return BaseModuleStorage.performStorageAction("latestCreatedSummary()", SummaryStorage.class,
        storage -> {
          CreatedDto createdDto = ((SummaryStorage) storage).latestCreatedSummary();
          return createdDto;
        });
  }
}
