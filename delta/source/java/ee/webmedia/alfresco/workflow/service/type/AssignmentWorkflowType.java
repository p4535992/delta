package ee.webmedia.alfresco.workflow.service.type;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getExcludedNodeRefsOnFinishWorkflows;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isActiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isInactiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import java.util.Iterator;
import java.util.List;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue.WorkflowQueueParameter;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;
import ee.webmedia.alfresco.workflow.service.event.WorkflowModifications;

/**
 * @author Alar Kvell
 */
public class AssignmentWorkflowType extends BaseWorkflowType implements WorkflowEventListenerWithModifications {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AssignmentWorkflowType.class);
    public static final QName TEMP_DELEGATED = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "delegated");

    private DocumentService documentService;
    private NodeService nodeService;

    @Override
    public void handle(WorkflowEvent event, WorkflowModifications workflowService, WorkflowEventQueue queue) {
        // If assignmentTask && ACTIVE && RESPONSIBLE=TRUE (and it is under document, not under compoundWorkflowDefinitions)
        if (!(event.getObject() instanceof Task)) {
            return;
        }
        Task task = (Task) event.getObject();
        CompoundWorkflow cWorkflow = task.getParent().getParent();
        if (!isActiveResponsible(task) || (cWorkflow instanceof CompoundWorkflowDefinition)) {
            return;
        }

        // Delegation

        // If task is created
        if (event.getType() == WorkflowEventType.CREATED) {

            // Finish all inactive responsible tasks
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
        } else if (event.getType() == WorkflowEventType.STATUS_CHANGED) {
            Boolean isRegisterDocQueue = queue.getParameter(WorkflowQueueParameter.TRIGGERED_BY_DOC_REGISTRATION);
            // If task is changed to IN_PROGRESS
            NodeRef docRef = cWorkflow.getParent();
            if (task.isStatus(Status.IN_PROGRESS)) {

                // Change document owner
                if (task.getOwnerId() != null && DocumentTypeHelper.isIncomingLetter(nodeService.getType(docRef))) {
                    if (log.isDebugEnabled()) {
                        log.debug("Active responsible task started on incoming letter, setting document ownerId to " + task.getOwnerId());
                    }
                    setDocumentOwnerFromTask(task);
                }
            } else if (task.isStatus(Status.FINISHED) && !isDelegated(task) && (isRegisterDocQueue == null || !isRegisterDocQueue)) { // if task status is changed to FINISHED
                Node document = documentService.getDocument(docRef);
                boolean isIncomingLetter = DocumentTypeHelper.isIncomingLetter(document.getType());
                if (isIncomingLetter) {
                    if (nodeService.getProperty(docRef, DocumentSpecificModel.Props.COMPLIENCE_DATE) == null) {
                        documentService.setPropertyAsSystemUser(DocumentSpecificModel.Props.COMPLIENCE_DATE, queue.getNow(), docRef);
                    }
                    documentService.setDocStatusFinished(docRef);
                }
                if (isIncomingLetter || DocumentTypeHelper.isOutgoingLetter(document.getType())) {
                    workflowService.setWorkflowsAndTasksFinished(queue, cWorkflow,
                            "task_outcome_unfinished_by_finishing_responsible_task", null, false, getExcludedNodeRefsOnFinishWorkflows(cWorkflow));
                    workflowService.addOtherCompundWorkflows(cWorkflow);
                    List<CompoundWorkflow> compoundWorkflows = cWorkflow.getOtherCompoundWorkflows();
                    for (Iterator<CompoundWorkflow> i = compoundWorkflows.iterator(); i.hasNext();) {
                        CompoundWorkflow compoundWorkflow = i.next();
                        if (compoundWorkflow.isStatus(Status.NEW)) {
                            this.workflowService.deleteCompoundWorkflow(compoundWorkflow.getNodeRef());
                            i.remove();
                        } else {
                            List<NodeRef> excludedNodeRefs = getExcludedNodeRefsOnFinishWorkflows(compoundWorkflow);
                            workflowService.setWorkflowsAndTasksFinished(queue, compoundWorkflow,
                                    "task_outcome_unfinished_by_finishing_responsible_task", null, false, excludedNodeRefs);
                        }
                    }
                }
            }
        }

    }

    private boolean isDelegated(Task task) {
        return Boolean.TRUE.equals(task.getNode().getProperties().get(TEMP_DELEGATED));
    }

    private void setDocumentOwnerFromTask(final Task task) {
        AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
            @Override
            public NodeRef doWork() throws Exception {
                documentService.setDocumentOwner(task.getParent().getParent().getParent(), task.getOwnerId());
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}