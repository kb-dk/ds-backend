package dk.kb.storage.facade;

import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
import dk.kb.storage.storage.BaseModuleStorage;
import dk.kb.storage.storage.RerunClusterStorage;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerunClusterFacade {

  private static final Logger log = LoggerFactory.getLogger(RerunClusterFacade.class);

  /**
   * Save list of RerunCluster in rerun_clusters table, update mtime in ds_records table and return
   * number of rows inserted or updated in rerun_clusters table.
   *
   * @param rerunClusterDtoList
   * @return RecordsCountDto number of rows inserted or updated
   */
  public static RecordsCountDto updateRerunClusters(List<RerunClusterDto> rerunClusterDtoList) {
    RecordsCountDto allRecordsCountDto = new RecordsCountDto();
    // Start the count at 0
    allRecordsCountDto.setCount(0);

    for (RerunClusterDto rerunClusterDto : rerunClusterDtoList) {
      BaseModuleStorage.performStorageAction(
          "updateRerunClusters() with fileId:" + rerunClusterDto.getFileId(),
          RerunClusterStorage.class, storage -> {
            RecordsCountDto recordsCountDto =
                ((RerunClusterStorage) storage).updateRerunClusters(rerunClusterDto);

            int touched = storage.updateMTimeForRecordByFileId(
                rerunClusterDto.getFileId().toString());

            allRecordsCountDto.setCount(allRecordsCountDto.getCount() + recordsCountDto.getCount());

            return allRecordsCountDto;
          });
    }

    log.info("Inserted/updated rows in rerun_clusters table:'{}'",
                allRecordsCountDto.getCount());
    return allRecordsCountDto;
  }

  /**
   * Return a RerunCluster by fileId.
   *
   * @param fileId UUID of fileId.
   * @return RerunClusterDto
   */
  public static RerunClusterDto getRerunClusterByFileId(UUID fileId) {
    return BaseModuleStorage.performStorageAction("getRerunClusterByFileId(" + fileId + ")",
        RerunClusterStorage.class, storage -> {
          RerunClusterDto rerunClusterDto =
              ((RerunClusterStorage) storage).getRerunClusterByFileId(fileId);

          return rerunClusterDto;
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
