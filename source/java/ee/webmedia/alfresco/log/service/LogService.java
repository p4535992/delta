package ee.webmedia.alfresco.log.service;

import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.filter.service.FilterService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.model.LogSetup;

/**
 * Delta business logic specific logging service.
 */
public interface LogService extends FilterService {

    String BEAN_NAME = "LogService";

    /**
     * Stores logging setup. Setup contains information about what kind of information gets logged.
     * 
     * @param logSetup Logging setup to use.
     */
    void saveLogSetup(LogSetup logSetup);

    /**
     * Provides current logging setup.
     * 
     * @return Object describing current logging setup.
     */
    LogSetup getCurrentLogSetup();

    /**
     * Adds a log entry.
     * 
     * @param log New log entry.
     */
    void addLogEntry(LogEntry log);

    /**
     * Provides current log entries from table. Filter can be provided to pre-filter the data.
     * 
     * @param filter Optional log entries filter.
     * @return Current log entries.
     */
    List<LogEntry> getLogEntries(LogFilter filter);

    Date getFirstLogEntryDate(NodeRef nodeRef);

    /** Should be used only for importing data from other systems; for creating regular log records, use addLogEntry(LogEntry log) */
    void addImportedLogEntry(LogEntry log, Date dateCreated);

    /** Should be used only for importing older log records; for creating regular log records, use addLogEntry(LogEntry log) */
    void addImportedLogEntry(LogEntry log, Date dateCreated, String idPrefix, long idSuffix);

    Date getFirstLogEntryDate();

    List<NodeRef> getDocumentsWithImapImportLog();

    /**
     * Sets predefined values for log entries that will be added by current thread. NB! Take care to call this method with {@code null} arguments from {@code finally} block to
     * restore normal behavior.
     * NB! For using createdDateTime, uses a local cache for past idSuffix calculation - so application must be run from one cluster node only.
     * 
     * @param createdDateTime createdDateTime; also idPrefix is set based on this
     * @throws RuntimeException if createdDateTime is in the future
     */
    void setThreadLocalOverride(Date createdDateTime, String creatorId, String creatorName);

    /**
     * NB! When using a local cache for past idSuffix calculation, application must be run from one cluster node only.
     */
    void clearPastIdSuffixCache();

    long retrieveLogSequenceNextval();

}
