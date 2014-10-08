package ee.alfresco.web.ui.common.renderer.data;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import javax.faces.webapp.UIComponentTag;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.renderer.data.RichListRenderer;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.springframework.util.Assert;

import ee.alfresco.web.ui.common.UITableRow;
import ee.webmedia.alfresco.utils.ComponentUtil;

public class RichListMultiTbodyRenderer extends RichListRenderer {
    private static final String NULL = new String();

    /**
     * Class that implements richList VIEWMODEID = "detailsMultiTbody" that allows to create multiple tbody elements
     */
    public static class DetailsViewRenderer extends RichListRenderer.DetailsViewRenderer {
        private static final long serialVersionUID = 1L;
        private static final String ATTR_GROUP_PREVIOUS = "groupPrevious";

        public static final String ATTR_GROUP_BY = "groupBy";
        @SuppressWarnings("hiding")
        public static final String VIEWMODEID = "detailsMultiTbody";
        /**
         * Can be used to render different attributes for each html tbody element created for different group.
         * <b>Map&lt;String <i>groupCode</i>, Map&lt;String <i>attributeName</i> , String <i>attributeValue</i>&gt;&gt;</b> -
         * map containing maps by group. Key-value pair of each group is used to render html attributes for tbody of given group
         */
        public static final String ATTR_GROUP_TBODY_ATTRIBUTES = "tbodyAttributesByGroup";
        public static final String ATTR_ADDITIONAL_ROW_STYLE_BINDING = "additionalRowStyleClassBinding";
        /** Can be used to render facets of type {@link UITableRow} after regular rows */
        public static final String ATTR_FACET_ROWS = "facetRows";
        private int rowIndex = 0;

        @Override
        public String getViewModeID() {
            return VIEWMODEID;
        }

        @Override
        public void renderListRow(FacesContext context, UIRichList richList, UIColumn[] columns, Object row) throws IOException {
            ResponseWriter out = context.getResponseWriter();
            createNewTbodyIfNeeded(richList, out, context);

            // output row or alt style row if set
            out.write("<tr");
            String rowStyle = (String) richList.getAttributes().get("rowStyleClass");
            String altStyle = (String) richList.getAttributes().get("altRowStyleClass");
            if (altStyle != null && ++rowIndex % 2 == 0) {
                rowStyle = altStyle;
            }

            rowStyle = getAdditionalRowStyleClass(rowStyle, richList, context);

            outputAttribute(out, rowStyle, "class");
            out.write('>');

            // find the actions column if it exists
            UIColumn actionsColumn = null;
            for (UIColumn column : columns) {
                if (column.isRendered() && column.getActions()) {
                    actionsColumn = column;
                    break;
                }
            }

            // output each column in turn and render all children
            boolean renderedFirst = false;
            for (UIColumn column : columns) {
                if (column.isRendered() == true) {
                    out.write("<td");
                    outputAttribute(out, column.getAttributes().get("style"), "style");
                    outputAttribute(out, column.getAttributes().get("styleClass"), "class");
                    outputAttribute(out, column.getAttributes().get("colspan"), "colspan");
                    out.write('>');

                    // for details view, we show the small column icon for the first column
                    if (renderedFirst == false) {
                        UIComponent smallIcon = column.getSmallIcon();
                        if (smallIcon != null) {
                            smallIcon.encodeBegin(context);
                            if (smallIcon.getRendersChildren()) {
                                smallIcon.encodeChildren(context);
                            }
                            smallIcon.encodeEnd(context);
                            out.write("&nbsp;");
                        }
                        renderedFirst = true;
                    }

                    if (column.getChildCount() != 0) {
                        if (column == actionsColumn) {
                            out.write("<nobr>");
                        }

                        // allow child controls inside the columns to render themselves
                        Utils.encodeRecursive(context, column);

                        if (column == actionsColumn) {
                            out.write("</nobr>");
                        }
                    }

                    out.write("</td>");
                }
            }
            out.write("</tr>");
        }

        private String getAdditionalRowStyleClass(String rowStyle, UIRichList richList, FacesContext context) {
            String additionalRowStyleClassBinding = ComponentUtil.getAttribute(richList, ATTR_ADDITIONAL_ROW_STYLE_BINDING, String.class);
            if (StringUtils.isNotBlank(additionalRowStyleClassBinding)) {
                String additionalRowStyleClass;
                if (UIComponentTag.isValueReference(additionalRowStyleClassBinding)) {
                    ValueBinding vb = context.getApplication().createValueBinding(additionalRowStyleClassBinding);
                    additionalRowStyleClass = vb.getValue(context).toString();
                } else {
                    additionalRowStyleClass = additionalRowStyleClassBinding;
                }
                rowStyle += " " + additionalRowStyleClass.toString();
            }
            return rowStyle;
        }

