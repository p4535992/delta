package ee.webmedia.alfresco.document.log.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.log.model.DocumentLog;

public interface DocumentLogService {

    String BEAN_NAME = "DocumentLogService";

    /**
     * @param document
     * @return all log entries of this document.
     */
    List<DocumentLog> getDocumentLogs(NodeRef document);

    /**
     * @param seriesRef
     * @return all log entries of this series.
     */
    List<DocumentLog> getSeriesLogs(NodeRef seriesRef);

    /**
     * Add a log entry.
     * 
     * @param document
     * @param event
     */
    void addDocumentLog(NodeRef document, String event);

    /**
     * Add a log entry, specify the creator.
     * 
     * @param document
     * @param event
     * @param creator
     */
    void addDocumentLog(NodeRef document, String event, String creator);

    void addSeriesLog(NodeRef document, String event);
}
