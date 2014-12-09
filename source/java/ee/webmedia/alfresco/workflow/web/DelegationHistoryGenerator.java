package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.component.data.UISortLink;
import org.alfresco.web.ui.common.converter.XMLDateConverter;
import org.alfresco.web.ui.common.tag.data.ColumnTag;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.DelegationHistoryUtil;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Generates component that shows the history of delegation
 */
public class DelegationHistoryGenerator extends BaseComponentGenerator {
    public static final QName TMP_MAIN_OWNER = RepoUtil.createTransientProp("mainOwner");
    public static final QName TMP_CO_OWNER = RepoUtil.createTransientProp("coOwner");
    public static final QName TMP_STYLE_CLASS = RepoUtil.createTransientProp("styleClass");

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {

        Application app = context.getApplication();
        String listId = context.getViewRoot().createUniqueId();

        List<Node> delegationHistories = getRows(propertySheet.getNode());

        UIRichList richList = (UIRichList) app.createComponent("org.alfresco.faces.RichList");
        richList.setId("delegHistoryList-" + listId);
        richList.setViewMode("details");
        richList.setValue(delegationHistories);
        richList.setRendererType("org.alfresco.faces.RichListRenderer");
        putAttribute(richList, "var", "r");
        putAttribute(richList, "rowStyleClass", "recordSetRow");
        putAttribute(richList, "altRowStyleClass", "recordSetRowAlt");
        putAttribute(richList, "width", "100%");
        putAttribute(richList, "styleClass", "delegationWrapper");
        addChildren(richList
                , createColumn(WorkflowCommonModel.Props.CREATOR_NAME, context)
                , createColumn(TMP_MAIN_OWNER, context)
                , createColumn(TMP_CO_OWNER, context)
                , createColumn(WorkflowSpecificModel.Props.RESOLUTION, context)
                , createColumn(WorkflowSpecificModel.Props.DUE_DATE, context, MessageUtil.getMessage("date_time_pattern")) //
        );
        return richList;
    }

    public int getPageSize() {
        // no paging, show all values
        return -1;
    }

    private UIColumn createColumn(QName property, FacesContext context) {
        return createColumn(property, context, null);
    }

    private UIColumn createColumn(QName property, FacesContext context, String dateFormat) {
        Application application = context.getApplication();
        UIColumn column = (UIColumn) application.createComponent(ColumnTag.COMPONENT_TYPE);

        UISortLink sortLink = (UISortLink) application.createComponent("org.alfresco.faces.SortLink");
        sortLink.setLabel(MessageUtil.getMessage("delegHist" + StringUtils.capitalize(property.getLocalName())));
        sortLink.setValue(property.toString());
        @SuppressWarnings("unchecked")
        final Map<String, UIComponent> facets = column.getFacets();
        facets.put("header", sortLink);
        HtmlOutputText outputText = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
        outputText.setValueBinding("styleClass", application.createValueBinding("#{r.properties['" + TMP_STYLE_CLASS + "']}"));
        UIComponentTagUtils.setValueProperty(context, outputText, "#{r.properties['" + property + "']}");
        if (dateFormat != null) {
            XMLDateConverter dateConverter = (XMLDateConverter) application.createConverter(XMLDateConverter.CONVERTER_ID);
            dateConverter.setPattern(dateFormat);
            outputText.setConverter(dateConverter);
        }

        addChildren(column, outputText);
        return column;
    }

    private List<Node> getRows(Node delegatableTask) {
        WorkflowService wfService = BeanHelper.getWorkflowService();
        List<Task> tasks4History = wfService.getTasks4DelegationHistory(delegatableTask);
        NodeRef taskRef = delegatableTask != null ? delegatableTask.getNodeRef() : null;
        return DelegationHistoryUtil.getDelegationNodes(taskRef, tasks4History);
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("This is never called!");
    }

}
