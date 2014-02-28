package ee.webmedia.alfresco.orgstructure.amr;

import static ee.webmedia.alfresco.common.web.BeanHelper.getApplicationService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDictionaryService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getRsAccessStatusBean;
import static ee.webmedia.alfresco.utils.RepoUtil.getPropertiesIgnoringSystem;
import static ee.webmedia.alfresco.utils.UserUtil.getPersonFullName1;
import static ee.webmedia.alfresco.utils.UserUtil.getUserFullNameAndId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.acegisecurity.Authentication;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.SimpleAcceptOrRejectAllAuthenticationComponentImpl;
import org.alfresco.repo.security.person.PersonServiceImpl;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.WebServiceTransportException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import smit.ametnik.services.AmetnikExt;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.orgstructure.amr.service.AMRService;
import ee.webmedia.alfresco.orgstructure.amr.service.RSService;
import ee.webmedia.alfresco.user.service.UserNotFoundException;

/**
 * Authenticator that uses AMRService for authentication. SYSTEM_USER_NAME will go through without amrService call, <br>
 * but other userNames will pass only if amrService returns a user with given userName.
 * 
 * @author Ats Uiboupin
 */
public class AMRSimpleAuthenticationImpl extends SimpleAcceptOrRejectAllAuthenticationComponentImpl {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AMRSimpleAuthenticationImpl.class);
    private AMRService amrService;
    private RSService rsService;
    private NamespacePrefixResolver namespacePrefixResolver;
    private AuthorityService authorityService;
    private AMRUserRegistry userRegistry;

    public AMRSimpleAuthenticationImpl() {
        super();
    }

    @Override
    public Authentication setCurrentUser(String userName) throws AuthenticationException {
        updateUserData(userName);
        return super.setCurrentUser(userName);
    }

    private void updateUserData(String userName) {
        if (isSystemUserName(userName)) {
            log.info("Not updating person details of system user '" + userName + "'");
            return;
        }
        try {
            AmetnikExt user = amrService.getAmetnikByIsikukood(userName);
            if (user == null) {
                String msg = "Didn't manage to get user with id '" + userName + "' from AMRService.";
                log.debug(msg);
                throw new UserNotFoundException("Didn't manage to get user with id '" + userName + "' from AMRService.");
            }
            log.debug("Found user with id '" + userName + "'");
            if (!StringUtils.equals(userName, user.getIsikukood())) {
                throw new AuthenticationException("Social security id is supposed to be equal to userName");
            }
            boolean hasRsAccess = false;
            if (rsService.isRestrictedDelta() || StringUtils.isNotBlank(((WebServiceTemplate) rsService).getDefaultUri())) {
                hasRsAccess = rsService.hasRsLubaByIsikukood(userName);
            }
            if (rsService.isRestrictedDelta() && !hasRsAccess) {
                throw new AuthenticationException("User " + userName + " has been granted no access to this instance of restricted Delta.");
            }
            getRsAccessStatusBean().setCanUserAccessRestrictedDelta(hasRsAccess);
            NodeRef person = null;
            try {
                person = getPersonService().getPerson(userName);
                Map<QName, Serializable> personProperties = getNodeService().getProperties(person);
                Map<QName, Serializable> personOldProperties = getPropertiesIgnoringSystem(personProperties, getDictionaryService());
                userRegistry.fillPropertiesFromAmetnik(user, personProperties);
                getPersonService().setPersonProperties(userName, personProperties);
                String diff = new PropDiffHelper().watchUser().diff(personOldProperties, getPropertiesIgnoringSystem(personProperties, getDictionaryService()));
                if (diff != null) {
                    getLogService().addLogEntry(LogEntry.create(LogObject.USER, userName, getPersonFullName1(personOldProperties), "applog_user_edit",
                            getUserFullNameAndId(personProperties), diff));
                }
            } catch (NoSuchPersonException e) {
                // try to create person
            }
            if (person == null) {
                PersonServiceImpl.validCreatePersonCall.set(Boolean.TRUE);
                try {
                    Map<QName, Serializable> personProperties = new HashMap<QName, Serializable>();
                    userRegistry.fillPropertiesFromAmetnik(user, personProperties);
                    person = getPersonService().createPerson(personProperties);
                    person = getPersonService().getPerson(userName); // creates home folder if necessary
                } finally {
                    PersonServiceImpl.validCreatePersonCall.set(null);
                }
            }
            addToAuthorityZone(person, userName, "AUTH.EXT.amr1");
        } catch (WebServiceTransportException e) {
            if (StringUtils.equals(e.getMessage(), "Not Found [404]")) {
                log.warn("AMRService is not responding", e);
            } else if (StringUtils.equals(e.getMessage(), "Service Unavailable [503]")) {
                log.warn("AMRService is not available", e);
            } else {
                throw e;
            }
        } catch (WebServiceIOException e) {
            log.warn("AMRService is not available", e);
        } catch (SoapFaultClientException e) {
            String msg = "Didn't get response from AMR to get user with id '" + userName + "'";
            if (getApplicationService().isTest()) {
                log.warn(msg + ". Ignoring, as project.test=true"); // Web service is down - Just log in and ignore failure
            } else {
                log.error(msg, e);
                throw new UserNotFoundException("Didn't manage to get user with id '" + userName + "' from AMRService: " + e.getMessage(), e);
            }
        }
    }

    private void addToAuthorityZone(NodeRef personRef, String userName, String zone) {
        // Add the person to an authentication zone (corresponding to an external user registry)
        // Let's preserve case on this child association
        final NodeRef authZone = authorityService.getOrCreateZone(zone);
        final List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(authZone //
                , ContentModel.ASSOC_IN_ZONE, QName.createQName("cm", userName, namespacePrefixResolver));
        if (childAssocs.size() == 0) { // is person already added to given zone ?
            getNodeService().addChild(authZone, personRef //
                    , ContentModel.ASSOC_IN_ZONE, QName.createQName("cm", userName, namespacePrefixResolver));
        }
    }

    // START: getters / setters
    public void setAmrService(AMRService amrService) {
        this.amrService = amrService;
    }

    public void setNamespacePrefixResolver(NamespacePrefixResolver namespacePrefixResolver) {
        this.namespacePrefixResolver = namespacePrefixResolver;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setUserRegistry(AMRUserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    public void setRsService(RSService rsService) {
        this.rsService = rsService;
    }
    // END: getters / setters

}
