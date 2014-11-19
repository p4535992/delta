package ee.webmedia.alfresco.common.listener;

import org.alfresco.web.bean.dialog.IDialogBean;

/**
 * If implemented by {@link IDialogBean}, then it gets notified of browser refresh button.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public interface RefreshEventListener {

    /**
     * Browser refresh button is pressed (or any old view is being submitted).
     */
    void refresh();

}
