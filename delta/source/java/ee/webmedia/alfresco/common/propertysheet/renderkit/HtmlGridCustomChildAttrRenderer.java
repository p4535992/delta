package ee.webmedia.alfresco.common.propertysheet.renderkit;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.util.Pair;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.renderkit.html.HtmlGridRenderer;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import ee.webmedia.alfresco.utils.MessageUtil;

public class HtmlGridCustomChildAttrRenderer extends HtmlGridRenderer {

    public static final String HTML_GRID_CUSTOM_CHILD_ATTR_RENDERER_TYPE = HtmlGridCustomChildAttrRenderer.class.getCanonicalName();
    public static final String CHILDREN_COLSPANS_ATTR = "childColspans";
    public static final String CHILDREN_CLASSES_ATTR = "childClasses";
    public static final String HEADING_KEYS_ATTR = "headingKeys";
    public static final String FOOTER_ACTIONS_FACET = "footerActions";
    public static final String FOOTER_SUMS_ATTR = "footerSums";

    @Override
    protected int childAttributes(FacesContext context,
            ResponseWriter writer,
            UIComponent component,
            int columnIndex)
            throws IOException {
        int colspan = 1;
        Map childrenColspanAttrib = (Map) component.getParent().getAttributes().get(CHILDREN_COLSPANS_ATTR);
        if (childrenColspanAttrib != null) {
            Integer colspanInt = (Integer) childrenColspanAttrib.get(component.getId());
            if (colspanInt != null) {
                writer.writeAttribute(HTML.COLSPAN_ATTR, colspanInt, null);
                colspan = colspanInt;
            }
        }
        Map componentClassAttrib = (Map) component.getParent().getAttributes().get(CHILDREN_CLASSES_ATTR);
        writeStringAttribute(writer, component, componentClassAttrib, HTML.CLASS_ATTR);

        return columnIndex + colspan - 1;
    }

    private void writeStringAttribute(ResponseWriter writer, UIComponent component, Map componentAttrib, String attribName) throws IOException {
        if (componentAttrib != null) {
            if (componentAttrib.get(component.getId()) != null) {
                String attributeValue = (String) componentAttrib.get(component.getId());
                if (StringUtils.isNotBlank(attributeValue)) {
                    writer.writeAttribute(attribName, attributeValue, null);
                }
            }
        }
    }

    // Override header and footer rendering; assume that there is right number of headings available
    @Override
    protected void renderHeaderOrFooter(FacesContext context, ResponseWriter writer, UIComponent component, int columns, boolean header)
            throws IOException {
        if (header) {
            List<String> headingKeys = (List<String>) component.getAttributes().get(HEADING_KEYS_ATTR);
            writer.startElement("thead", component);
            writer.startElement("tr", component);
            int lastHeading = headingKeys.size() - 1;
            int i = 0;
            for (String headingKey : headingKeys) {
                writer.startElement("th", component);
                if (i == lastHeading - 1 && StringUtils.isBlank(headingKeys.get(lastHeading))) {
                    writer.writeAttribute("style", "text-align: right;", null);
                }
                writer.write(StringUtils.isBlank(headingKey) ? "" : MessageUtil.getMessage(headingKey));
                writer.endElement("th");
                i++;
            }
            writer.endElement("tr");
            writer.endElement("thead");

        } else {
            writer.startElement("tfoot", component);
            writer.writeAttribute(HTML.CLASS_ATTR, "trans-sum", null);
            writer.startElement("tr", component);
            writer.startElement("td", component);
            writer.writeAttribute("colspan", 2, null);
            UIComponent footerActions = (UIComponent) component.getFacets().get(FOOTER_ACTIONS_FACET);
            if (footerActions != null) {
                Utils.encodeRecursive(context, footerActions);
            }
            writer.endElement("td");
            writer.startElement("td", component);
            writer.writeAttribute("colspan", Integer.toString(columns - 2), null);
            writer.writeAttribute("style", "text-align: right;", null);
            List<Pair<String, Pair<String, String>>> footerSums = (List<Pair<String, Pair<String, String>>>) component.getAttributes().get(FOOTER_SUMS_ATTR);
            if (footerSums != null) {
                for (Pair<String, Pair<String, String>> labelAndSum : footerSums) {
                    writer.write(labelAndSum.getFirst() + ": ");
                    writer.startElement("strong", component);
                    Pair<String, String> sumAndColor = labelAndSum.getSecond();
                    String sum = sumAndColor.getFirst();
                    String color = sumAndColor.getSecond();
                    if (StringUtils.isNotBlank(color)) {
                        writer.writeAttribute("style", "color: " + color + ";", null);
                    }
                    writer.write(sum);
                    writer.endElement("strong");
                    writer.write(" ");
                }
            }
            writer.endElement("td");
            writer.endElement("tr");
            writer.endElement("tfoot");
        }
    }
}
