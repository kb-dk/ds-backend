package dk.kb.storage.facade;

import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterRequestDto;
import dk.kb.storage.model.v1.RerunClusterResponseDto;
import dk.kb.storage.storage.BaseModuleStorage;
import dk.kb.storage.storage.RerunClusterStorage;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerunClusterFacade {

  private static final Logger log = LoggerFactory.getLogger(RerunClusterFacade.class);

  /**
   * Save list of RerunClusterRequestDto in rerun_clusters table, update mtime in ds_records table
   * and return number of rows inserted or updated in rerun_clusters table.
   *
   * @param rerunClusterRequestDtoList
   * @return RecordsCountDto number of rows inserted or updated
   */
  public static RecordsCountDto updateRerunClusters(
      List<RerunClusterRequestDto> rerunClusterRequestDtoList) {
    return BaseModuleStorage.performStorageAction("updateRerunClusters()",
        RerunClusterStorage.class, storage -> {
          RecordsCountDto recordsCountDto =
              ((RerunClusterStorage) storage).updateRerunClusters(rerunClusterRequestDtoList);

          return recordsCountDto;
        });
  }

  /**
   * Return a RerunClusterResponseDto by fileId.
   *
   * @param fileId UUID of fileId.
   * @return RerunClusterResponseDto
   * @throws NotFoundServiceException if no rerun cluster exists for the fileId
   */
  public static RerunClusterResponseDto getRerunClusterByFileId(UUID fileId) {
    return BaseModuleStorage.performStorageAction("getRerunClusterByFileId(" + fileId + ")",
        RerunClusterStorage.class, storage -> {
          RerunClusterResponseDto rerunClusterResponseDto =
              ((RerunClusterStorage) storage).getRerunClusterByFileId(fileId);

          if (rerunClusterResponseDto == null) {
            throw new NotFoundServiceException(
                "No rerun cluster found for fileId '" + fileId + "'");
          }

          return rerunClusterResponseDto;
        });
  }

  /**
   * Return latest created datetime from rerun_clusters table. Can be null.
   *
   * @return CreatedDto with latest created datetime
   */
  public static CreatedDto latestCreated() {
    return BaseModuleStorage.performStorageAction("latestCreated()", RerunClusterStorage.class,
        storage -> {
          CreatedDto createdDto = ((RerunClusterStorage) storage).latestCreated();
          return createdDto;
        });
  }
}
