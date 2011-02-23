package ee.webmedia.alfresco.user.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class PermissionsAddDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private UserListDialog userListDialog;
    private transient UserService userService;
    private transient PermissionService permissionService;

    private NodeRef nodeRef;
    private String permission;
    private SelectItem[] usersGroupsFilters;
    private List<Authority> authorities;
    private transient ListDataModel authoritiesModel;

    public void setup(ActionEvent event) {
        nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        permission = ActionUtil.getParam(event, "permission");
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        FacesContext context = FacesContext.getCurrentInstance();
        usersGroupsFilters = new SelectItem[] {
            new SelectItem("0", MessageUtil.getMessage(context, "users")),
            new SelectItem("1", MessageUtil.getMessage(context, "groups"))
        };
        authorities = new ArrayList<Authority>();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // We need to run this in elevated rights, so regular users could use PermissionAddDialog
        // TODO - Assign correct permissions 
        AuthenticationUtil.runAs(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                for (Authority authority : authorities) {
                    getPermissionService().setPermission(nodeRef, authority.getAuthority(), permission, true);
                }
                return null;
            }

        }, AuthenticationUtil.getSystemUserName());
    
        reset();
        return outcome;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return authorities.isEmpty();
    }

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getUsersGroupsFilters() {
        return usersGroupsFilters;
    }

    public SelectItem[] searchUsersGroups(int filterIndex, String contains) {
        if (filterIndex == 0) {
            return userListDialog.searchUsers(-1, contains);
        } else if (filterIndex == 1) {
            return searchGroups(-1, contains);
        }
        throw new RuntimeException("filterIndex must be 0 or -1, but is " + filterIndex);
    }

    public SelectItem[] searchGroups(int filterIndex, String contains) {
        List<Authority> results = getUserService().searchGroups(contains, true);
        SelectItem[] selectItems = new SelectItem[results.size()];
        int i = 0;
        for (Authority authority : results) {
            selectItems[i++] = new SelectItem(authority.getAuthority(), authority.getName());
        }
        WebUtil.sort(selectItems);
        return selectItems;
    }

    /**
     * Action handler called when the Add button is pressed to process the current selection
     */
    public void addAuthorities(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent().findComponent("picker");

        String[] results = picker.getSelectedResults();
        if (results != null) {
            if(authorities == null) {
                authorities = new ArrayList<Authority>();
            }
            for (int i = 0; i < results.length; i++) {
                if (!authorities.contains(new Authority(results[i], false, null))) {
                    Authority authority = getUserService().getAuthorityOrNull(results[i]);
                    if (authority != null) {
                        authorities.add(authority);
                    }
                }
            }
        }
    }

    /**
     * Returns the properties for current user-roles JSF DataModel
     * 
     * @return JSF DataModel representing the current user-roles
     */
    public DataModel getAuthorities() {
        if (authoritiesModel == null) {
            authoritiesModel = new ListDataModel();
            authoritiesModel.setWrappedData(authorities);
        }
        return authoritiesModel;
    }
    
    public NodeRef getNodeRef() {
        return nodeRef;
    }

    /**
     * Action handler called when the Remove button is pressed to remove a user+role
     */
    public void removeAuthority(ActionEvent event) {
        Authority authority = (Authority) authoritiesModel.getRowData();
        if (authority != null) {
            authorities.remove(authority);
        }
    }

    private void reset() {
        nodeRef = null;
        permission = null;
        authorities = null;
        authoritiesModel = null;
    }

    // START: getters / setters
    public void setUserListDialog(UserListDialog userListDialog) {
        this.userListDialog = userListDialog;
    }

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
