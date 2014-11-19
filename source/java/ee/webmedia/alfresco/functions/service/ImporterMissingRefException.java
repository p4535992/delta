<<<<<<< HEAD
package ee.webmedia.alfresco.functions.service;

import org.alfresco.service.cmr.view.ImporterException;

/**
 * Thrown to notify that found reference that doesn't refer to existing node
 * 
 * @author Ats Uiboupin
 */
public class ImporterMissingRefException extends ImporterException {

    private static final long serialVersionUID = -3267292062787517743L;

    public ImporterMissingRefException(String msg) {
        super(msg);
    }

}
=======
package ee.webmedia.alfresco.functions.service;

import org.alfresco.service.cmr.view.ImporterException;

/**
 * Thrown to notify that found reference that doesn't refer to existing node
 */
public class ImporterMissingRefException extends ImporterException {

    private static final long serialVersionUID = -3267292062787517743L;

    public ImporterMissingRefException(String msg) {
        super(msg);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
