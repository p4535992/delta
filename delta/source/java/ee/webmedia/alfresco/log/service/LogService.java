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
 * 
 * @author Martti Tamm
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

}
