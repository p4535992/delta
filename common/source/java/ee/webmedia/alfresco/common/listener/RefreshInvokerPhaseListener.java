package ee.webmedia.alfresco.common.listener;

import java.util.Map;

import javax.faces.application.NavigationHandler;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.IDialogBean;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

/**
 * If browser refresh button is detected, invokes refresh event on current dialog, just after restore view phase has been completed.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class RefreshInvokerPhaseListener implements PhaseListener {
    private static final long serialVersionUID = 1L;

    @Override
    public void beforePhase(PhaseEvent event) {
        // do nothing
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = event.getFacesContext().getExternalContext().getRequestMap();
        if (!Boolean.TRUE.equals(requestMap.get(JspStateManagerImpl.BROWSER_REFRESH_OR_INCORRECT_VIEW_STATE_ATTR))) {
            return;
        }

        NavigationHandler navigationHandler = event.getFacesContext().getApplication().getNavigationHandler();
        if (navigationHandler instanceof AlfrescoNavigationHandler) {
            IDialogBean dialogBean = ((AlfrescoNavigationHandler) navigationHandler).getCurrentDialogOrWizard(event.getFacesContext());
            if (dialogBean instanceof RefreshEventListener) {
                ((RefreshEventListener) dialogBean).refresh();
            }
        }
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

}
