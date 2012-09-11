package ee.webmedia.alfresco.common.listener;

import org.alfresco.web.bean.dialog.IDialogBean;

/**
 * If implemented by {@link IDialogBean}, then it gets notified of browser refresh button.
 * 
 * @author Alar Kvell
 */
public interface RefreshEventListener {

    /**
     * Browser refresh button is pressed (or any old view is being submitted).
     */
    void refresh();

}
