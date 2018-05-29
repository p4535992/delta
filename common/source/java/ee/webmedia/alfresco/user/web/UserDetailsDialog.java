package ee.webmedia.alfresco.user.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getApplicationConstantsBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getAuthorityService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.utils.UserUtil.getUserDisplayUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.users.UsersBeanProperties;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.web.SubstituteListDialog;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class UserDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "UserDetailsDialog";

    private UsersBeanProperties properties;
    private List<Map<String, String>> groups;
    private String groupToAdd;

    private SubstituteListDialog substituteListDialog;

    private Node user;
    public static final String NOTIFICATION_SENDER_LABEL = "NotificationSender";

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
        groups = UserUtil.getGroupsFromAuthorities(getAuthorityService(), getUserService(), authorities);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            trimDefaultTelephoneForSigning();
            substituteListDialog.save();
            BeanHelper.getUserService().updateUser(user);
            setupUser((String) user.getProperties().get(ContentModel.PROP_USERNAME.toString()));
        }
        isFinished = false;
        return null;
    }

    private boolean validate() {
        Map<String, Object> userProps = user.getProperties();
        String clientExtensions = (String) userProps.get(ContentModel.PROP_OPEN_OFFICE_CLIENT_EXTENSIONS.toString());
        String strippedExtensions = StringUtils.deleteWhitespace(clientExtensions);
        if (StringUtils.isNotBlank(strippedExtensions) && !StringUtils.isAlpha(strippedExtensions.replaceAll(",", ""))) {
            MessageUtil.addErrorMessage("user_openOfficeClientExtensions_error");
            return false;
        }
        if (!StringUtils.equals(clientExtensions, strippedExtensions)) {
            userProps.put(ContentModel.PROP_OPEN_OFFICE_CLIENT_EXTENSIONS.toString(), strippedExtensions);
        }
        return true;
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

    public boolean isShowEmptyTaskMenuNotEditable() {
        return !isShowEmptyTaskMenuEditable();
    }

    public void trimDefaultTelephoneForSigning() {
        String tel = StringUtils.trimToEmpty((String) user.getProperties().get(ContentModel.DEFAULT_TELEPHONE_FOR_SIGNING.toString()));
        user.getProperties().put(ContentModel.DEFAULT_TELEPHONE_FOR_SIGNING.toString(), tel);
    }

    public boolean isShowEmptyTaskMenuEditable() {
        return isAdministratorOrCurrentUser();
    }

    public boolean isAdministratorOrCurrentUser() {
        return BeanHelper.getUserService().isAdministrator() || isCurrentUser();
    }

    public boolean isAdministratorOrDocManagerOrCurrentUser() {
        UserService userService = BeanHelper.getUserService();
        return userService.isAdministrator() || userService.isDocumentManager() || isCurrentUser();
    }

    private boolean isCurrentUser() {
        return user.getProperties().get(ContentModel.PROP_USERNAME.toString()).equals(AuthenticationUtil.getRunAsUser());
    }

    public boolean isServiceRankRendered() {
        return StringUtils.isNotBlank((String) user.getProperties().get(ContentModel.PROP_SERVICE_RANK.toString()));
    }

    /**
     * Action event called by all actions that need to setup a Person context on
     * the current user before an action page is called. The context will be a
     * Person Node in setPerson() which can be retrieved on the action page from
     * UsersDialog.getCurrentUserNode().
     */
    public void setupCurrentUser(@SuppressWarnings("unused") ActionEvent event) {
        setupUser(AuthenticationUtil.getRunAsUser());
        BeanHelper.getUsersDialog().setupUserAction(user.getId());
    }

    /**
     * Action event called by all actions that need to setup a Person context on
     * the Users bean before an action page is called. The context will be a
     * Person Node in setPerson() which can be retrieved on the action page from
     * UsersDialog.getPerson().
     */
    public void setupUser(ActionEvent event) {
        setupUser(ActionUtil.getParam(event, "id"));
    }

    /**
     * Used in JSP to set up person context
     *
     * @param userName
     */
    public void setupUser(String userName) {
        user = new Node(BeanHelper.getPersonService().getPersonFromRepo(userName));
        fillUserProps();
        BeanHelper.getAssignResponsibilityBean().updateLiabilityGivenToPerson(user);
        substituteListDialog.setUserNodeRef(user.getNodeRef());
        substituteListDialog.refreshData();
    }

    public void reloadUser() {
        if (user != null) {
            setupUser((String) user.getProperties().get(ContentModel.PROP_USERNAME));
        }
    }

    private void fillUserProps() {
        getOrganizationStructureService().loadUserUnit(user);
        setupGroups();
        Map<String, Object> props = user.getProperties();

        props.put("{temp}unit", getUserDisplayUnit(props));
        props.put("{temp}jobAddress", TextUtil.joinNonBlankStringsWithComma(Arrays.asList((String) props.get(ContentModel.PROP_STREET_HOUSE),
                (String) props.get(ContentModel.PROP_VILLAGE), (String) props.get(ContentModel.PROP_MUNICIPALITY), (String) props.get(ContentModel.PROP_POSTAL_CODE),
                (String) props.get(ContentModel.PROP_COUNTY))));

        Boolean showEmpty = (Boolean) user.getProperties().get(ContentModel.SHOW_EMPTY_TASK_MENU.toString());
        if (showEmpty == null) {
            user.getProperties().put(ContentModel.SHOW_EMPTY_TASK_MENU.toString(), false);
            showEmpty = false;
        }
        String emptyTaskMenuString = showEmpty ? getApplicationConstantsBean().getMessageYes() : getApplicationConstantsBean().getMessageNo();
        user.getProperties().put("{temp}" + ContentModel.SHOW_EMPTY_TASK_MENU.getLocalName(), emptyTaskMenuString);
    }

    public void removeFromGroup(ActionEvent event) {
        String group = ActionUtil.getParam(event, "group");
        if (StringUtils.isBlank(group)) {
            return;
        }
        BeanHelper.getUserService().removeUserFromGroup(group, user);
        setupGroups();
        MessageUtil.addInfoMessage("user_removed_from_group");
    }

    public void addToGroup(String group) {
        if (StringUtils.isBlank(group)) {
            return;
        }
        try {
            BeanHelper.getUserService().addUserToGroup(group, user);
        } catch (DuplicateChildNodeNameException e) {
            MessageUtil.addWarningMessage("user_already_added_to_group", getAuthorityService().getAuthorityDisplayName(group));
            return;
        }
        setupGroups();
        MessageUtil.addInfoMessage("user_added_to_group");
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

    @Override
    public void clean() {
        user = null;
        substituteListDialog.cancel();
        groups = null;
        groupToAdd = null;
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

    public List<String> getDimensionSuggesterValues(FacesContext contect, UIInput input) {
        return new ArrayList<String>();
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

    public void setSubstituteListDialog(SubstituteListDialog substituteListDialog) {
        this.substituteListDialog = substituteListDialog;
    }

}
