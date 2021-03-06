package ee.webmedia.alfresco.menu.ui.component;

import static ee.webmedia.alfresco.common.web.BeanHelper.getApplicationConstantsBean;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.context.servlet.ServletFacesContextImpl;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemFilter;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.orgstructure.amr.service.RSService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;

public class MenuRenderer extends BaseRenderer {

    private static final String TREE_SCRIPTS_WRITTEN = "_alfTreeScripts";
    public static final String SECONDARY_MENU_PREFIX = "sm";
    public static final String PRIMARY_MENU_PREFIX = "pm";

    private UserService userService;
    private WorkflowConstantsBean workflowConstantsBean;
    private MenuService menuService;
    private EInvoiceService einvoiceService;

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        // Prepare scrolling info
        MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
        if (StringUtils.isBlank(menuBean.getScrollToY()) || !menuBean.getScrollToY().equals("0")) {
            @SuppressWarnings("cast")
            String scrollToY = (String) context.getExternalContext().getRequestParameterMap().get("scrollToY");
            menuBean.setScrollToY(scrollToY);
        } else {
            menuBean.setScrollToY(null);
        }

        writeScripts(context);
        context.getResponseWriter().write("<ul>");
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {

        UIMenuComponent menuComponent = (UIMenuComponent) component;
        MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
        Menu menu = menuBean.getMenu();
        boolean primary = ((Boolean) menuComponent.getAttributes().get(UIMenuComponent.PRIMARY_ATTRIBUTE_KEY));

        String activeItemId = menuBean.getActiveItemId();

        if (primary) {
            renderPrimary(context, activeItemId, menu, component);
        } else {
            renderSecondary(context, activeItemId, menu, component);
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        out.write("</ul>");

        UIMenuComponent menuComponent = (UIMenuComponent) component;
        boolean primary = ((Boolean) menuComponent.getAttributes().get(UIMenuComponent.PRIMARY_ATTRIBUTE_KEY));
        if (!primary && !getApplicationConstantsBean().isMyTasksAndDocumentsMenuClosed()
                && Integer.parseInt(BeanHelper.getMenuBean().getActiveItemId()) == MenuBean.MY_TASKS_AND_DOCUMENTS_ID) {
            out.write("<script type=\"text/javascript\">\n");
            out.write("$jQ(document).ready(function() {\n");
            out.write("updateMenuItemsCount();\n");
            out.write("});\n");
            out.write("</script>\n");
        }
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    /**
     * Renders scripts necessary for BrowseMenuItems
     *
     * @param context
     * @param menu
     * @throws IOException
     */
    static void writeScripts(FacesContext context) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        Object present = requestMap.get(TREE_SCRIPTS_WRITTEN);
        ResponseWriter out = context.getResponseWriter();
        if (present == null) {
            if (!isScrollDisabled(context)) {
                MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);

                final boolean scrollToAnchor = StringUtils.isNotEmpty(menuBean.getScrollToAnchor());
                if (scrollToAnchor || StringUtils.isNotEmpty(menuBean.getScrollToY())) {
                    final String scrollTo = scrollToAnchor ? "'" + menuBean.getScrollToAnchor() + "'" : menuBean.getScrollToY();
                    StringBuilder sb = new StringBuilder("<script type=\"text/javascript\">")
                            .append("$jQ(document).ready(function(){")
                            .append("$jQ.scrollTo(")
                            .append(scrollTo)
                            .append(")});")
                            .append("</script>");

                    out.write(sb.toString());
                }
            }
            requestMap.put(TREE_SCRIPTS_WRITTEN, Boolean.TRUE);
        }
    }

    private static boolean isScrollDisabled(FacesContext context) {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        final Boolean scrollDisabled = (Boolean) requestMap.get(ServletFacesContextImpl.SCROLL_DISABLED);
        return scrollDisabled != null && scrollDisabled;
    }

