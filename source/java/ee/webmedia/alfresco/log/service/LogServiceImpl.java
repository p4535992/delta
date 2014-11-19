package ee.webmedia.alfresco.log.service;

import static org.springframework.util.StringUtils.hasLength;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
<<<<<<< HEAD
import java.util.concurrent.ConcurrentHashMap;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
<<<<<<< HEAD
import org.apache.commons.lang.time.FastDateFormat;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.search.DbSearchUtil;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.filter.model.FilterVO;
import ee.webmedia.alfresco.log.LogHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.model.LogLevel;
import ee.webmedia.alfresco.log.model.LogSetup;

/**
 * Main implementation of {@link LogService}. This class does not rely on Alfresco, and exchanges data with the database using JDBC(Template) directly.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class LogServiceImpl implements LogService, InitializingBean {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(LogServiceImpl.class);
<<<<<<< HEAD
    private static final FastDateFormat LOG_DATE_FORMAT = FastDateFormat.getInstance("yyyyMMdd");
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    private SimpleJdbcTemplate jdbcTemplate;

    private boolean useClientIpFromXForwardedForHttpHeader;
    private GeneralService generalService;

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

<<<<<<< HEAD
    private static final ThreadLocal<Date> overrideCreatedDateTime = new ThreadLocal<Date>();
    private static final ThreadLocal<String> overridecreatorId = new ThreadLocal<String>();
    private static final ThreadLocal<String> overridecreatorName = new ThreadLocal<String>();
    private final Map<String /* idPrefix */, Integer /* idSuffix */> pastIdSuffixCache = new ConcurrentHashMap<String, Integer>(0);

    @Override
    public void setThreadLocalOverride(Date createdDateTime, String creatorId, String creatorName) {
        Assert.isTrue(createdDateTime == null || !(new Date().before(createdDateTime)), "LogEntry createdDateTime is in the future: " + createdDateTime);
        overrideCreatedDateTime.set(createdDateTime);
        overridecreatorId.set(creatorId);
        overridecreatorName.set(creatorName);
    }

    @Override
    public void clearPastIdSuffixCache() {
        pastIdSuffixCache.clear();
    }

    private int getPastLastIdSuffix(String idPrefix) {
        return jdbcTemplate.queryForInt("SELECT COALESCE(MAX(CAST(substr(log_entry_id, 9) AS int8)), 0) FROM delta_log WHERE log_entry_id LIKE ?", idPrefix + "%");
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    @Override
    public void addLogEntry(LogEntry log) {
        addImportedLogEntry(log, null);
    }

    @Override
    public void addImportedLogEntry(LogEntry log, Date dateCreated) {
<<<<<<< HEAD
        checkDescriptionNotNull(log);

        if (!logEnabled(log)) {
            return;
        }

        String entryId = null;
        Timestamp now = null;

        if (overrideCreatedDateTime.get() != null) {
            dateCreated = overrideCreatedDateTime.get();
            String idPrefixNow = LOG_DATE_FORMAT.format(new Date());
            String idPrefix = LOG_DATE_FORMAT.format(dateCreated);
            if (!idPrefixNow.equals(idPrefix)) {
                Assert.isTrue(idPrefix.compareTo(idPrefixNow) < 0, "LogEntry createdDateTime is in the future: " + dateCreated + ", idPrefix=" + idPrefix + ", idPrefixNow="
                        + idPrefixNow);
                Integer idSuffix = pastIdSuffixCache.get(idPrefix);
                if (idSuffix == null) {
                    idSuffix = getPastLastIdSuffix(idPrefix);
                }
                idSuffix = idSuffix + 1;
                pastIdSuffixCache.put(idPrefix, idSuffix);
                entryId = idPrefix + idSuffix;
            }
        }

        if (entryId == null) {
            // Fetch some log entry data:
            Map<String, Object> result = jdbcTemplate.queryForMap("SELECT delta_log_date.idprefix, to_char(CURRENT_DATE,'YYYYMMDD') AS idprefix_now, " +
                    "nextval('delta_log_seq') AS idsuffix, current_timestamp AS now FROM delta_log_date LIMIT 1");
            String idPrefix = (String) result.get("idprefix");
            String idPrefixNow = (String) result.get("idprefix_now");
            Long idSuffix = (Long) result.get("idsuffix");
            now = (Timestamp) result.get("now");

            // Check if current date has changed - if so, log sequence needs to be reset to 1.
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            if (!idPrefixNow.equals(idPrefix)) {
                jdbcTemplate.update("LOCK TABLE delta_log_date");
                Map<String, Object> result2 = jdbcTemplate.queryForMap("SELECT delta_log_date.idprefix FROM delta_log_date LIMIT 1");
                String idPrefix2 = (String) result2.get("idprefix");
                if (!idPrefixNow.equals(idPrefix2)) {
                    jdbcTemplate.update("UPDATE delta_log_date SET idprefix = ?", idPrefixNow);
                    jdbcTemplate.queryForMap("SELECT setval('delta_log_seq', 1, false)");

<<<<<<< HEAD
                    Map<String, Object> result3 = jdbcTemplate.queryForMap("SELECT delta_log_date.idprefix, to_char(CURRENT_DATE,'YYYYMMDD') AS idprefix_now, " +
                            "nextval('delta_log_seq') AS idsuffix, current_timestamp AS now FROM delta_log_date LIMIT 1");
=======
                    Map<String, Object> result3 = jdbcTemplate
                            .queryForMap("SELECT delta_log_date.idprefix, to_char(CURRENT_DATE,'YYYYMMDD') AS idprefix_now, nextval('delta_log_seq') AS idsuffix, current_timestamp AS now FROM delta_log_date LIMIT 1");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                    idPrefix = (String) result3.get("idprefix");
                    idPrefixNow = (String) result3.get("idprefix_now");
                    idSuffix = (Long) result3.get("idsuffix");
                    now = (Timestamp) result3.get("now");
                    Assert.isTrue(idPrefixNow.equals(idPrefix), "idPrefixNow=" + idPrefixNow + " idPrefix=" + idPrefix);
                }
            }

<<<<<<< HEAD
            // Defensive approach: check if the entry ID is not already used. Update sequence and ID, when ID is used.

            int i = idSuffix.intValue();
            entryId = idPrefix + i;
            boolean collisionDetected = false;

            while (jdbcTemplate.queryForInt("SELECT COUNT(*) FROM delta_log WHERE log_entry_id = ?", entryId) != 0) {
                entryId = idPrefix + i++;
                collisionDetected = true;
            }

            if (collisionDetected) {
                jdbcTemplate.queryForInt("SELECT setval('delta_log_seq', ?, false)", i);
                LOG.info("Avoided log entry ID collision, updated suffix to: " + i);
            }
        }

        addLogEntry(log, entryId, dateCreated == null ? now : new Timestamp(dateCreated.getTime()));
=======
            addLogEntry(log, idPrefix, idSuffix, dateCreated == null ? now : new Timestamp(dateCreated.getTime()));
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public long getLogSequenceNextval() {
        return jdbcTemplate.queryForLong("SELECT nextval('delta_log_seq')");
    }

    @Override
    public void addImportedLogEntry(LogEntry log, Date dateCreated, String idPrefix, long idSuffix) {
<<<<<<< HEAD
        checkDescriptionNotNull(log);
        Assert.isTrue(dateCreated != null && StringUtils.isNotBlank(idPrefix));
        addLogEntry(log, idPrefix + idSuffix, new Timestamp(dateCreated.getTime()));
    }

    private void checkDescriptionNotNull(LogEntry log) {
=======
        if (!checkDescriptionNotNull(log)) {
            return;
        }
        Assert.isTrue(dateCreated != null && StringUtils.isNotBlank(idPrefix));
        addLogEntry(log, idPrefix, idSuffix, new Timestamp(dateCreated.getTime()));
    }

    private boolean checkDescriptionNotNull(LogEntry log) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        // This check provided just-in-case to catch empty event descriptions.
        // Exception should not be thrown as not logging is non-critical
        // compared to breaking serviced wanting to just log an action.
        if (log.getEventDescription() == null) {
<<<<<<< HEAD
            String err = "No event description was provided for event caused by " + log.getObjectName() + " with level " + log.getLevel();
            LOG.error(err);
            throw new RuntimeException(err);
        }
    }

    private void addLogEntry(LogEntry log, String entryId, Timestamp now) {
        String creatorId = log.getCreatorId();
        if (overridecreatorId.get() != null) {
            creatorId = overridecreatorId.get();
        }
        String creatorName = log.getCreatorName();
        if (overridecreatorName.get() != null) {
            creatorName = overridecreatorName.get();
        }
        jdbcTemplate.update(
                "INSERT INTO delta_log (log_entry_id,created_date_time,level,creator_id,creator_name,computer_ip,computer_name,object_id,object_name,description) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?)",
                new Object[] { entryId, now, log.getLevel(), creatorId, creatorName, log.getComputerIp(), log.getComputerName(),
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
            List<String> queryParts = new ArrayList<String>();
            List<Object> parameters = new ArrayList<Object>();
            if (hasLength(filter.getLogEntryId())) {
                queryParts.add("log_entry_id LIKE ?");
                parameters.add(filter.getLogEntryId() + "%");
            }
            if (filter.getDateCreatedStart() != null) {
                queryParts.add("date(created_date_time) >= ?");
                parameters.add(filter.getDateCreatedStart());
            }
            if (filter.getDateCreatedEnd() != null) {
                queryParts.add("date(created_date_time) <= ?");
                parameters.add(filter.getDateCreatedEnd());
            }
            if (hasLength(filter.getCreatorName())) {
                queryParts.add(DbSearchUtil.generateStringWordsWildcardQuery("creator_name"));
                parameters.add(generalService.getTsquery(filter.getCreatorName()));
            }
            if (hasLength(filter.getComputerId())) {
                // creating indexes for these fields is future development (can be done used trigram index); currently these fields are not indexed
                String computerId = "%" + filter.getComputerId().toLowerCase() + "%";
                queryParts.add("(lower(computer_ip) LIKE ? OR lower(computer_name) LIKE ?)");
                parameters.add(computerId);
                parameters.add(computerId);
            }
            if (hasLength(filter.getDescription())) {
                queryParts.add(DbSearchUtil.generateStringWordsWildcardQuery("description"));
                parameters.add(generalService.getTsquery(filter.getDescription()));
            }
            if (hasLength(filter.getObjectName())) {
                // creating index for this field is future development (can be done used trigram index); currently the field is not indexed
                queryParts.add("lower(object_name) LIKE ?");
                parameters.add("%" + filter.getObjectName().toLowerCase() + "%");
            }
            List<String> objectIds = filter.getObjectId();
            if (objectIds != null && !objectIds.isEmpty()) {
                List<String> notEmptyObjectIds = new ArrayList<String>();
                for (String objectId : objectIds) {
                    if (hasLength(objectId)) {
                        notEmptyObjectIds.add(objectId);
                    }
                }
                if (!notEmptyObjectIds.isEmpty()) {
                    StringBuilder objectCondition;
                    boolean exactObjectId = filter.isExactObjectId();
                    if (exactObjectId) {
                        objectCondition = new StringBuilder("object_id IN (").append(org.apache.commons.lang.StringUtils.repeat("?, ", notEmptyObjectIds.size()));
                        objectCondition.setLength(objectCondition.length() - 2);
                    } else {
                        objectCondition = new StringBuilder(org.apache.commons.lang.StringUtils.repeat("(object_id LIKE ? OR ", notEmptyObjectIds.size()));
                        objectCondition.setLength(objectCondition.length() - 4);
                    }
                    objectCondition.append(')').toString();
                    queryParts.add(objectCondition.toString());
                    String sep = exactObjectId ? "" : "%";
                    for (String objectId : notEmptyObjectIds) {
                        parameters.add(sep + objectId + sep);
                    }
                }
            }

            if (!queryParts.isEmpty()) {
                q.append(" WHERE ");
                boolean firstCondition = true;
                for (String condition : queryParts) {
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
            values = parameters.toArray(new Object[parameters.size()]);

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
        String query = q.toString();
<<<<<<< HEAD
        List<LogEntry> results = jdbcTemplate.query(query, new LogRowMapper(), values);
=======
        Long logQueryStart = System.currentTimeMillis();
        List<LogEntry> results = jdbcTemplate.query(query, new LogRowMapper(), values);
        LOG.info("Log entry query duration: " + (System.currentTimeMillis() - logQueryStart));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        explainQuery(query, values);
        return results;
    }

    private void explainQuery(String sqlQuery, Object... args) {
        generalService.explainQuery(sqlQuery, LOG, args);
    }

    @Override
    public List<NodeRef> getDocumentsWithImapImportLog() {
        return jdbcTemplate.query("SELECT object_id FROM delta_log WHERE creator_name='IMAP' AND level='DOCUMENT'", new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                String nodeRefStr = rs.getString(1);
                if (StringUtils.isNotBlank(nodeRefStr)) {
                    try {
                        return new NodeRef(nodeRefStr);
                    } catch (AlfrescoRuntimeException e) {
                        return null;
                    }
                }
                return null;
            }
        });
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

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
