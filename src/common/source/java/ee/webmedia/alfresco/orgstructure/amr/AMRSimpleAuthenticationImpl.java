package ee.webmedia.alfresco.orgstructure.amr;

import java.io.Serializable;
import java.util.Map;

import net.sf.acegisecurity.Authentication;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.SimpleAcceptOrRejectAllAuthenticationComponentImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.ws.soap.client.SoapFaultClientException;

import smit.ametnik.services.Ametnik;
import ee.webmedia.alfresco.common.service.ApplicationService;
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
    private ApplicationService applicationService;
    private String testEmail;

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
            if (!StringUtils.equals(userName, user.getIsikukood())) {
                throw new AuthenticationException("Social security id is supposed to be equal to userName");
            }
            NodeRef person = getPersonService().getPerson(userName);
            Map<QName, Serializable> personProperties = getNodeService().getProperties(person);
            personProperties.put(ContentModel.PROP_USERNAME, user.getIsikukood());
            personProperties.put(ContentModel.PROP_FIRSTNAME, user.getEesnimi());
            personProperties.put(ContentModel.PROP_LASTNAME, user.getPerekonnanimi());
            String email = user.getEmail();
            if (applicationService.isTest()) {
                email = testEmail;
            }
            personProperties.put(ContentModel.PROP_EMAIL, email);
            personProperties.put(ContentModel.PROP_TELEPHONE, user.getKontakttelefon());
            personProperties.put(ContentModel.PROP_JOBTITLE, user.getAmetikoht());
            personProperties.put(ContentModel.PROP_ORGID, user.getYksusId());
            getPersonService().setPersonProperties(userName, personProperties);
        } catch (SoapFaultClientException e) {
            log.error("Didn't manage to get user with id '" + userName + "' from AMRService.", e);
            throw new AMRAuthenticationException("Didn't manage to get user with id '" + userName + "' from AMRService.", e);
        }
    }

    // START: getters / setters
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
