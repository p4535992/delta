package ee.webmedia.alfresco.privilege.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAuthorityService;
<<<<<<< HEAD
import static ee.webmedia.alfresco.common.web.BeanHelper.getPermissionService;
=======
>>>>>>> develop-5.1
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.privilege.service.PrivilegeServiceImpl.GROUPLESS_GROUP;
import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

<<<<<<< HEAD
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
=======
import org.alfresco.service.cmr.repository.NodeRef;
>>>>>>> develop-5.1
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.tag.data.ColumnTag;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;
import org.springframework.web.util.HtmlUtils;

import ee.alfresco.web.ui.common.UITableCell;
import ee.alfresco.web.ui.common.UITableRow;
import ee.alfresco.web.ui.common.renderer.data.RichListMultiTbodyRenderer.DetailsViewRenderer;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
<<<<<<< HEAD
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.PrivMappings;
=======
import ee.webmedia.alfresco.privilege.model.PrivMappings;
import ee.webmedia.alfresco.privilege.model.Privilege;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.user.web.GroupUsersListDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import flexjson.JSONSerializer;

/**
 * Dialog bean for managing privileges that could be inherited to the user through group and/or through parent nodes.
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
 */
public class ManageInheritablePrivilegesDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ManageInheritablePrivilegesDialog.class);
    public static final String BEAN_NAME = "ManageInheritablePrivilegesDialog";

    private static final String PARAM_CURRENT_GROUP = "currentGroup";

    private static final String USERGROUP_MARKER_CLASS = "tbGroup";

    private static Comparator<UserPrivileges> tableRowComparator;
    // UIComponents
    private transient UIRichList permissionsRichList;
    private transient UIGenericPicker picker;

    /**
     * could be used to indicate that
     * 1) permissions are inherited from parent node
     * or
     * 2) on series privileges management dialog false means that users without viewDocumentMetaData privilege shouldn't even know about documents that user doesn't have privilege
     * to open
     */
    private transient UIComponent checkbox;

    private boolean markPrivilegesBaseState;
    private PrivilegesHandler typeHandler;

    private State state;

    public class State implements Serializable {
        private static final long serialVersionUID = 1L;
        private PrivMappings privMappings;

        private NodeRef manageableRef;
        private List<UserPrivileges> userPrivileges;

        public State(NodeRef manageableRef) {
            this.manageableRef = manageableRef;
        }

        public NodeRef getManageableRef() {
            return manageableRef;
        }

        public void reset() {
            manageableRef = null;
            userPrivileges = null;
            privMappings = null;
        }

        public List<UserPrivileges> getUserPrivileges() {
            return userPrivileges;
        }

        public PrivMappings getPrivMappings() {
            return privMappings;
        }

    }

    private boolean rebuildUserPrivilegesRows;
<<<<<<< HEAD
    private boolean permissionsChecked;

