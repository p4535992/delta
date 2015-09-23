/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * and Open Source Software ("FLOSS") applications as described in Alfresco's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.bean.groups;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.transaction.UserTransaction;

import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.dialog.ChangeViewSupport;
import org.alfresco.web.bean.dialog.FilterViewSupport;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.IBreadcrumbHandler;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIBreadcrumb;
import org.alfresco.web.ui.common.component.UIListItem;
import org.alfresco.web.ui.common.component.UIModeList;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * Backing Bean for the Groups Management pages.
 *
 * @author Kevin Roast
 */
public class GroupsDialog extends BaseDialogBean
        implements IContextListener, FilterViewSupport, ChangeViewSupport
{
    private static final long serialVersionUID = -624617545796275734L;

    public static final String KEY_GROUP = "group";
    public static final String PARAM_GROUP = "group";
    public static final String PARAM_GROUP_NAME = "groupName";

    public static final String BEAN_NAME = "GroupsDialog";
    private boolean disableActions;

    /** The AuthorityService to be used by the bean */
    transient private AuthorityService authService;

    /** personService bean reference */
    transient private PersonService personService;

    /** Component references */
    protected UIRichList groupsRichList;
    protected UIRichList usersRichList;

    /** Currently visible Group Authority */
    protected String group = null;
    protected String groupName = null;
    private GroupDataProvider groupDataProvider;
    private UserDataProvider userDataProvider;

    /** RichList view mode */
    protected String viewMode = VIEW_ICONS;

    /** Filter mode */
    protected String filterMode = FILTER_CHILDREN;

    /** Groups path breadcrumb location */
    protected List<IBreadcrumbHandler> location = null;

    private static final String VIEW_ICONS = "icons";
    private static final String VIEW_DETAILS = "details";
    private static final String FILTER_CHILDREN = "children";
    private static final String FILTER_ALL = "all";

    private static final String LABEL_VIEW_ICONS = "group_icons";
    private static final String LABEL_VIEW_DETAILS = "group_details";
    private static final String LABEL_FILTER_CHILDREN = "group_filter_children";
    private static final String LABEL_FILTER_ALL = "group_filter_all";

    private static final String MSG_ROOT_GROUPS = "root_groups";
    private static final String MSG_CLOSE = "close";

    private static Log logger = LogFactory.getLog(GroupsDialog.class);

    // ------------------------------------------------------------------------------
    // Construction

    /**
     * Default Constructor
     */
    public GroupsDialog()
    {
        UIContextService.getInstance(FacesContext.getCurrentInstance()).registerBean(this);
    }

    // ------------------------------------------------------------------------------
    // Dialog implementation

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception
    {
        return null;
    }

    @Override
    public String getContainerSubTitle()
    {
        String subtitle = null;

        if (group != null)
        {
            subtitle = groupName;
        }
        else
        {
            subtitle = Application.getMessage(FacesContext.getCurrentInstance(), MSG_ROOT_GROUPS);
        }

        return subtitle;
    }

    @Override
    public String getCancelButtonLabel()
    {
        return Application.getMessage(FacesContext.getCurrentInstance(), "back_button");
    }

    @Override
    public String getContainerTitle() {
        if (group != null) {
            return MessageUtil.getMessage("groups_management_subgroup", authService.getAuthorityDisplayName(group));
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
        group = null;
        groupName = null;
        groupDataProvider = null;
        userDataProvider = null;
    }

    public void setDisableActions(boolean disableActions) {
        this.disableActions = disableActions;
    }

    public boolean isDisableActions() {
        return disableActions;
    }

    public void reset(ActionEvent event) {
        disableActions = false;
        restored();
    }

    @Override
    public void restored()
    {
        setCurrentGroup(null, Application.getMessage(FacesContext.getCurrentInstance(), MSG_ROOT_GROUPS));
    }

    @Override
    public Object getActionsContext()
    {
        return this;
    }

    // ------------------------------------------------------------------------------
    // FilterViewSupport implementation

    @Override
    public List<UIListItem> getFilterItems()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        List<UIListItem> items = new ArrayList<UIListItem>(2);

        UIListItem item1 = new UIListItem();
        item1.setValue(FILTER_CHILDREN);
        item1.setLabel(Application.getMessage(context, LABEL_FILTER_CHILDREN));
        items.add(item1);

        UIListItem item2 = new UIListItem();
        item2.setValue(FILTER_ALL);
        item2.setLabel(Application.getMessage(context, LABEL_FILTER_ALL));
        items.add(item2);

        return items;
    }

    @Override
    public void filterModeChanged(ActionEvent event)
    {
        UIModeList filterList = (UIModeList) event.getComponent();

        // update list filter mode from user selection
        setFilterMode(filterList.getValue().toString());
    }

    @Override
    public String getFilterMode()
    {
        return filterMode;
    }

    @Override
    public void setFilterMode(String filterMode)
    {
        this.filterMode = filterMode;

        // clear datalist cache ready to change results based on filter setting
        contextUpdated();
    }

    // ------------------------------------------------------------------------------
    // ChangeViewSupport implementation

    @Override
    public List<UIListItem> getViewItems()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        List<UIListItem> items = new ArrayList<UIListItem>(2);

        UIListItem item1 = new UIListItem();
        item1.setValue(VIEW_ICONS);
        item1.setLabel(Application.getMessage(context, LABEL_VIEW_ICONS));
        items.add(item1);

        UIListItem item2 = new UIListItem();
        item2.setValue(VIEW_DETAILS);
        item2.setLabel(Application.getMessage(context, LABEL_VIEW_DETAILS));
        items.add(item2);

        return items;
    }

    @Override
    public void viewModeChanged(ActionEvent event)
    {
        UIModeList viewList = (UIModeList) event.getComponent();

        // update view mode from user selection
        setViewMode(viewList.getValue().toString());
    }

    @Override
    public String getViewMode()
    {
        return viewMode;
    }

    @Override
    public void setViewMode(String viewMode)
    {
        this.viewMode = viewMode;
    }

    // ------------------------------------------------------------------------------
    // Bean property getters and setters

    public String getGroup()
    {
        return group;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setAuthService(AuthorityService authService)
    {
        this.authService = authService;
    }

    private AuthorityService getAuthorityService()
    {
        if (authService == null)
        {
            authService = BeanHelper.getAuthorityService();
        }
        return authService;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    private PersonService getPersonService()
    {
        if (personService == null)
        {
            personService = BeanHelper.getPersonService();
        }
        return personService;
    }

    public UIRichList getGroupsRichList()
    {
        return groupsRichList;
    }

    public void setGroupsRichList(UIRichList groupsRichList)
    {
        this.groupsRichList = groupsRichList;
    }

    public UIRichList getUsersRichList()
    {
        return usersRichList;
    }

    public void setUsersRichList(UIRichList usersRichList)
    {
        this.usersRichList = usersRichList;
    }

    /**
     * @return Breadcrumb location list
     */
    public List<IBreadcrumbHandler> getLocation()
    {
        if (location == null)
        {
            List<IBreadcrumbHandler> loc = new ArrayList<IBreadcrumbHandler>(8);
            loc.add(new GroupBreadcrumbHandler(null, Application.getMessage(FacesContext.getCurrentInstance(), MSG_ROOT_GROUPS)));

            location = loc;
        }

        return location;
    }

    /**
     * @return The list of group objects to display. Returns the list of root groups or the
     *         list of sub-groups for the current group if set.
     */
    public LazyListDataProvider<String, Map<String, String>> getGroups()
    {
        if (groupDataProvider == null) {
            UserTransaction tx = null;
            try
            {
                FacesContext context = FacesContext.getCurrentInstance();
                tx = Repository.getUserTransaction(context);
                tx.begin();

                Set<String> authorities;
                boolean immediate = (filterMode.equals(FILTER_CHILDREN));
                if (group == null)
                {
                    // root groups
                    if (immediate == true)
                    {
                        authorities = getAuthorityService().getAllRootAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);
                    }
                    else
                    {
                        authorities = getAuthorityService().getAllAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);
                    }
                }
                else
                {
                    // sub-group of an existing group
                    authorities = getAuthorityService().getContainedAuthorities(AuthorityType.GROUP, group, immediate);
                }
                if (!BeanHelper.getApplicationConstantsBean().isEinvoiceEnabled()) {
                    authorities.remove(BeanHelper.getUserService().getAccountantsGroup());
                }
                groupDataProvider = new GroupDataProvider(authorities);
                // commit the transaction
                tx.commit();
            } catch (Throwable err)
            {
                Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
                        FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), err.getMessage()), err);
                groupDataProvider = new GroupDataProvider();
                try {
                    if (tx != null) {
                        tx.rollback();
                    }
                } catch (Exception tex) {
                }
            }
        }
        return groupDataProvider;
    }

    /**
     * @return The list of user objects to display. Returns the list of user for the current group.
     */
    public UserDataProvider getUsers()
    {
        if (userDataProvider == null) {
            UserTransaction tx = null;
            try
            {
                FacesContext context = FacesContext.getCurrentInstance();
                tx = Repository.getUserTransaction(context, true);
                tx.begin();

                Set<String> authorities;
                boolean structUnitBased = false;
                if (group == null)
                {
                    authorities = Collections.<String> emptySet();
                }
                else
                {
                    // users of an existing group
                    boolean immediate = (filterMode.equals(FILTER_CHILDREN));
                    authorities = getAuthorityService().getContainedAuthorities(AuthorityType.USER, group, immediate);
                    structUnitBased = getAuthorityService().getAuthorityZones(group).contains(OrganizationStructureService.STRUCT_UNIT_BASED);
                }
                userDataProvider = new UserDataProvider(authorities, structUnitBased);
                // commit the transaction
                tx.commit();
            } catch (Throwable err)
            {
                Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
                        FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), err.getMessage()), err);
                userDataProvider = new UserDataProvider();
                try {
                    if (tx != null) {
                        tx.rollback();
                    }
                } catch (Exception tex) {
                }
            }
        }
        return userDataProvider;
    }

    /**
     * Set the current Group Authority.
     * <p>
     * Setting this value causes the UI to update and display the specified node as current.
     * 
     * @param group The current group authority.
     */
    protected void setCurrentGroup(String group, String groupName)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Setting current group: " + group);
        }

        // set the current Group Authority for our UI context operations
        this.group = group;
        this.groupName = groupName;
        groupDataProvider = null;
        userDataProvider = null;

        // inform that the UI needs updating after this change
        contextUpdated();
    }

    // ------------------------------------------------------------------------------
    // Action handlers

    /**
     * Action called when a Group folder is clicked.
     * Navigate into the Group and show child Groups and child Users.
     */
    public void clickGroup(ActionEvent event)
    {
        UIActionLink link = (UIActionLink) event.getComponent();
        Map<String, String> params = link.getParameterMap();
        String group = params.get("id");
        if (group != null && group.length() != 0)
        {
            // refresh UI based on node selection
            updateUILocation(group);
        }
    }

    /**
     * Remove specified user from the current group
     */
    public void removeUser(ActionEvent event)
    {
        UIActionLink link = (UIActionLink) event.getComponent();
        Map<String, String> params = link.getParameterMap();
        String authority = params.get("id");
        userDataProvider = null;
        if (authority != null && authority.length() != 0)
        {
            UserTransaction tx = null;
            try
            {
                FacesContext context = FacesContext.getCurrentInstance();
                tx = Repository.getUserTransaction(context);
                tx.begin();

                getAuthorityService().removeAuthority(group, authority);

                // commit the transaction
                tx.commit();

                // refresh UI after change
                contextUpdated();
                MessageUtil.addInfoMessage("delete_user_from_group");
            } catch (Throwable err)
            {
                Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
                        FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), err.getMessage()), err);
                try {
                    if (tx != null) {
                        tx.rollback();
                    }
                } catch (Exception tex) {
                }
            }
        }
    }

    // ------------------------------------------------------------------------------
    // Helpers

    /**
     * Update the breadcrumb with the clicked Group location
     */
    protected void updateUILocation(String group)
    {
        String groupName = getAuthorityService().getShortName(group);
        // NOTE: "fix" to stay on the same location.
        // this.location.add(new GroupBreadcrumbHandler(group, groupName));
        setCurrentGroup(group, groupName);
    }

    protected void removeFromBreadcrumb(String group)
    {
        // remove this node from the breadcrumb if required
        List<IBreadcrumbHandler> location = getLocation();
        GroupBreadcrumbHandler handler = (GroupBreadcrumbHandler) location.get(location.size() - 1);

        // see if the current breadcrumb location is our Group
        if (group.equals(handler.Group))
        {
            location.remove(location.size() - 1);

            // now work out which Group to set the list to refresh against
            if (location.size() != 0)
            {
                handler = (GroupBreadcrumbHandler) location.get(location.size() - 1);
                setCurrentGroup(handler.Group, handler.Label);
            }
        }
    }

    // ------------------------------------------------------------------------------
    // IContextListener implementation

    /**
     * @see org.alfresco.web.app.context.IContextListener#contextUpdated()
     */
    @Override
    public void contextUpdated()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Invalidating Group Management Components...");
        }

        // force a requery of the richlist dataset
        if (groupsRichList != null)
        {
            groupsRichList.setValue(null);
        }

        if (usersRichList != null)
        {
            usersRichList.setValue(null);
        }
    }

    /**
     * @see org.alfresco.web.app.context.IContextListener#areaChanged()
     */
    @Override
    public void areaChanged()
    {
        // nothing to do
    }

    /**
     * @see org.alfresco.web.app.context.IContextListener#spaceChanged()
     */
    @Override
    public void spaceChanged()
    {
        // nothing to do
    }

    // ------------------------------------------------------------------------------
    // Inner classes

    /**
     * Class to handle breadcrumb interaction for Group pages
     */
    private class GroupBreadcrumbHandler implements IBreadcrumbHandler
    {
        private static final long serialVersionUID = 1871876653151036630L;

        /**
         * Constructor
         * 
         * @param group The group for this navigation element if any
         * @param label Element label
         */
        public GroupBreadcrumbHandler(String group, String label)
        {
            Group = group;
            Label = label;
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return Label;
        }

        /**
         * @see org.alfresco.web.ui.common.component.IBreadcrumbHandler#navigationOutcome(org.alfresco.web.ui.common.component.UIBreadcrumb)
         */
        @Override
        public String navigationOutcome(UIBreadcrumb breadcrumb)
        {
            // All group breadcrumb elements relate to a Group
            // when selected we set the current Group Id and return
            setCurrentGroup(Group, Label);
            location = (List<IBreadcrumbHandler>) breadcrumb.getValue();

            return null;
        }

        public String Group;
        public String Label;
    }

    /**
     * Simple wrapper bean exposing user authority and person details for JSF results list
     */
    public static class UserAuthorityDetails implements Serializable
    {
        private static final long serialVersionUID = 1056255933962068348L;

        public UserAuthorityDetails(String name, String authority)
        {
            this.name = name;
            this.authority = authority;
        }

        public String getName()
        {
            return name;
        }

        public String getAuthority()
        {
            return authority;
        }

        private final String name;
        private final String authority;
    }

    private class GroupDataProvider extends LazyListDataProvider<String, Map<String, String>> {

        public GroupDataProvider() {
            objectKeys = Collections.emptyList();
        }

        public GroupDataProvider(Set<String> authorities) {
            objectKeys = new ArrayList<>(authorities);
        }

        @Override
        protected boolean loadOrderFromDb(String column, boolean descending) {
            return true;
        }

        @Override
        protected void resetObjectKeyOrder(List<Map<String, String>> orderedRows) {

        }

        @Override
        protected Map<String, Map<String, String>> loadData(List<String> rowsToLoad) {
            return UserUtil.getGroupsAsMapFromAuthorities(getAuthorityService(), BeanHelper.getUserService(), rowsToLoad);
        }

        @Override
        protected String getKeyFromValue(Map<String, String> groupProperties) {
            return groupProperties.get("id");
        }
    }

    private class UserDataProvider extends LazyListDataProvider<String, Map<String, String>> {

        private final boolean structUnitBased;

        public UserDataProvider() {
            structUnitBased = false;
        }

        public UserDataProvider(Collection<String> authorities, boolean structUnitBased) {
            this.structUnitBased = structUnitBased;
            objectKeys = new ArrayList<>(authorities);
        }


        @Override
        protected boolean loadOrderFromDb(String column, boolean descending) {
            return true;
        }

        @Override
        protected void resetObjectKeyOrder(List<Map<String, String>> orderedRows) {

        }

        @Override
        protected Map<String, Map<String, String>> loadData(List<String> rowsToLoad) {
            Map<String, Map<String, String>> data = new HashMap<>();

            Map<String, NodeRef> authorityUserRefs = new HashMap<>();
            for (String authority : rowsToLoad) {
                authorityUserRefs.put(authority, getPersonService().getPerson(authority));
            }

            Set<QName> personProperties = new HashSet<>();
            personProperties.add(ContentModel.PROP_USERNAME);
            personProperties.add(ContentModel.PROP_FIRSTNAME);
            personProperties.add(ContentModel.PROP_LASTNAME);
            Map<NodeRef, Node> personNodes = BeanHelper.getBulkLoadNodeService().loadNodes(authorityUserRefs.values(), personProperties);

            for (String authority : rowsToLoad) {
                String userName = getAuthorityService().getShortName(authority);
                Map<String, String> authMap = new HashMap<>(4, 1.0f);
                authMap.put("userName", userName);
                authMap.put("id", authority);
                authMap.put("structUnitBased", (structUnitBased) ? "true" : "false");
                authMap.put("name", UserUtil.getPersonFullName2(personNodes.get(authority).getProperties()));

                data.put(authority, authMap);
            }
            return data;
        }

        @Override
        protected String getKeyFromValue(Map<String, String> userProperties) {
            return userProperties.get("id");
        }
    }
}
