package ee.webmedia.alfresco.user.service;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Authentication via JAAS - we are using it to authenticate with Kerberos (against Active Directory)
 * 
 * For JAAS to work, a configuration file needs to exist and some environment variables need to be set.
 * Currently this is done in CAS application - it means that CAS application needs to be deployed on the same application server as this application.
 */
public class JAASAuthenticationComponent extends SimpleUpdatingAuthenticationComponentImpl {
    private static final Log log = LogFactory.getLog(JAASAuthenticationComponent.class);

    private String jaasConfigEntryName = "CAS";

    public JAASAuthenticationComponent() {
        super();
        setQueryByIdCode(false); // Query by username
    }

    @Override
    protected void authenticateImpl(String userName, char[] password) throws AuthenticationException {
        log.debug("Trying to authenticate username '" + userName + "'");
        LoginContext lc;
        try {
            lc = new LoginContext(jaasConfigEntryName, new SimpleCallback(userName, password));
            log.trace("LoginContext creation succeeded");
            lc.login();
            log.trace("Login succeeded");
            lc.logout();
            log.trace("Logout succeeded");
            log.debug("Authenticate succeeded for username '" + userName + "'");
            // Login has gone through OK, set up the acegi context
            setCurrentUser(userName);
            log.trace("SetCurrentUser succeeded");
        } catch (LoginException e) {
            log.error("Authentication failed", e);
            throw new AuthenticationException("Authenticaion failed for '" + userName + "': " + e.getMessage(), e);
        }
    }

    private static class SimpleCallback implements CallbackHandler {

        String userName;
        char[] password;

        SimpleCallback(String userName, char[] password) {
            this.userName = userName;
            this.password = password;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback cb = (NameCallback) callbacks[i];
                    cb.setName(userName);
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback cb = (PasswordCallback) callbacks[i];
                    cb.setPassword(password);
                } else {
                    throw new UnsupportedCallbackException(callbacks[i]);
                }
            }
        }
    }

    public void setJaasConfigEntryName(String jaasConfigEntryName) {
        this.jaasConfigEntryName = jaasConfigEntryName;
    }

}
