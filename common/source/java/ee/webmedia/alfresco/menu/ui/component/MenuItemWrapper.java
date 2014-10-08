package ee.webmedia.alfresco.menu.ui.component;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.SelfRenderingComponent;
import org.apache.commons.lang.StringUtils;

public class MenuItemWrapper extends SelfRenderingComponent {

    private boolean dropdownWrapper = false, skinnable = false, expanded = false, plain = false;
    private String submenuId;
    private String menuId;

    @Override
    public Object saveState(FacesContext context) {
        Object values[] = new Object[7];
        values[0] = super.saveState(context);
        values[1] = dropdownWrapper;
        values[2] = skinnable;
        values[3] = expanded;
        values[4] = plain;
        values[5] = submenuId;
        values[6] = menuId;
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
        menuId = (String) values[6];
    }

    @Override
    public String getFamily() {
        return MenuItemWrapper.class.getCanonicalName();
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter out = context.getResponseWriter();

        if (isPlain()) {
            out.write("<li");
            if (getMenuId() != null) {
                out.write(" menuitemid=\"");
                out.write(getMenuId());
                out.write("\"");
            }
            String styleClass = (String) getAttributes().get("styleClass");
            if (StringUtils.isNotEmpty(styleClass)) {
                out.write(" class=\"");
                out.write(styleClass);
                out.write("\"");
            }
            out.write(">");
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

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

}
