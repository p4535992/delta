package ee.webmedia.alfresco.menu.model;

import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.menu.ui.component.MenuItemWrapper;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * @author Kaarel Jõgeva
 */
@XStreamAlias("dropdown")
public class DropdownMenuItem extends MenuItem {

    @XStreamOmitField
    private static final long serialVersionUID = 0L;
    public static final String DROPDOWN = "dropdown";
    @XStreamAsAttribute
    private boolean expanded;
    @XStreamAsAttribute
    private boolean temporary;
    @XStreamAsAttribute
    private boolean hover;
    @XStreamAsAttribute
    private boolean skinnable;
    @XStreamAlias("submenu-id")
    private String submenuId;

    /**
     * Constructor for {@link DropdownMenuItem}
     * 
     * @param title
     * @param outcome
     */
    public DropdownMenuItem(String title, String outcome) {
        super(title, outcome);
    }

    /**
     * Constructor for {@link MenuItem}
     * 
     * @param title
     * @param outcome
     * @param children
     */
    public DropdownMenuItem(String title, String outcome, List<MenuItem> children) {
        super(title, outcome, children);
    }

    @Override
    public UIComponent createComponent(FacesContext context, String id, UserService userService) {
        return createComponent(context, id, userService, null);
    }

    @Override
    public UIComponent createComponent(FacesContext context, String id, UserService userService, DocumentTypeService docTypeService) {
        if (isRestricted() && !hasPermissions(userService)) {
            return null;
        }

        javax.faces.application.Application application = context.getApplication();

        MenuItemWrapper wrapper = (MenuItemWrapper) application.createComponent(MenuItemWrapper.class.getCanonicalName());
        wrapper.setDropdownWrapper(true);
        wrapper.setExpanded(isExpanded());
        wrapper.setSubmenuId(getSubmenuId());

        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        FacesHelper.setupComponentId(context, link, id);
        link.setValue(getTitle());
        link.setTooltip(getTitle());
        @SuppressWarnings("unchecked")
        Map<String, Object> attr = link.getAttributes();
        attr.put(DropdownMenuItem.DROPDOWN, Boolean.TRUE);

        if (isTemporary()) {
            link.setOnclick("_toggleMenu(event, '" + getSubmenuId() + "')");
        } else if (isHover()) {
            attr.put("styleClass", "dropdown-hover");
            link.setHref("#");
        } else {
            link.setOnclick("_togglePersistentMenu(event, '" + getSubmenuId() + "')");
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrapper.getChildren();
        children.add(link);

        MenuItemWrapper childrenWrapper = (MenuItemWrapper) createChildrenComponents(context, id, userService, docTypeService);
        if (childrenWrapper != null) {
            childrenWrapper.setDropdownWrapper(false);
            childrenWrapper.setSkinnable(isSkinnable());
            childrenWrapper.setExpanded(isExpanded());
            children.add(childrenWrapper);
        }

        return wrapper;
    }

    public UIComponent createChildrenComponents(FacesContext context, String parentId, UserService userService, DocumentTypeService docTypeService) {
        MenuItemWrapper wrapper = (MenuItemWrapper) context.getApplication().createComponent(MenuItemWrapper.class.getCanonicalName());
        FacesHelper.setupComponentId(context, wrapper, null);
        wrapper.setDropdownWrapper(true);
        wrapper.setSubmenuId(getSubmenuId());

        int i = 0;
        String id = parentId + "_";
        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrapper.getChildren();
        for (MenuItem item : getSubItems()) {
            if (isRestricted() && !hasPermissions(userService)) {
                continue;
            }

            UIComponent childItem;
            childItem = item.createComponent(context, id + i, userService, docTypeService);

            if (childItem != null) {
                children.add(childItem);
            }
            i++;
        }

        return wrapper;
    }

    public String getSubmenuId() {
        return submenuId;
    }

    public void setSubmenuId(String submenuId) {
        this.submenuId = submenuId;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
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

    public boolean isHover() {
        return hover;
    }

    public void setHover(boolean hover) {
        this.hover = hover;
    }
}
