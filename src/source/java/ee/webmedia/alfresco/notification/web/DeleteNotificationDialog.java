package ee.webmedia.alfresco.notification.web;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;

import ee.webmedia.alfresco.document.file.web.DeleteFileDialog;

/**
 * The bean that backs up the Delete notification
 * @author Ats Uiboupin
 */
public class DeleteNotificationDialog extends DeleteFileDialog {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        super.finishImpl(context, outcome);
        NotificationBean notificationBean = (NotificationBean) FacesHelper
        .getManagedBean(FacesContext.getCurrentInstance(), "NotificationBean");
        notificationBean.setUpdateCount(-1);
        return outcome;
    }
}
