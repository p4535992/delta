package ee.webmedia.alfresco.user.service;

import org.alfresco.repo.security.authentication.AuthenticationException;

/**
 * Used to notify {@link SimpleAuthenticationFilter} that user does not exist, even after synchronizing from userRegistry.
 */
public class UserNotFoundException extends AuthenticationException {
    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String msgId) {
        super(msgId);
    }

    public UserNotFoundException(String msgId, Throwable cause) {
        super(msgId, cause);
    }
}
