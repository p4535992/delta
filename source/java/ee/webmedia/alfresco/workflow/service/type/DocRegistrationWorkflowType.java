package ee.webmedia.alfresco.workflow.service.type;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

public class DocRegistrationWorkflowType extends BaseWorkflowType implements WorkflowEventListener {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocRegistrationWorkflowType.class);

    private DocumentService documentService;

    @Override
    public void handle(WorkflowEvent event, WorkflowEventQueue queue) {
        if (event.getType() == WorkflowEventType.STATUS_CHANGED && event.getObject() instanceof Workflow
                && WorkflowUtil.isStatus(event.getObject(), Status.IN_PROGRESS)) {
            if (log.isDebugEnabled()) {
                log.debug("Received event STATUS_CHANGED to IN_PROGRESS, registering document");
            }
            final Workflow workflow = (Workflow) event.getObject();
            AuthenticationUtil.runAs(new RunAsWork<Object>() {
                @Override
                public Object doWork() throws Exception {
                    registerDocuments(workflow);
                    return null;
                }

            }, AuthenticationUtil.getSystemUserName());
        }
    }

    private void registerDocuments(final Workflow workflow) {
        CompoundWorkflow compoundWorkflow = workflow.getParent();
        boolean foundDocuments = false;
        if (compoundWorkflow.isDocumentWorkflow()) {
            registerDocument(compoundWorkflow.getParent());
            foundDocuments = true;
            BeanHelper.getMenuBean().processTaskItems();
        } else if (compoundWorkflow.isIndependentWorkflow()) {
            for (NodeRef docRef : workflowService.getCompoundWorkflowDocumentRefs(compoundWorkflow)) {
                if (RegisterDocumentEvaluator.canRegister(new Node(docRef), false)) {
                    registerDocument(docRef);
                }
                foundDocuments = true;
            }
        } else if (compoundWorkflow.isCaseFileWorkflow()) {
            for (NodeRef docRef : documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(compoundWorkflow.getParent())) {
                if (RegisterDocumentEvaluator.canRegister(new Node(docRef))) {
                    registerDocument(docRef);
                }
                foundDocuments = true;
            }
        }
        if (foundDocuments) {
            BeanHelper.getMenuBean().processTaskItems();
        }
    }

    private void registerDocument(NodeRef docRef) {
        if (documentService.registerDocumentIfNotRegistered(docRef, true)) {
            BeanHelper.getDocumentTemplateService().updateGeneratedFiles(docRef, true);
        }
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

}
