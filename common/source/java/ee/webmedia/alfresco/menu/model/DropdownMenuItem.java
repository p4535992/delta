<<<<<<< HEAD
package ee.webmedia.alfresco.menu.model;

import static org.apache.commons.lang.StringUtils.startsWith;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.menu.ui.component.MenuItemWrapper;
import ee.webmedia.alfresco.menu.ui.component.MenuRenderer;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent.ClearViewStackActionListener;
import ee.webmedia.alfresco.orgstructure.amr.service.RSService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Kaarel Jõgeva
 */
@XStreamAlias("dropdown")
public class DropdownMenuItem extends MenuItem {

    @XStreamOmitField
    private static final long serialVersionUID = 0L;
    public static final String DROPDOWN = "dropdown";
    public static final String ATTRIBUTE_XPATH = "xPath";
    public static final String ATTRIBUTE_NODEREF = "nodeRef";
    public static final String ATTRIBUTE_STORE = "store";
    @XStreamAsAttribute
    private boolean expanded;
    @XStreamAsAttribute
    private boolean temporary;
    @XStreamAsAttribute
    private boolean hover;
    @XStreamAsAttribute
    private boolean skinnable;
    @XStreamAsAttribute
    private boolean browse;
    @XStreamAlias("submenu-id")
    private String submenuId;
    @XStreamAlias("xpath")
    private String xPath;
    private String store;
    @XStreamOmitField
    private NodeRef nodeRef;
    @XStreamAlias("child-filter")
    private String childFilter;
    private String transientOrderString;

    public DropdownMenuItem() {
        super();
    }

    @Override
    public UIComponent createComponent(FacesContext context, String id, UserService userService, WorkflowService workflowService, EInvoiceService einvoiceService,
            RSService rsService) {
        return createComponent(context, id, userService, workflowService, einvoiceService, rsService, true);
    }

