package ee.webmedia.alfresco.privilege.web;

import static ee.webmedia.alfresco.privilege.service.PrivilegeServiceImpl.GROUPLESS_GROUP;
import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.FixedOrderComparator;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;
import org.springframework.web.util.HtmlUtils;

import ee.alfresco.web.ui.common.UITableCell;
import ee.alfresco.web.ui.common.UITableRow;
import ee.alfresco.web.ui.common.renderer.data.RichListMultiTbodyRenderer.DetailsViewRenderer;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.permissions.SeriesDocManagerDynamicAuthority;
import ee.webmedia.alfresco.document.service.event.DocumentWorkflowStatusEventListener;
import ee.webmedia.alfresco.document.web.evaluator.IsAdminOrDocManagerEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.IsOwnerEvaluator;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.service.PrivilegeServiceImpl.PrivilegeMappings;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.Transformer;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import flexjson.JSONSerializer;

/**
 * Dialog bean for managing privileges.
 * 
 * @author Ats Uiboupin
 */
@SuppressWarnings("unchecked")
public class ManagePrivilegesDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ManagePrivilegesDialog.class);
    public static final String BEAN_NAME = "ManagePrivilegesDialog";

    private static final String PARAM_CURRENT_GROUP = "currentGroup";
    private Comparator<UserPrivilegesRow> tableRowComparator;

    private static final Set<String> dynamicPrivilegesGroups = new HashSet<String>(Arrays.asList(UserService.AUTH_ADMINISTRATORS_GROUP
            , "curSeries_" + UserService.AUTH_DOCUMENT_MANAGERS_GROUP, UserService.AUTH_DOCUMENT_MANAGERS_GROUP));
    private static final String CUR_SER_DOC_MANAGERS_GROUP_CODE = "curSeries_" + UserService.AUTH_DOCUMENT_MANAGERS_GROUP;

    private static final String USERGROUP_MARKER_CLASS = "tbGroup";

    private transient GeneralService generalService;
    private transient PermissionService permissionService;
    private transient PrivilegeService privilegeService;
    private transient UserService userService;
    private transient AuthorityService authorityService;

    private boolean editable;
    private boolean markPrivilegesBaseState;
    private UIRichList permissionsRichList;
    private UIGenericPicker picker;
    private NodeRef manageableRef;
    private Collection<String> manageablePermissions;

    private PrivilegeMappings privMappings;

    private List<UserPrivilegesRow> userPrivilegesRows;
    private boolean rebuildUserPrivilegesRows;
    private boolean permissionsChecked;

    private final GroupTranslatorMap groupNamesByCode = new GroupTranslatorMap();
    private static final JSONSerializer PRIV_DEPENDENCIES_SERIALIZER = new JSONSerializer().include("*").exclude("*.class");

    public void init(ActionEvent event) {
        NodeRef tmpManageableRef = new NodeRef(ActionUtil.getParam(event, "manageableRef"));
        Collection<String> tmpManageablePermissions = Arrays.asList(StringUtils.split(ActionUtil.getParam(event, "manageablePermissions")));
        init(tmpManageableRef, tmpManageablePermissions, false);
    }

    @SuppressWarnings("hiding")
    private void init(NodeRef manageableRef, Collection<String> manageablePermissions, boolean reInit) {
        resetState(reInit);
        this.manageableRef = manageableRef;
        this.manageablePermissions = manageablePermissions;
        LOG.debug("Managing permissions of " + manageableRef);
        editable = isEditable();
        privMappings = getPrivilegeService().getPrivilegeMappings(manageableRef);
        markPrivilegesBaseState = true;
    }

    public boolean isEditable() {
        return new IsAdminOrDocManagerEvaluator().evaluate(manageableRef) || new IsOwnerEvaluator().evaluate(manageableRef);
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return !editable;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        isFinished = false;
        if (!editable) {
            MessageUtil.addErrorMessage("manage_permissions_save_error_noPermissions");
            return null;
        }
        Map<String, UserPrivileges> loosingPrivileges = getVosThatLoosePrivileges();
        if (validate(loosingPrivileges)) {
            try {
                getPrivilegeService().savePrivileges(manageableRef, privMappings.getPrivilegesByUsername(), dynamicPrivilegesGroups, DocumentCommonModel.Types.DOCUMENT);
                MessageUtil.addInfoMessage("save_success");
                init(manageableRef, manageablePermissions, true);
                List<String> users = new ArrayList<String>();
                for (UserPrivileges vo : loosingPrivileges.values()) {
                    if (vo.getPrivilegesToDelete().contains(DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA)
                                && !vo.getActivePrivileges().contains(DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA)) {
                        users.add(vo.getUserDisplayName());
                    }
                }
                if (!users.isEmpty()) {
                    String msgUsers = StringUtils.join(users, ", ");
                    msgUsers.substring(1, msgUsers.length() - 2);
                    MessageUtil.addStatusMessage(new MessageDataImpl(MessageSeverity.INFO, "manage_permissions_save_warning_lostEditDocMeta", msgUsers));
                }
            } catch (RuntimeException e) {
                LOG.error("Saving privileges failed for " + manageableRef, e);
                MessageUtil.addErrorMessage("save_failed", e.getMessage());
            }
            rebuildUserPrivilegesRows = true;
            picker.queueEvent(new UIGenericPicker.PickerEvent(picker, 1 /* ACTION_CLEAR */, 0, null, null));
        }
        return null;
    }

    private boolean validate(Map<String, UserPrivileges> loosingPrivileges) {
        if (removedWFPrivilege(loosingPrivileges)) {
            return false;
        }
        return true;
    }

    private boolean removedWFPrivilege(Map<String, UserPrivileges> loosingPrivileges) {
        if (loosingPrivileges.isEmpty()) {
            return false; // no privilege will be lost(if static is removed, then there is still dynamic privilege)
        }
        WorkflowService ws = BeanHelper.getWorkflowService();
        NodeRef docRef = manageableRef;
        Set<Task> tasks = ws.getTasks(docRef, new Predicate<Task>() {

            @Override
            public boolean evaluate(Task task) {
                if (!task.isStatus(Status.IN_PROGRESS)) {
                    return false;
                }
                if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
                    return true;
                }
                return false;
            }
        });
        if (tasks.isEmpty()) {
            return false;
        }
        FileService fileService = BeanHelper.getFileService();
        Map<String, Set<String>> missingPrivsByUser = new HashMap<String, Set<String>>();
        for (Task task : tasks) {
            String ownerId = task.getOwnerId();
            UserPrivileges userPrivileges = loosingPrivileges.get(ownerId);
            if (userPrivileges == null) {
                continue;
            }
            Set<String> requiredPrivileges = DocumentWorkflowStatusEventListener.getRequiredPrivsForInprogressTask(task, docRef, fileService);
            requiredPrivileges.removeAll(userPrivileges.getActivePrivileges());
            if (!requiredPrivileges.isEmpty()) {
                Set<String> missingPrivileges = missingPrivsByUser.get(userPrivileges.getUserName());
                if (missingPrivileges == null) {
                    missingPrivileges = requiredPrivileges;
                } else {
                    missingPrivileges.addAll(requiredPrivileges);
                }
                LOG.debug("User " + userPrivileges.getUserName() + " is missing required privileges '" + missingPrivileges + "' for task " + task.getNode().getNodeRef());
                missingPrivsByUser.put(userPrivileges.getUserName(), missingPrivileges);
            }

        }
        boolean removedWFPrivilege = !missingPrivsByUser.isEmpty();
        if (removedWFPrivilege) {
            List<MessageData> missingUserPrivilegeMessages = new ArrayList<MessageData>();
            for (Entry<String, Set<String>> entry : missingPrivsByUser.entrySet()) {
                String userName = entry.getKey();
                String userDisplayName = loosingPrivileges.get(userName).getUserDisplayName();
                List<String> missingPrivileges = new ArrayList<String>();
                for (String privilege : missingPrivsByUser.get(userName)) {
                    FacesContext context = FacesContext.getCurrentInstance();
                    missingPrivileges.add(MessageUtil.getMessage(context, "permission_" + privilege));
                }
                missingUserPrivilegeMessages.add(new MessageDataImpl("manage_permissions_save_error_removedWfPrivileges_missingUserPrivileges"
                        , userDisplayName, missingPrivileges));
            }
            MessageUtil.addErrorMessage("manage_permissions_save_error_removedWfPrivileges", missingUserPrivilegeMessages);
        }
        return removedWFPrivilege;
    }

    private Map<String, UserPrivileges> getVosThatLoosePrivileges() {
        Map<String /* userName */, UserPrivileges> vos = new HashMap<String, UserPrivileges>();
        for (UserPrivileges vo : privMappings.getPrivilegesByUsername().values()) {
            Set<String> privilegesToDelete = vo.getPrivilegesToDelete();
            if (!privilegesToDelete.isEmpty() && !vo.getActivePrivileges().containsAll(privilegesToDelete)) {
                vos.put(vo.getUserName(), vo); // at least one static privilege will be lost when saving
            }
        }
        return vos;
    }

    @Override
    public String cancel() {
        resetState(false);
        return super.cancel();
    }

    private void resetState(boolean reInit) {
        if (!reInit) {
            permissionsRichList = null;
            picker = null;
            permissionsChecked = false;
        }
        tableRowComparator = null;
        manageableRef = null;
        manageablePermissions = null;

        userPrivilegesRows = null;
        privMappings = null;
        editable = false;
    }

    public UIGenericPicker getPicker() {
        return picker;
    }

    public void setPicker(UIGenericPicker picker) {
        this.picker = picker;
    }

    public UIRichList getPermissionsRichList() {
        return permissionsRichList;
    }

    /**
     * @param permissionsRichList - partially preconfigured RichList from jsp
     */
    public void setPermissionsRichList(UIRichList permissionsRichList) {
        if (this.permissionsRichList == null) {
            ComponentUtil.putAttribute(permissionsRichList, DetailsViewRenderer.ATTR_GROUP_BY, "#{r.currentGroup}");
            ComponentUtil.putAttribute(permissionsRichList, DetailsViewRenderer.ATTR_ADDITIONAL_ROW_STYLE_BINDING, "#{r.additionalRowStyleClass}");

            ComponentUtil.putAttribute(permissionsRichList, DetailsViewRenderer.ATTR_GROUP_TBODY_ATTRIBUTES, new HashMap<String, Map<String, String>>());

            FacesContext context = FacesContext.getCurrentInstance();
            List<UIComponent> permissionRLChildren = ComponentUtil.getChildren(permissionsRichList);
            for (String permission : manageablePermissions) {
                boolean columnMaybeEditable = editable && !DocumentCommonModel.Privileges.READ_ONLY_PRIVILEGES.contains(permission);
                permissionRLChildren.add(createPermissionColumn(permission, context, columnMaybeEditable));
            }
            permissionRLChildren.add(createActionsColumn(context));
        }
        this.permissionsRichList = permissionsRichList;
    }

    /**
     * Action handler called when the Add button is pressed to process the current selection
     */
    public void addAuthorities(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent().findComponent("picker");
        String[] results = picker.getSelectedResults();
        if (results != null) {
            Set<String> userNames = new HashSet<String>();
            for (String authorityName : results) {
                addAuthorityUsers(userNames, authorityName, GROUPLESS_GROUP);
                rebuildUserPrivilegesRows = true;
            }
        }
    }

    /** @param event passed to MethodBinding */
    public void deleteGroup(ActionEvent event) {
        String groupName = ActionUtil.getParam(event, PARAM_CURRENT_GROUP);
        Set<String> members = new HashSet<String>(privMappings.getMembersByGroup(groupName));// creating copy to avoid ConcurrentModificationEx on membersByGroup
        for (String userName : members) {
            deletePerson(userName);
        }
        rebuildUserPrivilegesRows = true;
    }

    /** @param event passed to MethodBinding */
    public void deletePerson(ActionEvent event) {
        String userName = ActionUtil.getParam(event, "userName");
        deletePerson(userName);
    }

    /**
     * Try to delete person from all groups. Person can't be deleted from privileges list while it still has privileges, hence person can't be deleted from:<br>
     * 1) dynamicPrivilegesGroups<br>
     * 2) groupless group if user is document owner
     * 
     * @param userName
     */
    private void deletePerson(String userName) {
        UserPrivileges curUserVO = privMappings.getPrivilegesByUsername().get(userName);
        curUserVO.setDeleted(true);

        { // remove user from groups where it belonged to
            for (Entry<String, Set<String>> entry : privMappings.getMembersByGroups().entrySet()) {
                String groupCode = entry.getKey();
                if (!canBeRemovedFromGroup(curUserVO, groupCode)) {
                    continue; // user has special privileges and should be left to group where groupless users are
                }
                Set<String> members = entry.getValue();
                for (Iterator<String> it = members.iterator(); it.hasNext();) {
                    String member = it.next();
                    if (StringUtils.equals(userName, member)) {
                        it.remove();
                        break;
                    }
                }
                curUserVO.getGroups().remove(groupCode); // avoid reappearing in a groups when user is again added before saving
            }
        }

        curUserVO.deletePrivileges(manageablePermissions);

        // remove table rows of the user that is being removed
        for (Iterator<UserPrivilegesRow> it = userPrivilegesRows.iterator(); it.hasNext();) {
            UserPrivilegesRow row = it.next();
            if (StringUtils.equals(userName, row.getUserName())) {
                it.remove();
            }
        }
    }

    private boolean canBeRemovedFromGroup(UserPrivileges curUserPrivileges, String groupCode) {
        if (curUserPrivileges.isReadOnly()) {
            if (dynamicPrivilegesGroups.contains(groupCode)) {
                return false;
            }
            Set<String> curUserGroups = curUserPrivileges.getGroups();
            if (!curUserGroups.contains(groupCode)) {
                return false; // user doesn't even belong to this group
            }
            if (groupCode == GROUPLESS_GROUP && StringUtils.equals(getDocumentOwner(), curUserPrivileges.getUserName())) {
                return false;
            }
        }
        return true;
    }

    public String getPrivilegeDependencies() {
        return PRIV_DEPENDENCIES_SERIALIZER.serialize(DocumentCommonModel.Privileges.PRIVILEGE_DEPENDENCIES);
    }

    /** @return permissions table for JSF value binding */
    public Collection<UserPrivilegesRow> getUserPrivilegesRows() {
        if (userPrivilegesRows != null && !rebuildUserPrivilegesRows) {
            return userPrivilegesRows;
        }
        userPrivilegesRows = new ArrayList<UserPrivilegesRow>();
        rebuildUserPrivilegesRows = false;
        for (Entry<String/* group */, Set<String> /* members */> entry : privMappings.getMembersByGroups().entrySet()) {
            String group = entry.getKey();
            addTbodyAttributesByGroup(group);
            for (String member : entry.getValue()) {
                UserPrivileges vo = privMappings.getOrCreateUserPrivilegesVO(member);
                getOrCreateUserPrivilegesRow(vo, group);
            }
        }
        addDynamicUserPrivilegesRows();
        if (markPrivilegesBaseState) {
            for (UserPrivileges row : privMappings.getPrivilegesByUsername().values()) {
                row.markBaseState();
            }
            markPrivilegesBaseState = false;
        }
        Collections.sort(userPrivilegesRows, getTableRowComparator());
        validatePermissions();
        return userPrivilegesRows;
    }

    private void validatePermissions() {
        if (!permissionsChecked && BeanHelper.getApplicationService().isTest()) {
            final ArrayList<String> msgs = new ArrayList<String>();
            for (final UserPrivileges row : privMappings.getPrivilegesByUsername().values()) {
                final String userName = row.getUserName();
                AuthenticationUtil.runAs(new RunAsWork<Object>() {
                    @Override
                    public Object doWork() throws Exception {
                        for (Entry<String, Boolean> entry : row.getPrivileges().entrySet()) {
                            String privilege = entry.getKey();
                            if (entry.getValue()) {
                                AccessStatus hasPermission = getPermissionService().hasPermission(manageableRef, privilege);
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
                String msg = msgs.size() + " privileges are missing on node " + manageableRef + ":\n" + StringUtils.join(msgs, "\n");
                LOG.error(msg);
                throw new RuntimeException(msg);
            }
            permissionsChecked = true;
        }
    }

    private Comparator<? super UserPrivilegesRow> getTableRowComparator() {
        if (tableRowComparator == null) {
            Object[] groupOrderHigh = new Object[] { groupNamesByCode.get(GROUPLESS_GROUP) };
            FixedOrderComparator grouplessFirstComp = new FixedOrderComparator(groupOrderHigh);
            grouplessFirstComp.setUnknownObjectBehavior(FixedOrderComparator.UNKNOWN_AFTER);

            List<String> groupOrderLow = Arrays.asList(groupNamesByCode.get(UserService.AUTH_DOCUMENT_MANAGERS_GROUP), groupNamesByCode.get(UserService.AUTH_ADMINISTRATORS_GROUP),
                    groupNamesByCode.get(CUR_SER_DOC_MANAGERS_GROUP_CODE));
            Collections.sort(groupOrderLow);
            FixedOrderComparator specialGroupsLastComp = new FixedOrderComparator(groupOrderLow);
            specialGroupsLastComp.setUnknownObjectBehavior(FixedOrderComparator.UNKNOWN_BEFORE);

            Transformer<UserPrivilegesRow> privGroupTransformer = new Transformer<UserPrivilegesRow>() {
                @Override
                public Object tr(UserPrivilegesRow input) {
                    return groupNamesByCode.get(input.getCurrentGroup());
                }
            };

            ComparatorChain chain = new ComparatorChain();
            // rows order by groups: groupless users, regular usergroups, special usergroups
            chain.addComparator(new TransformingComparator(privGroupTransformer, new NullComparator(grouplessFirstComp)));
            chain.addComparator(new TransformingComparator(privGroupTransformer, new NullComparator(specialGroupsLastComp)));
            chain.addComparator(new TransformingComparator(privGroupTransformer, new NullComparator(false)));
            // withing same group sort users by display name
            chain.addComparator(new TransformingComparator(new Transformer<UserPrivilegesRow>() {
                @Override
                public Object tr(UserPrivilegesRow input) {
                    return input.getUserDisplayName();
                }
            }, new NullComparator()));
            tableRowComparator = chain;
        }
        return tableRowComparator;
    }

    private void addDynamicUserPrivilegesRows() {
        boolean immediate = true;
        { // add rows for administrators group members
            String group = UserService.AUTH_ADMINISTRATORS_GROUP;
            Set<String> authorities = getAuthorityService().getContainedAuthorities(AuthorityType.USER, group, immediate);
            LOG.debug("authorities=" + authorities);
            String extraPrivilegeReason = MessageUtil.getMessage("manage_permissions_extraInfo_userIsAdmin");
            addRows(group, manageablePermissions, authorities, extraPrivilegeReason);
        }

        { // add rows for document managers group members
            String group = UserService.AUTH_DOCUMENT_MANAGERS_GROUP;
            Set<String> authorities = getAuthorityService().getContainedAuthorities(AuthorityType.USER, group, immediate);
            Set<String> seriesDocManagerAuths = filterSeriesAuthorities(authorities, manageableRef);
            String groupPreffix = "curSeries_";
            String extraPrivilegeReason = MessageUtil.getMessage("manage_permissions_extraInfo_userIsDocManagerOfCurSeries");
            addRows(groupPreffix + group, manageablePermissions, seriesDocManagerAuths, extraPrivilegeReason);

            Collection<String> privileges = Arrays.asList(DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA
                    , DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA, DocumentCommonModel.Privileges.DELETE_DOCUMENT_META_DATA);
            extraPrivilegeReason = MessageUtil.getMessage("manage_permissions_extraInfo_userIsDocManager");
            Set<String> docManagerAuthorities = new HashSet<String>(authorities);
            docManagerAuthorities.removeAll(seriesDocManagerAuths);
            addRows(group, privileges, docManagerAuthorities, extraPrivilegeReason);
        }

        { // add all privileges to document owner(and make row readOnly)
            String ownerId = getDocumentOwner();
            UserPrivilegesRow ownerRow = null;
            for (UserPrivilegesRow userPrivilegesRow : userPrivilegesRows) {
                if (userPrivilegesRow.getUserName().equals(ownerId)) {
                    ownerRow = userPrivilegesRow;
                    break;
                }
            }
            String extraPrivilegeReason = MessageUtil.getMessage("manage_permissions_extraInfo_userIsDocOwner");
            if (ownerRow == null) {
                // this might happen when rebuilding UserPrivilegesRows after removing all members of group
                ownerRow = addAuthorityRow(ownerId, manageablePermissions, GROUPLESS_GROUP, extraPrivilegeReason);
                privMappings.getMembersByGroup(GROUPLESS_GROUP).add(ownerId);// add member to groupless users "group"
            } else {
                for (String permission : manageablePermissions) {
                    // if (!ownerRow.getPrivileges().containsKey(permission)) {
                    ownerRow.addDynamicPrivilege(permission, extraPrivilegeReason);
                    // }
                }
            }
            ownerRow.setReadOnly(true);
        }

        { // when document is public, then everybody implicit rights to viewDocumentMetaData and viewDocumentFiles
          // (but showing those privileges only on rows already added because of some other reason)
            String accessRestriction = (String) getNodeService().getProperty(manageableRef, DocumentCommonModel.Props.ACCESS_RESTRICTION);
            if (StringUtils.equals(accessRestriction, AccessRestriction.OPEN.getValueName())) {
                String docIsPublic = MessageUtil.getMessage("manage_permissions_extraInfo_documentIsPublic");
                for (UserPrivilegesRow row : userPrivilegesRows) {
                    row.addDynamicPrivilege(DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA, docIsPublic);
                    row.addDynamicPrivilege(DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES, docIsPublic);
                }
            }
        }
    }

    /**
     * @return usernames of those authorities that are given and also have SeriesDocManagerDynamicAuthority.SERIES_MANAGEABLE_PERMISSION permission for series of given document
     */
    public Set<String> filterSeriesAuthorities(Set<String> authorities, NodeRef seriesOrDecendantOfSeriesRef) {
        final NodeRef seriesRef;
        if (SeriesModel.Types.SERIES.equals(getNodeService().getType(seriesOrDecendantOfSeriesRef))) {
            seriesRef = seriesOrDecendantOfSeriesRef;
        } else {
            seriesRef = getGeneralService().getAncestorNodeRefWithType(seriesOrDecendantOfSeriesRef, SeriesModel.Types.SERIES);
        }
        Set<AccessPermission> seriesPermissions = getPermissionService().getAllSetPermissions(seriesRef);
        Set<String> seriesAuths = new HashSet<String>();
        for (AccessPermission seriesPermission : seriesPermissions) {
            if (SeriesDocManagerDynamicAuthority.SERIES_MANAGEABLE_PERMISSION.equals(seriesPermission.getPermission())
                    && AccessStatus.ALLOWED.equals(seriesPermission.getAccessStatus()) && authorities.contains(seriesPermission.getAuthority())) {
                seriesAuths.add(seriesPermission.getAuthority());
            }
        }
        return seriesAuths;
    }

    private String getDocumentOwner() {
        return (String) getNodeService().getProperty(manageableRef, DocumentCommonModel.Props.OWNER_ID);
    }

    private void addRows(String group, Collection<String> privileges, Set<String> authorities, String extraPrivilegeReason) {
        for (String authority : authorities) {
            addAuthorityRow(authority, privileges, group, extraPrivilegeReason);
        }
    }

    private UserPrivilegesRow addAuthorityRow(String authority, Collection<String> privileges, String group, String extraPrivilegeReason) {
        UserPrivileges vo = privMappings.getOrCreateUserPrivilegesVO(authority);
        vo.setReadOnly(true);
        vo.addGroup(group);
        if (extraPrivilegeReason != null) {
            for (String privilege : privileges) {
                vo.addDynamicPrivilege(privilege, extraPrivilegeReason);
            }
        } else {
            vo.addPrivileges(privileges);
        }
        UserPrivilegesRow privilegeTableRow = getOrCreateUserPrivilegesRow(vo, group);
        addTbodyAttributesByGroup(group);
        return privilegeTableRow;
    }

    private UserPrivilegesRow getOrCreateUserPrivilegesRow(UserPrivileges vo, String group) {
        UserPrivilegesRow privilegeTableRow = new UserPrivilegesRow(vo);
        privilegeTableRow.setCurrentGroup(group);
        int indexOf = userPrivilegesRows.indexOf(privilegeTableRow);
        if (indexOf >= 0) {
            privilegeTableRow = userPrivilegesRows.get(indexOf);
        } else {
            userPrivilegesRows.add(privilegeTableRow);
        }
        return privilegeTableRow;
    }

    class GroupTranslatorMap extends HashMap<String, String> {
        private static final long serialVersionUID = 1L;

        @Override
        public String get(Object key) {
            String groupCode = (String) key;
            String value = super.get(groupCode);
            if (value == null) {
                if (GROUPLESS_GROUP.equals(key)) {
                    value = MessageUtil.getMessage("manage_permissions_group_groupless");
                } else {
                    if (CUR_SER_DOC_MANAGERS_GROUP_CODE.equals(groupCode)) {
                        value = MessageUtil.getMessage(groupCode);
                    } else {
                        value = getAuthorityService().getAuthorityDisplayName(groupCode);
                    }
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
        Map<String/* groupCode */, Map<String/* attributeName */, String/* attributeValue */>> tbodyAttributesByGroup = (Map<String, Map<String, String>>) ComponentUtil
                .getAttribute(permissionsRichList, DetailsViewRenderer.ATTR_GROUP_TBODY_ATTRIBUTES);
        tbodyAttributesByGroup.put(groupCode, tbodyAttributes);
        String groupDisplayName = groupNamesByCode.get(groupCode);
        UITableRow tr = (UITableRow) permissionsRichList.getFacet(groupCode);
        if (tr == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            Application application = context.getApplication();
            tr = new UITableRow();
            putAttribute(tr, "styleClass", "grHeader");
            // "permission_" + privilege

            UITableCell tableCell = new UITableCell();
            putAttribute(tableCell, "styleClass", GROUPLESS_GROUP.equals(groupCode) ? "expanded" : "collapsed");
            addChildren(tr, tableCell);
            {
                UITableCell uiTableCell = new UITableCell();
                putAttribute(uiTableCell, "styleClass", "left");
                HtmlOutputText headerText = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
                headerText.setValue(groupDisplayName);
                addChildren(uiTableCell, headerText);
                addChildren(tr, uiTableCell);
            }

            for (String permission : manageablePermissions) {
                tableCell = new UITableCell();
                HtmlSelectBooleanCheckbox cb = (HtmlSelectBooleanCheckbox) application.createComponent(HtmlSelectBooleanCheckbox.COMPONENT_TYPE);
                boolean columnMaybeEditable = editable && !DocumentCommonModel.Privileges.READ_ONLY_PRIVILEGES.contains(permission);
                cb.setDisabled(!columnMaybeEditable);
                cb.setStyleClass("permission_" + permission);
                addChildren(tableCell, cb);
                addChildren(tr, tableCell);
            }

            tableCell = new UITableCell();
            if (editable && !dynamicPrivilegesGroups.contains(groupCode)) {
                UIActionLink removeGroupLink = createRemoveLink(application, false);
                addChildren(removeGroupLink, createUIParam(PARAM_CURRENT_GROUP, "#{r.currentGroup}", application));
                addChildren(tableCell, removeGroupLink);
            }
            addChildren(tr, tableCell);
            ComponentUtil.addFacet(permissionsRichList, groupCode, tr);
        }
    }

    /** Used for JSF binding */
    public Map<String, UserPrivileges> getPrivilegesByUsername() {
        return privMappings.getPrivilegesByUsername();
    }

    private void addAuthorityUsers(Set<String> userNames, String authorityName, String group) {
        Authority authority = getUserService().getAuthorityOrNull(authorityName);
        LOG.debug("ADDING AUTHORITY " + ReflectionToStringBuilder.reflectionToString(authority,
                ToStringStyle.MULTI_LINE_STYLE));
        if (authority == null) {
            throw new RuntimeException("Didn't find authority based on '" + authorityName + "'");
        }
        if (authority.isGroup()) {
            Set<String> containedAuthorities = getAuthorityService().getContainedAuthorities(AuthorityType.USER, authorityName, true);

            for (String containedAuthority : containedAuthorities) {
                addAuthorityUsers(userNames, containedAuthority, authorityName);
            }
        } else { // authority is user
            UserPrivileges userPrivileges = privMappings.getOrCreateUserPrivilegesVO(authorityName);
            userPrivileges.setDeleted(false);// maybe existed and was deleted before saving
            Set<String> curGroupMembers = privMappings.getMembersByGroup(group);
            curGroupMembers.add(authorityName);
            userPrivileges.addGroup(group);

        }
    }

    private UIColumn createActionsColumn(FacesContext context) {
        Application application = context.getApplication();
        UIColumn column = (UIColumn) application.createComponent("org.alfresco.faces.RichListColumn");

        if (editable) {
            UIActionLink removePersonLink = createRemoveLink(application, true);
            UIComponentTagUtils.setValueBinding(context, removePersonLink, "rendered", "#{!r.readOnly}");
            addChildren(removePersonLink, createUIParam("userName", "#{r.userName}", application));

            HtmlOutputText nbsp = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
            nbsp.setValue(StringEscapeUtils.unescapeHtml("&nbsp;")); // workaround for IE7 - otherwise if removePersonLink is not rendered, then underline is not rendered either

            addChildren(column, removePersonLink, nbsp);
        }
        return column;
    }

    private UIActionLink createRemoveLink(Application application, boolean isPerson) {
        UIActionLink removeLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        removeLink.setValue("");
        removeLink.setShowLink(false);
        removeLink.setImage("/images/icons/" + (isPerson ? "remove_user.gif" : "delete_group.gif"));
        removeLink.setTooltip(MessageUtil.getMessage(isPerson ? "manage_permissions_deletePerson" : "manage_permissions_deleteGroupMembers"));
        removeLink.setActionListener(application.createMethodBinding(
                "#{" + BEAN_NAME + (isPerson ? ".deletePerson}" : ".deleteGroup}"), new Class[] { javax.faces.event.ActionEvent.class }));
        ComponentUtil.putAttribute(removeLink, "styleClass", isPerson ? "deletePerson" : "deleteGroup");
        return removeLink;
    }

    private UIColumn createPermissionColumn(String privilege, FacesContext context, boolean columnMaybeEditable) {
        Application application = context.getApplication();
        UIColumn column = (UIColumn) application.createComponent("org.alfresco.faces.RichListColumn");

        HtmlOutputText headerText = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
        headerText.setValue(MessageUtil.getMessage("permission_" + privilege));
        ComponentUtil.addFacet(column, "header", headerText);

        HtmlSelectBooleanCheckbox cb = (HtmlSelectBooleanCheckbox) application.createComponent(HtmlSelectBooleanCheckbox.COMPONENT_TYPE);
        UIComponentTagUtils.setValueProperty(context, cb, "#{" + BEAN_NAME + ".privilegesByUsername[r.userName].privileges['" + privilege + "']}");
        if (columnMaybeEditable) {
            UIComponentTagUtils.setValueBinding(context, cb, "disabled", "#{r.dynamicPrivileges['" + privilege + "']==false}");
        } else {
            cb.setDisabled(true);
        }
        UIComponentTagUtils.setValueBinding(context, cb, "title", "#{r.dynamicPrivReasons['" + privilege + "']}");
        UIComponentTagUtils.setValueBinding(context, cb, "styleClass", "userId_#{r.userName} tooltip permission_" + privilege);

        addChildren(column, cb);
        return column;
    }

    private GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = BeanHelper.getGeneralService();
        }
        return generalService;
    }

    private PermissionService getPermissionService() {
        if (permissionService == null) {
            permissionService = BeanHelper.getPermissionService();
        }
        return permissionService;
    }

    private PrivilegeService getPrivilegeService() {
        if (privilegeService == null) {
            privilegeService = BeanHelper.getPrivilegeService();
        }
        return privilegeService;
    }

    private UserService getUserService() {
        if (userService == null) {
            userService = BeanHelper.getUserService();
        }
        return userService;
    }

    private AuthorityService getAuthorityService() {
        if (authorityService == null) {
            authorityService = BeanHelper.getAuthorityService();
        }
        return authorityService;
    }

    // /// PRIVATE METHODS /////

    // START: getters / setters

    // END: getters / setters
    public static class UserPrivilegesRow implements Comparable<UserPrivilegesRow>, Serializable {
        private static final long serialVersionUID = 1L;

        private String currentGroup;
        private final UserPrivileges wrapped;

        public UserPrivilegesRow(UserPrivileges vo) {
            wrapped = vo;
        }

        // START: getters / setters
        /** used by JSF value binding */
        public Map<String, Boolean> getPrivileges() {
            return wrapped.getPrivileges();
        }

        /** used by JSF value binding */
        public Map<String, String> getDynamicPrivReasons() {
            return wrapped.getDynamicPrivReasons();
        }

        /** used by JSF value binding */
        public Map<String, Boolean> getDynamicPrivileges() {
            return wrapped.getDynamicPrivileges();
        }

        /** used by JSF value binding */
        public boolean isReadOnly() {
            return wrapped.isReadOnly();
        }

        public void setReadOnly(boolean readOnly) {
            wrapped.setReadOnly(readOnly);
        }

        public String getUserDisplayName() {
            return wrapped.getUserDisplayName();
        }

        public String getUserName() {
            return wrapped.getUserName();
        }

        public void addDynamicPrivilege(String privilege, String reason) {
            wrapped.addDynamicPrivilege(privilege, reason);
        }

        public void setCurrentGroup(String currentGroup) {
            this.currentGroup = currentGroup;
        }

        /** used by JSF value binding */
        public String getCurrentGroup() {
            return currentGroup;
        }

        /** used by JSF value binding */
        public String getAdditionalRowStyleClass() {
            return "userId_" + getUserName();
        }

        // END: getters / setters

        @Override
        public int compareTo(UserPrivilegesRow o) {
            if (currentGroup == null && o.getCurrentGroup() != null) {
                return -1;
            }
            if (o.getCurrentGroup() == null && currentGroup != null) {
                return 1;
            }
            int groupsRes;
            if (currentGroup == null && o.getCurrentGroup() == null) {
                groupsRes = 0;
            } else {
                groupsRes = currentGroup.compareTo(o.getCurrentGroup());
            }
            if (groupsRes == 0) {
                return getUserName().compareTo(o.getUserName());
            }
            return groupsRes;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((currentGroup == null) ? 0 : currentGroup.hashCode());
            result = prime * result + ((getUserName() == null) ? 0 : getUserName().hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            UserPrivilegesRow other = (UserPrivilegesRow) obj;
            if (currentGroup == null) {
                if (other.currentGroup != null) {
                    return false;
                }
            } else if (!currentGroup.equals(other.currentGroup)) {
                return false;
            }
            if (getUserName() == null) {
                if (other.getUserName() != null) {
                    return false;
                }
            } else if (!getUserName().equals(other.getUserName())) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "currentGroup=" + currentGroup + " " + wrapped.toString();
        }

    }
}
