package ee.webmedia.alfresco.notification.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.notification.model.GeneralNotification;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.utils.ActionUtil;

public class NotificationListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient NotificationService notificationService;
    private List<GeneralNotification> notifications;
    
    @Override
    public void init(Map<String, String> parameters) {
        notifications = notificationService.getGeneralNotifications();
        super.init(parameters);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // nothing to do, finish button is not shown
        return null;
    }
    
    @Override
    public String cancel() {
        notifications = null;
        return super.cancel();
    }
    
    @Override
    public void restored() {
        notifications = notificationService.getGeneralNotifications();
        super.restored();
    }
    
    @Override
    public Object getActionsContext() {
        return null;
    }
    
    public void setupNotificationAction(ActionEvent event) {
        browseBean
        .setupContentAction(
                (new NodeRef(
                        ActionUtil.getParam(
                                event, "nodeRef"))
                .getId()), true);
    }

    // START: getters/setters
    
    protected NotificationService getNotificationService() {
        if (notificationService == null) {
            notificationService = (NotificationService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(NotificationService.BEAN_NAME);
        }
        return notificationService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public List<GeneralNotification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<GeneralNotification> notifications) {
        this.notifications = notifications;
    }

    // END: getters/setters

}
