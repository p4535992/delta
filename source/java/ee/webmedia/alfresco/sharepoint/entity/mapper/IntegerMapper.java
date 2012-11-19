package ee.webmedia.alfresco.sharepoint.entity.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class IntegerMapper implements ParameterizedRowMapper<Integer> {

    public static final IntegerMapper INSTANCE = new IntegerMapper();

    @Override
    public Integer mapRow(ResultSet rs, int i) throws SQLException {
        return readInt(rs, 1);
    }

    public static Integer readInt(ResultSet rs, int i) throws SQLException {
        int result = rs.getInt(i);
        return result == 0 && rs.wasNull() ? null : Integer.valueOf(result);
    }
}
