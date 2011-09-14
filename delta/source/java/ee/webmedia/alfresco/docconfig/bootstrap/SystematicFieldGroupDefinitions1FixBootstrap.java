package ee.webmedia.alfresco.docconfig.bootstrap;

import java.util.ArrayList;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

public class SystematicFieldGroupDefinitions1FixBootstrap extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SystematicFieldGroupDefinitions1FixBootstrap.class);

    private GeneralService generalService;

    @Override
    protected void executeInternal() throws Throwable {
        String documentOwnerXPath = DocumentAdminModel.Repo.FIELD_GROUP_DEFINITIONS_SPACE + "/docadmin:documentOwner";
        NodeRef nodeRef = generalService.getNodeRef(documentOwnerXPath);
        @SuppressWarnings("unchecked")
        ArrayList<QName> fieldDefinitionIds = (ArrayList<QName>) serviceRegistry.getNodeService().getProperty(nodeRef, DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS);
        LOG.info("documentOwner.fieldDefinitionIds=" + fieldDefinitionIds);
        if (fieldDefinitionIds.size() == 5) {
            fieldDefinitionIds.add(1, DocumentDynamicModel.Props.OWNER_SERVICE_RANK);
            fieldDefinitionIds.add(4, DocumentDynamicModel.Props.OWNER_WORK_ADDRESS);
            LOG.info("documentOwner.fieldDefinitionIds=" + fieldDefinitionIds);
            serviceRegistry.getNodeService().setProperty(nodeRef, DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS, fieldDefinitionIds);
        }
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
