package ee.webmedia.alfresco.user.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.users.UsersBeanProperties;
import org.alfresco.web.bean.users.UsersDialog;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.web.SubstituteListDialog;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;

public class UserDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "UserDetailsDialog";

    private transient UserService userService;
    private transient OrganizationStructureService organizationStructureService;
    private UsersBeanProperties properties;

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
        if (validate()) {
            substituteListDialog.save(context);
            BeanHelper.getUserService().updateUser(user);
            setupUser((String) user.getProperties().get(ContentModel.PROP_USERNAME));
        }
        isFinished = false;
        return null;
    }

    private boolean validate() {
        List<String> erroneousValues = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        List<String> relatedFundCenters = (List<String>) user.getProperties().get(ContentModel.PROP_RELATED_FUNDS_CENTER);
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
    }

    private void fillUserProps(List<Node> users) {
        user = getOrganizationStructureService().setUsersUnit(users).get(0);
        EInvoiceService eInvoiceService = BeanHelper.getEInvoiceService();
        NodeRef dimensionRef = eInvoiceService.getDimension(Dimensions.INVOICE_FUNDS_CENTERS);
        @SuppressWarnings("unchecked")
        List<String> relatedFundsCenters = (List<String>) user.getProperties().get(ContentModel.PROP_RELATED_FUNDS_CENTER);
        if (relatedFundsCenters == null || relatedFundsCenters.isEmpty()) {
            relatedFundsCenters = eInvoiceService.getDimensionDefaultValueList(Dimensions.INVOICE_FUNDS_CENTERS, null);
        }
        user.getProperties().put(ContentModel.PROP_RELATED_FUNDS_CENTER.toString(), relatedFundsCenters);
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
        List<Node> users = new ArrayList<Node>(1);
        users.add(new Node(properties.getPersonService().getPerson(userName)));
        fillUserProps(users);
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

}
