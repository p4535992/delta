package ee.webmedia.alfresco.user.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class PermissionsListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient UIRichList authoritiesRichList;
    private transient UserService userService;

    private NodeRef nodeRef;
    private String permission;
    private List<Authority> authorities;
    private String alternateConfigId;
    private String alternateDialogTitleId;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button is not used
        return null; // but in case someone clicks finish button twice on the previous dialog,
                     // then silently ignore it and stay on the same page
    }

    @Override
    public String cancel() {
        nodeRef = null;
        permission = null;
        restored();
        return super.cancel();
    }

    public void setup(ActionEvent event) {
        nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        permission = ActionUtil.getParam(event, "permission");
        if (ActionUtil.hasParam(event, "alternateConfigId")) {
            alternateConfigId = ActionUtil.getParam(event, "alternateConfigId");
        }
        if (ActionUtil.hasParam(event, "alternateDialogTitleId")) {
            alternateDialogTitleId = ActionUtil.getParam(event, "alternateDialogTitleId");
        }
        restored();
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
    // END: getters / setters
}
