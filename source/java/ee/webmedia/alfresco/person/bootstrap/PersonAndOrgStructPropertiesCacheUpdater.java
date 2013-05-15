package ee.webmedia.alfresco.person.bootstrap;

import java.util.List;
import java.util.Set;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructureModel;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ProgressTracker;

/**
 * Fill person and organization structure properties cache.
 * 
 * @author Riina Tens
 */
public class PersonAndOrgStructPropertiesCacheUpdater extends AbstractModuleComponent {

    public static final String BEAN_NAME = "PersonAndOrgStructPropertiesCacheUpdater";
    private static final Log LOG = LogFactory.getLog(PersonAndOrgStructPropertiesCacheUpdater.class);

    private GeneralService generalService;
    private PersonService personService;
    private UserService userService;
    private OrganizationStructureService organizationStructureService;
    private NodeService nodeService;
    private boolean running;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("PersonAndOrgStructPropertiesCacheUpdater started.");
        running = true;
        generalService.runOnBackground(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                try {
                    updatePersonCache();
                    updateOrgStructCache();
                    LOG.info("PersonAndOrgStructPropertiesCacheUpdater finished.");
                    return null;
                } finally {
                    running = false;
                }
            }

        }, "loadPersonPropertiesCache", true);
    }

    private void updatePersonCache() {
        Set<String> usernames = userService.getAllUsersUsernames();
        if (usernames.isEmpty()) {
            return;
        }
        int userCount = usernames.size();
        LOG.info("Updating person properties cache for " + userCount + " persons");
        int count = 0;
        ProgressTracker progress = new ProgressTracker(userCount, 0);
        for (String userName : usernames) {
            personService.getPersonProperties(userName);
            if (++count >= 100) {
                String info = progress.step(count);
                count = 0;
                if (info != null) {
                    LOG.info("Updated person properties cache: " + info);
                }
            }
        }
        String info = progress.step(count);
        if (info != null) {
            LOG.info("Updated person properties cache: " + info);
        }
    }

    private void updateOrgStructCache() {
        List<NodeRef> orgStructRefs = organizationStructureService.getAllOrganizationStructureRefs();
        if (orgStructRefs.isEmpty()) {
            return;
        }
        int orgStructCount = orgStructRefs.size();
        LOG.info("Updating organization structure properties cache for " + orgStructCount + " organization structures");
        int count = 0;
        ProgressTracker progress = new ProgressTracker(orgStructCount, 0);
        for (NodeRef orgStructRef : orgStructRefs) {
            String unitId = (String) nodeService.getProperty(orgStructRef, OrganizationStructureModel.Props.UNIT_ID);
            organizationStructureService.getOrganizationStructure(unitId);
            if (++count >= 100) {
                String info = progress.step(count);
                count = 0;
                if (info != null) {
                    LOG.info("Updated organization structure properties cache: " + info);
                }
            }
        }
        String info = progress.step(count);
        if (info != null) {
            LOG.info("Updated organization structure properties cache: " + info);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
