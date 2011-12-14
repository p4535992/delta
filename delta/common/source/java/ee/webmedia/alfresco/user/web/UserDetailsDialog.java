package ee.webmedia.alfresco.user.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAuthorityService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
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
import javax.faces.event.ValueChangeEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.users.UsersBeanProperties;
import org.alfresco.web.bean.users.UsersDialog;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.web.SubstituteListDialog;
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

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        substituteListDialog = new SubstituteListDialog();
        substituteListDialog.setUserNodeRef(user.getNodeRef());
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

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            substituteListDialog.save();
            BeanHelper.getUserService().updateUser(user);
            setupUser((String) user.getProperties().get(ContentModel.PROP_USERNAME.toString()));
        }
        isFinished = false;
        return null;
    }

    private boolean validate() {
        List<String> erroneousValues = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        List<String> relatedFundCenters = (List<String>) user.getProperties().get(ContentModel.PROP_RELATED_FUNDS_CENTER.toString());
        if (relatedFundCenters != null) {
            EInvoiceService eInvoiceService = BeanHelper.getEInvoiceService();
            NodeRef dimensionRef = eInvoiceService.getDimension(Dimensions.INVOICE_FUNDS_CENTERS);
            for (String dimensionName : relatedFundCenters) {
                if (StringUtils.isNotBlank(dimensionName)) {
                    DimensionValue dimensionValue = eInvoiceService.getDimensionValue(dimensionRef, dimensionName);
                    if (dimensionValue == null) {
                        erroneousValues.add(dimensionName);
                    }
                }
            }
        }
        if (!erroneousValues.isEmpty()) {
            MessageUtil.addErrorMessage("user_relatedFundsCenter_no_existing_value", MessageUtil.getMessage("user_relatedFundsCenter"),
                    TextUtil.joinNonBlankStringsWithComma(erroneousValues));
            return false;
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

    public void showEmptyTaskMenuChanged(ValueChangeEvent e) {
        user.getProperties().put(ContentModel.SHOW_EMPTY_TASK_MENU.toString(), DefaultTypeConverter.INSTANCE.convert(Boolean.class, e.getNewValue()));
    }

    public boolean isShowEmptyTaskMenuEditable() {
        return BeanHelper.getUserService().isAdministrator() || user.getProperties().get(ContentModel.PROP_USERNAME.toString()).equals(AuthenticationUtil.getRunAsUser());
    }

    public boolean isRelatedFundsCenterNotEditable() {
        return !isRelatedFundsCenterEditable();
    }

    public boolean isRelatedFundsCenterEditable() {
        return BeanHelper.getUserService().isAdministrator();
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
        Node node = new Node(BeanHelper.getUserService().getPerson(AuthenticationUtil.getRunAsUser()));
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
        Map<String, Object> props = user.getProperties();
        EInvoiceService eInvoiceService = BeanHelper.getEInvoiceService();
        NodeRef dimensionRef = eInvoiceService.getDimension(Dimensions.INVOICE_FUNDS_CENTERS);
        @SuppressWarnings("unchecked")
        List<String> relatedFundsCenters = (List<String>) user.getProperties().get(ContentModel.PROP_RELATED_FUNDS_CENTER);
        if (relatedFundsCenters == null || relatedFundsCenters.isEmpty()) {
            relatedFundsCenters = eInvoiceService.getDimensionDefaultValueList(Dimensions.INVOICE_FUNDS_CENTERS, null);
        }
        props.put("{temp}unit", getUserDisplayUnit(props));
        props.put("{temp}jobAddress", TextUtil.joinNonBlankStringsWithComma(Arrays.asList((String) props.get(ContentModel.PROP_STREET_HOUSE),
                (String) props.get(ContentModel.PROP_VILLAGE), (String) props.get(ContentModel.PROP_MUNICIPALITY), (String) props.get(ContentModel.PROP_POSTAL_CODE),
                (String) props.get(ContentModel.PROP_COUNTY))));
        props.put(ContentModel.PROP_RELATED_FUNDS_CENTER.toString(), relatedFundsCenters);

        StringBuilder sb = new StringBuilder("");

        int dimensionIndex = 0;
        for (String dimensionName : relatedFundsCenters) {
            if (StringUtils.isNotBlank(dimensionName)) {
                if (dimensionIndex > 0) {
                    sb.append(", ");
                }
                DimensionValue dimensionValue = eInvoiceService.getDimensionValue(dimensionRef, dimensionName);
                sb.append("<span title=\"");
                sb.append(org.alfresco.web.ui.common.StringUtils.encode(TextUtil.joinStringAndStringWithSeparator(dimensionValue.getValue(),
                            dimensionValue.getValueComment(), ";")));
                sb.append("\" class=\"tooltip\">");
                sb.append(org.alfresco.web.ui.common.StringUtils.encode(dimensionValue.getValueName())).append("</span>");
                dimensionIndex++;
            }
        }
        user.getProperties().put("{temp}relatedFundsCenter", sb.toString());

        Boolean showEmpty = (Boolean) user.getProperties().get(ContentModel.SHOW_EMPTY_TASK_MENU.toString());
        if (showEmpty == null) {
            user.getProperties().put(ContentModel.SHOW_EMPTY_TASK_MENU.toString(), false);
            showEmpty = false;
        }
        String emptyTaskMenuString = MessageUtil.getMessage(showEmpty ? "yes" : "no");
        user.getProperties().put("{temp}" + ContentModel.SHOW_EMPTY_TASK_MENU.getLocalName(), emptyTaskMenuString);
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
        BeanHelper.getAssignResponsibilityBean().updateLiabilityGivenToPerson(new Node(BeanHelper.getUserService().getPerson(userName)));
    }

    /**
     * Used in JSP to set up person context
     * 
     * @param userName
     */
    public void setupUser(String userName) {
        List<Node> users = new ArrayList<Node>(1);
        users.add(new Node(BeanHelper.getUserService().getPerson(userName)));
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

    // ///

}
