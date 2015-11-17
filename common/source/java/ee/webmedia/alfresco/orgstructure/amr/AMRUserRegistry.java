package ee.webmedia.alfresco.orgstructure.amr;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.alfresco.repo.security.sync.NodeDescription;
import org.alfresco.repo.security.sync.UserRegistry;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import smit.ametnik.services.Aadress;
import smit.ametnik.services.AmetnikExt;
import smit.ametnik.services.YksusExt;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.orgstructure.amr.service.AMRService;
import ee.webmedia.alfresco.orgstructure.amr.service.RSService;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructureModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * A {@link UserRegistry} implementation with the ability to query Alfresco-like descriptions of users and groups from a SIM "Ametnikeregister".
 */
public class AMRUserRegistry implements UserRegistry, ActivateableBean {
    private static Log log = LogFactory.getLog(AMRUserRegistry.class);

    private UserService userService;
    private AMRService amrService;
    private RSService rsService;
    private ApplicationService applicationService;
    private String testEmail;

    /** Is this bean active? I.e. should this part of the subsystem be used? */
    private boolean active = true;

    @Override
    public Iterator<NodeDescription> getPersons(Date modifiedSince) {
        AmetnikExt[] ametnikArray = amrService.getAmetnikByAsutusId();
        ArrayList<NodeDescription> persons = new ArrayList<NodeDescription>(ametnikArray.length);
        boolean isRestrictedDelta = rsService.isRestrictedDelta();
        List<String> restrictedDeltaUsers = new ArrayList<String>();
        if (isRestrictedDelta) {
            // avoid retrieving restricted delta users if not in restricted delta
            restrictedDeltaUsers = Arrays.asList(rsService.getIsikukoodByAsutusIdAndHasRsLubaRequest());
        }
        for (AmetnikExt ametnik : ametnikArray) {
            if (checkRestrictedDeltaUsers(isRestrictedDelta, restrictedDeltaUsers, ametnik)) {
                continue;
            }
            NodeDescription person = mergePersonDescription(ametnik);
            person.setLastModified(new Date());// actually should be when modified in remote system
            persons.add(person);
            if (log.isDebugEnabled()) {
                log.debug("firstName=" + ametnik.getEesnimi() + "; lastName=" + ametnik.getPerekonnanimi() + "; id=" + ametnik.getIsikukood() + "; unitId="
                        + ametnik.getYksusId() + "; jobTitle=" + ametnik.getAmetikoht() + "; phone=" + ametnik.getKontakttelefon() + "; email="
                        + ametnik.getEmail());
            }
        }
        return persons.iterator();
    }

    private boolean checkRestrictedDeltaUsers(boolean isRestrictedDelta, List<String> restrictedDeltaUsers, AmetnikExt ametnik) {
        return isRestrictedDelta && !restrictedDeltaUsers.contains(ametnik.getIsikukood());
    }

    @Override
    public Iterator<NodeDescription> getPersonByIdCode(String idCode) {
        AmetnikExt ametnik = amrService.getAmetnikByIsikukood(idCode);
        if (ametnik == null) {
            return Collections.<NodeDescription> emptyList().iterator();
        }
        // TODO merge logic from AMRSimpleAuthenticationImpl to here (RsAccessStatusBean and other...)
        if (rsService.isRestrictedDelta() && !rsService.hasRsLubaByIsikukood(idCode)) {
            return Collections.<NodeDescription> emptyList().iterator();
        }
        return Collections.singleton(mergePersonDescription(ametnik)).iterator();
    }

