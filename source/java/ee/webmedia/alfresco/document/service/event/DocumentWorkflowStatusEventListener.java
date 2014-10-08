package ee.webmedia.alfresco.document.service.event;

import static ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator.isNotRegistered;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.lock.service.DocLockService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

/**
 * Performs operations on documents based on events related to workflow status changes
 */
public class DocumentWorkflowStatusEventListener implements WorkflowEventListener, InitializingBean {

    private WorkflowService workflowService;
    private NodeService nodeService;
    private DocumentService documentService;
    private LogService logService;
    private UserService userService;
    private TransactionService transactionService;
    private DocLockService docLockService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerEventListener(this);
    }

    @Override
    public void handle(WorkflowEvent event, WorkflowEventQueue queue) {
        final BaseWorkflowObject object = event.getObject();

        if (object instanceof CompoundWorkflow) {
            CompoundWorkflow cWorkflow = (CompoundWorkflow) object;
            if (event.getType().equals(WorkflowEventType.CREATED) || event.getType().equals(WorkflowEventType.UPDATED)) {
                workflowService.updateCompWorkflowDocsSearchProps(cWorkflow);
            }
            if (event.getType().equals(WorkflowEventType.STATUS_CHANGED) && cWorkflow.isStatus(Status.FINISHED)) {
                if (cWorkflow.isCaseFileWorkflow()) {
                    NodeRef caseFileRef = cWorkflow.getParent();
                    Map<QName, Serializable> caseFileProps = nodeService.getProperties(caseFileRef);
                    if (caseFileProps.get(DocumentDynamicModel.Props.WORKFLOW_END_DATE) == null) {
                        Pair<CaseFileType, DocumentTypeVersion> typeAndVersion = BeanHelper.getDocumentAdminService().getCaseFileTypeAndVersion(
                                (String) caseFileProps.get(DocumentAdminModel.Props.OBJECT_TYPE_ID),
                                (Integer) caseFileProps.get(DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR));
                        Collection<Field> fields = typeAndVersion.getSecond().getFieldsById(Collections.singleton(DocumentDynamicModel.Props.WORKFLOW_END_DATE.getLocalName()));
                        if (!fields.isEmpty()) {
                            nodeService.setProperty(caseFileRef, DocumentDynamicModel.Props.WORKFLOW_END_DATE, new Date());
                        }
                    }
                } else if (cWorkflow.isIndependentWorkflow()) {
                    List<NodeRef> documentRefs = workflowService.getCompoundWorkflowDocumentRefs(cWorkflow.getNodeRef());
                    if (documentRefs.isEmpty()) {
                        return;
                    }
                    boolean allowFinishUnregisteredDocs = documentService.isFinishUnregisteredDocumentEnabled();
                    for (final NodeRef documentRef : documentRefs) {
                        endDocument(documentRef, allowFinishUnregisteredDocs);
                    }
                }
            }
        }
    }

    private void endDocument(final NodeRef documentRef, boolean allowFinishUnregisteredDocs) {
        Node document = new Node(documentRef);
        if (!DocumentStatus.FINISHED.equals((String) document.getProperties().get(DocumentCommonModel.Props.DOC_STATUS))) {
            if (!allowFinishUnregisteredDocs && isNotRegistered(document)) {
                logService.addLogEntry(LogEntry.create(LogObject.DOCUMENT, userService, documentRef, "applog_compoundWorkflow_finish_document_error_not_registered"));
            } else if (docLockService.isLockByOther(documentRef)) {
                Pair<String, Object[]> errorMessageKeyAndValueHolders = BeanHelper.getDocumentLockHelperBean().getErrorMessageKeyAndValueHolders("document_end_error_docLocked",
                        documentRef, new Object[0]);
                logService.addLogEntry(LogEntry.create(LogObject.DOCUMENT, userService, documentRef, errorMessageKeyAndValueHolders.getFirst(),
                        errorMessageKeyAndValueHolders.getSecond()));
            } else {
                documentService.endDocument(documentRef);
            }
        }
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setDocLockService(DocLockService docLockService) {
        this.docLockService = docLockService;
    }

    // END: getters/setters

}
