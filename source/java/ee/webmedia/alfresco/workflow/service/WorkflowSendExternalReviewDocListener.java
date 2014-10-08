<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.service;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

public class WorkflowSendExternalReviewDocListener implements WorkflowEventListener, InitializingBean {
    private static Log log = LogFactory.getLog(WorkflowSendExternalReviewDocListener.class);

    private WorkflowService workflowService;
    private DvkService dvkService;
    private NodeService nodeService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerEventListener(this);
    }

    @Override
    public void handle(WorkflowEvent event, WorkflowEventQueue queue) {
        final BaseWorkflowObject object = event.getObject();

        if (object instanceof Workflow) {
            handleWorkflowEvent(event, queue);
        }

        if (object instanceof Task) {
            handleTaskEvent(event, queue);
        }

    }

    private void handleTaskEvent(WorkflowEvent event, WorkflowEventQueue queue) {
        Task task = (Task) event.getObject();
        if (!task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
            return;
        }
        if (!task.getParent().getParent().isDocumentWorkflow()) {
            return;
        }
        NodeRef docRef = task.getParent().getParent().getParent();
        // NB! it is necessary to identify whether task is changed by initiator or by receiver!
        if (workflowService.isRecievedExternalReviewTask(task)) {
            if (!task.isStatus(Status.FINISHED)) {
                // send workflow to recipients
                if (!queue.getExternalReviewProcessedDocuments().contains(docRef)) {
                    if (sendExternalReviewWorkflow(task.getParent().getParent().getWorkflows(), queue.getAdditionalExternalReviewRecipients())) {
                        queue.getExternalReviewProcessedDocuments().add(docRef);
                    }
                }
            }
        } else {
            // send task back to initiating institution
            if (WorkflowEventType.STATUS_CHANGED.equals(event.getType())
                    && task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
                if (!task.isStatus(Status.FINISHED)) {
                    // this should actually never happen
                    log.error("Sending back not finished task is unsupported, task=" + task);
                } else {
                    sendExternalReviewTask(task);
                }
            }
        }

    }

    private void sendExternalReviewTask(Task task) {
        dvkService.sendDvkTask(task);
    }

    private void handleWorkflowEvent(WorkflowEvent event, WorkflowEventQueue queue) {
        Workflow workflow = (Workflow) event.getObject();
        if (!workflow.getParent().isDocumentWorkflow()) {
            return;
        }
        NodeRef docRef = workflow.getParent().getParent();
        if (!queue.getExternalReviewProcessedDocuments().contains(docRef)) {
            if (sendExternalReviewWorkflow(workflow.getParent().getWorkflows(), queue.getAdditionalExternalReviewRecipients())) {
                queue.getExternalReviewProcessedDocuments().add(docRef);
            }
        }
    }

    // Send if something is changed in compound workflow containing external review
    private boolean sendExternalReviewWorkflow(List<Workflow> workflows, Map<NodeRef, List<String>> additionalRecipients) {
        for (Workflow workflow : workflows) {
            if (workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                dvkService.sendDvkTasksWithDocument(workflow.getParent().getParent(), workflow.getParent().getNodeRef(), additionalRecipients);
                return true;
            }
        }
        return false;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
=======
package ee.webmedia.alfresco.workflow.service;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

public class WorkflowSendExternalReviewDocListener implements WorkflowEventListener, InitializingBean {
    private static Log log = LogFactory.getLog(WorkflowSendExternalReviewDocListener.class);

    private WorkflowService workflowService;
    private DvkService dvkService;
    private NodeService nodeService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerEventListener(this);
    }

    @Override
    public void handle(WorkflowEvent event, WorkflowEventQueue queue) {
        final BaseWorkflowObject object = event.getObject();

        if (object instanceof Workflow) {
            handleWorkflowEvent(event, queue);
        }

        if (object instanceof Task) {
            handleTaskEvent(event, queue);
        }

    }

    private void handleTaskEvent(WorkflowEvent event, WorkflowEventQueue queue) {
        Task task = (Task) event.getObject();
        if (!task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
            return;
        }
        if (!task.getParent().getParent().isDocumentWorkflow()) {
            return;
        }
        NodeRef docRef = task.getParent().getParent().getParent();
        // NB! it is necessary to identify whether task is changed by initiator or by receiver!
        if (workflowService.isRecievedExternalReviewTask(task)) {
            if (!task.isStatus(Status.FINISHED)) {
                // send workflow to recipients
                if (!queue.getExternalReviewProcessedDocuments().contains(docRef)) {
                    if (sendExternalReviewWorkflow(task.getParent().getParent().getWorkflows(), queue.getAdditionalExternalReviewRecipients(), WorkflowUtil.getTaskMessageForRecipient(task))) {
                        queue.getExternalReviewProcessedDocuments().add(docRef);
                    }
                }
            }
        } else {
            // send task back to initiating institution
            if (WorkflowEventType.STATUS_CHANGED.equals(event.getType())
                    && task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
                if (!task.isStatus(Status.FINISHED)) {
                    // this should actually never happen
                    log.error("Sending back not finished task is unsupported, task=" + task);
                } else {
                    sendExternalReviewTask(task);
                }
            }
        }

    }

    private void sendExternalReviewTask(Task task) {
        dvkService.sendDvkTask(task);
    }

    private void handleWorkflowEvent(WorkflowEvent event, WorkflowEventQueue queue) {
        Workflow workflow = (Workflow) event.getObject();
        if (!workflow.getParent().isDocumentWorkflow()) {
            return;
        }
        NodeRef docRef = workflow.getParent().getParent();
        if (!queue.getExternalReviewProcessedDocuments().contains(docRef)) {
            if (sendExternalReviewWorkflow(workflow.getParent().getWorkflows(), queue.getAdditionalExternalReviewRecipients(), null)) {
                queue.getExternalReviewProcessedDocuments().add(docRef);
            }
        }
    }

    // Send if something is changed in compound workflow containing external review
    private boolean sendExternalReviewWorkflow(List<Workflow> workflows, Map<NodeRef, List<String>> additionalRecipients, String messageForRecipient) {
        for (Workflow workflow : workflows) {
            if (workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                dvkService.sendDvkTasksWithDocument(workflow.getParent().getParent(), workflow.getParent().getNodeRef(), additionalRecipients, messageForRecipient);
                return true;
            }
        }
        return false;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
>>>>>>> develop-5.1
