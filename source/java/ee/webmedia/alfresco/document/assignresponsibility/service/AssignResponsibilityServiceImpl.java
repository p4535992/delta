package ee.webmedia.alfresco.document.assignresponsibility.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Alar Kvell
 */
public class AssignResponsibilityServiceImpl implements AssignResponsibilityService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AssignResponsibilityServiceImpl.class);

    private DocumentDynamicService documentDynamicService;
    private WorkflowService workflowService;
    private DocumentSearchService documentSearchService;
    private UserService userService;

    @Override
    public void changeOwnerOfAllDesignatedObjects(String fromOwnerId, String toOwnerId, boolean isLeaving) {
        if (log.isDebugEnabled()) {
            log.debug("Assigning responsibility of working documents and new tasks from " + fromOwnerId + " to " + toOwnerId);
        }
        long startTime = System.currentTimeMillis();
        String newOwnerId = (isLeaving) ? toOwnerId : fromOwnerId;

        List<NodeRef> documents = documentSearchService.searchWorkingDocumentsByOwnerId(fromOwnerId, !isLeaving);
        for (NodeRef document : documents) {
            documentDynamicService.setOwner(document, newOwnerId, isLeaving);
        }

        List<NodeRef> tasks = documentSearchService.searchNewTasksByOwnerId(fromOwnerId, !isLeaving);
        for (NodeRef task : tasks) {
            workflowService.setTaskOwner(task, newOwnerId, isLeaving);
        }

        List<NodeRef> compoundWorkflows = documentSearchService.searchCompoundWorkflowsOwnerId(fromOwnerId, !isLeaving);
        for (NodeRef cwf : compoundWorkflows) {
            workflowService.setCompoundWorkflowOwner(cwf, newOwnerId, isLeaving);
        }

        List<NodeRef> caseFiles = documentSearchService.searchOpenCaseFilesOwnerId(fromOwnerId, !isLeaving);
        for (NodeRef caseFile : caseFiles) {
            documentDynamicService.setOwner(caseFile, newOwnerId, isLeaving);
        }

        if (log.isDebugEnabled()) {
            log.debug("Assigning responsibility of " + documents.size() + " working documents and " + tasks.size() + " new tasks from " + fromOwnerId + " to "
                    + toOwnerId + " took " + (System.currentTimeMillis() - startTime) + " ms");
        }

        // Mark or remove the leaving aspect
        userService.markUserLeaving(fromOwnerId, toOwnerId, isLeaving);
    }

    // START: getters / setters

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    // END: getters / setters

}
