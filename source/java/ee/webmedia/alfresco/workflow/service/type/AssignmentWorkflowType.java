package ee.webmedia.alfresco.workflow.service.type;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isActiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isInactiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
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
    public static final QName TEMP_DELEGATED = RepoUtil.createTransientProp("delegated");

    private DocumentService documentService;
    private DocumentDynamicService documentDynamicService;
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
            String objectTypeId = (String) nodeService.getProperty(docRef, DocumentAdminModel.Props.OBJECT_TYPE_ID);
            boolean isIncomingLetter = SystematicDocumentType.INCOMING_LETTER.isSameType(objectTypeId);
            boolean isOutgoingLetter = SystematicDocumentType.OUTGOING_LETTER.isSameType(objectTypeId);
            if (task.isStatus(Status.IN_PROGRESS)) {

                // Change document owner
                if (task.getOwnerId() != null && isIncomingLetter) {
                    if (log.isDebugEnabled()) {
                        log.debug("Active responsible task started on incoming letter, setting document ownerId to " + task.getOwnerId());
                    }
                    setDocumentOwnerFromTask(task);
                }
            } else if (task.isStatus(Status.FINISHED) && !isDelegated(task) && !Boolean.TRUE.equals(isRegisterDocQueue) && WorkflowUtil.isActiveResponsible(task)) {
                if (isIncomingLetter) {
                    if (nodeService.getProperty(docRef, DocumentSpecificModel.Props.COMPLIENCE_DATE) == null) {
                        documentService.setPropertyAsSystemUser(DocumentSpecificModel.Props.COMPLIENCE_DATE, queue.getNow(), docRef);
                    }
                    if (StringUtils.isBlank((String) nodeService.getProperty(docRef, DocumentSpecificModel.Props.COMPLIENCE_NOTATION))) {
                        documentService.setPropertyAsSystemUser(DocumentSpecificModel.Props.COMPLIENCE_NOTATION, task.getComment(), docRef);
                    }
                    documentService.setDocStatusFinished(docRef);
                }
                if (isIncomingLetter || isOutgoingLetter) {
                    workflowService.unfinishTasksByFinishingLetterResponsibleTask(task, queue);
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
                documentDynamicService.setOwner(task.getParent().getParent().getParent(), task.getOwnerId(), false);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}