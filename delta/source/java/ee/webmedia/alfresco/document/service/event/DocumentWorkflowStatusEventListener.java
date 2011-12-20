package ee.webmedia.alfresco.document.service.event;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isActiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
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
                CompoundWorkflow cWorkFlow = workflow.getParent();
                NodeRef docRef = cWorkFlow.getParent();

                Set<String> requiredPrivileges = getRequiredPrivsForInprogressTask(task, docRef, fileService);
                privilegeService.addPrivilege(docRef, null, DocumentCommonModel.Types.DOCUMENT, taskOwnerId, null, requiredPrivileges);
            }
        }
    }

    // FIXME PRIV2 Ats - EDIT_DOCUMENT
    public static Set<String> getRequiredPrivsForInprogressTask(Task task, NodeRef docRef, FileService fileService) {
        Set<String> requiredPrivileges = new HashSet<String>(4);
        // 3.1.18.4 c
        requiredPrivileges.add(DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES);
        boolean isReview = task.isType(WorkflowSpecificModel.Types.REVIEW_TASK);
        if (isReview || task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
            // 3.1.18.3 a
            requiredPrivileges.add(DocumentCommonModel.Privileges.EDIT_DOCUMENT);
        }
        boolean isActiveResponsible = isActiveResponsible(task);
        if (isReview || isActiveResponsible || isFirstSignatureTask(task, docRef, fileService)) {
            // 3.1.18.5 a,b,c
            requiredPrivileges.add(DocumentCommonModel.Privileges.EDIT_DOCUMENT);
        }
        if (isActiveResponsible) {
            // 3.1.18.6 a
            // requiredPrivileges.add(DocumentCommonModel.Privileges.DELETE_DOCUMENT_FILES);
        }
        return requiredPrivileges;
    }

    public static boolean isFirstSignatureTask(Task sTask, NodeRef docRef, FileService fileService) {
        if (!sTask.isType(WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
            return false; // not a signature task
        }
        List<File> allActiveFiles = fileService.getAllActiveFiles(docRef);
        if (allActiveFiles.size() == 1 && allActiveFiles.get(0).isDigiDocContainer()) {
            return false;
        }
        return true;
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
