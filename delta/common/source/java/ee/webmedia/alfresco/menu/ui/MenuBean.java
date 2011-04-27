package ee.webmedia.alfresco.menu.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.faces.application.Application;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.IDialogBean;
import org.alfresco.web.config.DialogsConfigElement.DialogConfig;
import org.alfresco.web.config.WizardsConfigElement.WizardConfig;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIOutputText;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.ClearStateNotificationHandler;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.component.MenuItemWrapper;
import ee.webmedia.alfresco.menu.ui.component.MenuRenderer;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Kaarel Jõgeva
 */
public class MenuBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MenuBean.class);

    public static final String BEAN_NAME = "MenuBean";
    public static final String UPDATE_TREE_ACTTIONLISTENER = "#{MenuBean.updateTree}";
    public static final String COUNT = "count";

    // NB! this order defined in menu-structure.xml
    // If you change menu item order in menu-structure.xml, also update constants below!
    public static final int MY_TASKS_AND_DOCUMENTS_ID = 0;
    public static final int DOCUMENT_REGISTER_ID = 1;
    public static final int CREATE_NEW_DOCUMENT = 5;
    public static final int MY_DOCUMENTS_ID = 2;

    private transient HtmlPanelGroup shortcutsPanelGroup;
    private transient HtmlPanelGroup breadcrumb;

    private transient MenuService menuService;
    private transient GeneralService generalService;
    private transient UserService userService;
    private transient WorkflowService workflowService;
    private transient EInvoiceService einvoiceService;

    private Menu menu;
    private int updateCount = 0;
    private String lastLinkId;
    private NodeRef linkNodeRef;
    private List<String> shortcuts;
    private String activeItemId = "0";
    private String clickedId = "";
    private Stack<String> stateList = new Stack<String>();
    private String scrollToAnchor;
    private String scrollToY;

    /**
     * Watch out for a gotcha moment! If event comes from ActionLink or CommandButton,
     * handleNavigation is called for a second time and it resets this setting!
     * 
     * @param anchor ID of the HTML element in the form of "#my-panel"
     */
    public void scrollToAnchor(String anchor) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getApplication().getNavigationHandler().handleNavigation(context, null, anchor);
    }

    public void resetBreadcrumb() {
        stateList.clear();
    }

    public void addBreadcrumbItem(DialogConfig config) {
        String title = "";

        String beanName = config.getManagedBean();
        Object bean = FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), beanName);
        IDialogBean dialog = null;
        if (bean instanceof IDialogBean) {
            dialog = (IDialogBean) bean;
        } else {
            throw new AlfrescoRuntimeException("Failed to start dialog as managed bean '" + beanName +
                    "' does not implement the required IDialogBean interface");
        }

        try {
            final String containerTitle = dialog.getContainerTitle();
            if (containerTitle != null) {
                title = containerTitle;
            } else if (config.getTitle() != null) {
                title = config.getTitle();
            } else {
                title = MessageUtil.getMessage(config.getTitleId());
            }
        } catch (NullPointerException npe) {
            // XXX is any action necessary?
        }

        addBreadcrumbItem(title);
    }

    public void addBreadcrumbItem(WizardConfig wizard) {
        String title = "";
        if (wizard.getTitle() != null) {
            title = wizard.getTitle();
        } else if (wizard.getTitleId() != null) {
            title = MessageUtil.getMessage(wizard.getTitleId());
        }

        addBreadcrumbItem(title);
    }

    @SuppressWarnings("unchecked")
    public void addBreadcrumbItem(String title) {
        int i = 0;
        List<String> leftMenuTitles = new ArrayList<String>();
        leftMenuTitles.add(MessageUtil.getMessage("volume_list"));
        leftMenuTitles.add(MessageUtil.getMessage("series_list"));
        leftMenuTitles.add(MessageUtil.getMessage("document_list"));
        for (String listTitle : stateList) {
            if (leftMenuTitles.contains(title) && listTitle.equals(title)) { // left side menu provides functionality for backwards navigation.
                stateList.setSize(i);
                Stack viewStack = (Stack) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get(UIMenuComponent.VIEW_STACK);
                if (viewStack != null) {
                    viewStack.setSize(i + 1);
                }
                break;
            }
            i++;
        }
        stateList.push(title);
    }

    public void removeBreadcrumbItem() {
        removeBreadcrumbItems(1);

    }

    public void removeBreadcrumbItems(int numberToRemove) {
        for (int i = 0; i < numberToRemove; i++) {
            if (!stateList.isEmpty()) {
                stateList.pop();
            }
        }
    }

    public Stack<String> getStateList() {
        return stateList;
    }

    public void setStateList(Stack<String> stateList) {
        this.stateList = stateList;
    }

    public void setBreadcrumb(HtmlPanelGroup breadcrumb) {
        this.breadcrumb = breadcrumb;
    }

    public HtmlPanelGroup getBreadcrumb() {
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();

        if (breadcrumb == null) {
            breadcrumb = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            breadcrumb.setId("breadcrumb");
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = breadcrumb.getChildren();
        children.clear();
        // TODO - this would be nice to have (cannot clear view stack)
        // children.add(getActiveMainMenuItem().createComponent(FacesContext.getCurrentInstance(), "id" + GUID.generate(), getUserService(), false, true));
        children.add(generateText(getActiveMainMenuItem().getTitle(), false));

        Object[] items = stateList.toArray();
        int i = 1;
        for (Object item : items) {
            UIComponent component = null;
            if (items.length > i) {
                component = generateLink(application, (String) item, (items.length - i));
            } else {
                component = generateText((String) item, true);
            }
            children.add(component);
            i++;
        }

        return breadcrumb;
    }

    /**
     * @param title
     * @return
     */
    @SuppressWarnings("unchecked")
    private UIComponent generateText(String title, boolean styleClass) {
        UIComponent component = new UIOutputText();
        if (styleClass) {
            component.getAttributes().put("styleClass", "breadcrumb-link");
        }
        ((UIOutputText) component).setValue(title);
        return component;
    }

    @SuppressWarnings("unchecked")
    private UIActionLink generateLink(Application application, String title, int closeCount) {
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        link.setValue(title);
        link.getAttributes().put("styleClass", "breadcrumb-link");
        MethodBinding action = application.createMethodBinding("#{MenuBean.closeBreadcrumbItem}", UIActions.ACTION_CLASS_ARGS);
        link.setActionListener(action);

        UIParameter count = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
        count.setName(COUNT);
        count.setValue(closeCount);
        link.getChildren().add(count);

        link.getAttributes().put(COUNT, closeCount);
        return link;
    }

    public void closeBreadcrumbItem(ActionEvent event) {
        Integer closeCount = null;
        if (ActionUtil.hasParam(event, COUNT)) {
            closeCount = Integer.parseInt(ActionUtil.getParam(event, COUNT).toString());
        } else {
            return;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        context.getApplication().getNavigationHandler().handleNavigation(context, "closeBreadcrumbItem", "dialog:close[" + closeCount + "]");

    }

    public void processTaskItem(String... menuItemIds) {
        final Collection<String> menuItemsListToProcess;
        if (menuItemIds != null) {
            menuItemsListToProcess = new HashSet<String>(menuItemIds.length);
            menuItemsListToProcess.addAll(Arrays.asList(menuItemIds));
        } else {
            menuItemsListToProcess = null;
        }
        getMenuService().processTasks(menu, menuItemsListToProcess);
    }

    public void processTaskItems() {
        getMenuService().processTasks(menu);
    }

    public void updateTree(ActionEvent event) {
        final UIComponent link = event.getComponent();
        setLastLinkId(((UIActionLink) link).getId());

        // NOTE: In XML nodes are referenced by xPath, but since all child association names are with the same (function, series etc)
        // Therefore items generated at runtime should be referenced by NodeRef
        if (link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_NODEREF) != null) {
            linkNodeRef = (NodeRef) link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_NODEREF);
        } else if (link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_XPATH) != null) {
            String xpath = (String) link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_XPATH);

            // if store is specified, ask link node reference from specific store
            if (link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_STORE) != null) {
                StoreRef storeRef = new StoreRef((String) link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_STORE));
                linkNodeRef = getGeneralService().getNodeRef(xpath, storeRef);
            } else {
                linkNodeRef = getGeneralService().getNodeRef(xpath);
            }

        } else {
            log.error("NodeRef and xPath cannot be null at the same time on DropdownMenuItem!");
            throw new RuntimeException();
        }
        updateTree();
    }

    public void updateTree() {
        if (lastLinkId == null || linkNodeRef == null) {
            String msg = "MenuBean.updateTree() called, but info from last UIActionLink ActionEvent is missing!";
            log.error(msg);
            throw new RuntimeException(msg);
        }

        String[] path = lastLinkId.substring(MenuRenderer.SECONDARY_MENU_PREFIX.length()).split(UIMenuComponent.VALUE_SEPARATOR);

        MenuItem item = getActiveMainMenuItem();
        if (Integer.parseInt(activeItemId) == DOCUMENT_REGISTER_ID) {
            collapseMenuItems(item);
        } else if (Integer.parseInt(activeItemId) == MY_TASKS_AND_DOCUMENTS_ID) {
            collapseMenuItems(item.getSubItems().get(MY_DOCUMENTS_ID));
        }

        // Let's go to the clicked link
        for (String step : path) {
            if (item.getSubItems() != null) {
                if (item instanceof DropdownMenuItem) {
                    ((DropdownMenuItem) item).setExpanded(true); // Mark our trail
                }
                int index = Integer.parseInt(step);
                if (index < item.getSubItems().size()) {
                    item = item.getSubItems().get(index);
                } else {
                    // probably menu has changed so previous link is not valid any longer
                    item = null;
                    break;
                }
            } else if (path.length > 1 && item instanceof DropdownMenuItem) { // if necessary, fetch children
                DropdownMenuItem dropdownItem = ((DropdownMenuItem) item);
                NodeRef nodeRef = dropdownItem.getNodeRef();
                if (nodeRef == null) {
                    nodeRef = getGeneralService().getNodeRef(dropdownItem.getXPath());
                }
                if (item.getSubItems() == null) {
                    item.setSubItems(new ArrayList<MenuItem>());
                }
                for (NodeRef childItemRef : getMenuService().openTreeItem(dropdownItem, nodeRef)) {
                    DropdownMenuItem childItem = new DropdownMenuItem();
                    childItem.setChildFilter(dropdownItem.getChildFilter()); // By default pass on parent's filter. Can be overridden in setupTreeItem
                    getMenuService().setupTreeItem(childItem, childItemRef);
                    item.getSubItems().add(childItem);
                }
                Collections.sort(item.getSubItems(), new DropdownMenuComparator());
                dropdownItem.setExpanded(true);
                int index = Integer.parseInt(step);
                if (index < item.getSubItems().size()) {
                    item = dropdownItem.getSubItems().get(Integer.parseInt(step));
                } else {
                    // probably menu has changed so previous link is not valid any longer
                    item = null;
                    break;
                }
            }
        }

        if (item instanceof DropdownMenuItem) {
            DropdownMenuItem dd = (DropdownMenuItem) item;
            // When XML configuration doesn't specify any children, this list will be null!
            if (dd.getSubItems() == null) {
                dd.setSubItems(new ArrayList<MenuItem>());
            }
            dd.getSubItems().clear();

            // Toggle the link
            dd.setExpanded(!dd.isExpanded());
            if (!dd.isExpanded()) {
                return; // When hiding, we don't need to refresh children
            }

            log.debug("Fetching children for: " + dd.getTitle());
            // Decide what outcome is needed for children and load proper data
            List<NodeRef> children = getMenuService().openTreeItem(dd, linkNodeRef);
            if (children != null) {
                for (NodeRef child : children) {
                    DropdownMenuItem ddChild = new DropdownMenuItem();
                    ddChild.setActionListener(UPDATE_TREE_ACTTIONLISTENER);
                    ddChild.setNodeRef(child);
                    ddChild.setBrowse(true);
                    ddChild.setSubmenuId(lastLinkId);
                    ddChild.setChildFilter(dd.getChildFilter());
                    getMenuService().setupTreeItem(ddChild, child);
                    dd.getSubItems().add(ddChild);
                }
                Collections.sort(dd.getSubItems(), new DropdownMenuComparator());
            }
            dd.setExpanded(true);
        }
    }

    /**
     * Collapses other items, so user can understand where the heck he/she is...
     * 
     * @param item
     */
    public void collapseMenuItems(MenuItem item) {
        if (item == null) {
            item = menu.getSubItems().get(DOCUMENT_REGISTER_ID);
        }

        if (item.getSubItems() != null) {
            for (MenuItem dmi : item.getSubItems()) {
                if (dmi instanceof DropdownMenuItem) {
                    final DropdownMenuItem dropdownMenuItem = (DropdownMenuItem) dmi;
                    dropdownMenuItem.setExpanded(false);
                }
                collapseMenuItems(dmi);
            }
        }
    }

    public Menu getMenu() {
        if (getMenuService().getUpdateCount() != updateCount || menu == null) {
            log.debug("Fetching new menu structure from service.");
            reloadMenu(); // XXX - Somehow this makes it work... Although menu structure in service isn't modified.
            menu = getMenuService().getMenu();
            updateCount = getMenuService().getUpdateCount();
            if (lastLinkId != null && linkNodeRef != null) {
                updateTree();
            }
        }
        return menu;
    }

    public void reset() {
        menu = null;
        lastLinkId = null;
        linkNodeRef = null;
    }

    public MenuItem getActiveMainMenuItem() {
        return menu.getSubItems().get(Integer.parseInt(activeItemId));
    }

    public String getActiveItemId() {
        return activeItemId;
    }

    public void setActiveItemId(String activeItemId) {
        if (StringUtils.isNotBlank(activeItemId)) {
            this.activeItemId = activeItemId;
        }
    }

    private void setMenuItemId(String primaryId, String secondaryId) {
        setActiveItemId(primaryId);
        clickedId = secondaryId;
    }

    @SuppressWarnings("unchecked")
    public static void clearViewStack(String primaryId, String secondaryId) {
        FacesContext context = FacesContext.getCurrentInstance();

        // Clear the view stack, otherwise it would grow too big as the cancel button is hidden in some views
        // Later in the life-cycle the view where this action came from is added to the stack, so visible cancel buttons will function properly
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        sessionMap.put(UIMenuComponent.VIEW_STACK, new Stack());

        MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
        menuBean.setMenuItemId(primaryId, secondaryId);
        menuBean.resetBreadcrumb();

        // let the ClearStateNotificationHandler notify all the interested listeners
        ClearStateNotificationHandler clearStateNotificationHandler = (ClearStateNotificationHandler) FacesHelper.getManagedBean(context,
                ClearStateNotificationHandler.BEAN_NAME);
        clearStateNotificationHandler.notifyClearStateListeners();
    }

    public void clearViewStack(ActionEvent event) {
        String primaryId = ActionUtil.getParam(event, "primaryId");
        clearViewStack(primaryId, null);
    }

    public void reloadMenu() {
        getMenuService().reload();
    }

    public String getLastLinkId() {
        return lastLinkId;
    }

    public void setLastLinkId(String lastLinkId) {
        this.lastLinkId = lastLinkId;
    }

    public NodeRef getLinkNodeRef() {
        return linkNodeRef;
    }

    public void setLinkNodeRef(NodeRef linkNodeRef) {
        this.linkNodeRef = linkNodeRef;
    }

    // ========================================================================
    // =============================== SHORTCUTS ==============================
    // ========================================================================

    public List<String> getShortcuts() {
        if (shortcuts == null) {
            shortcuts = getMenuService().getShortcuts();
        }
        return shortcuts;
    }

    public HtmlPanelGroup getShortcutsPanelGroup() {
        return shortcutsPanelGroup;
    }

    public void setShortcutsPanelGroup(HtmlPanelGroup shortcutsPanelGroup) {
        this.shortcutsPanelGroup = shortcutsPanelGroup;
        if (shortcuts == null) {
            // First time during session
            generateShortcutLinks();
        }
    }

    private void generateShortcutLinks() {
        shortcutsPanelGroup.getChildren().clear();
        for (Iterator<String> i = getShortcuts().iterator(); i.hasNext();) {
            String menuItemId = i.next();
            if (!generateAndAddShortcut(menuItemId)) {
                i.remove();
            }
        }
    }

    public static MenuItem getMenuItemFromShortcut(String shortcut, Menu menu) {
        if (shortcut == null) {
            return null;
        }
        String[] path = getPathFromShortcut(shortcut);

        List<MenuItem> subItems = menu.getSubItems();
        MenuItem item = null;
        int index;
        for (String pathElem : path) {
            if (subItems == null) {
                return null;
            }
            try {
                index = Integer.parseInt(pathElem);
            } catch (NumberFormatException e) {
                return null;
            }
            if (index >= subItems.size()) {
                return null;
            }
            item = subItems.get(index);
            if (item == null) {
                return null;
            }
            subItems = item.getSubItems();
        }
        return item;
    }

    private boolean generateAndAddShortcut(String menuItemId) {
        FacesContext context = FacesContext.getCurrentInstance();
        Pair<MenuItem, String[]> menuItemAndPath = getMenuItemAndPathFromMenuItemId(menuItemId);
        if (menuItemAndPath == null) {
            return false;
        }
        MenuItem item = menuItemAndPath.getFirst();
        MenuItemWrapper wrapper = (MenuItemWrapper) item.createComponent(context, "shortcut-" + shortcutsPanelGroup.getChildCount(), getUserService(), getWorkflowService(),
                getEinvoiceService(), false);
        wrapper.setPlain(true);

        UIActionLink link = (UIActionLink) wrapper.getChildren().get(0);
        String shortcut = getShortcutFromPath(menuItemAndPath.getSecond());
        link.addActionListener(new ShortcutClickedActionListener(shortcut));

        String title = (String) link.getValue();
        if (title.endsWith(")")) {
            link.setValue(title.substring(0, title.lastIndexOf('(')));
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = shortcutsPanelGroup.getChildren();
        children.add(wrapper);
        return true;
    }

    private Pair<MenuItem, String[]> getMenuItemAndPathFromMenuItemId(String menuItemId) {
        List<String> path = new ArrayList<String>();
        MenuItem menuItem = getMenuItemById(menuItemId, menu.getSubItems(), path);
        if (menuItem == null) {
            return null;
        }
        Collections.reverse(path);
        return new Pair<MenuItem, String[]>(menuItem, path.toArray(new String[path.size()]));
    }

    private static MenuItem getMenuItemById(String menuItemId, List<MenuItem> subItems, List<String> path) {
        if (subItems == null) {
            return null;
        }
        for (int i = 0; i < subItems.size(); i++) {
            MenuItem item = subItems.get(i);
            if (item == null) {
                continue;
            }
            if (menuItemId.equals(item.getId())) {
                path.add(Integer.toString(i));
                return item;
            }
            item = getMenuItemById(menuItemId, item.getSubItems(), path);
            if (item != null) {
                path.add(Integer.toString(i));
                return item;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public int getShortcutAddable() {
        if (!StringUtils.contains(clickedId, ":sm")) {
            return 0;
        }

        Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        Stack viewStack = (Stack) sessionMap.get(UIMenuComponent.VIEW_STACK);
        if (viewStack.size() > 1) {
            return 0;
        }

        String[] path = getPathFromClickedId();
        String shortcut = getShortcutFromPath(path);
        String menuItemId = getMenuItemIdFromShortcut(shortcut);
        if (menuItemId == null) {
            return 0;
        }
        if (shortcuts.contains(menuItemId)) {
            return -1;
        }

        List<MenuItem> subItems = menu.getSubItems();
        MenuItem item = null;
        int index;
        for (String pathElem : path) {
            if (subItems == null) {
                return 0;
            }
            try {
                index = Integer.parseInt(pathElem);
            } catch (NumberFormatException e) {
                return 0;
            }
            if (index >= subItems.size()) {
                return 0;
            }
            item = subItems.get(index);
            if (item == null) {
                return 0;
            }
            if (item instanceof DropdownMenuItem) {
                if (StringUtils.isNotBlank(((DropdownMenuItem) item).getXPath())) {
                    return 0;
                }
            }
            subItems = item.getSubItems();
        }

        return 1;
    }

    private String getMenuItemIdFromShortcut(String shortcut) {
        return getMenuItemIdFromShortcut(shortcut, menu);
    }

    public static String getMenuItemIdFromShortcut(String shortcut, Menu menu) {
        MenuItem menuItem = getMenuItemFromShortcut(shortcut, menu);
        if (menuItem == null) {
            return null;
        }
        return menuItem.getId();
    }

    public void addShortcut(@SuppressWarnings("unused") ActionEvent event) {
        String shortcut = getShortcutFromClickedId();
        String menuItemId = getMenuItemIdFromShortcut(shortcut);
        if (menuItemId == null || shortcuts.contains(menuItemId)) {
            return;
        }
        if (generateAndAddShortcut(menuItemId)) {
            getMenuService().addShortcut(menuItemId);
            shortcuts.add(menuItemId);
        }
    }

    public void removeShortcut(@SuppressWarnings("unused") ActionEvent event) {
        String shortcut = getShortcutFromClickedId();
        String menuItemId = getMenuItemIdFromShortcut(shortcut);
        if (menuItemId == null || !shortcuts.contains(menuItemId)) {
            return;
        }
        getMenuService().removeShortcut(menuItemId);
        shortcuts.remove(menuItemId);
        generateShortcutLinks();
    }

    private String getShortcutFromClickedId() {
        return StringUtils.join(getPathFromClickedId(), UIMenuComponent.VALUE_SEPARATOR);
    }

    public static String getShortcutFromPath(String[] path) {
        return StringUtils.join(path, UIMenuComponent.VALUE_SEPARATOR);
    }

    private String[] getPathFromClickedId() {
        if (StringUtils.isBlank(clickedId)) {
            return null;
        }
        List<String> path = new ArrayList<String>();
        if (!StringUtils.contains(clickedId, "shortcut:")) {
            path.add(activeItemId);
        }
        path.addAll(Arrays.asList(clickedId.replaceAll("^[^0-9]*", "").split(UIMenuComponent.VALUE_SEPARATOR)));
        return path.toArray(new String[path.size()]);
    }

    public static String[] getPathFromShortcut(String shortcut) {
        return shortcut.replaceAll("^[^0-9]*", "").split(UIMenuComponent.VALUE_SEPARATOR);
    }

    public static class ShortcutClickedActionListener implements ActionListener, StateHolder {

        private String shortcut;

        public ShortcutClickedActionListener() {
            // for StateHolder
        }

        public ShortcutClickedActionListener(String shortcut) {
            this.shortcut = shortcut;
        }

        @Override
        public void processAction(ActionEvent actionEvent) throws AbortProcessingException {
            // MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME);
            MenuBean.clearViewStack(getPathFromShortcut(shortcut)[0], "shortcut:sm" + shortcut);
        }

        @Override
        public void restoreState(FacesContext context, Object state) {
            Object values[] = (Object[]) state;
            shortcut = (String) values[0];
        }

        @Override
        public Object saveState(FacesContext context) {
            Object[] values = new Object[4];
            values[0] = shortcut;
            return values;
        }

        @Override
        public boolean isTransient() {
            return false;
        }

        @Override
        public void setTransient(boolean newTransientValue) {
            // do nothing
        }

    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public MenuService getMenuService() {
        if (menuService == null) {
            menuService = (MenuService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(MenuService.BEAN_NAME);
        }
        return menuService;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    protected WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(WorkflowService.BEAN_NAME);
        }
        return workflowService;
    }

    public EInvoiceService getEinvoiceService() {
        if (einvoiceService == null) {
            einvoiceService = (EInvoiceService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(EInvoiceService.BEAN_NAME);
        }
        return einvoiceService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public String getScrollToAnchor() {
        return scrollToAnchor;
    }

    public void setScrollToAnchor(String scrollToAnchor) {
        this.scrollToAnchor = scrollToAnchor;
    }

    public String getScrollToY() {
        return scrollToY;
    }

    public void setScrollToY(String scrollToY) {
        this.scrollToY = scrollToY;
    }

    // END: getters / setters

    public static class DropdownMenuComparator implements Comparator<MenuItem> {
        @Override
        public int compare(MenuItem o1, MenuItem o2) {
            if (o1 != null && o2 != null && o1 instanceof DropdownMenuItem && o2 instanceof DropdownMenuItem) {
                return ((DropdownMenuItem) o1).getTransientOrderString().compareToIgnoreCase(((DropdownMenuItem) o2).getTransientOrderString());
            }
            return 0;
        }
    }

}
