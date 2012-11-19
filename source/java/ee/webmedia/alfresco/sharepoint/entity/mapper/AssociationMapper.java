package ee.webmedia.alfresco.sharepoint.entity.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ee.webmedia.alfresco.sharepoint.entity.Association;

public class AssociationMapper implements ParameterizedRowMapper<Association> {

    @Override
    public Association mapRow(ResultSet rs, int i) throws SQLException {
        Association result = new Association();
        // First line is procedure ID - not needed.
        result.setFromNode(rs.getString(2));
        result.setType(rs.getString(3));
        result.setCreator(rs.getString(4));
        result.setCreatedDateTime(rs.getTimestamp(5));
        result.setMainDocument(rs.getBoolean(6));
        return result;
    }
}
