package ee.webmedia.alfresco.archivals.bootstrap;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;

/**
 * @author Alar Kvell
 */
public class RenameOldPrimaryArchivalsRootBootstrap extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(RenameOldPrimaryArchivalsRootBootstrap.class);

    private GeneralService generalService;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Renaming /fn:archivals to /fn:documentList");
        NodeRef nodeRef = generalService.getNodeRef("/fn:archivals", generalService.getArchivalsStoreRef());
        NodeService nodeService = serviceRegistry.getNodeService();
        NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        ChildAssociationRef oldAssocRef = nodeService.getPrimaryParent(nodeRef);
        ChildAssociationRef newAssocRef = nodeService.moveNode(nodeRef, parentRef, ContentModel.ASSOC_CHILDREN, QName.createQName(FunctionsModel.URI, "documentList"));
        LOG.info("Renaming completed:\n  old " + oldAssocRef + "\n  new " + newAssocRef);
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
