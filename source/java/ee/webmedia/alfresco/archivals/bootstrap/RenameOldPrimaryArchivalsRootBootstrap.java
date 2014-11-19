package ee.webmedia.alfresco.archivals.bootstrap;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class RenameOldPrimaryArchivalsRootBootstrap extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(RenameOldPrimaryArchivalsRootBootstrap.class);

    private GeneralService generalService;

    @Override
    protected void executeInternal() throws Throwable {
        NodeService nodeService = serviceRegistry.getNodeService();
        StoreRef archivalsStoreRef = generalService.getArchivalsStoreRef();
        if (!nodeService.exists(archivalsStoreRef)) {
            LOG.info("Skipping renaming /fn:archivals to /fn:documentList, store does not exist: " + archivalsStoreRef);
            return;
        }
        NodeRef nodeRef = generalService.getNodeRef("/fn:archivals", archivalsStoreRef);
        if (nodeRef == null) {
            LOG.info("Skipping renaming /fn:archivals to /fn:documentList, node does not exist: /fn:archivals");
            return;
        }
        LOG.info("Renaming /fn:archivals to /fn:documentList in " + archivalsStoreRef);
        NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        ChildAssociationRef oldAssocRef = nodeService.getPrimaryParent(nodeRef);
        ChildAssociationRef newAssocRef = nodeService.moveNode(nodeRef, parentRef, ContentModel.ASSOC_CHILDREN, QName.createQName(FunctionsModel.URI, "documentList"));
        LOG.info("Renaming completed:\n  old " + oldAssocRef + "\n  new " + newAssocRef);
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
