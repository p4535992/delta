package ee.webmedia.alfresco.notification.web;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;

import ee.webmedia.alfresco.common.web.InformingDeleteNodeDialog;

/**
 * The bean that backs up the Delete notification
 */
public class DeleteNotificationDialog extends InformingDeleteNodeDialog {
    private static final long serialVersionUID = 1L;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        super.finishImpl(context, outcome);
        NotificationBean notificationBean = (NotificationBean) FacesHelper
                .getManagedBean(FacesContext.getCurrentInstance(), "NotificationBean");
        notificationBean.setUpdateCount(-1);
        return outcome;
    }

    @Override
    protected String getDefaultMsgKey() {
        return "notification_delete_success";
    }

}
