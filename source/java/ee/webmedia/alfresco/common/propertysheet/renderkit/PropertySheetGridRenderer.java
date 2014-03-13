package ee.webmedia.alfresco.common.propertysheet.renderkit;

import java.io.IOException;
import java.util.Iterator;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.repo.component.property.UISeparator;
import org.apache.myfaces.renderkit.html.HtmlGridRenderer;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared_impl.util.ArrayUtils;
import org.apache.myfaces.shared_impl.util.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.propertysheet.search.Search;

public class PropertySheetGridRenderer extends HtmlGridRenderer {

    /**
     * Copied from Apache myfaces 1.1.7 shared_impl HtmlGridRendererBase and modified the <td></td> output part
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void renderChildren(FacesContext context, ResponseWriter writer, UIComponent component, int columns) throws IOException {
        writer.startElement(HTML.TBODY_ELEM, component);

        String columnClasses;
        String rowClasses;
        if (component instanceof HtmlPanelGrid) {
            columnClasses = ((HtmlPanelGrid) component).getColumnClasses();
            rowClasses = ((HtmlPanelGrid) component).getRowClasses();
        } else {
            columnClasses = (String) component.getAttributes().get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.COLUMN_CLASSES_ATTR);
            rowClasses = (String) component.getAttributes().get(JSFAttr.ROW_CLASSES_ATTR);
        }

        String[] columnClassesArray = (columnClasses == null) ? ArrayUtils.EMPTY_STRING_ARRAY : StringUtils.trim(StringUtils.splitShortString(columnClasses,
                ','));
        int columnClassesCount = columnClassesArray.length;

        String[] rowClassesArray = (rowClasses == null) ? org.apache.myfaces.shared_impl.util.ArrayUtils.EMPTY_STRING_ARRAY : StringUtils.trim(StringUtils
                .splitShortString(rowClasses, ','));
        int rowClassesCount = rowClassesArray.length;

        int childCount = getChildCount(component);
        if (childCount > 0) {
            int columnIndex = 0;
            int rowClassIndex = 0;
            boolean rowStarted = false;
            boolean inlineMode = false;
            String labelStyleClass = (String) component.getAttributes().get("labelStyleClass");
            labelStyleClass = (labelStyleClass != null) ? labelStyleClass : "";
            for (Iterator it = getChildren(component).iterator(); it.hasNext();) {
                UIComponent child = (UIComponent) it.next();
                if (child.isRendered()) {
                    if (columnIndex == 0 && !inlineMode) {
                        // start of new/next row
                        if (rowStarted) {
                            // do we have to close the last row?
                            writer.endElement(HTML.TR_ELEM);
                            HtmlRendererUtils.writePrettyLineSeparator(context);
                        }
                        writer.startElement(HTML.TR_ELEM, component);
                        if (rowClassIndex < rowClassesCount) {
                            writer.writeAttribute(HTML.CLASS_ATTR, rowClassesArray[rowClassIndex], null);
                        }
                        rowStarted = true;
                        rowClassIndex++;
                        if (rowClassIndex == rowClassesCount) {
                            rowClassIndex = 0;
                        }
                    }

                    writer.startElement(HTML.TD_ELEM, component);
                    if (columnIndex < columnClassesCount) {
                        writer.writeAttribute(HTML.CLASS_ATTR, columnClassesArray[columnIndex] + " " + labelStyleClass, null);
                    } else if (child instanceof UISeparator) {
                        writer.writeAttribute(HTML.CLASS_ATTR, "separator", null);
                        writer.writeAttribute(HTML.COLSPAN_ATTR, 2, null);
                    } else {
                        writer.writeAttribute(HTML.CLASS_ATTR, labelStyleClass, null);
                    }

                    columnIndex = childAttributes(context, writer, child, columnIndex);
                    String styleClass = (String) child.getAttributes().get("styleClass");
                    if (org.apache.commons.lang.StringUtils.isNotBlank(styleClass)) {
                        styleClass += " inline";
                    } else {
                        styleClass = "inline";
                    }
                    child.getAttributes().put("styleClass", styleClass);
                    RendererUtils.renderChild(context, child);

                    if ((child instanceof CustomAttributes || child instanceof Search) && component instanceof WMUIPropertySheet) {
                        String display = ((CustomAttributes) child).getCustomAttributes().get(WMUIPropertySheet.DISPLAY);
                        if (org.apache.commons.lang.StringUtils.isNotBlank(display) && display.equals(WMUIPropertySheet.INLINE)) {
                            if (!inlineMode) {
                                writer.startElement(HTML.TABLE_ELEM, component);
                                writer.writeAttribute(HTML.CELLPADDING_ATTR, 0, null);
                                writer.writeAttribute(HTML.CELLSPACING_ATTR, 0, null);
                                writer.writeAttribute(HTML.BORDER_ATTR, 0, null);
                                writer.writeAttribute(HTML.CLASS_ATTR, "inline", null);
                                writer.startElement(HTML.TR_ELEM, component);
                            }
                            inlineMode = true;
                        } else {
                            if (inlineMode) {
                                writer.endElement(HTML.TR_ELEM);
                                writer.endElement(HTML.TABLE_ELEM);
                                inlineMode = false;
                            }
                        }
                    }

                    writer.endElement(HTML.TD_ELEM);

                    columnIndex++;
                    if (columnIndex >= columns) {
                        columnIndex = 0;
                    }
                }
            }

            if (rowStarted) {
                if (columnIndex > 0) {
                    // Render empty columns, so that table is correct
                    for (; columnIndex < columns; columnIndex++) {
                        writer.startElement(HTML.TD_ELEM, component);
                        if (columnIndex < columnClassesCount) {
                            writer.writeAttribute(HTML.CLASS_ATTR, columnClassesArray[columnIndex], null);
                        }
                        writer.endElement(HTML.TD_ELEM);
                    }
                }
                writer.endElement(HTML.TR_ELEM);
                HtmlRendererUtils.writePrettyLineSeparator(context);
            }
        }

        writer.endElement(HTML.TBODY_ELEM);
    }
}
