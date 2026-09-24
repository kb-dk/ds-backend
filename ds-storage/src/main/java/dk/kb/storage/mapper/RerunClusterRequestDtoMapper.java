package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.RerunClusterRequestDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class RerunClusterRequestDtoMapper {

    /**
     * Create a {@link RerunClusterRequestDto} from a ResultSet
     *
     * @param resultSet containing values from rerun_clusters table
     * @return RerunClusterRequestDto populated with data
     * @throws SQLException
     */
    public RerunClusterRequestDto map(ResultSet resultSet) throws SQLException {
        RerunClusterRequestDto output = new RerunClusterRequestDto();

        output.setId(resultSet.getObject("id", UUID.class));
        output.setFileId(resultSet.getObject("file_id", UUID.class));
        output.setRerunClusterId(resultSet.getObject("rerun_cluster_id", UUID.class));
        output.setCreated(resultSet.getObject("created", OffsetDateTime.class));
        output.setJobId(resultSet.getString("job_id"));

        return output;
    }
}
