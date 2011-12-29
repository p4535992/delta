package ee.webmedia.alfresco.document.service.event;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;
import ee.webmedia.alfresco.workflow.service.event.WorkflowModifications;

/**
 * Performs operations on documents based on events related to workflow status changes
 * 
 * @author Ats Uiboupin
 */
public class DocumentWorkflowStatusEventListener implements WorkflowEventListenerWithModifications, InitializingBean {

    private WorkflowService workflowService;
    private NodeService nodeService;
    private PrivilegeService privilegeService;
    private FileService fileService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerImmediateEventListener(this);
    }

    @Override
    public void handle(WorkflowEvent event, WorkflowModifications workflowModifications, WorkflowEventQueue queue) {
        final BaseWorkflowObject object = event.getObject();

        if (object instanceof CompoundWorkflow) {
            CompoundWorkflow cWorkflow = (CompoundWorkflow) object;
            if (event.getType().equals(WorkflowEventType.STATUS_CHANGED) && isStatus(cWorkflow, Status.IN_PROGRESS)) {
                NodeRef docRef = cWorkflow.getParent();
                nodeService.setProperty(docRef, DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, true);
            }
        } else if (object instanceof Task) {
            Task task = (Task) object;
            String taskOwnerId = task.getOwnerId();
            if (StringUtils.isNotBlank(taskOwnerId) && event.getType().equals(WorkflowEventType.STATUS_CHANGED) && task.isStatus(Status.IN_PROGRESS)) {
                Workflow workflow = task.getParent();
                NodeRef docRef = workflow.getParent().getParent();
                Set<String> requiredPrivileges = PrivilegeUtil.getRequiredPrivsForInprogressTask(task, docRef, fileService);
                if (!requiredPrivileges.isEmpty()) {
                    privilegeService.setPermissions(docRef, taskOwnerId, requiredPrivileges);
                }
            }
        }
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }
    // END: getters/setters

}
