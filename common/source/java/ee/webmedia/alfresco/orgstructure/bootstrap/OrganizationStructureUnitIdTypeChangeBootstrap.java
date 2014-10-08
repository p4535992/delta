package ee.webmedia.alfresco.orgstructure.bootstrap;

import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructureModel;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;

/**
 * Set superUnitId from "0" to null on organization structures.
 */
public class OrganizationStructureUnitIdTypeChangeBootstrap extends AbstractModuleComponent {

    private OrganizationStructureService organizationStructureService;

    @Override
    protected void executeInternal() throws Throwable {
        List<OrganizationStructure> orgStructs = organizationStructureService.getAllOrganizationStructures();
        for (OrganizationStructure orgStruct : orgStructs) {
            if ("0".equals(orgStruct.getSuperUnitId())) {
                serviceRegistry.getNodeService().setProperty(orgStruct.getNodeRef(), OrganizationStructureModel.Props.SUPER_UNIT_ID, null);
            }
        }
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

}
