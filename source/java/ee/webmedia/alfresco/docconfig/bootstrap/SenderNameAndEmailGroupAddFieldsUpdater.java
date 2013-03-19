package ee.webmedia.alfresco.docconfig.bootstrap;

import java.io.Serializable;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

/**
 * Updater that adds senderInitialsToAdr field to SenderNameAndEmail field group
 * 
 * @author Kaarel Jõgeva
 */
public class SenderNameAndEmailGroupAddFieldsUpdater extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SenderNameAndEmailGroupAddFieldsUpdater.class);

    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());
        NodeRef senderNameAndEmailGroupDefinitionRef = generalService.getNodeRef("/docadmin:fieldGroupDefinitions/docadmin:senderNameAndEmail");
        if (senderNameAndEmailGroupDefinitionRef == null) {
            LOG.info("Could not find senderNameAndEmail field group definition. Stopping updater.");
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> existingIds = (List<String>) nodeService.getProperty(senderNameAndEmailGroupDefinitionRef, DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS);
        // Field group already contains this ID
        if (existingIds.contains(DocumentDynamicModel.Props.SENDER_INITIALS_TO_ADR.getLocalName())) {
            LOG.info("senderInitialsToAdr field already added to group. Stopping updater.");
            return;
        }
        existingIds.add(DocumentDynamicModel.Props.SENDER_INITIALS_TO_ADR.getLocalName());
        nodeService.setProperty(senderNameAndEmailGroupDefinitionRef, DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS, (Serializable) existingIds);
        LOG.info("Finished updating senderNameAndEmail field group definition.");
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
