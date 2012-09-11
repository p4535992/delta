package ee.webmedia.alfresco.menu.ui.component;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.ui.common.component.SelfRenderingComponent;

public class YahooTreeItem extends SelfRenderingComponent {

    private NodeRef nodeRef;
    private String title;
    private int childrenCount;
    private boolean showChildrenCount = true;

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        String id = getClientId(context).replaceAll("^*:_", "");
        out.write("<div id=\"tree" + id + "\"></div>");
        out.write("<script type=\"text/javascript\">\n");
        out.write("var tree" + id + ";\n");
        out.write("tree" + id + " = new YAHOO.widget.TreeView(\"tree" + id + "\");\n");
        out.write("var root = tree" + id + ".getRoot();\n");
        out.write("var n" + id + " = createYahooTreeNode(root, \"" + getNodeRefString() + "\", " +
                "\"" + getTitle() + "\", \"space-icon-default\", false, false, "
                + getChildrenCount() + ", " +
                +getLinkDepth() + ");\n");
        out.write("tree" + id + ".subscribe('collapse', informOfCollapse);\n");
        out.write("tree" + id + ".draw();\n");
        out.write("tree" + id + ".setDynamicLoad(loadDataForNode);\n");
        out.write("</script>\n");
    }

    @Override
    public String getFamily() {
        return YahooTreeItem.class.getCanonicalName();
    }

    /**
     * Return the depth of link in current menu structure
     * 
     * @param link The ActionLink to test
     * @return int distance to the nearest UIMenuComponent component
     */
    private int getLinkDepth() {
        UIComponent parent = getParent();
        int depth = 0;
        while (parent != null) {
            if (parent instanceof UIMenuComponent) {
                break;
            }
            parent = parent.getParent();
            depth++;
        }
        return depth;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public String getNodeRefString() {
        if (nodeRef != null) {
            return nodeRef.toString();
        }
        return "";
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getChildrenCount() {
        return isShowChildrenCount() ? childrenCount : -1;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public boolean isShowChildrenCount() {
        return showChildrenCount;
    }

    public void setShowChildrenCount(boolean showChildrenCount) {
        this.showChildrenCount = showChildrenCount;
    }
}
