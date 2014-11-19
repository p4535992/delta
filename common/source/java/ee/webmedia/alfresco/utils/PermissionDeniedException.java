<<<<<<< HEAD
package ee.webmedia.alfresco.utils;

import org.alfresco.repo.security.permissions.AccessDeniedException;

/**
 * Extends {@link AccessDeniedException} and contains additional information about navigationOutcome for JSF action
 * @author Ats Uiboupin
 */
public class PermissionDeniedException extends AccessDeniedException {
    private static final long serialVersionUID = 1L;

    private final String navigationOutcome;

    public PermissionDeniedException(String msg, String navigationOutcome) {
        this(msg, navigationOutcome, null);
    }

    public PermissionDeniedException(String msg, String navigationOutcome, Throwable cause) {
        super(msg, cause);
        this.navigationOutcome = navigationOutcome;
    }

    public String getNavigationOutcome() {
        return navigationOutcome;
    }
}
=======
package ee.webmedia.alfresco.utils;

import org.alfresco.repo.security.permissions.AccessDeniedException;

/**
 * Extends {@link AccessDeniedException} and contains additional information about navigationOutcome for JSF action
 */
public class PermissionDeniedException extends AccessDeniedException {
    private static final long serialVersionUID = 1L;

    private final String navigationOutcome;

    public PermissionDeniedException(String msg, String navigationOutcome) {
        this(msg, navigationOutcome, null);
    }

    public PermissionDeniedException(String msg, String navigationOutcome, Throwable cause) {
        super(msg, cause);
        this.navigationOutcome = navigationOutcome;
    }

    public String getNavigationOutcome() {
        return navigationOutcome;
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
