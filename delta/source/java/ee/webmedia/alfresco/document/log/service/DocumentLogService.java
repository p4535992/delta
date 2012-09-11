package ee.webmedia.alfresco.document.log.service;

import org.alfresco.service.cmr.repository.NodeRef;

public interface DocumentLogService {

    String BEAN_NAME = "DocumentLogService";

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

}
