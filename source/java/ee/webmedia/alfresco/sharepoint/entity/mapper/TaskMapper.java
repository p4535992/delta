package ee.webmedia.alfresco.sharepoint.entity.mapper;

import static ee.webmedia.alfresco.sharepoint.entity.mapper.IntegerMapper.readInt;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ee.webmedia.alfresco.sharepoint.entity.Task;

public class TaskMapper implements ParameterizedRowMapper<Task> {

    @Override
    public Task mapRow(ResultSet rs, int i) throws SQLException {
        Task result = new Task();
        // First line is procedure ID - not needed.
        result.setOrderNo(readInt(rs, 2));
        result.setType(rs.getString(3));
        result.setResponsible(rs.getBoolean(4));
        result.setOwnerName(rs.getString(5));
        result.setOwnerId(rs.getString(6));
        result.setOwnerEmail(rs.getString(7));
        result.setCreatorName(rs.getString(8));
        result.setCreatorId(rs.getString(9));
        result.setCreatorEmail(rs.getString(10));
        result.setResolution(rs.getString(11));
        result.setStartedDateTime(rs.getTimestamp(12));
        result.setDueDate(rs.getTimestamp(13));
        result.setCompletedDateTime(rs.getTimestamp(14));
        result.setStatus(rs.getString(15));
        result.setOutcome(rs.getString(16));
        result.setComment(rs.getString(17));
        result.setOwnerJobTitle(rs.getString(18));
        result.setOwnerOrganization(rs.getString(19));
        result.setWorkflowOrder(readInt(rs, 20));
        return result;
    }
}
