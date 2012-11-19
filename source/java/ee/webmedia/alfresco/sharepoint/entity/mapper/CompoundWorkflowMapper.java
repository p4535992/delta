package ee.webmedia.alfresco.sharepoint.entity.mapper;

import static ee.webmedia.alfresco.sharepoint.entity.mapper.IntegerMapper.readInt;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ee.webmedia.alfresco.sharepoint.entity.CompoundWorkflow;

public class CompoundWorkflowMapper implements ParameterizedRowMapper<CompoundWorkflow> {

    @Override
    public CompoundWorkflow mapRow(ResultSet rs, int i) throws SQLException {
        CompoundWorkflow result = new CompoundWorkflow();
        result.setProcedureId(readInt(rs, 1));
        result.setType(rs.getString(2));
        result.setTitle(rs.getString(3));
        result.setComment(rs.getString(4));
        result.setStatus(rs.getString(5));
        result.setOwnerName(rs.getString(6));
        result.setOwnerId(rs.getString(7));
        result.setOwnerEmail(rs.getString(8));
        result.setCreatorName(rs.getString(9));
        result.setCreatedDateTime(rs.getTimestamp(10));
        result.setStartedDateTime(rs.getTimestamp(11));
        result.setFinishedDateTime(rs.getTimestamp(12));
        result.setParentId(readInt(rs, 13));
        result.setOriginalProcedureId(readInt(rs, 14));
        return result;
    }
}
