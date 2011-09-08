package ee.webmedia.alfresco.orgstructure.amr;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
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
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.orgstructure.amr.service.AMRService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * A {@link UserRegistry} implementation with the ability to query Alfresco-like descriptions of users and groups from a SIM "Ametnikeregister".
 * 
 * @author Ats Uiboupin
 */
public class AMRUserRegistry implements UserRegistry, ActivateableBean {
    private static Log log = LogFactory.getLog(AMRUserRegistry.class);

    private UserService userService;
    private AMRService amrService;
    private ApplicationService applicationService;
    private String testEmail;

    /** Is this bean active? I.e. should this part of the subsystem be used? */
    private boolean active = true;

    @Override
    public Iterator<NodeDescription> getPersons(Date modifiedSince) {
        AmetnikExt[] ametnikArray = amrService.getAmetnikByAsutusId();
        ArrayList<NodeDescription> persons = new ArrayList<NodeDescription>(ametnikArray.length);
        for (AmetnikExt ametnik : ametnikArray) {
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

    @Override
    public Iterator<NodeDescription> getPersonByIdCode(String idCode) {
        AmetnikExt ametnik = amrService.getAmetnikByIsikukood(idCode);
        if (ametnik == null) {
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
        properties.put(ContentModel.PROP_ORGID, yksusId);
        properties.put(ContentModel.PROP_EMAIL, email);
        properties.put(ContentModel.PROP_USERNAME, ametnik.getIsikukood());
        properties.put(ContentModel.PROP_FIRSTNAME, ametnik.getEesnimi());
        properties.put(ContentModel.PROP_LASTNAME, ametnik.getPerekonnanimi());
        properties.put(ContentModel.PROP_TELEPHONE, ametnik.getKontakttelefon());
        properties.put(ContentModel.PROP_JOBTITLE, ametnik.getAmetikoht());
        properties.put(ContentModel.PROP_ORGANIZATION_PATH, UserUtil.formatYksusRadaToOrganizationPath(ametnik.getYksusRada()));
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
    // END: getters / setters

}
