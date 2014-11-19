package ee.webmedia.alfresco.document.service.event;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;
import ee.webmedia.alfresco.workflow.service.event.WorkflowModifications;

/**
 * Performs operations on documents based on events related to workflow status changes
 */
public class DocumentWorkflowStatusImmediateEventListener implements WorkflowEventListenerWithModifications, InitializingBean {

    private WorkflowService workflowService;
    private NodeService nodeService;
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
            // This event is in _immediate_ listener because we wish to compare compoundWorkflow status immediately.
            // In a regular listener, which is executed at the end - by then status may have been changed several times.
            if (event.getType().equals(WorkflowEventType.STATUS_CHANGED) && isStatus(cWorkflow, Status.IN_PROGRESS)) {
                final NodeRef docRef = cWorkflow.getParent();

                // Ignore document locking, because we are not changing a property that is user-editable or related to one
                AuthenticationUtil.runAs(new RunAsWork<Void>() {
                    @Override
                    public Void doWork() throws Exception {
                        nodeService.setProperty(docRef, DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, true);
                        return null;
                    }
                }, AuthenticationUtil.getSystemUserName());
            }
        }
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    // END: getters/setters

}
