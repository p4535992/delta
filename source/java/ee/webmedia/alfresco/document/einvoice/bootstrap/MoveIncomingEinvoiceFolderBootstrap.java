package ee.webmedia.alfresco.document.einvoice.bootstrap;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.imap.model.ImapModel;

/**
 * Moves /imap-ext:receivedInvoices (initial recieved invoices folder) to imap root folder
 * 
 * @author Riina Tens
 */
public class MoveIncomingEinvoiceFolderBootstrap extends AbstractModuleComponent {
    private static final String INITIAL_RECEIVED_INVOICES = "/imap-ext:receivedInvoices";
    private static final String IMAP_ROOT = "/imap-ext:imap-root";
    private static QName RECEIVED_INVOICES = QName.createQName(ImapModel.URI, "receivedInvoices");
    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        final NodeRef nodeRef = generalService.getNodeRef(INITIAL_RECEIVED_INVOICES);
        if (nodeRef != null) {
            nodeService.moveNode(nodeRef, generalService.getNodeRef(IMAP_ROOT), ContentModel.ASSOC_CONTAINS, RECEIVED_INVOICES);
        }
    }

    // START: getters / setters

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    // END: getters / setters
}
