package ee.webmedia.alfresco.sharepoint.entity.mapper;

import static ee.webmedia.alfresco.sharepoint.entity.mapper.IntegerMapper.readInt;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ee.webmedia.alfresco.sharepoint.entity.Workflow;

public class WorkflowMapper implements ParameterizedRowMapper<Workflow> {

    @Override
    public Workflow mapRow(ResultSet rs, int i) throws SQLException {
        Workflow result = new Workflow();
        // First line is procedure ID - not needed.
        result.setOrderNo(readInt(rs, 2));
        result.setType(rs.getString(3));
        result.setStartedDateTime(rs.getTimestamp(4));
        result.setCreatorName(rs.getString(5));
        result.setStatus(rs.getString(6));
        return result;
    }
}
