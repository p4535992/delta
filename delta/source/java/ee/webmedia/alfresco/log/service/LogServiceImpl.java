package ee.webmedia.alfresco.log.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;

import ee.webmedia.alfresco.filter.model.FilterVO;
import ee.webmedia.alfresco.log.model.Level;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.model.SystemLog;

/**
 * Main implementation of {@link LogService}. This class does not rely on Alfresco, and exchanges data with the database using JDBC(Template) directly.
 * 
 * @author Martti Tamm
 */
public class LogServiceImpl implements LogService {

    private JdbcTemplate jdbcTemplate;

    @Override
    public void saveLogSetup(LogSetup logSetup) {
        jdbcTemplate.update("TRUNCATE delta_log_level");
        jdbcTemplate.batchUpdate("INSERT INTO delta_log_level VALUES (?)", new LogSetupSqlParamSetter(logSetup.toLogLevels()));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public LogSetup getCurrentLogSetup() {
        List result = jdbcTemplate.query("SELECT * FROM delta_log_level", new LogLevelRowMapper());
        Set<Level> levels = new HashSet<Level>(result.size());
        for (Object level : result) {
            levels.add((Level) level);
        }
        return LogSetup.fromLogLevels(levels);
    }

    @Override
    public void addLogEntry(SystemLog log) {
        jdbcTemplate.update("INSERT INTO delta_log (log_entry_id,level,creator_id,creator_name,computer_ip,computer_name,object_id,object_name,description) "
                + "VALUES (to_char(CURRENT_DATE,'YYYYMMDD') || (SELECT COUNT(*) + 1 FROM delta_log WHERE date(created_date_time) = CURRENT_DATE),?,?,?,?,?,?,?,?)",
                new Object[] { log.getLevel(), log.getCreatorId(), log.getCreatorName(), log.getComputerIp(), log.getComputerName(), log.getObjectId(), log.getObjectName(),
                        log.getEventDescription() });
    }

    @Override
    public List<SystemLog> getLogEntries(LogFilter filter) {
        StringBuilder q = new StringBuilder("SELECT log_entry_id, level, created_date_time, creator_id, creator_name, computer_ip, computer_name, " +
                "object_id, object_name, description FROM delta_log");

        Object[] values = {};

        if (filter != null) {
            Map<String, Object> filterMap = new LinkedHashMap<String, Object>();
            if (StringUtils.hasLength(filter.getLogEntryId())) {
                filterMap.put("log_entry_id = ?", filter.getLogEntryId());
            }
            if (filter.getDateCreatedStart() != null) {
                filterMap.put("created_date_time >= ?", filter.getDateCreatedStart());
            }
            if (filter.getDateCreatedEnd() != null) {
                filterMap.put("created_date_time <= ?", filter.getDateCreatedEnd());
            }
            if (StringUtils.hasLength(filter.getCreatorName())) {
                filterMap.put("lower(creator_name) LIKE ?", "%" + filter.getCreatorName().toLowerCase() + "%");
            }
            if (StringUtils.hasLength(filter.getComputerId())) {
                filterMap.put("computer_ip = ?", filter.getComputerId());
            }
            if (StringUtils.hasLength(filter.getDescription())) {
                filterMap.put("lower(description) LIKE ?", "%" + filter.getDescription().toLowerCase() + "%");
            }
            if (StringUtils.hasLength(filter.getObjectName())) {
                filterMap.put("lower(object_name) LIKE ?", "%" + filter.getObjectName().toLowerCase() + "%");
            }
            if (StringUtils.hasLength(filter.getObjectId())) {
                filterMap.put("object_id LIKE ?", "%" + filter.getObjectId() + "%");
            }

            if (!filterMap.isEmpty()) {
                q.append(" WHERE ");
                boolean firstCondition = true;
                for (String condition : filterMap.keySet()) {
                    if (!firstCondition) {
                        q.append(" AND ");
                    } else {
                        firstCondition = false;
                    }
                    q.append(condition);
                }
            }
            values = filterMap.values().toArray(new Object[filterMap.values().size()]);
        }

        q.append(" ORDER BY created_date_time ASC");

        return jdbcTemplate.query(q.toString(), values, new LogRowMapper());
    }

    @Override
    public Node createFilter(Node filter, boolean isPrivate) {
        return filter;
    }

    @Override
    public Node createOrSaveFilter(Node filter, boolean isPrivate) {
        return filter;
    }

    @Override
    public void deleteFilter(NodeRef filter) {
    }

    @Override
    public Node getFilter(NodeRef filter) {
        return null;
    }

    @Override
    public QName getFilterNameProperty() {
        return null;
    }

    @Override
    public List<FilterVO> getFilters() {
        return Collections.emptyList();
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private class LogSetupSqlParamSetter implements BatchPreparedStatementSetter {

        private final Set<Level> levels;

        public LogSetupSqlParamSetter(Set<Level> levels) {
            this.levels = levels;
        }

        @Override
        public int getBatchSize() {
            return levels.size();
        }

        @Override
        public void setValues(PreparedStatement stmt, int i) throws SQLException {
            Iterator<Level> iterator = levels.iterator();
            stmt.setString(1, iterator.next().name());
            iterator.remove();
        }
    }

    private class LogLevelRowMapper implements RowMapper {

        @Override
        public Object mapRow(ResultSet rs, int i) throws SQLException {
            return Level.valueOf(rs.getString(1));
        }
    }

    private class LogRowMapper implements RowMapper {

        @Override
        public Object mapRow(ResultSet rs, int i) throws SQLException {
            SystemLog log = new SystemLog();
            log.setLogEntryId(rs.getString(1));
            log.setLevel(rs.getString(2));
            log.setCreatedDateTime(rs.getDate(3));
            log.setCreatorId(rs.getString(4));
            log.setCreatorName(rs.getString(5));
            log.setComputerIp(rs.getString(6));
            log.setComputerName(rs.getString(7));
            log.setObjectId(rs.getString(8));
            log.setObjectName(rs.getString(9));
            log.setEventDescription(rs.getString(10));
            return log;
        }
    }
}
