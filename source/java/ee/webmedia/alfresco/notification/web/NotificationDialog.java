package ee.webmedia.alfresco.notification.web;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.user.service.UserService;

public class NotificationDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private transient UserService userService;
    private transient NotificationService notificationService;

    private Node userPrefsNode;

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public void init(Map<String, String> parameters) {
        restored();
        super.init(parameters);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        notificationService.saveConfigurationChanges(userPrefsNode);
        // We need to stay on the same dialog
        isFinished = false;
        return null;
    }

    @Override
    public void restored() {
        final String userName = AuthenticationUtil.getRunAsUser();
        userPrefsNode = new Node(userService.retrieveUsersPreferenceNodeRef(userName));
        notificationService.addMissingConfigurations(userPrefsNode);
        super.restored();
    }

    public boolean isShowConfirmationTaskData() {
        return BeanHelper.getWorkflowConstantsBean().isConfirmationWorkflowEnabled();
    }

    public boolean isShowOrderAssignmentTaskData() {
        return BeanHelper.getWorkflowConstantsBean().isOrderAssignmentWorkflowEnabled();
    }

    public boolean isShowGroupAssignmentTaskData() {
        return BeanHelper.getWorkflowConstantsBean().isGroupAssignmentWorkflowEnabled();
    }

    @Override
    public void clean() {
        userPrefsNode = null;
    }

    // START: setters/getters

    public Node getUserPrefsNode() {
        return userPrefsNode;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // END: setters/getters

}
