package ee.webmedia.alfresco.notification.web;

import java.util.Calendar;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.GUID;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.notification.model.GeneralNotification;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;

public class NotificationDetailsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient NotificationService notificationService;
    private transient UserService userService;

    private Node notification;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        notificationService.updateGeneralNotification(notification);
        notification = null;
        return outcome;
    }

    @Override
    public String cancel() {
        notification = null;
        return super.cancel();
    }

    public void setupNewNotification(ActionEvent event) {
        GeneralNotification notificationObj = new GeneralNotification();
        notificationObj.setNodeRef(new NodeRef(event.hashCode() + "", event.hashCode() + "", GUID.generate()));
        notificationObj.setActive(true);
        notificationObj.setCreatedDateTime(Calendar.getInstance().getTime());
        notificationObj.setCreatorName(userService.getUserFullName());

        notification = notificationService.generalNotificationAsNode(notificationObj);

    }

    public void setupNotification(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        notification = notificationService.generalNotificationAsNode(notificationService.getGeneralNotificationByNodeRef(nodeRef));
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

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public Node getNotification() {
        return notification;
    }

    public void setNotification(Node notification) {
        this.notification = notification;
    }

    // END: getters/setters

}
