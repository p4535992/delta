package ee.webmedia.alfresco.simdhs;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.component.data.UISortLink;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Reads data directly from rich list data model.
 * <p/>
 * To exclude column from CSV export, add following facet to column: {@code
 * <f:facet name="csvExport">
 *  <a:param value="false"/>
 * </f:facet>
 * }
 * 
 * @author Romet Aidla
 */
public class RichListDataReader implements DataReader {
    private static Logger log = Logger.getLogger(RichListDataReader.class);

    private static final String CSV_EXPORT_FACET_LABEL = "csvExport";
    private static final String HEADER_FACET_LABEL = "header";
    private static final String URL_HEADER = "Link";

    @Override
    public List<String> getHeaderRow(UIRichList list, FacesContext fc) {
        List<UIColumn> columnsToExport = getColumnsToExport(list);
        return getHeaderRow(columnsToExport, isDocumentSearch(list));
    }

    @Override
    public List<List<String>> getDataRows(UIRichList list, FacesContext fc) {
        List<List<String>> data = new ArrayList<List<String>>();
        list.setRowIndex(-1);
        final List<UIColumn> columnsToExport = getColumnsToExport(list);
        boolean isDocumentSearch = isDocumentSearch(list);
        while (list.isAbsoluteDataAvailable()) {
            list.increment();
            NodeRef docRef = getDocumentNodeRef(list, isDocumentSearch);
            data.add(getDataRow(fc, columnsToExport, docRef));
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private NodeRef getDocumentNodeRef(UIRichList list, boolean isDocumentSearch) {
        if (isDocumentSearch) {
            List<Document> docs = (List<Document>) list.getValue();
            return docs.get(list.getRowIndex()).getNodeRef();
        }
        return null;
    }

    private boolean isDocumentSearch(UIRichList list) {
        @SuppressWarnings("rawtypes")
        List values = (List) list.getValue();
        return (!values.isEmpty() && values.get(0) instanceof Document);
    }

    @SuppressWarnings("unchecked")
    private List<UIColumn> getColumnsToExport(UIRichList list) {
        List<UIColumn> columnsToExport = new ArrayList<UIColumn>();
        List<UIComponent> components = list.getChildren();

        for (UIComponent component : components) {
            if (component instanceof UIColumn && component.isRendered()) {

                UIColumn column = (UIColumn) component;
                if (!isExcluded(column)) {
                    columnsToExport.add(column);
                }
            }
        }
        return columnsToExport;
    }

    // by default column is included, it must be disabled manually
    private boolean isExcluded(UIColumn column) {
        UIParameter parameter = (UIParameter) column.getFacet(CSV_EXPORT_FACET_LABEL);
        return parameter != null && parameter.getValue().toString().equals("false");
    }

    private List<String> getHeaderRow(List<UIColumn> columns, boolean isDocSearch) {
        List<String> row = new ArrayList<String>();
        for (UIColumn column : columns) {
            UIComponent component = (UIComponent) column.getFacets().get(HEADER_FACET_LABEL);
            Assert.notNull(component, "Header facet not found for column:" + column.getId());

            row.add(getHeaderValue(component));
        }
        if (!row.isEmpty() && isDocSearch) {
            row.add(URL_HEADER);
        }
        return row;
    }

    private static String getHeaderValue(UIComponent component) {
        if (component instanceof UISortLink) {
            UISortLink link = (UISortLink) component;
            return link.getLabel();
        } else {
            String componentClass = component.getClass().getName();
            if (log.isDebugEnabled()) {
                log.debug("Unsupported component type for header row:" + componentClass);
            }
            return componentClass;
        }
    }

    private List<String> getDataRow(FacesContext facesContext, List<UIColumn> columns, NodeRef docRef) {
        List<String> row = new ArrayList<String>();
        for (UIColumn column : columns) {
            List<UIComponent> children = ComponentUtil.getChildren(column);
            if (children != null && !children.isEmpty()) {
                UIComponent component = children.get(0);
                row.add(getRowValue(facesContext, component));
            } else {
                row.add("");
            }
        }
        if (!row.isEmpty() && docRef != null) {
            String docUrl = BeanHelper.getDocumentTemplateService().getDocumentUrl(docRef);
            row.add(docUrl);
        }
        return row;
    }

    private static String getRowValue(FacesContext facesContext, UIComponent component) {
        if (component == null) {
            return "";
        }
        if (component instanceof ValueHolder) { // handle texts
            ValueHolder valueHolder = (ValueHolder) component;
            Object value = valueHolder.getValue();

            // use converter if exists
            if (valueHolder.getConverter() != null) {
                value = valueHolder.getConverter().getAsString(facesContext, component, value);
            }

            return value != null ? value.toString() : "";
        } else if (component instanceof UICommand) { // handle links
            UICommand command = (UICommand) component;
            Object value = command.getValue();
            return value != null ? value.toString() : "";
        } else { // unsupported component type
            String componentClass = component.getClass().getName();
            if (log.isDebugEnabled()) {
                log.debug("Unsupported component type for data row:" + componentClass);
            }
            return componentClass;
        }
    }
}
