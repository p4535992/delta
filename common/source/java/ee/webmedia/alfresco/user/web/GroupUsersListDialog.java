package ee.webmedia.alfresco.user.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAuthorityService;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.transaction.UserTransaction;

import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.utils.MessageUtil;

public class GroupUsersListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "GroupUsersListDialog";
    private boolean disableActions;

    /** Component references */
    protected transient UIRichList usersRichList;

    /** Currently visible Group Authority */
    protected String group = null;
    protected String groupName = null;
    private List<Map<String, String>> users = null;

    /** RichList view mode */
    protected String viewMode = VIEW_ICONS;

    private static final String VIEW_ICONS = "icons";

    private static final String MSG_ROOT_GROUPS = "root_groups";

    private static Log logger = LogFactory.getLog(GroupUsersListDialog.class);

    // ------------------------------------------------------------------------------
    // Dialog implementation

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        return null;
    }

    @Override
    public void init(Map<String, String> parameters) {
        clearRichList();
        super.init(parameters);
    }

    @Override
    public String getContainerSubTitle() {
        return groupName;
    }

    @Override
    public String getCancelButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), "back_button");
    }

    @Override
    public String getContainerTitle() {
        if (group != null) {
            return MessageUtil.getMessage("groups_management_subgroup", getAuthorityService().getAuthorityDisplayName(group));
        }
        return MessageUtil.getMessage("groups_management");
    }

    @Override
    public String cancel() {
        setCurrentGroup(null, Application.getMessage(FacesContext.getCurrentInstance(), MSG_ROOT_GROUPS));
        return "dialog:close";
    }

    @Override
    public void clean() {
        users = null;
    }

    public void setDisableActions(boolean disableActions) {
        this.disableActions = disableActions;
    }

    public boolean isDisableActions() {
        return disableActions;
    }

    public void reset(@SuppressWarnings("unused") ActionEvent event) {
        disableActions = false;
        restored();
    }

    @Override
    public void restored() {
        setCurrentGroup(null, Application.getMessage(FacesContext.getCurrentInstance(), MSG_ROOT_GROUPS));
    }

    @Override
    public Object getActionsContext() {
        return this;
    }

    // ------------------------------------------------------------------------------
    // Bean property getters and setters

    public String getGroup() {
        return group;
    }

    public String getGroupName() {
        return groupName;
    }

    public UIRichList getUsersRichList() {
        return usersRichList;
    }

    public void setUsersRichList(UIRichList usersRichList) {
        this.usersRichList = usersRichList;
    }

    public void clearRichList() {
        if (usersRichList != null) {
            usersRichList.setValue(null);
        }
    }

    /**
     * @return The list of user objects to display. Returns the list of user for the current group.
     */
    public List<Map<String, String>> getUsers() {
        if (users == null) {
            UserTransaction tx = null;
            try {
                FacesContext context = FacesContext.getCurrentInstance();
                tx = Repository.getUserTransaction(context, true);
                tx.begin();

                Set<String> authorities;
                boolean structUnitBased = false;
                authorities = getAuthorityService().getContainedAuthorities(AuthorityType.USER, group, true);
                structUnitBased = getAuthorityService().getAuthorityZones(group).contains(OrganizationStructureService.STRUCT_UNIT_BASED);
                users = new ArrayList<Map<String, String>>(authorities.size());
                for (String authority : authorities) {
                    Map<String, String> authMap = new HashMap<String, String>(4, 1.0f);

                    String userName = getAuthorityService().getShortName(authority);
                    authMap.put("userName", userName);
                    authMap.put("id", authority);
                    authMap.put("structUnitBased", (structUnitBased) ? "true" : "false");
                    authMap.put("name", BeanHelper.getUserService().getUserFullName(userName));

                    users.add(authMap);
                }

                // commit the transaction
                tx.commit();
            } catch (Throwable err) {
                Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
                        FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), err.getMessage()), err);
                users = Collections.<Map<String, String>> emptyList();
                try {
                    if (tx != null) {
                        tx.rollback();
                    }
                } catch (Exception tex) {
                }
            }
        }
        return users;
    }

    /**
     * Set the current Group Authority.
     * <p>
     * Setting this value causes the UI to update and display the specified node as current.
     *
     * @param group The current group authority.
     */
    protected void setCurrentGroup(String group, String groupName) {
        if (logger.isDebugEnabled()) {
            logger.debug("Setting current group: " + group);
        }

        // set the current Group Authority for our UI context operations
        this.group = group;
        this.groupName = groupName;
        users = null;
    }

    // ------------------------------------------------------------------------------
    // Action handlers

    /**
     * Action called when a Group folder is clicked.
     * Navigate into the Group and show child Groups and child Users.
     */
    public void clickGroup(ActionEvent event) {
        UIActionLink link = (UIActionLink) event.getComponent();
        Map<String, String> params = link.getParameterMap();
        String group = params.get("id");
        if (StringUtils.isNotBlank(group)) {
            // refresh UI based on node selection
            updateUILocation(group);
        }
    }

    /**
     * Remove specified user from the current group
     */
    public void removeUser(ActionEvent event) {
        UIActionLink link = (UIActionLink) event.getComponent();
        Map<String, String> params = link.getParameterMap();
        String authority = params.get("id");
        users = null;
        if (authority != null && authority.length() != 0) {
            try {
                BeanHelper.getUserService().removeUserFromGroup(group, authority);
                MessageUtil.addInfoMessage("delete_user_from_group");
            } catch (Throwable err) {
                Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
                        FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), err.getMessage()), err);
            }
        }
    }

    // ------------------------------------------------------------------------------
    // Helpers

    /**
     * Update the breadcrumb with the clicked Group location
     */
    protected void updateUILocation(String group) {
        String groupName = getAuthorityService().getShortName(group);
        setCurrentGroup(group, groupName);
    }

    // ------------------------------------------------------------------------------
    // Inner classes

    /**
     * Simple wrapper bean exposing user authority and person details for JSF results list
     */
    public static class UserAuthorityDetails implements Serializable {
        private static final long serialVersionUID = 1056255933962068348L;

        public UserAuthorityDetails(String name, String authority) {
            this.name = name;
            this.authority = authority;
        }

        public String getName() {
            return name;
        }

        public String getAuthority() {
            return authority;
        }

        private final String name;
        private final String authority;
    }
}
