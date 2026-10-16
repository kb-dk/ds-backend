package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.DsRecordKalturaDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RecordKalturaDtoMapper {

    /**
     * Create a {@link DsRecordKalturaDto} from a ResultSet
     *
     * @param resultSet containing values from ds_records table
     * @return DsRecordKalturaDto populated with data
     * @throws SQLException
     */
    public DsRecordKalturaDto map(ResultSet resultSet) throws SQLException {
        DsRecordKalturaDto record = new DsRecordKalturaDto();

        record.setId(resultSet.getString("id"));
        record.setDeleted(resultSet.getInt("deleted") == 1);
        record.setmTime(resultSet.getLong("mtime"));
        record.setReferenceId(resultSet.getString("referenceid"));
        record.setKalturaId(resultSet.getString("kalturaid"));

        return record;
    }
}
