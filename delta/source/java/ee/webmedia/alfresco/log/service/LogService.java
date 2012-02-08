package ee.webmedia.alfresco.log.service;

import java.util.List;

import ee.webmedia.alfresco.filter.service.FilterService;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.model.LogSetup;
import ee.webmedia.alfresco.log.model.LogEntry;

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
}