    @Override
    public UIComponent createComponent(FacesContext context, String id, UserService userService, WorkflowService workflowService, EInvoiceService einvoiceService,
            RSService rsService,
            boolean createChildren) {
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
        link.addActionListener(new ClearViewStackActionListener());

        if (getTitle() == null) {
            setTitle(I18NUtil.getMessage(getTitleId()));
        }
        link.setValue(getTitle(startsWith(id, MenuRenderer.SECONDARY_MENU_PREFIX) || startsWith(id, MenuBean.SHORTCUT_MENU_ITEM_PREFIX)));

        link.setTooltip(getTitle());
        link.setAction(new ConstantMethodBinding(getOutcome()));
        if (StringUtils.isNotBlank(getActionListener())) {
            link.setActionListener(application.createMethodBinding(getActionListener(), new Class[] { javax.faces.event.ActionEvent.class }));
        }

        if (getParams() != null) {
            for (Entry<String, String> entry : getParams().entrySet()) {
                addParameter(context, link, entry.getKey(), entry.getValue());
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> attr = link.getAttributes();
        attr.put(DropdownMenuItem.DROPDOWN, Boolean.TRUE);
        if (getXPath() != null) {
            attr.put(ATTRIBUTE_XPATH, getXPath());
        }
        if (getStore() != null) {
            attr.put(ATTRIBUTE_STORE, getStore());
        }
        if (getNodeRef() != null) {
            attr.put(ATTRIBUTE_NODEREF, getNodeRef());
        }

        if (isBrowse()) {
            // avoid setting on-click
        } else if (isTemporary()) {
            link.setOnclick("_toggleTempMenu(event, '" + getSubmenuId() + "'); return false;");
        } else if (isHover()) {
            attr.put("styleClass", "dropdown-hover");
            link.setOnclick("return false;");
        } else {
            link.setOnclick("_togglePersistentMenu(event, '" + getSubmenuId() + "'); return false;");
        }
        // Check if MenuItem should be initially hidden
        if (StringUtils.isNotBlank(getHidden())) {
            boolean hideIt = getHidden().startsWith("#{")
                    ? (Boolean) application.createMethodBinding(getHidden(), new Class[] { String.class }).invoke(context, new Object[] { getId() }) //
                    : Boolean.valueOf(getHidden());

            wrapper.setRendered(!hideIt);
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrapper.getChildren();
        children.add(link);

        if (createChildren) {
            MenuItemWrapper childrenWrapper = (MenuItemWrapper) createChildrenComponents(context, id, userService, workflowService, einvoiceService, rsService);
            if (childrenWrapper != null) {
                childrenWrapper.setDropdownWrapper(false);
                childrenWrapper.setSkinnable(isSkinnable());
                childrenWrapper.setExpanded(isExpanded());
                children.add(childrenWrapper);
            }
            if (StringUtils.isBlank(getOutcome()) && StringUtils.isBlank(getActionListener()) && (childrenWrapper == null || childrenWrapper.getChildCount() == 0)) {
                return null;
            }
        }

        return wrapper;
    }

    public UIComponent createChildrenComponents(FacesContext context, String parentId, UserService userService, WorkflowService workflowService, EInvoiceService einvoiceService,
            RSService rsService) {
        MenuItemWrapper wrapper = (MenuItemWrapper) context.getApplication().createComponent(MenuItemWrapper.class.getCanonicalName());
        FacesHelper.setupComponentId(context, wrapper, null);
        wrapper.setDropdownWrapper(true);
        wrapper.setSubmenuId(getSubmenuId());

        int i = 0;
        String id = parentId + UIMenuComponent.VALUE_SEPARATOR;
        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrapper.getChildren();
        if (getSubItems() != null) {
            for (MenuItem item : getSubItems()) {
                if (isRestricted() && !hasPermissions(userService)) {
                    continue;
                }

                UIComponent childItem;
                childItem = item.createComponent(context, id + i, userService, workflowService, einvoiceService, rsService);

                if (childItem != null) {
                    children.add(childItem);
                }
                i++;
            }
        }

        return wrapper;
    }

    public void toggle() {
        setExpanded(!isExpanded());
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

    public boolean isBrowse() {
        return browse;
    }

    public void setBrowse(boolean browse) {
        this.browse = browse;
    }

    public String getXPath() {
        return xPath;
    }

    public void setXPath(String xPath) {
        this.xPath = xPath;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getChildFilter() {
        return childFilter;
    }

    public void setChildFilter(String childFilter) {
        this.childFilter = childFilter;
    }

    public String getTransientOrderString() {
        return transientOrderString;
    }

    public void setTransientOrderString(String transientOrderString) {
        this.transientOrderString = transientOrderString;
    }

}
=======
package ee.webmedia.alfresco.menu.model;

import static org.apache.commons.lang.StringUtils.startsWith;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.menu.ui.component.MenuItemWrapper;
import ee.webmedia.alfresco.menu.ui.component.MenuRenderer;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent.ClearViewStackActionListener;
import ee.webmedia.alfresco.orgstructure.amr.service.RSService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

@XStreamAlias("dropdown")
public class DropdownMenuItem extends MenuItem {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DropdownMenuItem.class);

    @XStreamOmitField
    private static final long serialVersionUID = 0L;
    public static final String DROPDOWN = "dropdown";
    public static final String ATTRIBUTE_XPATH = "xPath";
    public static final String ATTRIBUTE_NODEREF = "nodeRef";
    public static final String ATTRIBUTE_STORE = "store";
    @XStreamAsAttribute
    private boolean expanded;
    @XStreamAsAttribute
    private boolean temporary;
    @XStreamAsAttribute
    private boolean hover;
    @XStreamAsAttribute
    private boolean skinnable;
    @XStreamAsAttribute
    private boolean browse;
    @XStreamAlias("submenu-id")
    private String submenuId;
    @XStreamAlias("xpath")
    private String xPath;
    private String store;
    @XStreamOmitField
    private NodeRef nodeRef;
    @XStreamAlias("child-filter")
    private String childFilter;
    private String transientOrderString;

    public DropdownMenuItem() {
        super();
    }

    @Override
    public UIComponent createComponent(FacesContext context, String id, UserService userService, WorkflowService workflowService, EInvoiceService einvoiceService,
            RSService rsService) {
        return createComponent(context, id, userService, workflowService, einvoiceService, rsService, true);
    }

    @Override
    public UIComponent createComponent(FacesContext context, String id, UserService userService, WorkflowService workflowService, EInvoiceService einvoiceService,
            RSService rsService,
            boolean createChildren) {
        boolean isMyTasksMenu = isMyTasksMenu();
        if (isRestricted() && !hasPermissions(userService)) {
            if (isMyTasksMenu) {
                log.debug("Menu error; menuItem menu_my_tasks is not rendered");
            }
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
        link.addActionListener(new ClearViewStackActionListener());

        if (getTitle() == null) {
            setTitle(I18NUtil.getMessage(getTitleId()));
        }
        link.setValue(getTitle(startsWith(id, MenuRenderer.SECONDARY_MENU_PREFIX) || startsWith(id, MenuBean.SHORTCUT_MENU_ITEM_PREFIX)));

        link.setTooltip(getTitle());
        link.setAction(new ConstantMethodBinding(getOutcome()));
        if (StringUtils.isNotBlank(getActionListener())) {
            link.setActionListener(application.createMethodBinding(getActionListener(), new Class[] { javax.faces.event.ActionEvent.class }));
        }

        if (getParams() != null) {
            for (Entry<String, String> entry : getParams().entrySet()) {
                addParameter(context, link, entry.getKey(), entry.getValue());
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> attr = link.getAttributes();
        attr.put(DropdownMenuItem.DROPDOWN, Boolean.TRUE);
        if (getXPath() != null) {
            attr.put(ATTRIBUTE_XPATH, getXPath());
        }
        if (getStore() != null) {
            attr.put(ATTRIBUTE_STORE, getStore());
        }
        if (getNodeRef() != null) {
            attr.put(ATTRIBUTE_NODEREF, getNodeRef());
        }

        if (isBrowse()) {
            // avoid setting on-click
        } else if (isTemporary()) {
            link.setOnclick("_toggleTempMenu(event, '" + getSubmenuId() + "'); return false;");
        } else if (isHover()) {
            attr.put("styleClass", "dropdown-hover");
            link.setOnclick("return false;");
        } else {
            link.setOnclick("_togglePersistentMenu(event, '" + getSubmenuId() + "'); return false;");
        }
        // Check if MenuItem should be initially hidden
        if (StringUtils.isNotBlank(getHidden())) {
            boolean hideIt = getHidden().startsWith("#{")
                    ? (Boolean) application.createMethodBinding(getHidden(), new Class[] { String.class }).invoke(context, new Object[] { getId() }) //
                    : Boolean.valueOf(getHidden());

            wrapper.setRendered(!hideIt);
            if (hideIt && isMyTasksMenu) {
                log.debug("Menu error; menuItem menu_my_tasks is hidden");
            }
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrapper.getChildren();
        children.add(link);
        if (createChildren) {
            MenuItemWrapper childrenWrapper = (MenuItemWrapper) createChildrenComponents(context, id, userService, workflowService, einvoiceService, rsService);
            if (childrenWrapper != null) {
                childrenWrapper.setDropdownWrapper(false);
                childrenWrapper.setSkinnable(isSkinnable());
                childrenWrapper.setExpanded(isExpanded());
                children.add(childrenWrapper);
            }
            if (StringUtils.isBlank(getOutcome()) && StringUtils.isBlank(getActionListener()) && (childrenWrapper == null || childrenWrapper.getChildCount() == 0)) {
                if (isMyTasksMenu) {
                    log.debug("Menu error; menuItem menu_my_tasks is not rendered because it has no children");
                }
                return null;
            }
        }

        return wrapper;
    }

    public UIComponent createChildrenComponents(FacesContext context, String parentId, UserService userService, WorkflowService workflowService, EInvoiceService einvoiceService,
            RSService rsService) {
        MenuItemWrapper wrapper = (MenuItemWrapper) context.getApplication().createComponent(MenuItemWrapper.class.getCanonicalName());
        FacesHelper.setupComponentId(context, wrapper, null);
        wrapper.setDropdownWrapper(true);
        wrapper.setSubmenuId(getSubmenuId());

        int i = 0;
        String id = parentId + UIMenuComponent.VALUE_SEPARATOR;
        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrapper.getChildren();
        boolean isMyTasksMenu = isMyTasksMenu();
        if (getSubItems() != null) {
            int addedChildren = 0;
            for (MenuItem item : getSubItems()) {
                if (isRestricted() && !hasPermissions(userService)) {
                    continue;
                }

                UIComponent childItem = item.createComponent(context, id + i, userService, workflowService, einvoiceService, rsService);

                if (childItem != null) {
                    children.add(childItem);
                    addedChildren++;
                }
                i++;
            }
            if (isMyTasksMenu && addedChildren == 0) {
                log.debug("Menu error; menuItem menu_my_tasks no child components were created");
            }
        } else if (isMyTasksMenu) {
            log.debug("Menu error; menuItem menu_my_tasks is not rendered because subitems == null");
        }

        return wrapper;
    }

    @Override
    public List<MenuItem> getSubItems() {
        List<MenuItem> subItems = super.getSubItems();
        if (isMyTasksMenu() && (subItems == null || subItems.size() == 0)) {
            log.debug("Menu error; menuItem menu_my_tasks subItems is null or empty");
        }
        return subItems;
    }

    @Override
    public void setSubItems(List<MenuItem> subItems) {
        super.setSubItems(subItems);
        if (isMyTasksMenu() && (subItems == null || subItems.isEmpty())) {
            log.debug("Menu error; menuItem menu_my_tasks is set null or empty");
        }
    }

    private boolean isMyTasksMenu() {
        boolean isMyTasksMenu = "menu_my_tasks".equals(getId());
        return isMyTasksMenu;
    }

    public void toggle() {
        setExpanded(!isExpanded());
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

    public boolean isBrowse() {
        return browse;
    }

    public void setBrowse(boolean browse) {
        this.browse = browse;
    }

    public String getXPath() {
        return xPath;
    }

    public void setXPath(String xPath) {
        this.xPath = xPath;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getChildFilter() {
        return childFilter;
    }

    public void setChildFilter(String childFilter) {
        this.childFilter = childFilter;
    }

    public String getTransientOrderString() {
        return transientOrderString;
    }

    public void setTransientOrderString(String transientOrderString) {
        this.transientOrderString = transientOrderString;
    }

}
>>>>>>> develop-5.1
