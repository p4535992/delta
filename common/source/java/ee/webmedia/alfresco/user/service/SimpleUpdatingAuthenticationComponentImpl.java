package ee.webmedia.alfresco.user.service;

import net.sf.acegisecurity.Authentication;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AbstractAuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleUpdatingAuthenticationComponentImpl extends AbstractAuthenticationComponent {
    private static final Log log = LogFactory.getLog(SimpleUpdatingAuthenticationComponentImpl.class);

    private boolean queryByIdCode = true;

    public void setQueryByIdCode(boolean queryByIdCode) {
        this.queryByIdCode = queryByIdCode;
    }

    @Override
    protected boolean implementationAllowsGuestLogin() {
        return false;
    }

    @Override
    protected void authenticateImpl(String userId, char[] password) {
        setCurrentUser(userId);
    }

    // Identical to parent class method
    @Override
    public Authentication setCurrentUser(String idCodeOrUsername) throws AuthenticationException {
        if (isSystemUserName(idCodeOrUsername)) {
            throw new AuthenticationException("System user not allowed");
        } else {
            SetCurrentUserCallback callback = new SetCurrentUserCallback(idCodeOrUsername);
            Authentication auth;
            // If the repository is read only, we have to settle for a read only transaction. Auto user creation will
            // not be possible.
            if (getTransactionService().isReadOnly()) {
                auth = getTransactionService().getRetryingTransactionHelper().doInTransaction(callback, true, false);
            }
            // Otherwise, we want a writeable transaction, so if the current transaction is read only we set the
            // requiresNew flag to true
            else {
                auth = getTransactionService().getRetryingTransactionHelper().doInTransaction(callback, false,
                        AlfrescoTransactionSupport.getTransactionReadState() == TxnReadState.TXN_READ_ONLY);
            }
            if ((auth == null) || (callback.ae != null)) {
                throw callback.ae;
            }
            return auth;
        }
    }

    class SetCurrentUserCallback implements RetryingTransactionHelper.RetryingTransactionCallback<Authentication> {
        AuthenticationException ae = null;

        String idCodeOrUsername;

        SetCurrentUserCallback(String idCodeOrUsername) {
            this.idCodeOrUsername = idCodeOrUsername;
        }

        @Override
        public Authentication execute() throws Throwable {
            try {
                String name = AuthenticationUtil.runAs(new RunAsWork<String>()
                {
                    @Override
                    public String doWork() throws Exception
                    {
                        log.debug("Trying to synchronize user with " + (queryByIdCode ? "idCode" : "username") + " '" + idCodeOrUsername + "'");
                        String userId;
                        if (queryByIdCode) {
                            userId = getUserRegistrySynchronizer().createOrUpdatePersonByIdCode(idCodeOrUsername);
                        } else {
                            userId = getUserRegistrySynchronizer().createOrUpdatePersonByUsername(idCodeOrUsername);
                        }
                        if (userId != null)
                        {
                            // If above returns true, then person exists
                            // And the following call will not create a new person

                            log.debug("Synchronization returned userId '" + userId + "'");
                            NodeRef userNode = getPersonService().getPerson(userId);
                            log.trace("GetPerson returned " + userNode);
                            if (userNode != null)
                            {
                                // Get the person name and use that as the current user to line up with permission
                                // checks
                                String userIdFromRepository = (String) getNodeService().getProperty(userNode, ContentModel.PROP_USERNAME);
                                log.trace("UserIdFromRepository=" + userIdFromRepository);
                                return userIdFromRepository;
                            } else {
                                log.warn("Synchronization returned userId '" + userId + "', but person does not exist");
                            }
                        } else {
                            log.debug("Synchronization returned no userId");
                        }
                        throw new UserNotFoundException("Person '" + idCodeOrUsername + "' does not exist in Alfresco");
                    }
                }, getSystemUserName(getUserDomain(idCodeOrUsername)));

                return setCurrentUser(name, UserNameValidationMode.NONE);
            } catch (AuthenticationException ae) {
                this.ae = ae;
                return null;
            }
        }
    }

}
