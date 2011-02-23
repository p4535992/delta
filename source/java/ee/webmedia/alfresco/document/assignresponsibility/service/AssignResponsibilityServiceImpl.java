package ee.webmedia.alfresco.document.assignresponsibility.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Alar Kvell
 */
public class AssignResponsibilityServiceImpl implements AssignResponsibilityService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AssignResponsibilityServiceImpl.class);

    private DocumentService documentService;
    private WorkflowService workflowService;
    private DocumentSearchService documentSearchService;

    @Override
    public void changeOwnerOfAllDocumentsAndTasks(String fromOwnerId, String toOwnerId) {
        if (log.isDebugEnabled()) {
            log.debug("Assigning responsibility of working documents and new tasks from " + fromOwnerId + " to " + toOwnerId);
        }
        long startTime = System.currentTimeMillis();
        List<NodeRef> documents = documentSearchService.searchWorkingDocumentsByOwnerId(fromOwnerId);
        for (NodeRef document : documents) {
            documentService.setDocumentOwner(document, toOwnerId);
        }
        List<NodeRef> tasks = documentSearchService.searchNewTasksByOwnerId(fromOwnerId);
        for (NodeRef task : tasks) {
            workflowService.setTaskOwner(task, toOwnerId);
        }
        if (log.isDebugEnabled()) {
            log.debug("Assigning responsibility of " + documents.size() + " working documents and " + tasks.size() + " new tasks from " + fromOwnerId + " to "
                    + toOwnerId + " took " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    // START: getters / setters

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    // END: getters / setters

}
