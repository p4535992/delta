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
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import smit.ametnik.services.Ametnik;
import ee.webmedia.alfresco.orgstructure.amr.service.AMRService;

/**
 * A {@link UserRegistry} implementation with the ability to query Alfresco-like descriptions of users and groups from a SIM "Ametnikeregister".
 * 
 * @author Ats Uiboupin
 */
public class AMRUserRegistry implements UserRegistry, ActivateableBean {
    private static Log log = LogFactory.getLog(AMRUserRegistry.class);

    private PersonService personService;
    private NodeService nodeService;
    private AMRService amrService;

    /** Is this bean active? I.e. should this part of the subsystem be used? */
    private boolean active = true;

    @Override
    public Iterator<NodeDescription> getPersons(Date modifiedSince) {
        Ametnik[] ametnikArray = amrService.getAmetnikByAsutusId();
        ArrayList<NodeDescription> persons = new ArrayList<NodeDescription>(ametnikArray.length);
        for (Ametnik ametnik : ametnikArray) {
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
        throw new UnsupportedOperationException();
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
    private NodeDescription mergePersonDescription(Ametnik ametnik) {
        Map<QName, Serializable> properties;
        NodeDescription person = new NodeDescription();
        properties = person.getProperties();
        try {
            Map<QName, Serializable> existingProperties = nodeService.getProperties(personService.getPerson(ametnik.getIsikukood()));
            properties.putAll(existingProperties);
        } catch (NoSuchPersonException e) {
            // fall-through: creating new person
        }
        fillPropertiesFromAmetnik(ametnik, properties);
        return person;
    }

    /**
     * Reads properties from <code>Ametnik</code> object and puts them into <code>properties</code>
     * 
     * @param ametnik
     * @param properties
     */
    public void fillPropertiesFromAmetnik(Ametnik ametnik, Map<QName, Serializable> properties) {
        String email = ametnik.getEmail();
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

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setAmrService(AMRService amrService) {
        this.amrService = amrService;
    }
    // END: getters / setters

}
