package ee.webmedia.alfresco.docconfig.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

public class ContactSystematicGroupAddFieldsUpdater extends AbstractModuleComponent {

    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        NodeRef contactGroupDefinitionRef = generalService.getNodeRef("/docadmin:fieldGroupDefinitions/docadmin:contact");
        if (contactGroupDefinitionRef != null) {
            @SuppressWarnings("unchecked")
            List<String> existingIds = (List<String>) nodeService.getProperty(contactGroupDefinitionRef, DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS);
            List<String> idsToAdd = new ArrayList<String>();
            idsToAdd.add(DocumentDynamicModel.Props.CONTACT_STREET_HOUSE.getLocalName());
            idsToAdd.add(DocumentDynamicModel.Props.CONTACT_POSTAL_CITY.getLocalName());
            idsToAdd.add(DocumentDynamicModel.Props.CONTACT_FIRST_ADDITIONAL_EMAIL.getLocalName());
            idsToAdd.add(DocumentDynamicModel.Props.CONTACT_SECOND_ADDITIONAL_EMAIL.getLocalName());
            idsToAdd.add(DocumentDynamicModel.Props.CONTACT_FIRST_ADDITIONAL_PHONE.getLocalName());
            idsToAdd.add(DocumentDynamicModel.Props.CONTACT_SECOND_ADDITIONAL_PHONE.getLocalName());
            for (Iterator<String> i = idsToAdd.iterator(); i.hasNext();) {
                String idToAdd = i.next();
                if (existingIds.contains(idToAdd)) {
                    i.remove();
                }
            }
            if (!idsToAdd.isEmpty()) {
                for (int i = 0; i < existingIds.size(); i++) {
                    String existingId = existingIds.get(i);
                    if (DocumentDynamicModel.Props.CONTACT_ADDRESS.getLocalName().equals(existingId)) {
                        if (idsToAdd.contains(DocumentDynamicModel.Props.CONTACT_STREET_HOUSE.getLocalName())) {
                            existingIds.add(++i, DocumentDynamicModel.Props.CONTACT_STREET_HOUSE.getLocalName());
                        }
                        if (idsToAdd.contains(DocumentDynamicModel.Props.CONTACT_POSTAL_CITY.getLocalName())) {
                            existingIds.add(++i, DocumentDynamicModel.Props.CONTACT_POSTAL_CITY.getLocalName());
                        }
                    } else if (DocumentDynamicModel.Props.CONTACT_EMAIL.getLocalName().equals(existingId)) {
                        if (idsToAdd.contains(DocumentDynamicModel.Props.CONTACT_FIRST_ADDITIONAL_EMAIL.getLocalName())) {
                            existingIds.add(++i, DocumentDynamicModel.Props.CONTACT_FIRST_ADDITIONAL_EMAIL.getLocalName());
                        }
                        if (idsToAdd.contains(DocumentDynamicModel.Props.CONTACT_SECOND_ADDITIONAL_EMAIL.getLocalName())) {
                            existingIds.add(++i, DocumentDynamicModel.Props.CONTACT_SECOND_ADDITIONAL_EMAIL.getLocalName());
                        }
                    } else if (DocumentDynamicModel.Props.CONTACT_PHONE.getLocalName().equals(existingId)) {
                        if (idsToAdd.contains(DocumentDynamicModel.Props.CONTACT_FIRST_ADDITIONAL_PHONE.getLocalName())) {
                            existingIds.add(++i, DocumentDynamicModel.Props.CONTACT_FIRST_ADDITIONAL_PHONE.getLocalName());
                        }
                        if (idsToAdd.contains(DocumentDynamicModel.Props.CONTACT_SECOND_ADDITIONAL_PHONE.getLocalName())) {
                            existingIds.add(++i, DocumentDynamicModel.Props.CONTACT_SECOND_ADDITIONAL_PHONE.getLocalName());
                        }
                    }

                }
                nodeService.setProperty(contactGroupDefinitionRef, DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS, (Serializable) existingIds);
            }
        }
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