=======
>>>>>>> develop-5.1
    private final GroupTranslatorMap groupNamesByCode = new GroupTranslatorMap();
    private static final JSONSerializer PRIV_DEPENDENCIES_SERIALIZER = new JSONSerializer().include("*").exclude("*.class");

    public void init(ActionEvent event) {
        NodeRef tmpManageableRef = new NodeRef(ActionUtil.getParam(event, "manageableRef"));
        init(tmpManageableRef, false);
    }

    private void init(NodeRef manageableRef, boolean reInit) {
        resetState(reInit);
        state = new State(manageableRef);
        LOG.debug("Managing permissions of " + manageableRef);
        QName manageableNodeType = getNodeService().getType(manageableRef);
        String beanName = StringUtils.capitalize(manageableNodeType.getLocalName()) + "TypePrivilegesHandler";
        typeHandler = BeanHelper.getSpringBean(PrivilegesHandler.class, beanName);
        typeHandler.reset();
        typeHandler.setDialogBean(this);
        typeHandler.setDialogState(state);
        MessageUtil.addStatusMessage(typeHandler.getInfoMessage());
        state.privMappings = getPrivilegeService().getPrivMappings(manageableRef, typeHandler.getManageablePermissions());
        markPrivilegesBaseState = true;
<<<<<<< HEAD
=======

>>>>>>> develop-5.1
    }

    @Override
    public boolean getFinishButtonDisabled() {
<<<<<<< HEAD
        // FIXME Kaarel - seda meetodit ei arvestata (seoses "Salvesta nupp alati nähtavaks" ümber tegemisega) - see meetod võiks määrata kas nupp on disabled.
=======
        // FIXME seda meetodit ei arvestata (seoses "Salvesta nupp alati nähtavaks" ümber tegemisega) - see meetod võiks määrata kas nupp on disabled.
>>>>>>> develop-5.1
        // ajutise workaround'ina on finishImpl'is kontroll + veateate kuvamine
        return !typeHandler.isEditable();
    }

    @Override
    public String getContainerTitle() {
        return typeHandler.getContainerTitle();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        isFinished = false;
        if (!typeHandler.isEditable()) {
            MessageUtil.addErrorMessage("manage_permissions_save_error_noPermissions");
            return null;
        }
        Map<String, UserPrivileges> loosingPrivileges = getVosThatLoosePrivileges();
        if (typeHandler.validate(loosingPrivileges)) {
            try {
                typeHandler.save();
                getPrivilegeService().savePrivileges(state.getManageableRef(), state.privMappings.getPrivilegesByUsername()
                        , state.privMappings.getPrivilegesByGroup(), typeHandler.getNodeType());
                MessageUtil.addInfoMessage("save_success");
            } catch (RuntimeException e) {
                LOG.error("Saving privileges failed for " + state.getManageableRef(), e);
                MessageUtil.addErrorMessage("save_failed", e.getMessage());
            }
            init(state.getManageableRef(), true);
            rebuildUserPrivilegesRows = true;
            picker.queueEvent(new UIGenericPicker.PickerEvent(picker, UIGenericPicker.ACTION_CLEAR, UserContactGroupSearchBean.USERS_FILTER, null, null));
        }
        return null;
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        // Perform indexing in a separate transaction, because if it takes a long time,
        // then this transaction has not done any changes in database (changes only index).
        // All changes to database were performed in finishImpl method.
        getPrivilegeService().updateIndexedPermissions(state.getManageableRef());
        return super.doPostCommitProcessing(context, outcome);
    }

    private Map<String, UserPrivileges> getVosThatLoosePrivileges() {
        Map<String /* userName */, UserPrivileges> vos = new HashMap<String, UserPrivileges>();
        for (UserPrivileges vo : state.privMappings.getPrivilegesByUsername().values()) {
<<<<<<< HEAD
            Set<String> privilegesToDelete = vo.getPrivilegesToDelete();
=======
            Set<Privilege> privilegesToDelete = vo.getPrivilegesToDelete();
>>>>>>> develop-5.1
            if (!privilegesToDelete.isEmpty() && !vo.getActivePrivileges().containsAll(privilegesToDelete)) {
                vos.put(vo.getUserName(), vo); // at least one static privilege will be lost when saving
            }
        }
        return vos;
    }

    @Override
    public String cancel() {
        resetState(false);
        if (typeHandler != null) {
            typeHandler.reset();
            typeHandler = null;
        }
        return super.cancel();
    }

    private void resetState(boolean reInit) {
        if (!reInit) {
            permissionsRichList = null;
            picker = null;
            checkbox = null;
<<<<<<< HEAD
            permissionsChecked = false;
=======
>>>>>>> develop-5.1
        }
        tableRowComparator = null;
        if (state != null) {
            state.reset();
        }
    }

    public UIGenericPicker getPicker() {
        return picker;
    }

    public void setPicker(UIGenericPicker picker) {
        this.picker = picker;
    }

    public UIComponent getCheckbox() {
        return checkbox;
    }

    public void setCheckbox(UIComponent checkbox) {
        this.checkbox = checkbox;
    }

    public UIRichList getPermissionsRichList() {
        return permissionsRichList;
    }

    /**
     * @param permissionsRichList - partially preconfigured RichList from jsp
     */
    public void setPermissionsRichList(UIRichList permissionsRichList) {
        if (this.permissionsRichList == null) {
            ComponentUtil.putAttribute(permissionsRichList, DetailsViewRenderer.ATTR_GROUP_BY, GROUPLESS_GROUP);
            ComponentUtil.putAttribute(permissionsRichList, DetailsViewRenderer.ATTR_ADDITIONAL_ROW_STYLE_BINDING, "userId_#{r.userName}");
            ComponentUtil.putAttribute(permissionsRichList, DetailsViewRenderer.ATTR_GROUP_TBODY_ATTRIBUTES, new HashMap<String, Map<String, String>>());

            FacesContext context = FacesContext.getCurrentInstance();
            List<UIComponent> permissionRLChildren = ComponentUtil.getChildren(permissionsRichList);
            boolean editable = typeHandler.isEditable();
<<<<<<< HEAD
            for (String permission : typeHandler.getManageablePermissions()) {
=======
            for (Privilege permission : typeHandler.getManageablePermissions()) {
>>>>>>> develop-5.1
                if (!permission.equals(typeHandler.getImplicitPrivilege())) { // don't add implicit privilege column
                    permissionRLChildren.add(createPermissionColumn(permission, context, editable));
                }
            }
            permissionRLChildren.add(createActionsColumn(context));
        }
        this.permissionsRichList = permissionsRichList;
    }

    /**
     * Action handler called when the Add button is pressed to process the current selection
     */
    public void addAuthorities(ActionEvent event) {
        @SuppressWarnings("hiding")
        UIGenericPicker picker = (UIGenericPicker) event.getComponent().findComponent("picker");
        String[] results = picker.getSelectedResults();
        if (results != null) {
            for (String authorityName : results) {
                addAuthorityUsers(authorityName);
                rebuildUserPrivilegesRows = true;
            }

        }
    }

    public void viewGroup(ActionEvent event) {
        GroupUsersListDialog groupsDialog = BeanHelper.getGroupUsersListDialog();
        groupsDialog.reset(event);
        groupsDialog.setDisableActions(true);
        groupsDialog.clickGroup(event);
        WebUtil.navigateTo("dialog:groupUsersListDialog");
    }

    /** @param event passed to MethodBinding */
    public void removeGroupWithUsers(ActionEvent event) {
        // remove group, but don't remove any privileges from group members(
        String groupName = ActionUtil.getParam(event, PARAM_CURRENT_GROUP);
        UserPrivileges groupPrivs = state.privMappings.getPrivilegesByGroup().get(groupName);
        groupPrivs.setDeleted(true);
        finish();
    }

    /** @param event passed to MethodBinding */
    public void inlineGroupUsers(ActionEvent event) {
        String groupName = ActionUtil.getParam(event, PARAM_CURRENT_GROUP);
        UserPrivileges groupPrivs = state.privMappings.getPrivilegesByGroup().get(groupName);
        groupPrivs.setDeleted(true);
        Map<String, Boolean> privileges = groupPrivs.getPrivileges();
<<<<<<< HEAD
        Set<String> groupActivePrivs = new HashSet<String>(privileges.size());
        for (Entry<String, Boolean> entry : privileges.entrySet()) {
            Boolean active = entry.getValue();
            if (active != null && active) {
                String permission = entry.getKey();
=======
        Set<Privilege> groupActivePrivs = new HashSet<Privilege>(privileges.size());
        for (Entry<String, Boolean> entry : privileges.entrySet()) {
            Boolean active = entry.getValue();
            if (active != null && active) {
                Privilege permission = Privilege.getPrivilegeByName(entry.getKey());
>>>>>>> develop-5.1
                groupActivePrivs.add(permission);
            }
        }
        for (String userName : getUserService().getUserNamesInGroup(groupName)) {
            UserPrivileges userPrivs = addUser(userName, GROUPLESS_GROUP.equals(groupName));
            userPrivs.addPrivilegesStatic(groupActivePrivs);
        }
        finish();
    }

    /** @param event passed to MethodBinding */
    public void removePerson(ActionEvent event) {
        String userName = ActionUtil.getParam(event, "userName");
        deletePerson(userName);
        MessageUtil.addInfoMessage("manage_permissions_removePerson_postponed");
        rebuildUserPrivilegesRows = true;
    }

    /**
     * Try to delete person from all groups. Person can't be deleted from privileges list while it still has privileges, hence person can't be deleted from:<br>
     * 1) dynamicPrivilegesGroups<br>
     * 2) groupless group if user is document owner
<<<<<<< HEAD
     * 
=======
     *
>>>>>>> develop-5.1
     * @param userName
     */
    private void deletePerson(String userName) {
        UserPrivileges curUserVO = state.privMappings.getPrivilegesByUsername().get(userName);
        curUserVO.setDeleted(true);
        curUserVO.deletePrivilegesStatic(typeHandler.getManageablePermissions());
    }

    public String getPrivilegeDependencies() {
<<<<<<< HEAD
        return PRIV_DEPENDENCIES_SERIALIZER.serialize(DocumentCommonModel.Privileges.PRIVILEGE_DEPENDENCIES);
=======
        return PRIV_DEPENDENCIES_SERIALIZER.serialize(Privilege.PRIVILEGE_DEPENDENCIES);
>>>>>>> develop-5.1
    }

    /** @return permissions table for JSF value binding */
    public Collection<UserPrivileges> getUserPrivilegesRows() {
        // get privileges from current state
        List<UserPrivileges> userPrivileges = state.getUserPrivileges();
        // we have valid privileges and no need to rebuild them
        if (userPrivileges != null && !rebuildUserPrivilegesRows) {
            return userPrivileges;
        }
        // we have a need to rebuild privileges
        // let's start from scratch
        userPrivileges = new ArrayList<UserPrivileges>();
        // set the pointer
        state.userPrivileges = userPrivileges;
        // facets based cleanup ?
        permissionsRichList.getFacets().clear(); // remove also group rows that are actually rendered from facets
        // ??
        getOrCreateFacetRows().clear();
        // let's be optimistic about end-result
        rebuildUserPrivilegesRows = false;
        // get current mappings, for both users??? and groups
        PrivMappings privMappings = state.privMappings;
        Collection<UserPrivileges> privileges = null;
        if (privMappings != null) {
            Map<String, UserPrivileges> privilegesByUsername = privMappings.getPrivilegesByUsername();
            privileges = privilegesByUsername.values();
            for (UserPrivileges userPrivs : privileges) {
                Set<String> groups = userPrivs.getGroups();
                if (!userPrivs.isDeleted() && !userPrivileges.contains(userPrivs) && groups.contains(GROUPLESS_GROUP)) {
                    userPrivileges.add(userPrivs);
                }
            }
        }
        // add or update owner's privileges.
        addDynamicOwnerPrivileges();
        //
        Map<String, UserPrivileges> privilegesByGroup = privMappings.getPrivilegesByGroup();
        addTbodyAttributesByGroup(GROUPLESS_GROUP); // this group is not in privilegesByGroup - need to call it so that groupless group header wouldn't disappear after refreshing
        for (Entry<String, UserPrivileges> entry : privilegesByGroup.entrySet()) {
            String groupCode = entry.getKey();
            UserPrivileges groupPrivs = entry.getValue();
<<<<<<< HEAD
            Set<String> groupPrivCodes = groupPrivs.getActivePrivileges();
=======
            Set<Privilege> groupPrivCodes = groupPrivs.getActivePrivileges();
>>>>>>> develop-5.1
            addTbodyAttributesByGroup(groupCode);
            addGroupRowIfNeeded(groupCode);
            String inheritedFromGroupReason = MessageUtil.getMessage("manage_permissions_extraInfo_inherited_fromGroup", groupNamesByCode.get(groupCode));

            // iterate through usernames belonging to group
            for (String userName : getUserService().getUserNamesInGroup(groupCode)) {
                // check if group username is in the list of userPrivileges already compiled (owner, responsible, hand picked)
                for (UserPrivileges userPrivilege : userPrivileges) {
                    // if is, then add userprivs
                    if (userName.equals(userPrivilege.getUserName())) {
                        // add privileges from group
<<<<<<< HEAD
                        for (String groupPriv : groupPrivCodes) {
=======
                        for (Privilege groupPriv : groupPrivCodes) {
>>>>>>> develop-5.1
                            userPrivilege.addPrivilegeDynamic(groupPriv, inheritedFromGroupReason);
                        }
                        // found and processed the right one, break the iteration through already choosen userPrivs
                        break;
                    }
                }
            }
        }

        // set up rights related to document_manage_permissions_extraInfo_documentIsPublic
        typeHandler.addDynamicPrivileges();

        if (markPrivilegesBaseState) {
            if (privileges != null) {
                for (UserPrivileges userPrivs : privileges) {
                    userPrivs.markBaseState();
                }
            }
            for (UserPrivileges groupPrivs : privilegesByGroup.values()) {
                groupPrivs.markBaseState();
            }
            markPrivilegesBaseState = false;
        }
        Collections.sort(userPrivileges, getTableRowComparator());
<<<<<<< HEAD
        validatePermissions();
=======
>>>>>>> develop-5.1
        return userPrivileges;
    }

    public PrivilegesHandler getTypeHandler() {
        return typeHandler;
    }

    public State getState() {
        return state;
    }

<<<<<<< HEAD
    private void validatePermissions() {
        if (!permissionsChecked && BeanHelper.getApplicationService().isTest()) {
            final Collection<String> manageablePermissions = typeHandler.getManageablePermissions();
            final ArrayList<String> msgs = new ArrayList<String>();
            for (final UserPrivileges row : state.privMappings.getPrivilegesByUsername().values()) {
                final String userName = row.getUserName();
                if (StringUtils.isBlank(userName)) {
                    continue;
                }
                AuthenticationUtil.runAs(new RunAsWork<Object>() {
                    @Override
                    public Object doWork() throws Exception {
                        for (Entry<String, Boolean> entry : row.getPrivileges().entrySet()) {
                            String privilege = entry.getKey();
                            if (entry.getValue()) {
                                AccessStatus hasPermission = getPermissionService().hasPermission(state.getManageableRef(), privilege);
                                if (!AccessStatus.ALLOWED.equals(hasPermission)) {
                                    String reason = row.getDynamicPrivReasons().get(privilege);
                                    if (reason == null) {
                                        reason = "";
                                    } else {
                                        reason = "(" + reason + ")";
                                    }
                                    String msg = "User " + userName + " doesn't have privilege " + privilege + reason;
                                    if (manageablePermissions.contains(privilege)) {
                                        msgs.add(msg);
                                    } else {
                                        LOG.warn(msg);
                                    }
                                }
                            }
                        }
                        return null;
                    }
                }, userName);
            }
            if (!msgs.isEmpty()) {
                LOG.error(msgs.size() + " problems with permissions");
                String msg = msgs.size() + " privileges are missing on node " + state.getManageableRef() + ":\n" + StringUtils.join(msgs, "\n");
                LOG.error(msg);
                throw new RuntimeException(msg);
            }
            permissionsChecked = true;
        }
    }

=======
>>>>>>> develop-5.1
    @SuppressWarnings("unchecked")
    private Comparator<? super UserPrivileges> getTableRowComparator() {
        if (tableRowComparator == null) {
            tableRowComparator = new TransformingComparator(new ComparableTransformer<UserPrivileges>() {
                @Override
                public Comparable<?> tr(UserPrivileges input) {
                    return input.getUserDisplayName();
                }
            }, new NullComparator(AppConstants.DEFAULT_COLLATOR));
        }
        return tableRowComparator;
    }

    /** add all privileges to object owner(and make row readOnly) */
    private void addDynamicOwnerPrivileges() {
        String ownerId = typeHandler.getObjectOwner();
        if (ownerId == null) {
            return;
        }
        UserPrivileges ownerRow = null;
        Collection<UserPrivileges> userPrivileges = state.getUserPrivileges();
        for (UserPrivileges userPrivilegesRow : userPrivileges) {
            if (StringUtils.equals(userPrivilegesRow.getUserName(), ownerId)) {
                ownerRow = userPrivilegesRow;
                break;
            }
        }
        String extraPrivilegeReason = MessageUtil.getMessage(typeHandler.getNodeType().getLocalName() + "_manage_permissions_extraInfo_userIsOwner");
<<<<<<< HEAD
        Collection<String> manageablePermissions = typeHandler.getManageablePermissions();
=======
        Collection<Privilege> manageablePermissions = typeHandler.getManageablePermissions();
>>>>>>> develop-5.1
        if (ownerRow == null) {
            // this might happen when rebuilding UserPrivilegesRows after removing all members of group
            ownerRow = addAuthorityRow(ownerId, manageablePermissions, GROUPLESS_GROUP, extraPrivilegeReason);
            userPrivileges.add(ownerRow);
        } else {
<<<<<<< HEAD
            for (String permission : manageablePermissions) {
=======
            for (Privilege permission : manageablePermissions) {
>>>>>>> develop-5.1
                if (!ownerRow.getPrivileges().containsKey(permission)) {
                    ownerRow.addPrivilegeDynamic(permission, extraPrivilegeReason);
                }
            }
        }
        ownerRow.setReadOnly(true);
    }

<<<<<<< HEAD
    private UserPrivileges addAuthorityRow(String authority, Collection<String> privileges, String group, String extraPrivilegeReason) {
        UserPrivileges vo = state.privMappings.getOrCreateUserPrivilegesVO(authority);
        vo.addGroup(group);
        for (String privilege : privileges) {
=======
    private UserPrivileges addAuthorityRow(String authority, Collection<Privilege> privileges, String group, String extraPrivilegeReason) {
        UserPrivileges vo = state.privMappings.getOrCreateUserPrivilegesVO(authority);
        vo.addGroup(group);
        for (Privilege privilege : privileges) {
>>>>>>> develop-5.1
            vo.addPrivilegeDynamic(privilege, extraPrivilegeReason);
        }
        addGroupRowIfNeeded(group);
        addTbodyAttributesByGroup(group);
        return vo;
    }

    private void addGroupRowIfNeeded(String group) {
        if (!GROUPLESS_GROUP.equals(group)) {
            Collection<String> attribute = getOrCreateFacetRows();
            if (!attribute.contains(group)) {
                attribute.add(group);
            }
        }
    }

    private Collection<String> getOrCreateFacetRows() {
        Collection<String> facetRows = DetailsViewRenderer.getFacetRows(permissionsRichList);
        if (facetRows == null) {
            facetRows = new TreeSet<String>(new AppConstants.SerializableDefaultCollatorDelegate());
            DetailsViewRenderer.setFacetRows(permissionsRichList, facetRows);
        }
        return facetRows;
    }

    public static class GroupTranslatorMap extends HashMap<String, String> {
        private static final long serialVersionUID = 1L;

        @Override
        public String get(Object key) {
            String groupCode = (String) key;
            String value = super.get(groupCode);
            if (value == null) {
                if (GROUPLESS_GROUP.equals(key)) {
                    value = MessageUtil.getMessage("manage_permissions_group_groupless");
                } else {
                    value = getAuthorityService().getAuthorityDisplayName(groupCode);
                }
                put(groupCode, value);
            }
            return value;
        }
    }

    private void addTbodyAttributesByGroup(String groupCode) {
        Map<String/* attributeName */, String/* attributeValue */> tbodyAttributes = new HashMap<String, String>();
        String groupCodeHtml = HtmlUtils.htmlEscape(groupCode).replaceAll(" ", "¤");
        tbodyAttributes.put("class", USERGROUP_MARKER_CLASS + " " + groupCodeHtml);
        Map<String/* groupCode */, Map<String/* attributeName */, String/* attributeValue */>> tbodyAttributesByGroup = (Map<String, Map<String, String>>)
                ComponentUtil.getAttribute(permissionsRichList, DetailsViewRenderer.ATTR_GROUP_TBODY_ATTRIBUTES);
        tbodyAttributesByGroup.put(groupCode, tbodyAttributes);
        String groupDisplayName = groupNamesByCode.get(groupCode);
        UITableRow tr = (UITableRow) permissionsRichList.getFacet(groupCode);
        if (tr == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            Application application = context.getApplication();
            boolean grouplessGroup = GROUPLESS_GROUP.equals(groupCode);
            tr = new UITableRow();
            putAttribute(tr, "styleClass", "grHeader" + (grouplessGroup ? " groupless" : ""));

            UITableCell tableCell = new UITableCell();
            putAttribute(tableCell, "styleClass", grouplessGroup ? "expanded" : "users");
            addChildren(tr, tableCell);
            String groupPrivsBindingPrefix = BEAN_NAME + ".privilegesByGroup['" + groupCode + "']";
            {
                UITableCell uiTableCell = new UITableCell();
                putAttribute(uiTableCell, "styleClass", "left");
                if (grouplessGroup) {
                    HtmlOutputText headerText = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
                    headerText.setValue(groupDisplayName);
                    addChildren(uiTableCell, headerText);
                } else {
                    UIActionLink viewGroup = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                    viewGroup.setValue(groupDisplayName);
                    viewGroup.setActionListener(application.createMethodBinding(
                            "#{" + BEAN_NAME + ".viewGroup}", new Class[] { javax.faces.event.ActionEvent.class }));
<<<<<<< HEAD
                    String tooltipVB = "#{" + groupPrivsBindingPrefix + ".explanationByPrivilege['" + typeHandler.getImplicitPrivilege() + "']}";
=======
                    String tooltipVB = "#{" + groupPrivsBindingPrefix + ".explanationByPrivilege['" + typeHandler.getImplicitPrivilege().getPrivilegeName() + "']}";
>>>>>>> develop-5.1
                    UIComponentTagUtils.setValueBinding(context, viewGroup, "tooltip", tooltipVB);
                    addChildren(viewGroup, ComponentUtil.createUIParam("id", groupCode, application));
                    addChildren(uiTableCell, viewGroup);
                }
                addChildren(tr, uiTableCell);
            }

            boolean editable = typeHandler.isEditable();
<<<<<<< HEAD
            for (String permission : typeHandler.getManageablePermissions()) {
                if (permission.equals(typeHandler.getImplicitPrivilege())) {
=======
            for (Privilege privilege : typeHandler.getManageablePermissions()) {
                if (privilege.equals(typeHandler.getImplicitPrivilege())) {
>>>>>>> develop-5.1
                    continue; // don't add implicit privilege column
                }
                tableCell = new UITableCell();
                // disable checkboxes when group only has dynamic privilege
<<<<<<< HEAD
                HtmlSelectBooleanCheckbox cb = createCB(permission, context, editable, groupPrivsBindingPrefix);
                FacesHelper.setupComponentId(context, cb, groupCode + "-" + permission);

                cb.setStyleClass("permission_" + permission + " tooltip"); // part of hack for showing tooltip on checkboxes that are disabled

                if (!grouplessGroup) { // can't manage permissions of virtual group GROUPLESS_GROUP
                    UIComponentTagUtils.setValueProperty(context, cb, "#{" + BEAN_NAME + ".privilegesByGroup['" + groupCode + "'].privileges['" + permission + "']}");
=======
                HtmlSelectBooleanCheckbox cb = createCB(privilege, context, editable, groupPrivsBindingPrefix);
                String privilegeName = privilege.getPrivilegeName();
                FacesHelper.setupComponentId(context, cb, groupCode + "-" + privilegeName);

                cb.setStyleClass("permission_" + privilegeName + " tooltip"); // part of hack for showing tooltip on checkboxes that are disabled

                if (!grouplessGroup) { // can't manage permissions of virtual group GROUPLESS_GROUP
                    UIComponentTagUtils.setValueProperty(context, cb, "#{" + BEAN_NAME + ".privilegesByGroup['" + groupCode + "'].privileges['" + privilegeName + "']}");
>>>>>>> develop-5.1
                }
                addChildren(tableCell, cb);
                addChildren(tr, tableCell);
            }

            tableCell = new UITableCell();
            if (editable && !UserService.AUTH_ADMINISTRATORS_GROUP.equals(groupCode)) {
                if (!grouplessGroup) {
                    UIActionLink inlineGroupLink = createRemoveLink(context, RemoveLink.INLINE_GROUP_USERS, groupPrivsBindingPrefix);
                    addChildren(inlineGroupLink, createUIParam(PARAM_CURRENT_GROUP, groupCode, application));
                    addChildren(tableCell, inlineGroupLink);

                    UIActionLink removeGroupLink = createRemoveLink(context, RemoveLink.REMOVE_GROUP_WITH_USERS, groupPrivsBindingPrefix);
                    addChildren(removeGroupLink, createUIParam(PARAM_CURRENT_GROUP, groupCode, application));
                    addChildren(tableCell, removeGroupLink);
                }
            }
            addChildren(tr, tableCell);
            ComponentUtil.addFacet(permissionsRichList, groupCode, tr);
        }
    }

    /** Used for JSF binding */
    public Map<String, UserPrivileges> getPrivilegesByUsername() {
        return state.privMappings.getPrivilegesByUsername();
    }

    /** Used for JSF binding to modify privileges of the group */
    public Map<String, UserPrivileges> getPrivilegesByGroup() {
        return state.privMappings.getPrivilegesByGroup();
    }

    private void addAuthorityUsers(String authorityName) {
        Authority authority = getUserService().getAuthorityOrNull(authorityName);
        if (LOG.isDebugEnabled()) {
            LOG.debug("ADDING AUTHORITY " + ReflectionToStringBuilder.reflectionToString(authority, ToStringStyle.MULTI_LINE_STYLE));
        }
        if (authority == null) {
            throw new RuntimeException("Didn't find authority based on '" + authorityName + "'");
        }
        UserPrivileges privs;
        if (authority.isGroup()) {
            Map<String, UserPrivileges> privilegesByGroup = state.privMappings.getPrivilegesByGroup();
            privs = privilegesByGroup.get(authorityName);
            if (privs == null) {
                // group was not yet added
                privs = new UserPrivileges(authorityName, getAuthorityService().getAuthorityDisplayName(authorityName));
                privilegesByGroup.put(authorityName, privs);
            }
        } else { // authority is user
            privs = addUser(authorityName, true);
        }
        privs.addPrivilegeStatic(typeHandler.getImplicitPrivilege());
    }

    private UserPrivileges addUser(String userName, boolean toNamelessGroup) {
        UserPrivileges userPrivs = state.privMappings.getOrCreateUserPrivilegesVO(userName);
        userPrivs.setDeleted(false);// maybe existed and was deleted before saving
        if (toNamelessGroup) {
            userPrivs.addGroup(GROUPLESS_GROUP);
        }
        return userPrivs;
    }

    private UIColumn createActionsColumn(FacesContext context) {
        Application application = context.getApplication();
        UIColumn column = (UIColumn) application.createComponent(ColumnTag.COMPONENT_TYPE);
        if (typeHandler.isEditable()) { // don't add remove link when dialog is not editable
            UIActionLink removePersonLink = createRemoveLink(context, RemoveLink.REMOVE_PERSON, "r");
            addChildren(removePersonLink, createUIParam("userName", "#{r.userName}", application));
            addChildren(column, removePersonLink);
        }
        UIPanel empty = new UIPanel();// workaround for IE7 - otherwise if removePersonLink is not rendered, then underline is not rendered either
        ComponentUtil.putAttribute(empty, "styleClass", "linkReplacement"); // workaround for IE make lines without remove link the same height as rest of them
        UIComponentTagUtils.setValueBinding(context, empty, "rendered", "#{!r.removable}");
        addChildren(column, empty);
        return column;
    }

    private enum RemoveLink {
        REMOVE_PERSON("remove_user.gif", "remove_user", "removePerson")
        , REMOVE_GROUP_WITH_USERS("delete_group.gif", "manage_permissions_tooltip_removeGroupWithUsers", "removeGroupWithUsers")
        , INLINE_GROUP_USERS("edit_group.gif", "manage_permissions_tooltip_inlineGroupUsers", "inlineGroupUsers");

        private final String iconName;
        private final String tooltip;
        /** NB! this stileClass is also used to bind confirm javaScript behavior to the remove link! */
        private final String styleClass;

        RemoveLink(String iconName, String tooltip, String styleClassAndRemoveMethod) {
            this.iconName = iconName;
            this.tooltip = tooltip;
            styleClass = styleClassAndRemoveMethod;
        }

        String getMethodBinding() {
            return "#{" + BEAN_NAME + "." + styleClass + "}";
        }
    }

    private UIActionLink createRemoveLink(FacesContext context, RemoveLink linkType, String rowBindingVar) {
        Application application = context.getApplication();
        UIActionLink removeLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        removeLink.setValue("");
        removeLink.setShowLink(false);
        removeLink.setImage("/images/icons/" + linkType.iconName);
        removeLink.setTooltip(MessageUtil.getMessage(linkType.tooltip));
        removeLink.setActionListener(application.createMethodBinding(linkType.getMethodBinding(), new Class[] { javax.faces.event.ActionEvent.class }));
        UIComponentTagUtils.setValueBinding(context, removeLink, "rendered", "#{" + rowBindingVar + ".removable}");
        ComponentUtil.putAttribute(removeLink, "styleClass", linkType.styleClass);
        return removeLink;
    }

<<<<<<< HEAD
    private UIColumn createPermissionColumn(String privilege, FacesContext context, boolean columnMaybeEditable) {
        UIColumn column = new UIColumn();
        HtmlOutputText headerText = new HtmlOutputText();
        headerText.setValue(MessageUtil.getMessage("permission_" + privilege));
        ComponentUtil.addFacet(column, "header", headerText);
        HtmlSelectBooleanCheckbox cb = createCB(privilege, context, columnMaybeEditable, "r");
        UIComponentTagUtils.setValueProperty(context, cb, "#{" + BEAN_NAME + ".privilegesByUsername[r.userName].privileges['" + privilege + "']}");
        UIComponentTagUtils.setValueBinding(context, cb, "styleClass", "userId_#{r.userName} permission_" + privilege + " tooltip");
=======
    private UIColumn createPermissionColumn(Privilege privilege, FacesContext context, boolean columnMaybeEditable) {
        UIColumn column = new UIColumn();
        HtmlOutputText headerText = new HtmlOutputText();
        String privilegeName = privilege.getPrivilegeName();
        headerText.setValue(MessageUtil.getMessage("permission_" + privilegeName));
        ComponentUtil.addFacet(column, "header", headerText);
        HtmlSelectBooleanCheckbox cb = createCB(privilege, context, columnMaybeEditable, "r");
        UIComponentTagUtils.setValueProperty(context, cb, "#{" + BEAN_NAME + ".privilegesByUsername[r.userName].privileges['" + privilegeName + "']}");
        UIComponentTagUtils.setValueBinding(context, cb, "styleClass", "userId_#{r.userName} permission_" + privilegeName + " tooltip");
>>>>>>> develop-5.1

        addChildren(column, cb);
        return column;
    }

<<<<<<< HEAD
    private HtmlSelectBooleanCheckbox createCB(String permission, FacesContext context, boolean maybeEditable, String rowBindingVar) {
        HtmlSelectBooleanCheckbox cb = new HtmlSelectBooleanCheckbox();
        if (maybeEditable) {
            maybeEditable = !typeHandler.isPermissionColumnDisabled(permission);
        }
        if (maybeEditable) {
            UIComponentTagUtils.setValueBinding(context, cb, "disabled", "#{" + rowBindingVar + ".disabledByPrivilege['" + permission + "']==true}");
=======
    private HtmlSelectBooleanCheckbox createCB(Privilege privilege, FacesContext context, boolean maybeEditable, String rowBindingVar) {
        HtmlSelectBooleanCheckbox cb = new HtmlSelectBooleanCheckbox();
        if (maybeEditable) {
            maybeEditable = !typeHandler.isPermissionColumnDisabled(privilege);
        }
        String privilegeName = privilege.getPrivilegeName();
        if (maybeEditable) {
            UIComponentTagUtils.setValueBinding(context, cb, "disabled", "#{" + rowBindingVar + ".disabledByPrivilege['" + privilegeName + "']==true}");
>>>>>>> develop-5.1
        } else {
            cb.setDisabled(true);
        }
        // Add tooltip explaining dynamic privilege reason.
<<<<<<< HEAD
        UIComponentTagUtils.setValueBinding(context, cb, "title", "#{" + rowBindingVar + ".explanationByPrivilege['" + permission + "']}");
=======
        UIComponentTagUtils.setValueBinding(context, cb, "title", "#{" + rowBindingVar + ".explanationByPrivilege['" + privilegeName + "']}");
>>>>>>> develop-5.1
        return cb;
    }

}
