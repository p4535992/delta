package ee.webmedia.alfresco.workflow.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue.WorkflowQueueParameter;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

public class IndependentWorkflowTaskStatusChangeListener implements WorkflowEventListener, InitializingBean {

    private WorkflowService workflowService;
    private PrivilegeService privilegeService;
    private DocumentDynamicService documentDynamicService;
    private WorkflowConstantsBean workflowConstantsBean;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerEventListener(this);
    }

    @Override
    public void handle(WorkflowEvent event, WorkflowEventQueue queue) {
        final BaseWorkflowObject object = event.getObject();

        if (object instanceof Task) {
            final Task task = (Task) object;
            String ownerId = task.getOwnerId();
            if (StringUtils.isBlank(ownerId)) {
                return;
            }
            CompoundWorkflow compoundWorkflow = task.getParent().getParent();
            if (compoundWorkflow.isIndependentWorkflow() && event.getType().equals(WorkflowEventType.STATUS_CHANGED)) {
                if (task.isStatus(Status.IN_PROGRESS)) {
                    NodeRef compoundWorkflowRef = compoundWorkflow.getNodeRef();
                    List<Document> documents = workflowService.getCompoundWorkflowDocuments(compoundWorkflowRef);
                    Set<Privilege> privileges = WorkflowUtil.getIndependentWorkflowDefaultDocPermissions();
                    boolean addEditPrivilege = task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) || WorkflowUtil.isFirstConfirmationTask(task);
                    boolean setOwnerData = WorkflowUtil.isActiveResponsible(task);
                    for (Document document : documents) {
                        Set<Privilege> documentPrivileges = new HashSet<Privilege>();
                        documentPrivileges.addAll(privileges);
                        boolean isWorkingDoc = document.isDocStatus(DocumentStatus.WORKING);
                        if (addEditPrivilege && isWorkingDoc) {
                            documentPrivileges.add(Privilege.EDIT_DOCUMENT);
                        }
                        NodeRef docRef = document.getNodeRef();
                        privilegeService.setPermissions(docRef, ownerId, documentPrivileges);
                        if (setOwnerData && isWorkingDoc) {
                            documentDynamicService.setOwner(docRef, ownerId, false);
                        }
                    }
                }
                if (task.isType(WorkflowSpecificModel.Types.REVIEW_TASK) && !task.isStatus(Status.NEW) && workflowConstantsBean.isReviewToOtherOrgEnabled()
                        && StringUtils.isNotBlank(BeanHelper.getParametersService().getStringParameter(Parameters.TASK_OWNER_STRUCT_UNIT))) {
                    String institutionCode = task.getInstitutionCode();
                    String creatorInstitutionCode = task.getCreatorInstitutionCode();
                    if (StringUtils.isNotBlank(creatorInstitutionCode) && StringUtils.isNotBlank(institutionCode) &&
                            !EqualsHelper.nullSafeEquals(institutionCode, creatorInstitutionCode)) {
                        BeanHelper.getGeneralService().runOnBackground(new RunAsWork<Void>() {
                            @Override
                            public Void doWork() throws Exception {
                                boolean sent = false;
                                try {
                                    sent = BeanHelper.getDvkService().sendReviewTaskNotification(task);
                                } catch (Exception e) {
                                    BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {

                                        @Override
                                        public Void execute() throws Throwable {
                                            logDvkSendError(task);
                                            return null;
                                        }
                                    }, false, true);
                                    throw e;
                                }
                                if (!sent) {
                                    logDvkSendError(task);
                                }
                                return null;
                            }
                        }, "sendReviewTaskToDvk", true);
                    }
                }
                if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && task.isStatus(Status.IN_PROGRESS)) {
                    String newOwner = (String) queue.getParameter(WorkflowQueueParameter.ASSIGNEMNT_TASK_STARTED_WITH_RESPONSIBLE_ASPECT);
                    if (newOwner != null) {
                        UserService userService = BeanHelper.getUserService();
                        String previousOwnerId = compoundWorkflow.getOwnerId();
                        String previousOwnerInMemory = compoundWorkflow.getProp(WorkflowCommonModel.Props.PREVIOUS_OWNER_ID);
                        if (!(previousOwnerInMemory != null && previousOwnerInMemory.equals(previousOwnerId))) {
                            BeanHelper.getWorkflowService().setCompoundWorkflowOwner(compoundWorkflow.getNodeRef(), newOwner, false);
                            WorkflowEvent workflowEvent = queue.getEvents().get(0);
                            if (WorkflowEventType.CREATED.equals(workflowEvent.getType()) && workflowEvent.getObject() instanceof CompoundWorkflow) {
                                BeanHelper.getLogService().addLogEntry(
                                        LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflow.getNodeRef(), "applog_compoundWorkflow_data_changed",
                                                MessageUtil.getMessage("workflow_responsible"), userService.getUserFullName(previousOwnerId),
                                                userService.getUserFullName(newOwner)));
                                compoundWorkflow.setProp(WorkflowCommonModel.Props.PREVIOUS_OWNER_ID, previousOwnerId);
                            }
                        }
                    }
                }
            }
        }
    }

    private void logDvkSendError(Task task) {
        BeanHelper.getLogService().addLogEntry(
                LogEntry.create(LogObject.TASK, BeanHelper.getUserService(), task.getNodeRef(), "applog_task_review_send_to_dvk_error", task.getOwnerName(),
                        task.getInstitutionName(), MessageUtil.getTypeName(task.getType())));
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setWorkflowConstantsBean(WorkflowConstantsBean workflowConstantsBean) {
        this.workflowConstantsBean = workflowConstantsBean;
    }

    // END: getters/setters

}
