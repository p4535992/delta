package ee.webmedia.alfresco.log.service;

import static org.springframework.util.StringUtils.hasLength;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.filter.model.FilterVO;
import ee.webmedia.alfresco.log.LogHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.model.LogLevel;
import ee.webmedia.alfresco.log.model.LogSetup;

/**
 * Main implementation of {@link LogService}. This class does not rely on Alfresco, and exchanges data with the database using JDBC(Template) directly.
 * 
 * @author Martti Tamm
 */
public class LogServiceImpl implements LogService, InitializingBean {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(LogServiceImpl.class);

    private SimpleJdbcTemplate jdbcTemplate;

    private boolean useClientIpFromXForwardedForHttpHeader;

    @Override
    public void afterPropertiesSet() throws Exception {
        LogHelper.setUseClientIpFromXForwardedForHttpHeader(useClientIpFromXForwardedForHttpHeader);
    }

    @Override
    public void saveLogSetup(LogSetup logSetup) {
        jdbcTemplate.update("TRUNCATE delta_log_level");
        jdbcTemplate.getJdbcOperations().batchUpdate("INSERT INTO delta_log_level VALUES (?)", new LogSetupSqlParamSetter(logSetup.toLogLevels()));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public LogSetup getCurrentLogSetup() {
        List result = jdbcTemplate.query("SELECT * FROM delta_log_level", new LogLevelRowMapper());
        Set<LogLevel> levels = new HashSet<LogLevel>(result.size());
        for (Object level : result) {
            levels.add((LogLevel) level);
        }
        return LogSetup.fromLogLevels(levels);
    }

    @Override
    public void addLogEntry(LogEntry log) {
        addImportedLogEntry(log, null);
    }

    @Override
    public void addImportedLogEntry(LogEntry log, Date dateCreated) {
        if (!checkDescriptionNotNull(log)) {
            return;
        }

        if (logEnabled(log)) {

            Map<String, Object> result = jdbcTemplate
                    .queryForMap("SELECT delta_log_date.idprefix, to_char(CURRENT_DATE,'YYYYMMDD') AS idprefix_now, nextval('delta_log_seq') AS idsuffix, current_timestamp AS now FROM delta_log_date LIMIT 1");
            String idPrefix = (String) result.get("idprefix");
            String idPrefixNow = (String) result.get("idprefix_now");
            Long idSuffix = (Long) result.get("idsuffix");
            Timestamp now = (Timestamp) result.get("now");
            if (!idPrefixNow.equals(idPrefix)) {
                jdbcTemplate.update("LOCK TABLE delta_log_date");
                Map<String, Object> result2 = jdbcTemplate.queryForMap("SELECT delta_log_date.idprefix FROM delta_log_date LIMIT 1");
                String idPrefix2 = (String) result2.get("idprefix");
                if (!idPrefixNow.equals(idPrefix2)) {
                    jdbcTemplate.update("UPDATE delta_log_date SET idprefix = ?", idPrefixNow);
                    jdbcTemplate.queryForMap("SELECT setval('delta_log_seq', 1, false)");

                    Map<String, Object> result3 = jdbcTemplate
                            .queryForMap("SELECT delta_log_date.idprefix, to_char(CURRENT_DATE,'YYYYMMDD') AS idprefix_now, nextval('delta_log_seq') AS idsuffix, current_timestamp AS now FROM delta_log_date LIMIT 1");
                    idPrefix = (String) result3.get("idprefix");
                    idPrefixNow = (String) result3.get("idprefix_now");
                    idSuffix = (Long) result3.get("idsuffix");
                    now = (Timestamp) result3.get("now");
                    Assert.isTrue(idPrefixNow.equals(idPrefix), "idPrefixNow=" + idPrefixNow + " idPrefix=" + idPrefix);
                }
            }

            addLogEntry(log, idPrefix, idSuffix, dateCreated == null ? now : new Timestamp(dateCreated.getTime()));
        }
    }

    @Override
    public void addImportedLogEntry(LogEntry log, Date dateCreated, String idPrefix, long idSuffix) {
        if (!checkDescriptionNotNull(log)) {
            return;
        }
        Assert.isTrue(dateCreated != null && StringUtils.isNotBlank(idPrefix));
        addLogEntry(log, idPrefix, idSuffix, new Timestamp(dateCreated.getTime()));
    }

    private boolean checkDescriptionNotNull(LogEntry log) {
        // This check provided just-in-case to catch empty event descriptions.
        // Exception should not be thrown as not logging is non-critical
        // compared to breaking serviced wanting to just log an action.
        if (log.getEventDescription() == null) {
            LOG.error("No event description was provided for event caused by " + log.getObjectName() + " with level " + log.getLevel());
            return false;
        }
        return true;
    }

    private void addLogEntry(LogEntry log, String idPrefix, Long idSuffix, Timestamp now) {
        jdbcTemplate.update(
                "INSERT INTO delta_log (log_entry_id,created_date_time,level,creator_id,creator_name,computer_ip,computer_name,object_id,object_name,description) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?)",
                new Object[] { idPrefix + idSuffix.toString(), now, log.getLevel(), log.getCreatorId(), log.getCreatorName(), log.getComputerIp(), log.getComputerName(),
                        log.getObjectId(), log.getObjectName(), log.getEventDescription() });
    }

    private boolean logEnabled(LogEntry log) {
        return jdbcTemplate.queryForInt("SELECT COUNT(*) FROM delta_log_level WHERE level=?", log.getLevel()) != 0;
    }

    @Override
    public Date getFirstLogEntryDate(NodeRef nodeRef) {
        Assert.notNull(nodeRef);
        return jdbcTemplate.queryForObject("SELECT min(created_date_time) FROM delta_log WHERE object_id=?", Date.class, nodeRef.toString());
    }

    @Override
    public Date getFirstLogEntryDate() {
        return jdbcTemplate.queryForObject("SELECT min(created_date_time) FROM delta_log", Date.class);
    }

    @Override
    public List<LogEntry> getLogEntries(LogFilter filter) {
        StringBuilder q = new StringBuilder("SELECT log_entry_id, level, created_date_time, creator_id, creator_name, computer_ip, computer_name, " +
                "object_id, object_name, description FROM delta_log");

        Object[] values = {};

        if (filter != null) {
            Map<String, Object> filterMap = new LinkedHashMap<String, Object>();
            if (hasLength(filter.getLogEntryId())) {
                filterMap.put("log_entry_id LIKE ?", filter.getLogEntryId() + "%");
            }
            if (filter.getDateCreatedStart() != null) {
                filterMap.put("date(created_date_time) >= ?", filter.getDateCreatedStart());
            }
            if (filter.getDateCreatedEnd() != null) {
                filterMap.put("date(created_date_time) <= ?", filter.getDateCreatedEnd());
            }
            if (hasLength(filter.getCreatorName())) {
                filterMap.put("lower(creator_name) LIKE ?", "%" + filter.getCreatorName().toLowerCase() + "%");
            }
            if (hasLength(filter.getComputerId())) {
                String computerId = "%" + filter.getComputerId().toLowerCase() + "%";
                filterMap.put("(lower(computer_ip) LIKE ? OR lower(computer_name) LIKE ?)", computerId);
                filterMap.put(null, computerId);
            }
            if (hasLength(filter.getDescription())) {
                filterMap.put("lower(description) LIKE ?", "%" + filter.getDescription().toLowerCase() + "%");
            }
            if (hasLength(filter.getObjectName())) {
                filterMap.put("lower(object_name) LIKE ?", "%" + filter.getObjectName().toLowerCase() + "%");
            }
            if (hasLength(filter.getObjectId())) {
                filterMap.put("object_id LIKE ?", "%" + filter.getObjectId() + "%");
            }

            if (!filterMap.isEmpty()) {
                q.append(" WHERE ");
                boolean firstCondition = true;
                for (String condition : filterMap.keySet()) {
                    if (condition != null) {
                        if (!firstCondition) {
                            q.append(" AND ");
                        } else {
                            firstCondition = false;
                        }
                        q.append(condition);
                    }
                }
            }
            values = filterMap.values().toArray(new Object[filterMap.values().size()]);

            Set<String> excludedDescriptions = filter.getExcludedDescriptions();
            if (excludedDescriptions != null) {
                boolean firstCondition = q.length() == 0;
                for (String excludedDescription : excludedDescriptions) {
                    if (!firstCondition) {
                        q.append(" AND ");
                    } else {
                        firstCondition = false;
                    }
                    q.append("lower(description) NOT LIKE ?");
                    values = ArrayUtils.add(values, excludedDescription.toLowerCase());
                }
            }
        }

        q.append(" ORDER BY created_date_time ASC");

        return jdbcTemplate.query(q.toString(), new LogRowMapper(), values);
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

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private class LogSetupSqlParamSetter implements BatchPreparedStatementSetter {

        private final Set<LogLevel> levels;

        public LogSetupSqlParamSetter(Set<LogLevel> levels) {
            this.levels = levels;
        }

        @Override
        public int getBatchSize() {
            return levels.size();
        }

        @Override
        public void setValues(PreparedStatement stmt, int i) throws SQLException {
            Iterator<LogLevel> iterator = levels.iterator();
            stmt.setString(1, iterator.next().name());
            iterator.remove();
        }
    }

    private class LogLevelRowMapper implements ParameterizedRowMapper<LogLevel> {

        @Override
        public LogLevel mapRow(ResultSet rs, int i) throws SQLException {
            return LogLevel.valueOf(rs.getString(1));
        }
    }

    private class LogRowMapper implements ParameterizedRowMapper<LogEntry> {

        @Override
        public LogEntry mapRow(ResultSet rs, int i) throws SQLException {
            LogEntry log = new LogEntry();
            log.setLogEntryId(rs.getString(1));
            log.setLevel(rs.getString(2));
            log.setCreatedDateTime(rs.getTimestamp(3));
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

    public void setUseClientIpFromXForwardedForHttpHeader(boolean useClientIpFromXForwardedForHttpHeader) {
        this.useClientIpFromXForwardedForHttpHeader = useClientIpFromXForwardedForHttpHeader;
    }

}