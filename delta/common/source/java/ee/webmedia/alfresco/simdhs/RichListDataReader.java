package ee.webmedia.alfresco.simdhs;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;

import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.component.data.UISortLink;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

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

    @Override
    public List<String> getHeaderRow(UIRichList list, FacesContext fc) {
        List<UIColumn> columnsToExport = getColumnsToExport(list);
        return getHeaderRow(columnsToExport);
    }

    @Override
    public List<List<String>> getDataRows(UIRichList list, FacesContext fc) {
        List<List<String>> data = new ArrayList<List<String>>();
        list.setRowIndex(-1);
        final List<UIColumn> columnsToExport = getColumnsToExport(list);
        while (list.isAbsoluteDataAvailable()) {
            list.increment();
            data.add(getDataRow(fc, columnsToExport));
        }
        return data;
    }

    private List<String> getDataFromRow(Object row, List<UIColumn> columnsToExport) {
        for (UIColumn uiColumn : columnsToExport) {
            System.out.println(row);
            // row.getClass().getMethods()
            // uiColumn.get
        }
        // TODO Auto-generated method stub
        return null;
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

    private List<String> getHeaderRow(List<UIColumn> columns) {
        List<String> row = new ArrayList<String>();
        for (UIColumn column : columns) {
            UIComponent component = (UIComponent) column.getFacets().get(HEADER_FACET_LABEL);
            Assert.notNull(component, "Header facet not found for column:" + column.getId());

            row.add(getHeaderValue(component));
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

    private List<String> getDataRow(FacesContext facesContext, List<UIColumn> columns) {
        List<String> row = new ArrayList<String>();
        for (UIColumn column : columns) {
            UIComponent component = (UIComponent) column.getChildren().get(0);
            row.add(getRowValue(facesContext, component));
        }
        return row;
    }

    private static String getRowValue(FacesContext facesContext, UIComponent component) {
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
