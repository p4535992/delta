package ee.webmedia.alfresco.casefile.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * Adds cf:caseFileContainer aspect to drafts folder to avoid circular dependency between doccom and cf model.
 * 
 * @author Kaarel Jõgeva
 */
public class AddCaseFileContainerAspectToDrafts extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AddCaseFileContainerAspectToDrafts.class);

    private NodeService nodeService;
    private DocumentService documentService;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());

        NodeRef draftsNodeRef = documentService.getDrafts();
        LOG.info("Adding " + CaseFileModel.Aspects.CASE_FILE_CONTAINER + " aspect to " + draftsNodeRef);
        nodeService.addAspect(draftsNodeRef, CaseFileModel.Aspects.CASE_FILE_CONTAINER, null);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }
}
