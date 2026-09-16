package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.RerunClusterApi;
import dk.kb.storage.facade.RerunClusterFacade;
import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
import dk.kb.util.webservice.ImplBase;
import java.util.List;
import java.util.UUID;
import org.apache.cxf.interceptor.InInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ds-storage
 */
@InInterceptors(interceptors = "dk.kb.storage.webservice.KBAuthorizationInterceptor")
public class RerunClusterApiServiceImpl extends ImplBase implements RerunClusterApi {
  private static final Logger log = LoggerFactory.getLogger(RerunClusterApiServiceImpl.class);

  /**
   * Save list of RerunCluster in rerun_clusters table, update mtime in ds_records table and return
   * number of rows inserted or updated in rerun_clusters table.
   *
   * @param rerunClusterDtoList
   * @return RecordsCountDto number of rows inserted or updated
   */
  @Override
  public RecordsCountDto updateRerunClusters(List<RerunClusterDto> rerunClusterDtoList) {
    try {
      return RerunClusterFacade.updateRerunClusters(rerunClusterDtoList);
    } catch (Exception exception) {
      throw handleException(exception);
    }
  }

  /**
   * Return a RerunCluster by fileId.
   *
   * @param fileId
   * @return RerunClusterDto
   */
  @Override
  public RerunClusterDto getRerunClusterByFileId(UUID fileId) {
    try {
      return RerunClusterFacade.getRerunClusterByFileId(fileId);
    } catch (Exception exception) {
      throw handleException(exception);
    }
  }

  /**
   * Return latest created datetime from rerun_clusters table. Can be null.
   *
   * @return CreatedDto with latest created datetime
   */
  @Override
  public CreatedDto latestCreated() {
    try {
      return RerunClusterFacade.latestCreated();
    } catch (Exception exception) {
      throw handleException(exception);
    }
  }
}
