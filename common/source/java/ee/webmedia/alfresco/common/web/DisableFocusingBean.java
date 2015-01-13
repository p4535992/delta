package ee.webmedia.alfresco.common.web;

import java.io.Serializable;

/**
 * Request-scoped bean for disabling/enabling focusing.
 */
public class DisableFocusingBean implements Serializable {

    public static final String BEAN_NAME = "DisableFocusingBean";

    private static final long serialVersionUID = 1L;

    private boolean disableInputFocus;
    private boolean disable = true;

    public boolean isDisableInputFocus() {
        return disableInputFocus;
    }

    /**
     * Focus is set only once during request, because
     * if event comes from ActionLink or CommandButton,
     * handleNavigation is called for a second time and it could reset this setting!
     * (See also MenuBean.scrollToAnchor javascript comment)
     */
    public void setDisableInputFocus(boolean disableInputFocus) {
        if (disable) {
            this.disableInputFocus = disableInputFocus;
            disable = false;
        }
    }

}
