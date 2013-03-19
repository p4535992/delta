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
