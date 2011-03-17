package ee.webmedia.alfresco.orgstructure.service;

import java.util.List;

import org.alfresco.util.BaseAlfrescoSpringTest;

import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;

/**
 * Test {@link OrganizationStructureServiceImpl}
 * 
 * @author Ats Uiboupin
 */
public class OrganizationStructureServiceImplTest extends BaseAlfrescoSpringTest {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OrganizationStructureServiceImplTest.class);
    private static OrganizationStructureService organizationStructureService;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        organizationStructureService = (OrganizationStructureService) this.applicationContext.getBean(OrganizationStructureService.BEAN_NAME);
    }

    public void testWarmUp() {
        log.debug("Warmup completed");
    }

    public void testUpdateOrganisationStructures() {
        int updateCount = organizationStructureService.updateOrganisationStructures();
        assertTrue(updateCount > 0);
        if (log.isDebugEnabled()) {
            List<OrganizationStructure> allOrganizationStructures = organizationStructureService.getAllOrganizationStructures();
            log.debug(allOrganizationStructures.size() + " organizations after updating");
            for (OrganizationStructure org : allOrganizationStructures) {
                log.debug("1\tallOrganizationStructure=" + org);
            }
        }
    }

    public void testGetAllOrganizationStructures() {
        List<OrganizationStructure> organizations = organizationStructureService.getAllOrganizationStructures();
        assertTrue(organizations.size() > 0);
        if (log.isDebugEnabled()) {
            for (OrganizationStructure org : organizations) {
                log.debug("2\tallOrganizationStructure=" + org);
            }
        }
    }

}