    /**
     * Renders the sub menu for currently active page section.
     *
     * @param context
     * @param activeItemId main menu active item id
     * @param menu menu component that is being rendered
     * @param component parent component
     * @throws IOException
     */
    private void renderSecondary(FacesContext context, String activeItemId, Menu menu, UIComponent component) throws IOException {

        if (activeItemId == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        children.clear();

        List<MenuItem> menuItems = menu.getSubItems().get(Integer.parseInt(activeItemId)).getSubItems();
        if (menuItems != null) {

            int i = 0;
            String id = SECONDARY_MENU_PREFIX;
            for (MenuItem item : menuItems) {
                UIComponent menuItem = item.createComponent(context, id + i, getUserService(), getWorkflowConstantsBean(), getRsService());
                if (menuItem != null) {
                    children.add(menuItem);
                }
                i++;
            }

            for (Object o : children) {
                Utils.encodeRecursive(context, (UIComponent) o);
            }
        }
    }

    protected RSService getRsService() {
        return BeanHelper.getRSService();
    }

    /**
     * Renders the primary menu; usually top level menu with different site sections
     *
     * @param context
     * @param activeItemid currently active menu item id
     * @param menu menu component that is being rendered
     * @param component parent component
     * @throws IOException
     */
    private void renderPrimary(FacesContext context, String activeItemid, Menu menu, UIComponent component) throws IOException {

        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        children.clear();

        List<MenuItem> menuItems = menu.getSubItems();
        int i = 0;
        String id = PRIMARY_MENU_PREFIX;
        MenuItemFilter filter = null;
        Map<String, MenuItemFilter> menuItemFilters = getMenuService().getMenuItemFilters();
        for (MenuItem item : menuItems) {
            if (activeItemid.equals(Integer.toString(i))) {
                UIComponent menuItem = item.createComponent(context, id + i, true, getUserService(), getWorkflowConstantsBean(), getRsService(), false);
                if (menuItem != null) {
                    children.add(menuItem);
                }
            } else if (item instanceof DropdownMenuItem) { // Only the drop-down item in primary menu needs the DocumentTypeService
                if (((DropdownMenuItem) item).getChildFilter() != null && menuItemFilters != null
                        && menuItemFilters.containsKey(((DropdownMenuItem) item).getChildFilter())) {
                    filter = menuItemFilters.get(((DropdownMenuItem) item).getChildFilter());
                }

                // Substituting users are not allowed to create new documents
                if (filter != null && !filter.passesFilter(item, null)) {
                    filter.openItemActionsForType((DropdownMenuItem) item, null, null);
                    filter = null; // reset for next cycle
                }

                UIComponent menuItem = item.createComponent(context, id + i, getUserService(), getWorkflowConstantsBean(), getRsService());
                if (menuItem != null) {
                    children.add(removeTooltipRecursive(menuItem));
                }
            } else {
                UIComponent menuItem = item.createComponent(context, id + i, getUserService(), getWorkflowConstantsBean(), getRsService());
                if (menuItem != null) {
                    children.add(menuItem);
                }
            }
            i++;
        }

        for (Object o : children) {
            Utils.encodeRecursive(context, (UIComponent) o);
        }

    }

    /**
     * Removes tooltips so they don't block the view.
     *
     * @param menuItem
     * @return
     */
    private UIComponent removeTooltipRecursive(UIComponent menuItem) {
        UIActionLink al;

        if (menuItem instanceof MenuItemWrapper) {
            if (menuItem.getChildCount() > 0) {
                @SuppressWarnings("unchecked")
                final List<UIComponent> childList = menuItem.getChildren();
                if (childList.get(0) instanceof UIActionLink) {
                    al = (UIActionLink) childList.get(0);
                    al.setTooltip("");
                }
                int children = menuItem.getChildCount();
                for (int i = 0; i < children; i++) {
                    removeTooltipRecursive(childList.get(i));
                }
            }
        } else if (menuItem instanceof UIActionLink) {
            al = (UIActionLink) menuItem;
            al.setTooltip("");
        } else {
            throw new RuntimeException("UIComponent must be either MenuItemWrapper with UIActionLink child or UIActionLink itself.");
        }

        return menuItem;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    protected MenuService getMenuService() {
        if (menuService == null) {
            menuService = (MenuService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(MenuService.BEAN_NAME);
        }
        return menuService;
    }

    public EInvoiceService getEinvoiceService() {
        if (einvoiceService == null) {
            einvoiceService = (EInvoiceService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(EInvoiceService.BEAN_NAME);
        }
        return einvoiceService;
    }

    public WorkflowConstantsBean getWorkflowConstantsBean() {
        if (workflowConstantsBean == null) {
            workflowConstantsBean = BeanHelper.getWorkflowConstantsBean();
        }
        return workflowConstantsBean;
    }

}
