package ee.webmedia.alfresco.user.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.users.UsersBeanProperties;
import org.alfresco.web.bean.users.UsersDialog;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.web.SubstituteListDialog;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class UserDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "UserDetailsDialog";

    private transient UserService userService;
    private transient OrganizationStructureService organizationStructureService;
    private transient AuthorityService authorityService;
    private UsersBeanProperties properties;
    private List<Map<String, String>> groups;
    private String groupToAdd;

    private SubstituteListDialog substituteListDialog;

    private Node user;
    public static final String NOTIFICATION_SENDER_LABEL = "NotificationSender";

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        substituteListDialog = new SubstituteListDialog();
        substituteListDialog.setUserNodeRef(user.getNodeRef());
        setNotificationSender();
        substituteListDialog.refreshData();
        setupGroups();
    }

    protected void setupGroups() {
        groupToAdd = null;
        Set<String> authorities = getAuthorityService().getAuthoritiesForUser((String) user.getProperties().get(ContentModel.PROP_USERNAME));
        // Remove all roles and GROUP_EVERYONE
        for (Iterator<String> iterator = authorities.iterator(); iterator.hasNext();) {
            String authority = iterator.next();
            if (authority.startsWith(PermissionService.ROLE_PREFIX) || PermissionService.ALL_AUTHORITIES.equals(authority)) {
                iterator.remove();
            }
        }
        groups = UserUtil.getGroupsFromAuthorities(getAuthorityService(), authorities);
    }

    private void setNotificationSender() {
        SubstituteListDialog.NotificationSender notificationSender =
                (SubstituteListDialog.NotificationSender) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), NOTIFICATION_SENDER_LABEL);
        if (notificationSender != null) {
            substituteListDialog.setNotificationSender(notificationSender);
        }
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        substituteListDialog.save(context);
        BeanHelper.getUserService().updateUser(user);
        isFinished = false;
        return null;
    }

    @Override
    public String cancel() {
        user = null;
        substituteListDialog.cancel();
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        // since we are using actions, but not action context,
        // we don't need instance of NavigationBean that is used in the overloadable method
        return null;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    public boolean isRelatedFundsCenterNotEditable() {
        return !isRelatedFundsCenterEditable();
    }

    public boolean isRelatedFundsCenterEditable() {
        return BeanHelper.getUserService().isAdministrator();
    }

    /**
     * Action event called by all actions that need to setup a Person context on
     * the current user before an action page is called. The context will be a
     * Person Node in setPerson() which can be retrieved on the action page from
     * UsersDialog.getCurrentUserNode().
     */
    public void setupCurrentUser(@SuppressWarnings("unused") ActionEvent event) {
        Node node = new Node(properties.getPersonService().getPerson(AuthenticationUtil.getRunAsUser()));
        // Eagerly load properties
        node.getProperties();

        // take care of UsersDialog
        UsersDialog dialog = (UsersDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UsersDialog.BEAN_NAME);
        dialog.setupUserAction(node.getId());

        List<Node> users = new ArrayList<Node>(1);
        users.add(node);
        fillUserProps(users);
        BeanHelper.getAssignResponsibilityBean().updateLiabilityGivenToPerson(node);
    }

    private void fillUserProps(List<Node> users) {
        user = getOrganizationStructureService().setUsersUnit(users).get(0);
        setupGroups();
        if (user.getProperties().get(ContentModel.PROP_RELATED_FUNDS_CENTER) == null) {
            user.getProperties().put(ContentModel.PROP_RELATED_FUNDS_CENTER.toString(), new ArrayList<String>());
        }
        user.getProperties().put("{temp}relatedFundsCenter", user.getProperties().get(ContentModel.PROP_RELATED_FUNDS_CENTER));
    }

    public void removeFromGroup(ActionEvent event) {
        String group = ActionUtil.getParam(event, "group");
        if (StringUtils.isBlank(group)) {
            return;
        }
        getAuthorityService().removeAuthority(group, (String) user.getProperties().get(ContentModel.PROP_USERNAME));
        setupGroups();
        MessageUtil.addInfoMessage("user_removed_from_group");
    }

    public void addToGroup(String group) {
        if (StringUtils.isBlank(group)) {
            return;
        }

        getAuthorityService().addAuthority(group, (String) user.getProperties().get(ContentModel.PROP_USERNAME));
        setupGroups();
        MessageUtil.addInfoMessage("user_added_to_group");
    }

    /**
     * Action event called by all actions that need to setup a Person context on
     * the Users bean before an action page is called. The context will be a
     * Person Node in setPerson() which can be retrieved on the action page from
     * UsersDialog.getPerson().
     */
    public void setupUser(ActionEvent event) {
        String userName = ActionUtil.getParam(event, "id");
        setupUser(userName);
        BeanHelper.getAssignResponsibilityBean().updateLiabilityGivenToPerson(new Node(BeanHelper.getPersonService().getPerson(userName)));
    }

    /**
     * Used in JSP to set up person context
     * 
     * @param userName
     */
    public void setupUser(String userName) {
        List<Node> users = new ArrayList<Node>(1);
        users.add(new Node(properties.getPersonService().getPerson(userName)));
        fillUserProps(users);
        setupGroups();
    }

    public void refreshCurrentUser() {
        if (user == null) {
            return;
        }
        String username = (String) user.getProperties().get(ContentModel.PROP_USERNAME);
        if (StringUtils.isNotBlank(username)) {
            setupUser(username);
        }
    }

    public UsersBeanProperties getProperties() {
        return properties;
    }

    public void setProperties(UsersBeanProperties properties) {
        this.properties = properties;
    }

    public Node getUser() {
        return user;
    }

    public void setUser(Node user) {
        this.user = user;
    }

    // //
    public List<Substitute> getSubstitutes() {
        return substituteListDialog.getSubstitutes();
    }

    public void deleteSubstitute(ActionEvent event) {
        substituteListDialog.deleteSubstitute(event);
    }

    public void setPersonToSubstitute(String userName, Substitute substitute) {
        substituteListDialog.setPersonToSubstitute(userName, substitute);
    }

    public void addNewValue(ActionEvent event) {
        substituteListDialog.addNewValue(event);
    }

    public String getEmailSubject() {
        return substituteListDialog.getEmailSubject();
    }

    public Map<String, String> getEmailAddress() {
        return substituteListDialog.getEmailAddress();
    }

    public void setGroups(List<Map<String, String>> groups) {
        this.groups = groups;
    }

    public List<Map<String, String>> getGroups() {
        if (groups == null) {
            setupGroups();
        }
        return groups;
    }

    public String getGroupToAdd() {
        return groupToAdd;
    }

    public void setGroupToAdd(String groupToAdd) {
        this.groupToAdd = groupToAdd;
    }

    // ///

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

    protected OrganizationStructureService getOrganizationStructureService() {
        if (organizationStructureService == null) {
            organizationStructureService = (OrganizationStructureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(OrganizationStructureService.BEAN_NAME);
        }
        return organizationStructureService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

    protected AuthorityService getAuthorityService() {
        if (authorityService == null) {
            authorityService = BeanHelper.getAuthorityService();
        }
        return authorityService;
    }

}
