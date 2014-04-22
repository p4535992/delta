package ee.webmedia.alfresco.docconfig.bootstrap;

import java.io.Serializable;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

/**
 * Adds recipientId and additionalRecipientId to systematic groups.
 */
public class RecipientIdAddFieldsUpdater extends AbstractModuleComponent {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(RecipientIdAddFieldsUpdater.class);

    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());
        addFieldToFieldGroup("recipients", DocumentDynamicModel.Props.RECIPIENT_ID);
        addFieldToFieldGroup("additionalRecipients", DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_ID);
        LOG.info("Finished updating recipient and additionalRecipient field group definition.");
    }

    private void addFieldToFieldGroup(String groupName, QName field) {
        NodeRef groupDefinitionRef = generalService.getNodeRef("/docadmin:fieldGroupDefinitions/docadmin:" + groupName);
        if (groupDefinitionRef == null) {
            LOG.info("Could not find '" + groupName + "' field group definition. Skipping field group.");
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> existingIds = (List<String>) nodeService.getProperty(groupDefinitionRef, DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS);
        // Check if field group already contains this ID
        if (existingIds.contains(field.getLocalName())) {
            LOG.info(field.getLocalName() + " field already added to '" + groupName + "' group. Skipping field group.");
            return;
        }
        existingIds.add(field.getLocalName());
        nodeService.setProperty(groupDefinitionRef, DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS, (Serializable) existingIds);
        LOG.info("Added '" + field.getLocalName() + "' to '" + groupName + "' group");
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
