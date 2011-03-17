package ee.webmedia.alfresco.workflow.service.type;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;

import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

/**
 * @author Alar Kvell
 */
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
                    documentService.registerDocumentIfNotRegistered(workflow.getParent().getParent(), true);
                    return null;
                }
            }, AuthenticationUtil.getSystemUserName());
        }
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

}
