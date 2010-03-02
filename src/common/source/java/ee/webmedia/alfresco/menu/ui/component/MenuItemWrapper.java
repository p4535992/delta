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
    public Object saveState(FacesContext context) {
        Object values[] = new Object[6];
        values[0] = super.saveState(context);
        values[1] = dropdownWrapper;
        values[2] = skinnable;
        values[3] = expanded;
        values[4] = plain;
        values[5] = submenuId;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        dropdownWrapper = (Boolean) values[1];
        skinnable = (Boolean) values[2];
        expanded = (Boolean) values[3];
        plain = (Boolean) values[4];
        submenuId = (String) values[5];
    }

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
