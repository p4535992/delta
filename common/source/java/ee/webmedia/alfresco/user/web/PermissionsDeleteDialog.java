package ee.webmedia.alfresco.user.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class PermissionsDeleteDialog extends BaseDialogBean {
    public static final String BEAN_NAME = "PermissionsDeleteDialog";
    private static final long serialVersionUID = 1L;

    private transient UserService userService;
    private transient PermissionService permissionService;

    private NodeRef nodeRef;
    private String permission;
    private Authority authority;

    public void setup(ActionEvent event) {
        nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        permission = ActionUtil.getParam(event, "permission");
        authority = getUserService().getAuthority(ActionUtil.getParam(event, "authority"));
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        BeanHelper.getPrivilegeService().removeAllPermissions(nodeRef, authority.getAuthority());
        // Execute callback
        String callbackMethodBinding = BeanHelper.getPermissionsListDialog().getCallbackMethodBinding();
        if (StringUtils.isNotBlank(callbackMethodBinding)) {
            context.getApplication().createMethodBinding(callbackMethodBinding, new Class[] {}).invoke(context, new Object[] {});
        }
        reset();
        return outcome;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    public String getPrompt() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "prompt_remove_user_group", authority.getName());
    }

    private void reset() {
        nodeRef = null;
        permission = null;
        authority = null;
    }

    @Override
    public void clean() {
        reset();
    }

    // START: getters / setters
    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    protected PermissionService getPermissionService() {
        if (permissionService == null) {
            permissionService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPermissionService();
        }
        return permissionService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    // END: getters / setters
}
