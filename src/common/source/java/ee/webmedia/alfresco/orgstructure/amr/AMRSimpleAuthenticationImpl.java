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
import org.springframework.ws.soap.client.SoapFaultClientException;

import smit.ametnik.services.Ametnik;
import ee.webmedia.alfresco.orgstructure.amr.service.AMRService;

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
            Ametnik user = amrService.getAmetnikByIsikukood(userName);
            if (user == null) {
                log.debug("Didn't manage to get user with id '" + userName + "' from AMRService.");
                throw new AMRAuthenticationException("Didn't manage to get user with id '" + userName + "' from AMRService.");
            }
            if (!StringUtils.equals(userName, user.getIsikukood())) {
                throw new AuthenticationException("Social security id is supposed to be equal to userName");
            }
            NodeRef person = getPersonService().getPerson(userName);
            Map<QName, Serializable> personProperties = getNodeService().getProperties(person);
            userRegistry.fillPropertiesFromAmetnik(user, personProperties);
            getPersonService().setPersonProperties(userName, personProperties);
            addToAuthorityZone(person, userName, "AUTH.EXT.amr1");
        } catch (SoapFaultClientException e) {
            log.error("Didn't manage to get user with id '" + userName + "' from AMRService.", e);
            throw new AMRAuthenticationException("Didn't manage to get user with id '" + userName + "' from AMRService.", e);
        }
    }

    private void addToAuthorityZone(NodeRef personRef, String userName, String zone) {
        // Add the person to an authentication zone (corresponding to an external user registry)
        // Let's preserve case on this child association
        final List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(authorityService.getOrCreateZone(zone) //
                , ContentModel.ASSOC_IN_ZONE, QName.createQName("cm", userName, namespacePrefixResolver));
        if (childAssocs.size() == 0) { // is person already added to given zone ?
            getNodeService().addChild(authorityService.getOrCreateZone(zone), personRef //
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
