package ee.webmedia.alfresco.workflow.service.type;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isActiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isInactiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;
import ee.webmedia.alfresco.workflow.service.event.WorkflowModifications;

/**
 * @author Alar Kvell
 */
public class AssignmentWorkflowType extends BaseWorkflowType implements WorkflowEventListenerWithModifications {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AssignmentWorkflowType.class);

    private DocumentService documentService;

    @Override
    public void handle(WorkflowEvent event, WorkflowModifications workflowService, WorkflowEventQueue queue) {
        // If assignmentTask && ACTIVE && RESPONSIBLE=TRUE (and it is under document, not under compoundWorkflowDefinitions), is
        //   * created
        //   * or ownerId is changed on such an existing task
        if ((event.getType() == WorkflowEventType.CREATED || event.getType() == WorkflowEventType.UPDATED) && event.getObject() instanceof Task) {
            Task task = (Task) event.getObject();
            if (isActiveResponsible(task) && !(task.getParent().getParent() instanceof CompoundWorkflowDefinition)) {

                // Delegation

                // Finish all inactive responsible tasks
                if (event.getType() == WorkflowEventType.CREATED) {
                    for (Task otherTask : task.getParent().getTasks()) {
                        if (isInactiveResponsible(otherTask) && isStatus(otherTask, Status.IN_PROGRESS, Status.STOPPED)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Finishing inactive responsible task (ownerName='" + otherTask.getOwnerName()
                                        + "'), because an active responsible task was created (ownerName='" + task.getOwnerName() + "')");
                            }
                            workflowService.setTaskFinished(queue, otherTask);
                            otherTask.setComment(I18NUtil.getMessage("task_comment_delegated"));
                        }
                    }
                }

                // Change document owner 
                if (event.getType() == WorkflowEventType.CREATED) {
                    if (log.isDebugEnabled()) {
                        log.debug("Active responsible task created, setting document ownerId to " + task.getOwnerId());
                    }
                    documentService.setDocumentOwner(task.getParent().getParent().getParent(), task.getOwnerId());
                } else {
                    @SuppressWarnings("unchecked")
                    Map<QName, Serializable> props = (Map<QName, Serializable>) event.getExtras()[0];
                    if (props.containsKey(WorkflowCommonModel.Props.OWNER_ID)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Active responsible task ownerId updated, setting document ownerId to" + task.getOwnerId());
                        }
                        documentService.setDocumentOwner(task.getParent().getParent().getParent(), task.getOwnerId());
                    }
                }
            }
        }
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

}
