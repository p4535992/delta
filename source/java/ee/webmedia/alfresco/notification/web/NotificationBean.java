package ee.webmedia.alfresco.notification.web;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;

import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.notification.model.GeneralNotification;
import ee.webmedia.alfresco.notification.service.NotificationService;

public class NotificationBean implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "NotificationBean";
    
    private transient NotificationService notificationService;
    private List<GeneralNotification> generalNotifications;
    private int updateCount = 0;
    
    private boolean isUpdated() {
            return updateCount != getNotificationService().getUpdateCount();
    }

    //START: setters/getters
    
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

    public List<GeneralNotification> getGeneralNotifications() {
        if(generalNotifications == null || isUpdated()) {
            generalNotifications = getNotificationService().getActiveGeneralNotifications();
            setUpdateCount(getNotificationService().getUpdateCount());
        }
        return generalNotifications;
    }

    public void setGeneralNotifications(List<GeneralNotification> generalNotifications) {
        this.generalNotifications = generalNotifications;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    //END setters/getters

}
