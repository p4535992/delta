<<<<<<< HEAD
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

/**
 * @author Erko Hansar
 * @author Kaarel Jõgeva
 */
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
            for (Iterator<UIComponent> it = getChildren(component).iterator(); it.hasNext();) {
                UIComponent child = it.next();

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


                if (child.isRendered()) {
                    if (columnIndex < columnClassesCount) {
                        writer.writeAttribute(HTML.CLASS_ATTR, columnClassesArray[columnIndex] + " " + labelStyleClass, null);
                    } else if (child instanceof UISeparator) {
                        writer.writeAttribute(HTML.CLASS_ATTR, "separator", null);
                        writer.writeAttribute(HTML.COLSPAN_ATTR, 2, null);
                    } else {
                        writer.writeAttribute(HTML.CLASS_ATTR, labelStyleClass, null);
                    }
                    String styleClass = (String) child.getAttributes().get("styleClass");
                    if (org.apache.commons.lang.StringUtils.isNotBlank(styleClass)) {
                        styleClass += " inline";
                    } else {
                        styleClass = "inline";
                    }
                    child.getAttributes().put("styleClass", styleClass);
                    RendererUtils.renderChild(context, child);
                }
                columnIndex = childAttributes(context, writer, child, columnIndex);

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
=======
package ee.webmedia.alfresco.common.propertysheet.renderkit;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.repo.component.property.UISeparator;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.renderkit.html.HtmlGridRenderer;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared_impl.util.ArrayUtils;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.propertysheet.search.Search;

public class PropertySheetGridRenderer extends HtmlGridRenderer {

    @Override
    protected void renderChildren(FacesContext context, ResponseWriter writer, UIComponent component, int columns) throws IOException {
        int childCount = getChildCount(component);
        if (childCount <= 0) {
            return;
        }
        writer.startElement(HTML.TBODY_ELEM, component);

        String[] columnClasses;
        String[] rowClasses;
        if (component instanceof HtmlPanelGrid) {
            columnClasses = getClassesArray(((HtmlPanelGrid) component).getColumnClasses());
            rowClasses = getClassesArray(((HtmlPanelGrid) component).getRowClasses());
        } else {
            columnClasses = getClassesArray((String) component.getAttributes().get(JSFAttr.COLUMN_CLASSES_ATTR));
            rowClasses = getClassesArray((String) component.getAttributes().get(JSFAttr.ROW_CLASSES_ATTR));
        }

        String labelStyleClass = (String) component.getAttributes().get("labelStyleClass");
        labelStyleClass = (labelStyleClass != null) ? labelStyleClass : "";

        int currentColumn = 0;
        int currentRow = 0;

        for (@SuppressWarnings("unchecked")
        Iterator<UIComponent> it = getChildren(component).iterator(); it.hasNext();) {
            UIComponent child = it.next();

            boolean inline = childAttributes(context, writer, child, currentColumn) == currentColumn;
            boolean childRendered = child.isRendered();

            // Check if we can skip the row entirely
            UIComponent next = inline && it.hasNext() ? it.next() : null;
            boolean nextInlineRendered = next != null && next.isRendered();
            if (!childRendered && (!inline || !nextInlineRendered)) {
                currentRow++;
                continue; // We can skip the entire row, if child isn't rendered nor it has inline attribute
            }

            // Start a new table row
            if (currentColumn == 0) {
                writer.startElement(HTML.TR_ELEM, component);
                if (currentRow < rowClasses.length) {
                    writer.writeAttribute(HTML.CLASS_ATTR, rowClasses[currentRow], null);
                }
                currentRow++;
            }

            if (childRendered) {
                writer.startElement(HTML.TD_ELEM, child);
                writeCellAttributes(child, writer, labelStyleClass, columnClasses, currentColumn);

                // Check in advance if next one is rendered. If not, then let control occupy full space
                if (inline && !nextInlineRendered) {
                    removeInline(child);
                }

                RendererUtils.renderChild(context, child);

                if (inline && !nextInlineRendered) {
                    markInline(child);
                }
                writer.endElement(HTML.TD_ELEM);
            }

            // If the element is displayed inline, render the next item in the same value cell
            if (inline && nextInlineRendered) {
                renderInline(context, writer, next, labelStyleClass, columnClasses, 0, childRendered);
            } else {
                // Increase column index if needed
                currentColumn = currentColumn >= columns ? 0 : ++currentColumn;
            }

            if (currentColumn == columns) {
                currentColumn = 0;
                writer.endElement(HTML.TR_ELEM);
                HtmlRendererUtils.writePrettyLineSeparator(context);
            }
        }
        writer.endElement(HTML.TBODY_ELEM);
    }

    private void renderInline(FacesContext context, ResponseWriter writer, UIComponent component, String labelStyleClass, String[] columnClasses,
            int currentColumn, boolean previousRendered) throws IOException {

        writer.startElement(HTML.TD_ELEM, component);
        writeCellAttributes(component, writer, labelStyleClass, columnClasses, currentColumn);

        if (!component.isRendered()) {
            // Fill empty space
            writer.writeAttribute(HTML.COLSPAN_ATTR, 2, null);
            writer.endElement(HTML.TD_ELEM);
            return;
        }

        // If previous is rendered, we should mark this also as inline to avoid taking up 4 cells
        if (previousRendered) {
            markInline(component);
        }

        RendererUtils.renderChild(context, component);
        writer.endElement(HTML.TD_ELEM);

        // Clean up
        if (previousRendered) {
            removeInline(component);
        }
    }

    @Override
    protected int childAttributes(FacesContext context, ResponseWriter writer, UIComponent component, int columnIndex) throws IOException {
        super.childAttributes(context, writer, component, columnIndex);

        @SuppressWarnings("unchecked")
        Map<String, String> attributes = component.getAttributes();
        String styleClass = attributes.get("styleClass");
        if (org.apache.commons.lang.StringUtils.isNotBlank(styleClass)) {
            styleClass += " inline";
        } else {
            styleClass = "inline";
        }
        attributes.put("styleClass", styleClass);

        if (!isInlineComponent(component)) {
            columnIndex++;
        }

        return columnIndex;
    }

    public void writeCellAttributes(UIComponent child, ResponseWriter writer, String labelStyleClass, String[] columnClasses, int currentColumn) throws IOException {
        if (child instanceof UISeparator) {
            writer.writeAttribute(HTML.CLASS_ATTR, "separator", null);
            writer.writeAttribute(HTML.COLSPAN_ATTR, 4, null);
            return;
        }

        StringBuilder styleClass = new StringBuilder();
        if (currentColumn < columnClasses.length) {
            styleClass.append(columnClasses[currentColumn]);
        }
        if (StringUtils.isNotBlank(labelStyleClass)) {
            if (styleClass.length() > 0) {
                styleClass.append(" ");
            }
            styleClass.append(labelStyleClass);
        }

        if (styleClass.length() > 0) {
            writer.writeAttribute(HTML.CLASS_ATTR, styleClass.toString(), null);
        }
    }

    public static boolean isInlineComponent(UIComponent component) {
        return (component instanceof CustomAttributes || component instanceof Search)
                && component.getParent() instanceof WMUIPropertySheet
                && WMUIPropertySheet.INLINE.equals(((CustomAttributes) component).getCustomAttributes().get(WMUIPropertySheet.DISPLAY));
    }

    private String[] getClassesArray(String classes) {
        String[] classesArray = (classes == null) ? ArrayUtils.EMPTY_STRING_ARRAY : StringUtils.split(classes, ',');
        return classesArray;
    }

    private void markInline(UIComponent component) {
        ((CustomAttributes) component).getCustomAttributes().put(WMUIPropertySheet.DISPLAY, WMUIPropertySheet.INLINE);
    }

    private void removeInline(UIComponent component) {
        ((CustomAttributes) component).getCustomAttributes().remove(WMUIPropertySheet.DISPLAY);
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
