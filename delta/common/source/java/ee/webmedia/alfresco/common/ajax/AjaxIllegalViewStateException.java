package ee.webmedia.alfresco.common.ajax;

/**
 * Throw when ajax call detects invalid view state (for example when user has changed view in another tab)
 * 
 * @author Riina Tens
 */
public class AjaxIllegalViewStateException extends RuntimeException {

    private final String currentViewId;

    private static final long serialVersionUID = 1L;

    public AjaxIllegalViewStateException(String currentViewId) {
        this.currentViewId = currentViewId;
    }

    public String getCurrentViewId() {
        return currentViewId;
    }

}
