package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.RerunClusterResponseDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class RerunClusterResponseDtoMapper {

    /**
     * Create a {@link RerunClusterResponseDto} from a ResultSet
     *
     * @param resultSet containing values from rerun_clusters table
     * @return RerunClusterResponseDto populated with data
     * @throws SQLException
     */
    public RerunClusterResponseDto map(ResultSet resultSet) throws SQLException {
        RerunClusterResponseDto output = new RerunClusterResponseDto();

        output.setId(resultSet.getObject("id", UUID.class));
        output.setFileId(resultSet.getObject("file_id", UUID.class));
        output.setRerunClusterId(resultSet.getObject("rerun_cluster_id", UUID.class));
        output.setRerunClusterIdCount(resultSet.getObject("rerun_cluster_id_count", Integer.class));
        output.setCreated(resultSet.getObject("created", OffsetDateTime.class));
        output.setJobId(resultSet.getString("job_id"));
        output.setInserted(resultSet.getObject("inserted", OffsetDateTime.class));
        output.setUpdated(resultSet.getObject("updated", OffsetDateTime.class));

        return output;
    }
}