    @Override
    public Iterator<NodeDescription> getPersonByUsername(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<NodeDescription> getGroups(Date modifiedSince) {
        return Collections.<NodeDescription> emptyList().iterator();
    }

    /**
     * @param ametnik
     * @return NodeDescription with properties from given <code>ametnik</code><br>
     *         (merged with propertis that person with the same userName has - if such person exists)
     */
    private NodeDescription mergePersonDescription(AmetnikExt ametnik) {
        Map<QName, Serializable> properties;
        NodeDescription person = new NodeDescription();
        properties = person.getProperties();
        Map<QName, Serializable> existingProperties = userService.getUserProperties(ametnik.getIsikukood());
        if (existingProperties != null) {
            properties.putAll(existingProperties);
        }
        fillPropertiesFromAmetnik(ametnik, properties);
        return person;
    }

    /**
     * Reads properties from <code>AmetnikExt</code> object and puts them into <code>properties</code>
     * 
     * @param ametnik
     * @param properties
     */
    public void fillPropertiesFromAmetnik(AmetnikExt ametnik, Map<QName, Serializable> properties) {
        String email = ametnik.getEmail();

        // This is actually implemented in ChainingUserRegistrySynchronizer and the correct thing to do would be to
        // switch from AMRSimpleAuthenticationImpl to SimpleUpdatingAuthenticationComponentImpl - this has been the idea all along, but needs testing
        if (applicationService.isTest()) {
            email = testEmail;
        }

        BigInteger yksusId = ametnik.getYksusId();
        if (BigInteger.valueOf(-1).equals(yksusId)) {
            yksusId = null;
        }
        properties.put(ContentModel.PROP_ORGID, yksusId != null ? yksusId.toString() : null);
        properties.put(ContentModel.PROP_EMAIL, email);
        properties.put(ContentModel.PROP_USERNAME, ametnik.getIsikukood());
        properties.put(ContentModel.PROP_FIRSTNAME, ametnik.getEesnimi());
        properties.put(ContentModel.PROP_LASTNAME, ametnik.getPerekonnanimi());
        properties.put(ContentModel.PROP_TELEPHONE, ametnik.getKontakttelefon());
        properties.put(ContentModel.PROP_JOBTITLE, ametnik.getAmetikoht());
        properties.put(ContentModel.PROP_ORGANIZATION_PATH, (ArrayList<String>) UserUtil.formatYksusRadaToOrganizationPath(ametnik.getYksusRada()));
        Aadress aadress = ametnik.getAadress();
        if (aadress != null) {
            properties.put(ContentModel.PROP_COUNTY, aadress.getMaakond());
            properties.put(ContentModel.PROP_MUNICIPALITY, aadress.getOmavalitsus());
            properties.put(ContentModel.PROP_VILLAGE, aadress.getAsustusYksus());
            properties.put(ContentModel.PROP_STREET_HOUSE, aadress.getKohanimi());
            properties.put(ContentModel.PROP_POSTAL_CODE, aadress.getSihtkood());
        }
        properties.put(ContentModel.PROP_SERVICE_RANK, ametnik.getTeenistusaste());
    }

    @Override
    public Iterator<NodeDescription> getOrganizationStructures() {
        YksusExt[] yksusArray = amrService.getYksusByAsutusId();
        List<NodeDescription> orgStructures = new ArrayList<NodeDescription>(yksusArray.length);
        for (YksusExt yksus : yksusArray) {
            orgStructures.add(yksusToOrganizationStructure(yksus));
        }
        return orgStructures.iterator();
    }

    private NodeDescription yksusToOrganizationStructure(YksusExt yksus) {
        NodeDescription org = new NodeDescription();
        org.getProperties().put(OrganizationStructureModel.Props.UNIT_ID, yksus.getId().toString());
        org.getProperties().put(OrganizationStructureModel.Props.NAME, yksus.getNimetus());
        BigInteger ylemYksusId = yksus.getYlemYksusId();
        if (ylemYksusId != null) {
            org.getProperties().put(OrganizationStructureModel.Props.SUPER_UNIT_ID, ylemYksusId.toString());
        }
        String email = yksus.getEmail();
        if (email != null) {
            org.getProperties().put(OrganizationStructureModel.Props.GROUP_EMAIL, email);
        }
        Serializable organizationPath = (Serializable) UserUtil.formatYksusRadaToOrganizationPath(yksus.getYksusRada());
        org.getProperties().put(OrganizationStructureModel.Props.ORGANIZATION_PATH, organizationPath);
        return org;
    }

    // START: getters / setters
    /**
     * Indicates whether this bean is active. I.e. should this part of the subsystem be used?
     * 
     * @param active <code>true</code> if this bean is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setAmrService(AMRService amrService) {
        this.amrService = amrService;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setTestEmail(String testEmail) {
        this.testEmail = testEmail;
    }

    public void setRsService(RSService rsService) {
        this.rsService = rsService;
    }

    // END: getters / setters

}
