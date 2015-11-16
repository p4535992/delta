package ee.webmedia.alfresco.log.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.CaseInsensitiveMap;

import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogFilter;

public class LogEntryDataProvider extends LazyListDataProvider<String, LogEntry> {

    private static final long serialVersionUID = 1L;
    /**
     * Mapping for list columns to distinguish property namespaces (common vs specific model)
     */
    private static final Map<String, String> COLUMN_MAPPING = new CaseInsensitiveMap<>();
    private final LogFilter logFilter;

    public LogEntryDataProvider() {
        this(null, false);
    }

    public LogEntryDataProvider(LogFilter logFilter) {
    	this(logFilter, true);
  }
    
    public LogEntryDataProvider(LogFilter logFilter, boolean preloadKeys) {
      this.logFilter = logFilter;
      if (preloadKeys) {
      	loadOrderFromDb("createdDateTime", false);
      }
    }
    

    @Override
    protected boolean loadOrderFromDb(String column, boolean descending) {
        if (logFilter == null) {
            objectKeys = Collections.emptyList();
        } else {
            objectKeys = BeanHelper.getLogService().getLogEntryOrder(logFilter, getDatabaseColumn(column), descending, -1, -1);
        }
        return true;
    }

    @Override
    protected void resetObjectKeyOrder(List<LogEntry> orderedRows) {
        objectKeys.clear();
        for (LogEntry entry : orderedRows) {
            objectKeys.add(entry.getLogEntryId());
        }
    }

    @Override
    protected Map<String, LogEntry> loadData(List<String> rowsToLoad) {
        final LogFilter filter = new LogFilter();
        filter.setSpecificLogEntries(rowsToLoad);
        final List<LogEntry> logEntries = BeanHelper.getLogService().getLogEntries(filter);
        Map<String, LogEntry> result = new HashMap<>();
        for (LogEntry logEntry : logEntries) {
            result.put(logEntry.getLogEntryId(), logEntry);
        }
        return result;
    }

    private String getDatabaseColumn(String column) {

        final String columnName = COLUMN_MAPPING.get(column);
        if (columnName == null) {
            throw new RuntimeException("Column " + column + " is not yet mapped! See LogEntryDataProvider#COLUMN_MAPPING");
        }

        return columnName;
    }

    static {
        COLUMN_MAPPING.put("logEntryId", "log_entry_id");
        COLUMN_MAPPING.put("createdDateTime", "created_date_time");
        COLUMN_MAPPING.put("creatorName", "creator_name");
        COLUMN_MAPPING.put("computerIp", "computer_ip");
        COLUMN_MAPPING.put("eventDescription", "description");
        COLUMN_MAPPING.put("objectName", "object_name");
        COLUMN_MAPPING.put("objectId", "object_id");
    }

    @Override
    protected String getKeyFromValue(LogEntry value) {
        // not implemented
        throw new UnsupportedOperationException();
    }
}
