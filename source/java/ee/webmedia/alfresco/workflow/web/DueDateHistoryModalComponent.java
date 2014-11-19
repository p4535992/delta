package ee.webmedia.alfresco.workflow.web;

import java.util.List;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;

import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.service.DueDateHistoryRecord;
import ee.webmedia.alfresco.workflow.service.Task;

public class DueDateHistoryModalComponent extends ModalLayerComponent {

    private static final long serialVersionUID = 1L;
    public static final String DUE_DATE_HISTORY_MODAL_ID_PREFIX = "dueDateHistoryModal";
    public static final String ATTR_TASK_DUE_DATE_HISTORY_RECORDS = "taskDueDateHistoryRecords";
    /** If page contains multiple DueDateHistoryModalComponent modal components, this attribute value must be unique for every instance */
    public static final String ATTR_TASK_DUE_DATE_HISTORY_MODAL_ID = "taskDueDateHistoryModalId";

    public String getTaskExtensionHistoryModalId() {
        return getTaskExtensionHistoryModalId(getUniqueId());
    }

    public static String getTaskExtensionHistoryModalId(String uniqueId) {
        return DUE_DATE_HISTORY_MODAL_ID_PREFIX + "-" + uniqueId;
    }

    private String getUniqueId() {
        return (String) getAttributes().get(ATTR_TASK_DUE_DATE_HISTORY_MODAL_ID);
    }

    /** This constructor also sets component id based on modalId parameter value */
    public DueDateHistoryModalComponent(FacesContext context, String modalId, List<DueDateHistoryRecord> historyRecords) {
        this();
        getAttributes().put(ATTR_TASK_DUE_DATE_HISTORY_MODAL_ID, modalId);
        getAttributes().put(ATTR_TASK_DUE_DATE_HISTORY_RECORDS, historyRecords);
        setId(getTaskExtensionHistoryModalId());
        generateModalChildren(context);
    }

    public DueDateHistoryModalComponent() {
        super();
        getAttributes().put(ATTR_SUBMIT_BUTTON_HIDDEN, true);
        getAttributes().put(ATTR_HEADER_KEY, "due_date_history_modal_title");
    }

    @Override
    protected String getModalHtmlId(FacesContext context) {
        return getId();
    }

    @Override
    protected String generateCloseOnClick(FacesContext context) {
        return "return hideModal();";
    }

    private void generateModalChildren(FacesContext context) {
        List<UIComponent> modalChildren = ComponentUtil.getChildren(this);
        modalChildren.clear();
        List<DueDateHistoryRecord> historyRecords = (List<DueDateHistoryRecord>) getAttributes().get(ATTR_TASK_DUE_DATE_HISTORY_RECORDS);
        if (historyRecords == null) {
            return;
        }
        String modalId = getUniqueId();
        Application application = context.getApplication();
        final HtmlPanelGrid historyGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        historyGrid.setId("due-date-history-grid-" + modalId);
        historyGrid.setColumns(2);
        modalChildren.add(historyGrid);
        List<UIComponent> tableChildren = ComponentUtil.getChildren(historyGrid);

        int historyRecordId = 0;
        for (DueDateHistoryRecord historyRecord : historyRecords) {
            String modalRowId = historyRecord.getTaskId() + "-" + historyRecordId;

            UIOutput component = (UIOutput) application.createComponent(ComponentConstants.JAVAX_FACES_OUTPUT);
            component.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
            component.setId("due-date-history-text-" + modalRowId);
            String previousDateStr = historyRecord.getPreviousDate() != null ? Task.dateTimeFormat.format(historyRecord.getPreviousDate()) : "";
            component.setValue(MessageUtil.getMessage("task_due_date_history_modal_info", previousDateStr, historyRecord.getChangeReason()));
            tableChildren.add(component);

            NodeRef extensionWorkflowNodeRef = historyRecord.getExtensionWorkflowNodeRef();
            if (extensionWorkflowNodeRef != null) {
                UIActionLink relatedCompoundWorkflowLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                FacesHelper.setupComponentId(context, relatedCompoundWorkflowLink, "due-date-history-related-workflow-link-" + modalRowId);
                relatedCompoundWorkflowLink.setValue(MessageUtil.getMessage("compoundWorkflow_due_date_extension_title"));
                relatedCompoundWorkflowLink.setAction(new ConstantMethodBinding("dialog:compoundWorkflowDialog"));
                relatedCompoundWorkflowLink.setActionListener(application.createMethodBinding("#{CompoundWorkflowDialog.setupWorkflow}", UIActions.ACTION_CLASS_ARGS));
                ComponentUtil.addChildren(relatedCompoundWorkflowLink, ComponentUtil.createUIParam("nodeRef", extensionWorkflowNodeRef, application));
                tableChildren.add(relatedCompoundWorkflowLink);
            } else {
                // dummy placeholder
                UIOutput selectDummy = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                selectDummy.setValue("");
                tableChildren.add(selectDummy);
            }

            historyRecordId++;
        }
    }

}
