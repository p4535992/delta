package ee.webmedia.alfresco.menu.ui.component;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.SelfRenderingComponent;

public class MenuItemWrapper extends SelfRenderingComponent {

    private boolean dropdownWrapper = false, skinnable = false, expanded = false, plain = false;
    private String submenuId;

    @Override
    public String getFamily() {
        return MenuItemWrapper.class.getCanonicalName();
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter out = context.getResponseWriter();

        if (isPlain()) {
            out.write("<li>");
            return;
        }

        if (isDropdownWrapper()) {
            out.write("<li class=\"dropdown");
            if (isExpanded()) {
                out.write(" expanded");
            }
            out.write("\">");
        } else {
            if (isSkinnable()) {
                out.write("<div id=\"");
            } else {
                out.write("<ul id=\"");
            }
            out.write(getSubmenuId());
            if (isExpanded()) {
                out.write("\" style=\"display:block;\">");
            } else {
                out.write("\" style=\"display:none;\">");
            }
            if (isSkinnable()) {
                out.write("<span></span><div><ul>");
            }
        }
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        for (Object component : getChildren()) {
            Utils.encodeRecursive(context, (UIComponent) component);
        }
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter out = context.getResponseWriter();

        if (isPlain()) {
            out.write("</li>");
            return;
        }

        if (isDropdownWrapper()) {
            out.write("</li>");
        } else {
            if (isSkinnable()) {
                out.write("</ul></div></div>");
            } else {
                out.write("</ul>");
            }
        }
    }

    // TODO Save and restore state?

    public boolean isDropdownWrapper() {
        return dropdownWrapper;
    }

    public void setDropdownWrapper(boolean dropdownWrapper) {
        this.dropdownWrapper = dropdownWrapper;
    }

    public boolean isSkinnable() {
        return skinnable;
    }

    public void setSkinnable(boolean skinnable) {
        this.skinnable = skinnable;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public String getSubmenuId() {
        return (submenuId != null) ? submenuId : "";
    }

    public void setSubmenuId(String submenuId) {
        this.submenuId = submenuId;
    }

    public boolean isPlain() {
        return plain;
    }

    public void setPlain(boolean plain) {
        this.plain = plain;
    }

}
