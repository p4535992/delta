package ee.webmedia.alfresco.menu.model;

import static org.apache.commons.lang.StringUtils.startsWith;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;

import org.alfresco.config.Config;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.config.ActionsConfigElement;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.evaluator.ActionInstanceEvaluator;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.menu.ui.component.MenuItemWrapper;
import ee.webmedia.alfresco.menu.ui.component.MenuRenderer;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent.ClearViewStackActionListener;
import ee.webmedia.alfresco.orgstructure.amr.service.RSService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;

/**
 * Base class for menu items.
 */
@XStreamAlias("item")
public class MenuItem implements Serializable {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MenuItem.class);

    final static public String HIDDEN_MENU_ITEM = "hiddenMenuItem";
    public static final String ASSIGNMENT_TASKS = "assignmentTasks";
    public static final String CONFIRMATION_TASKS = "confirmationTasks";
    public static final String DVK_CORRUPT = "dvkCorrupt";
    public static final String DVK_DOCUMENTS = "dvkDocuments";
    public static final String DISCUSSIONS = "discussions";
    public static final String EMAIL_ATTACHMENTS = "emailAttachments";
    public static final String EXTERNAL_REVIEW_TASKS = "externalReviewTasks";
    public static final String FOR_REGISTERING_LIST = "forRegisteringList";
    public static final String INCOMING_EINVOICE = "incomingEInvoice";
    public static final String INCOMING_EMAILS = "incomingEmails";
    public static final String INFORMATION_TASKS = "informationTasks";
    public static final String OPINION_TASKS = "opinionTasks";
    public static final String ORDER_ASSIGNMENT_TASKS = "orderAssignmentTasks";
    public static final String OUTBOX_DOCUMENT = "outboxDocument";
    public static final String REVIEW_TASKS = "reviewTasks";
    public static final String SCANNED_DOCUMENTS = "scannedDocuments";
    public static final String SENT_EMAILS = "sentEmails";
    public static final String SEND_FAILURE_NOTIFICATION = "sendFailureNotification";
    public static final String SIGNATURE_TASKS = "signatureTasks";
    public static final String UNSENT_DOCUMENT = "unsentDocument";
    public static final String USER_CASE_FILES = "userCaseFiles";
    public static final String USER_COMPOUND_WORKFLOWS = "userCompoundWorkflows";
    public static final String USER_WORKING_DOCUMENTS = "userWorkingDocuments";
    public static final String WEB_SERVICE_DOCUMENTS = "webServiceDocuments";

    public static final List<String> MY_TASK_MENU_ITEMS = Arrays.asList(ASSIGNMENT_TASKS, INFORMATION_TASKS, ORDER_ASSIGNMENT_TASKS, OPINION_TASKS, DISCUSSIONS,
            REVIEW_TASKS, EXTERNAL_REVIEW_TASKS, CONFIRMATION_TASKS, SIGNATURE_TASKS, FOR_REGISTERING_LIST);

    @XStreamOmitField
    private static final long serialVersionUID = 0L;
    @XStreamAsAttribute
    private String id;
    @XStreamAsAttribute
    private String title;
    @XStreamAsAttribute
    @XStreamAlias("title-id")
    private String titleId;
    @XStreamAsAttribute
    private boolean admin;
    @XStreamAsAttribute
    @XStreamAlias("document-manager")
    private boolean docManager;
    @XStreamAsAttribute
    private boolean accountant;
    @XStreamAsAttribute
    private boolean supervisor;
    @XStreamAsAttribute
    private boolean archivist;
    private String outcome;
    @XStreamAlias("subitems")
    private List<MenuItem> subItems;
    @XStreamAlias("action-listener")
    private String actionListener;
    @XStreamAsAttribute
    private String href;
    @XStreamAsAttribute
    private String target;
    @XStreamOmitField
    private Map<String, String> params;
    private String processor;
    @XStreamImplicit(itemFieldName = "styleClass")
    private List<String> styleClass;
    @XStreamOmitField
    private boolean renderingDisabled;
    private String hidden;

    @XStreamOmitField
    private static final String ACTION_CONTEXT = "actionContext";
    @XStreamOmitField
    private static final String ATTR_VALUE = "value";
    @XStreamOmitField
    public static final String ATTR_PLAIN_MENU_ITEM = "plainMenuItem";

    public MenuItem() {
    }

    public UIComponent createComponent(FacesContext context, String id, UserService userService, WorkflowConstantsBean workflowConstantsBean, RSService rsService) {
        return createComponent(context, id, false, userService, workflowConstantsBean, rsService, false);
    }

    public UIComponent createComponent(FacesContext context, String id, UserService userService, WorkflowConstantsBean workflowConstantsBean,
            RSService rsService, boolean createChildren) {
        return createComponent(context, id, false, userService, workflowConstantsBean, rsService, false);
    }

    /**
     * Return ActionLink, based on xml configuration. Returns null, if user doesn't have permissions.
     *
     * @param context
     * @param application Faces Application
     * @return
     */
    public UIComponent createComponent(FacesContext context, String id, boolean active, UserService userService, WorkflowConstantsBean workflowConstantsBean,
            RSService rsService, boolean plainLink) {

        if (!isRendered(userService, workflowConstantsBean, rsService)) {
            return null;
        }

        javax.faces.application.Application application = context.getApplication();
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);

        UIComponent component = null;
        ResourceBundle messages = Application.getBundle(context);

        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        FacesHelper.setupComponentId(context, link, id);
        link.addActionListener(new ClearViewStackActionListener());

        if (title == null) {
            setTitle(MessageUtil.getMessage(getTitleId()));
        } else if (StringUtils.startsWith(title, "#{")) {
            ValueBinding vb = application.createValueBinding(title);
            if (vb != null) {
                setTitle((String) vb.getValue(context));
            }
        }
        link.setValue(getTitle(startsWith(id, MenuRenderer.SECONDARY_MENU_PREFIX) || startsWith(id, MenuBean.SHORTCUT_MENU_ITEM_PREFIX)));

        link.setTooltip(getTitle());
        String outcome2 = getOutcome();
        if (StringUtils.isNotBlank(href)) {
            if (StringUtils.startsWith(href, "#{")) {
                ValueBinding vb = application.createValueBinding(href);
                if (vb != null) {
                    setHref((String) vb.getValue(context));
                }
            }
            link.setHref(href);
            if (StringUtils.isNotBlank(target)) {
                link.setTarget(target);
            }
        } else {
            final MethodBinding mb;
            if (StringUtils.startsWith(outcome2, "#{")) {
                mb = application.createMethodBinding(outcome2, new Class[] {});
            } else {
                mb = new ConstantMethodBinding(outcome2);
            }
            link.setAction(mb);
            if (StringUtils.isNotBlank(getActionListener())) {
                link.setActionListener(application.createMethodBinding(getActionListener(), new Class[] { javax.faces.event.ActionEvent.class }));
            }
        }

        if (getParams() != null) {
            for (Entry<String, String> entry : getParams().entrySet()) {
                addParameter(context, link, entry.getKey(), entry.getValue());
            }
        }

        if (active) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = link.getAttributes();
            attributes.put("styleClass", "active");
        }

        // Check if MenuItem should be initially hidden
        if (StringUtils.isNotBlank(getHidden())) {
            boolean hideIt = hidden.startsWith("#{")
                    ? (Boolean) application.createMethodBinding(getHidden(), new Class[] { String.class }).invoke(context, new Object[] { getId() }) //
                            : Boolean.valueOf(hidden);

                    if (hideIt && !getStyleClass().contains(HIDDEN_MENU_ITEM)) {
                        getStyleClass().add(HIDDEN_MENU_ITEM);
                    } else if (!hideIt) {
                        getStyleClass().remove(HIDDEN_MENU_ITEM);
                    }
                    if (MY_TASK_MENU_ITEMS.contains(id) && hideIt) {
                        log.debug("setting menu_my_tasks subitem " + getId() + " hidden");
                    }
        }

        Config config = Application.getConfigService(context).getGlobalConfig();
        ActionDefinition actionDef = ((ActionsConfigElement) config.getConfigElement(ActionsConfigElement.CONFIG_ELEMENT_ID))
                .getActionDefinition(outcome2);

        // Check, if there is config for this action and overwrite properties if available
        if (actionDef != null) {
            // prepare any code based evaluators that may be present
            if (actionDef.Evaluator != null) {
                ActionInstanceEvaluator evaluator =
                        (ActionInstanceEvaluator) application.createComponent(UIActions.COMPONENT_ACTIONEVAL);
                FacesHelper.setupComponentId(context, evaluator, id + "-evaluator");
                evaluator.setEvaluator(actionDef.Evaluator);
                evaluator.setValueBinding(ATTR_VALUE, application.createValueBinding("#{" + ACTION_CONTEXT + "}"));

                // add the action evaluator component and walk down the hiearchy
                if (component != null) {
                    @SuppressWarnings("unchecked")
                    List<UIComponent> children = component.getChildren();
                    children.add(evaluator);
                }
                component = evaluator;
            }

            if (actionDef.TooltipMsg != null) {
                link.setTooltip(messages.getString(actionDef.TooltipMsg));
            } else if (actionDef.Tooltip != null) {
                if (UIComponentTagUtils.isValueReference(actionDef.Tooltip)) {
                    link.setValueBinding("tooltip", application.createValueBinding(actionDef.Tooltip));
                } else {
                    link.setValue(actionDef.Tooltip);
                }
            }

            if (actionDef.Label != null) {
                link.setValue(actionDef.Label);
            } else if (actionDef.LabelMsg != null) {
                link.setValue(messages.getString(actionDef.LabelMsg));
            }

            if (actionDef.Action != null) {
                if (UIComponentTagUtils.isValueReference(actionDef.Action)) {
                    link.setAction(application.createMethodBinding(actionDef.Action, null));
                } else {
                    link.setAction(new ConstantMethodBinding(actionDef.Action));
                }
            }
            if (actionDef.ActionListener != null) {
                link.setActionListener(application.createMethodBinding(actionDef.ActionListener, new Class[] { javax.faces.event.ActionEvent.class }));
            }

            // build any child params <f:param> components that are needed.
            Map<String, String> params = actionDef.getParams();
            if (params != null) {
                for (String name : params.keySet()) {
                    addParameter(context, link, name, params.get(name));
                }
            }
        }
        if (plainLink) {
            link.getAttributes().put(ATTR_PLAIN_MENU_ITEM, Boolean.TRUE);
            link.getAttributes().put("styleClass", StringUtils.join(getStyleClass(), " "));
            return link;
        }

        MenuItemWrapper wrap = (MenuItemWrapper) application.createComponent(MenuItemWrapper.class.getCanonicalName());
        FacesHelper.setupComponentId(context, wrap, id + "-wrapper");
        wrap.setPlain(true);
        if (getId() != null) {
            wrap.setMenuId(getId());
        }
        wrap.getAttributes().put("styleClass", StringUtils.join(getStyleClass(), " "));
        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrap.getChildren();
        children.add(link);

        return wrap;
    }

    protected boolean isRendered(UserService userService, WorkflowConstantsBean workflowConstantsBean, RSService rsService) {
        int reason = -1;
        boolean result = true;
        ApplicationConstantsBean appConstantsBean = BeanHelper.getApplicationConstantsBean();
        if (isRenderingDisabled() || isRestricted() && !hasPermissions(userService)) {
            result = false;
            reason = 1;
        } else if (isExternalReview() && !(isExternalReviewEnabled(workflowConstantsBean) || workflowConstantsBean.isReviewToOtherOrgEnabled())) {
            result = false;
            reason = 2;
        } else if (isOrderAssignment() && !isOrderAssignmentEnabled(workflowConstantsBean)) {
            result = false;
            reason = 3;
        } else if (isEinvoiceFunctionality() && !appConstantsBean.isEinvoiceEnabled()) {
            result = false;
            reason = 4;
        } else if (USER_COMPOUND_WORKFLOWS.equals(id) && !workflowConstantsBean.isIndependentWorkflowEnabled() && !appConstantsBean.isCaseVolumeEnabled()) {
            result = false;
            reason = 5;
        } else if (USER_CASE_FILES.equals(id) && !appConstantsBean.isCaseVolumeEnabled()) {
            result = false;
            reason = 6;
        } else if ("compoundWorkflowSearch".equals(id)) {
            result = appConstantsBean.isCaseVolumeEnabled() || workflowConstantsBean.isIndependentWorkflowEnabled();
            reason = 7;
        } else if (Arrays.asList("executedReports", "taskReports", "documentReports", "volumeReports").contains(id)) {
            result = BeanHelper.getReportService().isUsableByAdminDocManagerOnly() ? userService.isDocumentManager() : true;
            reason = 8;
        } else if (Arrays
                .asList("compoundWorkflows", ASSIGNMENT_TASKS, INFORMATION_TASKS, REVIEW_TASKS, EXTERNAL_REVIEW_TASKS, CONFIRMATION_TASKS, "taskSearch", "taskReports")
                .contains(id)) {
            result = workflowConstantsBean.isWorkflowEnabled();
            reason = 9;
        } else if (Arrays.asList(ORDER_ASSIGNMENT_TASKS, OPINION_TASKS, SIGNATURE_TASKS).contains(id)) {
            result = workflowConstantsBean.isDocumentWorkflowEnabled() || workflowConstantsBean.isIndependentWorkflowEnabled();
            reason = 10;
        } else if (EXTERNAL_REVIEW_TASKS.equals(id)) {
            result = workflowConstantsBean.isReviewToOtherOrgEnabled() || workflowConstantsBean.isExternalReviewWorkflowEnabled();
            reason = 11;
        } else {
            if (WEB_SERVICE_DOCUMENTS.equals(id) && StringUtils.isBlank(BeanHelper.getAddDocumentService().getWebServiceDocumentsMenuItemTitle())) {
                result = false;
                reason = 12;
            } else {
                boolean isRestrictedDelta = rsService.isRestrictedDelta();
                if ("regularDelta".equals(id) && (!isRestrictedDelta || StringUtils.isBlank(rsService.getDeltaUrl()))) {
                    result = false;
                    reason = 13;
                } else if ("restrictedDelta".equals(id)
                        && (isRestrictedDelta || StringUtils.isBlank(rsService.getRestrictedDeltaUrl()) || !BeanHelper.getRsAccessStatusBean().isCanUserAccessRestrictedDelta())) {
                    result = false;
                    reason = 14;
                }
            }
        }
        if (!result && MY_TASK_MENU_ITEMS.contains(getId())) {
            log.debug("menu_my_tasks subitem " + getId() + " is not rendered, reason=" + reason);
        }
        return result;
    }

    protected boolean isOrderAssignment() {
        return ORDER_ASSIGNMENT_TASKS.equals(id);
    }

    protected boolean isOrderAssignmentEnabled(WorkflowConstantsBean workflowConstantsBean) {
        return workflowConstantsBean.isOrderAssignmentWorkflowEnabled();
    }

    protected boolean isEinvoiceFunctionality() {
        return "dimensions".equals(id) || INCOMING_EINVOICE.equals(id);
    }

    protected void addParameter(FacesContext context, UIActionLink link, String name, String value) {
        UIParameter param = (UIParameter) context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PARAMETER);
        FacesHelper.setupComponentId(context, param, null);
        param.setName(name);
        if (UIComponentTagUtils.isValueReference(value)) {
            param.setValueBinding(ATTR_VALUE, context.getApplication().createValueBinding(value));
        } else {
            param.setValue(value);
        }
        @SuppressWarnings("unchecked")
        List<UIComponent> children = link.getChildren();
        children.add(param);
    }

    public boolean hasPermissions(UserService userService) {
        if (userService.isAdministrator()) { // Access all areas
            return true;
        }
        if (isDocManager() && userService.isDocumentManager()) {
            return true;
        }
        if (isAccountant() && userService.isAccountant()) {
            return true;
        }
        if (isSupervisor() && userService.isSupervisor()) {
            return true;
        }
        if (isArchivist() && userService.isArchivist()) {
            return true;
        }

        return false;
    }

    public boolean isRestricted() {
        return isAdmin() || isDocManager() || isAccountant() || isSupervisor() || isArchivist();
    }

    public boolean isExternalReview() {
        return EXTERNAL_REVIEW_TASKS.equals(id);
    }

    public boolean isLinkedReview() {
        return "linkedReviewTask".equals(id);
    }

    public boolean isIncomingEinvoice() {
        return INCOMING_EINVOICE.equals(id);
    }

    public boolean isExternalReviewEnabled(WorkflowConstantsBean workflowConstantsBean) {
        return workflowConstantsBean.isExternalReviewWorkflowEnabled();
    }

    public String getActionListener() {
        return actionListener;
    }

    public void setActionListener(String actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public String toString() {
        return "Title: " + title +
                "Outcome: " + outcome + ", " +
                "Admin: " + admin + ", " +
                "Subitems: " + (subItems == null ? null : subItems.size()) + "; ";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getTitle(boolean shorten) {
        if (shorten) {
            int suffixStart = title.lastIndexOf(CountAddingMenuItemProcessor.COUNT_SUFFIX_START);
            if (suffixStart > 0 && suffixStart < 25) { // Don't shorten if there is number added and it would be split
                return title;
            }

            String suffix = ""; // Suffix must be a numeric value between parentheses
            if (suffixStart > -1 && StringUtils.isNumeric(title.substring(suffixStart + 1, suffixStart + 2))) {
                suffix = " " + title.substring(suffixStart, title.length());
            }

            int maxWidth = Math.max(25 - suffix.length(), 25);

            return (maxWidth < 4 ? title : StringUtils.abbreviate(title, maxWidth)) + suffix;
        }
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleId() {
        return titleId;
    }

    public void setTitleId(String titleId) {
        this.titleId = titleId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public boolean hasSubItems() {
        return (subItems != null && subItems.size() > 0);
    }

    public List<MenuItem> getSubItems() {
        if (subItems == null) {
            subItems = new ArrayList<MenuItem>();
        }
        return subItems;
    }

    public void setSubItems(List<MenuItem> subItems) {
        this.subItems = subItems;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isDocManager() {
        return docManager;
    }

    public void setDocManager(boolean docManager) {
        this.docManager = docManager;
    }

    public Map<String, String> getParams() {
        if (params == null) {
            setParams(new HashMap<String, String>());
        }
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    public List<String> getStyleClass() {
        if (styleClass == null) {
            styleClass = new ArrayList<String>();
        }
        return styleClass;
    }

    public void setStyleClass(List<String> styleClass) {
        this.styleClass = styleClass;
    }

    public void setAccountant(boolean accountant) {
        this.accountant = accountant;
    }

    public boolean isAccountant() {
        return accountant;
    }

    public void setSupervisor(boolean supervisor) {
        this.supervisor = supervisor;
    }

    public boolean isSupervisor() {
        return supervisor;
    }

    public void setArchivist(boolean archivist) {
        this.archivist = archivist;
    }

    public boolean isArchivist() {
        return archivist;
    }

    public boolean isRenderingDisabled() {
        return renderingDisabled;
    }

    public void setRenderingDisabled(boolean renderingDisabled) {
        this.renderingDisabled = renderingDisabled;
    }

    public String getHidden() {
        return hidden;
    }

    public void setHidden(String hidden) {
        this.hidden = hidden;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

}
