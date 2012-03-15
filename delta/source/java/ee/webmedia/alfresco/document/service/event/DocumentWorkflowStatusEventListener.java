package ee.webmedia.alfresco.document.service.event;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

/**
 * Performs operations on documents based on events related to workflow status changes
 * 
 * @author Alar Kvell
 */
public class DocumentWorkflowStatusEventListener implements WorkflowEventListener, InitializingBean {

    private WorkflowService workflowService;
    private NodeService nodeService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerEventListener(this);
    }

    @Override
    public void handle(WorkflowEvent event, WorkflowEventQueue queue) {
        final BaseWorkflowObject object = event.getObject();

        if (object instanceof CompoundWorkflow) {
            if (event.getType().equals(WorkflowEventType.CREATED) || event.getType().equals(WorkflowEventType.UPDATED)) {
                CompoundWorkflow cWorkflow = (CompoundWorkflow) object;
                NodeRef docRef = cWorkflow.getParent();

                boolean hasAllFinishedCompoundWorkflows = workflowService.hasAllFinishedCompoundWorkflows(docRef);
                nodeService.setProperty(docRef, DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, hasAllFinishedCompoundWorkflows);
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