        private void createNewTbodyIfNeeded(UIRichList richList, ResponseWriter out, FacesContext context) throws IOException {
            String group = getGroupCode(richList, context);
            if (group == null) {
                group = NULL; // using special NULL object instead, because null is not allowed as attribute value
            }
            Object groupPrevious = ComponentUtil.getAttribute(richList, ATTR_GROUP_PREVIOUS);
            if (!(group.equals(groupPrevious))) {
                @SuppressWarnings("unchecked")
                Map<String/* groupCode */, Map<String/* attributeName */, String/* attributeValue */>> tbodyAttributesByGroup //
                = ComponentUtil.getAttribute(richList, ATTR_GROUP_TBODY_ATTRIBUTES, Map.class);

                StringBuilder tbodyAttributes = new StringBuilder();
                if (tbodyAttributesByGroup != null) {
                    Map<String, String> tbodyAttributesMap = tbodyAttributesByGroup.get(group == NULL ? null : group);
                    if (tbodyAttributesMap != null && tbodyAttributesMap.entrySet() != null) {
                        for (Entry<String/* attributeName */, String/* attributeValue */> entry : tbodyAttributesMap.entrySet()) {
                            String attributeName = entry.getKey();
                            String attributeValue = entry.getValue();
                            Assert.isTrue(StringUtils.isNotBlank(attributeName), "attribute name must not be blank");
                            attributeValue = StringEscapeUtils.escapeHtml(attributeValue);
                            tbodyAttributes.append(attributeName).append("='").append(attributeValue).append("' ");
                        }
                    }
                }
                out.write("<tr/></tbody><tbody " + tbodyAttributes + ">");
                UIComponent facet = richList.getFacet(group);
                if (facet instanceof UITableRow) {
                    UITableRow groupFirstTr = (UITableRow) facet;
                    RendererUtils.renderChild(context, groupFirstTr);
                }
            }
            ComponentUtil.putAttribute(richList, ATTR_GROUP_PREVIOUS, group);
        }

        private String getGroupCode(UIRichList richList, FacesContext context) {
            String groupByBinding = ComponentUtil.getAttribute(richList, ATTR_GROUP_BY, String.class);
            if (groupByBinding != null) {
                if (UIComponentTag.isValueReference(groupByBinding)) {
                    ValueBinding vb = context.getApplication().createValueBinding(groupByBinding);
                    Object value = vb.getValue(context);
                    return value != null ? value.toString() : null;
                }
            }
            return groupByBinding;
        }

        @Override
        public void renderListAfter(FacesContext context, UIRichList richList, UIColumn[] columns)
                throws IOException {
            ResponseWriter out = context.getResponseWriter();
            Object groupPrevious = ComponentUtil.getAttribute(richList, ATTR_GROUP_PREVIOUS);
            if (groupPrevious != null) {
                out.write("</tbody><tbody>");
            }

            out.write("<tr><td colspan='99' align='center' >");

            for (UIComponent child : ComponentUtil.getChildren(richList)) {
                if (child instanceof UIColumn == false) {
                    Utils.encodeRecursive(context, child);
                }
            }
            out.write("</td></tr>");
            Collection<String> facetRows = getFacetRows(richList);
            if (facetRows != null) {
                for (String rowFacetName : facetRows) {
                    UIComponent facet = richList.getFacet(rowFacetName);
                    if (facet instanceof UITableRow) {
                        UITableRow groupFirstTr = (UITableRow) facet;
                        RendererUtils.renderChild(context, groupFirstTr);
                    }
                }
            }
            // when we only have one group then without this line after refreshing group header would not be created
            ComponentUtil.putAttribute(richList, ATTR_GROUP_PREVIOUS, NULL);
        }

        public static void setFacetRows(UIRichList richList, Collection<String> facetRows) {
            ComponentUtil.putAttribute(richList, ATTR_FACET_ROWS, facetRows);
        }

        public static Collection<String> getFacetRows(UIRichList richList) {
            @SuppressWarnings("unchecked")
            Collection<String> facetRows = (Collection<String>) ComponentUtil.getAttribute(richList, ATTR_FACET_ROWS);
            return facetRows;
        }

    }
}
