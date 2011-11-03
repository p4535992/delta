package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

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
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Generates component that shows the history of delegation
 * 
 * @author Ats Uiboupin
 */
public class DelegationHistoryGenerator extends BaseComponentGenerator {
    private static final QName TMP_MAIN_OWNER = RepoUtil.createTransientProp("mainOwner");
    private static final QName TMP_CO_OWNER = RepoUtil.createTransientProp("coOwner");
    private static final QName TMP_STYLE_CLASS = RepoUtil.createTransientProp("styleClass");
    public static final Comparator<Task> COMPARATOR;
    static {
        COMPARATOR = getTaskComparator();
    }

    private static Comparator<Task> getTaskComparator() {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Task>() {
            @Override
            public Comparable<?> tr(Task input) {
                return input.getStartedDateTime();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Task>() {
            @Override
            public Comparable<?> tr(Task input) {
                return input.getDueDate();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Task>() {
            @Override
            public Comparable<?> tr(Task input) {
                return input.getOwnerName();
            }
        }, new NullComparator(AppConstants.DEFAULT_COLLATOR)));
        @SuppressWarnings("unchecked")
        Comparator<Task> tmp = chain;
        return tmp;
    }

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {

        Application app = context.getApplication();
        String listId = context.getViewRoot().createUniqueId();

        List<Node> delegationHistories = getRows(context, propertySheet.getNode());

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
                , createColumn(WorkflowSpecificModel.Props.DUE_DATE, context, MessageUtil.getMessage("date_pattern")) //
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
        outputText.setValueBinding("styleClass", context.getApplication().createValueBinding("#{r.properties['" + TMP_STYLE_CLASS + "']}"));
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
        List<Task> tasks4History = wfService.getTasks4DelegationHistory(delegatableTask);
        Collections.sort(tasks4History, COMPARATOR);
        List<Node> delegationHistories = new ArrayList<Node>(tasks4History.size());
        for (Task task : tasks4History) {
            WmNode taskNode = task.getNode();
            final QName mainOrCoOwner;
            if (task.isResponsible()) {
                mainOrCoOwner = TMP_MAIN_OWNER;
            } else {
                mainOrCoOwner = TMP_CO_OWNER;
            }
            Map<String, Object> props = taskNode.getProperties();
            props.put(mainOrCoOwner.toString(), task.getOwnerName());
            if (delegatableTask.getNodeRef().equals(task.getNodeRef())) {
                props.put(TMP_STYLE_CLASS.toString(), "bold");
            }
            delegationHistories.add(taskNode);
        }
        return delegationHistories;
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("This is never called!");
    }

}
