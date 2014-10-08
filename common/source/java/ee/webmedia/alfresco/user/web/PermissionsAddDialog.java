package ee.webmedia.alfresco.user.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.privilege.model.Privilege;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class PermissionsAddDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient UserService userService;
    private transient PermissionService permissionService;
    private transient DocumentSearchService documentSearchService;

    private NodeRef nodeRef;
<<<<<<< HEAD
    private String permission;
=======
    private Privilege permission;
>>>>>>> develop-5.1
    private String dialogTitleId;
    private List<Authority> authorities;
    private transient ListDataModel authoritiesModel;

    public void setup(ActionEvent event) {
        reset();

        nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
<<<<<<< HEAD
        permission = ActionUtil.getParam(event, "permission");
=======
        permission = Privilege.getPrivilegeByName(ActionUtil.getParam(event, "permission"));
>>>>>>> develop-5.1
        if (ActionUtil.hasParam(event, "dialogTitleId")) {
            String param = ActionUtil.getParam(event, "dialogTitleId");
            if (param != null && !param.equals("null")) {
                dialogTitleId = param;
            }
        }
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        authorities = new ArrayList<Authority>();
    }

    @Override
    public String getContainerTitle() {
        if (StringUtils.isNotBlank(dialogTitleId)) {
            return MessageUtil.getMessage(dialogTitleId);
        }
        return MessageUtil.getMessage("manage_invited_users");
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // We need to run this in elevated rights, so regular users could use PermissionAddDialog
        // TODO - Assign correct permissions
        AuthenticationUtil.runAs(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                for (Authority authority : authorities) {
<<<<<<< HEAD
                    getPermissionService().setPermission(nodeRef, authority.getAuthority(), permission, true);
=======
                    BeanHelper.getPrivilegeService().setPermissions(nodeRef, authority.getAuthority(), permission);
>>>>>>> develop-5.1
                }
                return null;
            }

        }, AuthenticationUtil.getSystemUserName());

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
    public Object getActionsContext() {
        return null;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return authorities.isEmpty();
    }

    /**
     * Action handler called when the Add button is pressed to process the current selection
     */
    public void addAuthorities(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent().findComponent("picker");

        String[] results = picker.getSelectedResults();
        if (results != null) {
            if (authorities == null) {
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
<<<<<<< HEAD
     * 
=======
     *
>>>>>>> develop-5.1
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
        dialogTitleId = null;
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

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }
    // END: getters / setters
}
