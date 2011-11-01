package ee.webmedia.alfresco.user.web;

import java.util.Iterator;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class PermissionsListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "PermissionsListDialog";

    private transient UIRichList authoritiesRichList;
    private transient UserService userService;

    private NodeRef nodeRef;
    private String permission;
    private List<Authority> authorities;
    private String alternateConfigId;
    private String alternateDialogTitleId;
    private String callbackMethodBinding;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button is not used
        return null; // but in case someone clicks finish button twice on the previous dialog,
                     // then silently ignore it and stay on the same page
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return false;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    private void reset() {
        // Don't call these from restored() since this dialog uses nested dialogs for actual rights management!
        nodeRef = null;
        permission = null;
        alternateConfigId = null;
        callbackMethodBinding = null;
        alternateDialogTitleId = null;
        restored();
    }

    public void setup(ActionEvent event) {
        reset();

        nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        permission = ActionUtil.getParam(event, "permission");
        if (ActionUtil.hasParam(event, "alternateConfigId")) {
            alternateConfigId = ActionUtil.getParam(event, "alternateConfigId");
        }
        if (ActionUtil.hasParam(event, "alternateDialogTitleId")) {
            alternateDialogTitleId = ActionUtil.getParam(event, "alternateDialogTitleId");
        }
        if (ActionUtil.hasParam(event, "callbackMethodBinding")) {
            callbackMethodBinding = "#{" + ActionUtil.getParam(event, "callbackMethodBinding") + "}";
        }
    }

    public void removeAuthorityAndSave(ActionEvent event) {
        String auth = ActionUtil.getParam(event, "authority");
        for (Iterator<Authority> it = authorities.iterator(); it.hasNext();) {
            Authority authority = it.next();
            if (StringUtils.equals(authority.getAuthority(), auth)) {
                BeanHelper.getPermissionService().deletePermission(nodeRef, authority.getAuthority(), permission);
                MessageUtil.addInfoMessage("delete_success");
                it.remove();
                break;
            }
        }
    }

    @Override
    public String getContainerTitle() {
        if (alternateDialogTitleId != null) {
            return MessageUtil.getMessage(alternateDialogTitleId);
        }
        return null;
    }

    @Override
    public String getActionsConfigId() {
        if (alternateConfigId != null) {
            return alternateConfigId;
        }
        return "browse_actions_permissions";
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public void restored() {
        authorities = null;
        if (authoritiesRichList != null) {
            authoritiesRichList.setValue(null);
        }
    }

    // START: getters / setters
    public List<Authority> getAuthorities() {
        if (authorities == null) {
            authorities = getUserService().getAuthorities(nodeRef, permission);
        }
        return authorities;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public String getPermission() {
        return permission;
    }

    public UIRichList getAuthoritiesRichList() {
        return authoritiesRichList;
    }

    public void setAuthoritiesRichList(UIRichList authoritiesRichList) {
        this.authoritiesRichList = authoritiesRichList;
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

    public String getCallbackMethodBinding() {
        return callbackMethodBinding;
    }

    public void setCallbackMethodBinding(String callbackMethodBinding) {
        this.callbackMethodBinding = callbackMethodBinding;
    }

    public String getAlternateDialogTitleId() {
        return alternateDialogTitleId;
    }

    // END: getters / setters
}
