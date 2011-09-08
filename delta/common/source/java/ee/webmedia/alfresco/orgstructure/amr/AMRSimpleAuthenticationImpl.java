package ee.webmedia.alfresco.orgstructure.amr;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import net.sf.acegisecurity.Authentication;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.SimpleAcceptOrRejectAllAuthenticationComponentImpl;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.WebServiceTransportException;
import org.springframework.ws.soap.client.SoapFaultClientException;

import smit.ametnik.services.AmetnikExt;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.orgstructure.amr.service.AMRService;
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
                if (BeanHelper.getApplicationService().isTest()) {
                    log.warn(msg + ". Ignoring, as project.test=true");
                    return;
                }
                log.debug(msg);
                throw new UserNotFoundException("Didn't manage to get user with id '" + userName + "' from AMRService.");
            }
            log.debug("Found user with id '" + userName + "'");
            if (!StringUtils.equals(userName, user.getIsikukood())) {
                throw new AuthenticationException("Social security id is supposed to be equal to userName");
            }
            NodeRef person = getPersonService().getPerson(userName);
            Map<QName, Serializable> personProperties = getNodeService().getProperties(person);
            userRegistry.fillPropertiesFromAmetnik(user, personProperties);
            getPersonService().setPersonProperties(userName, personProperties);
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
            if (BeanHelper.getApplicationService().isTest()) {
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
    // END: getters / setters

}
