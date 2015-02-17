package ee.webmedia.alfresco.user.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getJsfBindingHelper;

import java.util.Iterator;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class PermissionsListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "PermissionsListDialog";
    /** allows delegating actual permission removing to another method using given method binding expression */
    private static String DELEGATE_REMOVE_AUTHORITY_MB = "delegateRemoveAuthorityMB";

    private transient UserService userService;

    private NodeRef nodeRef;
    private Privilege permission;
    private List<Authority> authorities;
    private String alternateConfigId;
    private String alternateDialogTitleId;
    private String callbackMethodBinding;
    private String delegateRemoveAuthorityMB;

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

    @Override
    public void clean() {
        // Don't call these from restored() since this dialog uses nested dialogs for actual rights management!
        nodeRef = null;
        permission = null;
        alternateConfigId = null;
        callbackMethodBinding = null;
        alternateDialogTitleId = null;
    }

    private void reset() {
        clean();
        restored();
    }

    public void setup(ActionEvent event) {
        reset();

        nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        permission = Privilege.getPrivilegeByName(ActionUtil.getParam(event, "permission"));
        if (ActionUtil.hasParam(event, "alternateConfigId")) {
            alternateConfigId = ActionUtil.getParam(event, "alternateConfigId");
        }
        if (ActionUtil.hasParam(event, "alternateDialogTitleId")) {
            alternateDialogTitleId = ActionUtil.getParam(event, "alternateDialogTitleId");
        }
        if (ActionUtil.hasParam(event, "callbackMethodBinding")) {
            callbackMethodBinding = "#{" + ActionUtil.getParam(event, "callbackMethodBinding") + "}";
        }
        delegateRemoveAuthorityMB = ActionUtil.getParam(event, DELEGATE_REMOVE_AUTHORITY_MB, "");
    }

    public void removeAuthorityAndSave(ActionEvent event) {
        String auth = ActionUtil.getParam(event, "authority");
        for (Iterator<Authority> it = authorities.iterator(); it.hasNext();) {
            Authority authority = it.next();
            if (StringUtils.equals(authority.getAuthority(), auth)) {
                if (StringUtils.isNotBlank(delegateRemoveAuthorityMB)) {
                    FacesContext context = FacesContext.getCurrentInstance();
                    MethodBinding b = context.getApplication().createMethodBinding("#{" + delegateRemoveAuthorityMB + "}"
                            , new Class[] { NodeRef.class, String.class, String.class });
                    b.invoke(context, new Object[] { nodeRef, authority.getAuthority(), permission.getPrivilegeName() });
                } else {
                    BeanHelper.getPrivilegeService().removeAllPermissions(nodeRef, authority.getAuthority());
                    MessageUtil.addInfoMessage("delete_success");
                }
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
        UIRichList authoritiesListComponent = getAuthoritiesRichList();
        if (authoritiesListComponent != null) {
            authoritiesListComponent.setValue(null);
        }
    }

    // START: getters / setters
    public List<Authority> getAuthorities() {
        if (authorities == null) {
            authorities = userService.getAuthorities(nodeRef, permission);
        }
        return authorities;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public String getPermission() {
        return permission.getPrivilegeName();
    }

    public UIRichList getAuthoritiesRichList() {
        return (UIRichList) getJsfBindingHelper().getComponentBinding(getRichListBindingName());
    }

    public void setAuthoritiesRichList(UIRichList authoritiesRichList) {
        getJsfBindingHelper().addBinding(getRichListBindingName(), authoritiesRichList);
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
