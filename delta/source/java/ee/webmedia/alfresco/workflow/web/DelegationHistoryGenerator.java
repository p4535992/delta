package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.io.Serializable;
import java.util.Collections;
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
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Generates component that shows the history of delegation
 * 
 * @author Ats Uiboupin
 */
public class DelegationHistoryGenerator extends BaseComponentGenerator {
    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {

        Application app = context.getApplication();
        String listId = context.getViewRoot().createUniqueId();

        List<Node> delegationHistories = getRows(context, propertySheet.getNode());

        UIRichList richList = (UIRichList) app.createComponent("org.alfresco.faces.RichList");
        richList.setId("delegHistoryList-" + listId);
        richList.setViewMode("details");
        richList.setValue(delegationHistories);
        richList.setValueBinding("pageSize", context.getApplication().createValueBinding("#{BrowseBean.pageSizeContent}"));
        richList.setRendererType("org.alfresco.faces.RichListRenderer");
        putAttribute(richList, "var", "r");
        putAttribute(richList, "rowStyleClass", "recordSetRow");
        putAttribute(richList, "altRowStyleClass", "recordSetRowAlt");
        putAttribute(richList, "width", "100%");
        putAttribute(richList, "styleClass", "delegationWrapper");
        addChildren(richList
                , createColumn(WorkflowCommonModel.Props.DELEG_HIST_CREATOR_NAME, context)
                , createColumn(WorkflowCommonModel.Props.DELEG_HIST_OWNER_NAME, context)
                , createColumn(WorkflowCommonModel.Props.DELEG_HIST_CO_ASSIGNMENT_TASKS, context)
                , createColumn(WorkflowCommonModel.Props.DELEG_HIST_RESOLUTION, context)
                , createColumn(WorkflowCommonModel.Props.DELEG_HIST_DUE_DATE, context, MessageUtil.getMessage("date_pattern")) //
        );
        return richList;
    }

    private UIColumn createColumn(QName property, FacesContext context) {
        return createColumn(property, context, null);
    }

    private UIColumn createColumn(QName property, FacesContext context, String dateFormat) {
        Application application = context.getApplication();
        UIColumn column = (UIColumn) application.createComponent("org.alfresco.faces.RichListColumn");

        UISortLink sortLink = (UISortLink) application.createComponent("org.alfresco.faces.SortLink");
        sortLink.setLabel(MessageUtil.getMessage(property.getLocalName()));
        sortLink.setValue(property.toString());
        @SuppressWarnings("unchecked")
        final Map<String, UIComponent> facets = column.getFacets();
        facets.put("header", sortLink);

        HtmlOutputText outputText = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
        UIComponentTagUtils.setValueProperty(context, outputText, "#{r.properties['" + property + "']}");
        if (dateFormat != null) {
            XMLDateConverter dateConverter = (XMLDateConverter) application.createConverter(XMLDateConverter.CONVERTER_ID);
            dateConverter.setPattern(dateFormat);
            outputText.setConverter(dateConverter);
        }

        addChildren(column, outputText);
        return column;
    }

    private List<Node> getRows(FacesContext context, Node delegatableTask) {
        WorkflowService wfService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(//
                context).getBean(WorkflowService.BEAN_NAME);
        NodeRef taskRef = delegatableTask.getNodeRef();
        List<Node> delegationHistories = wfService.getDelegationHistoryNodes(taskRef);
        // create virtual history row based on delegatableTask
        Map<QName, Serializable> tempDelegationHistoryProps = wfService.getTempDelegationHistoryProps(delegatableTask);
        delegationHistories.add(new WmNode(null, WorkflowCommonModel.Types.DELEGATION_HISTORY, Collections.<QName> emptySet(), tempDelegationHistoryProps));
        return delegationHistories;
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("This is never called!");
    }

}
