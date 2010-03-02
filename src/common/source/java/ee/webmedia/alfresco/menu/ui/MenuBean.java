package ee.webmedia.alfresco.menu.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.GUID;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.DialogState;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.component.MenuItemWrapper;
import ee.webmedia.alfresco.menu.ui.component.MenuRenderer;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.user.web.UserDetailsDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Kaarel Jõgeva
 */
public class MenuBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MenuBean.class);

    public static final String BEAN_NAME = "MenuBean";
    public static final String UPDATE_TREE_ACTTIONLISTENER = "#{MenuBean.updateTree}";
    public static final int MY_TASKS_AND_DOCUMENTS_ID = 0; // XXX: this order defined in menu-structure.xml
    public static final int DOCUMENT_REGISTER_ID = 1;
    public static final String COUNT = "count";

    private transient HtmlPanelGroup shortcutsPanelGroup;

    private transient MenuService menuService;
    private transient GeneralService generalService;
    private transient UserService userService;

    private Menu menu;
    private int updateCount = 0;
    private String lastLinkId;
    private NodeRef linkNodeRef;
    private List<String> shortcuts;

    private String activeItemId = "0";

    Stack<DialogState> stateList = new Stack<DialogState>();

    public void handleState(DialogState state) {
        int i = 0;
        for(DialogState ds : stateList) {
            if(ds.getConfig().getName().equals(state.getConfig().getName())) {
                break;
            }
            i++;
        }
        stateList.setSize(i);
        stateList.push(state);
    }
    
    @SuppressWarnings("unchecked")
    public void resetStateList() {
        stateList = new Stack<DialogState>();
        List<UIComponent> children = breadcrumb.getChildren();
        children.clear();
    }

    public Stack<DialogState> getStateList() {
        return stateList;
    }

    public void setStateList(Stack<DialogState> stateList) {
        this.stateList = stateList;
    }
 
    private String clickedId = "";
    private transient HtmlPanelGroup breadcrumb;

    public void setBreadcrumb(HtmlPanelGroup breadcrumb) {
        this.breadcrumb = breadcrumb;
    }

    @SuppressWarnings("unchecked")
    public HtmlPanelGroup getBreadcrumb() {
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();

        if (breadcrumb == null) {
            breadcrumb = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            breadcrumb.setId("breadcrumb");
        }
        
        List children = breadcrumb.getChildren();
        children.clear();
        children.add(getActiveMainMenuItem().createComponent(FacesContext.getCurrentInstance(), "yeye" + GUID.generate(), getUserService()));
        
        Object[] items = stateList.toArray();
        int i = 1;
        for(Object item : items) {
            if(item instanceof DialogState) {
                children.add(getSeparator(application));
                DialogState state = (DialogState) item;
                String title;
                try {
                    if(state.getDialog().getContainerTitle() != null) {
                        title = state.getDialog().getContainerTitle();
                    } else {
                        title = MessageUtil.getMessage(state.getConfig().getTitleId());
                    }
                } catch (NullPointerException npe) {
                    title = "";
                }
                UIActionLink link = generateLink(application, title, (items.length - i));
                children.add(link);
                i++;
            }
        }
        
        return breadcrumb;
    }

    @SuppressWarnings("unchecked")
    private UIActionLink generateLink(Application application, String name, int closeCount) {
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        link.setValue(name);
        link.setAction(new ConstantMethodBinding("dialog:close["+ closeCount +"]"));
        link.setActionListener(application.createMethodBinding("#{MenuBean.popBreadcrumbItems}", new Class[] {javax.faces.event.ActionEvent.class}));
        link.getAttributes().put(COUNT, closeCount +"");
        return link;
    }
    
    public void popBreadcrumbItems(ActionEvent event) {
        int count = Integer.parseInt(event.getComponent().getAttributes().get(COUNT).toString());
        for (int i = 0; i < count; i++) {
            stateList.pop();
        }
        
    }
    
    /**
     * @param application
     */
    private UIComponent getSeparator(javax.faces.application.Application application) {
        UIOutput separator = (UIOutput) application.createComponent("org.alfresco.faces.OutputText");
        separator.setValue(" > ");
        separator.setTransient(true);
        return separator;
    }

    public String getClickedId() {
        return clickedId;
    }

    public void setClickedId(String clickedId) {
        this.clickedId = clickedId;
    }

    // END - BREADCRUMB
    // /////////////////////////////////////////////////////////////

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
        collapseMenuItems(item);

        // Let's go to the clicked link
        for (String step : path) {
            if (item.getSubItems() != null) {
                if (item instanceof DropdownMenuItem) {
                    ((DropdownMenuItem) item).setExpanded(true); // Mark our trail
                }
                item = item.getSubItems().get(Integer.parseInt(step));
            } else if (path.length > 1) { // if necessary, fetch children
                DropdownMenuItem dropdownItem = ((DropdownMenuItem) item);
                NodeRef nr = dropdownItem.getNodeRef();
                if (nr == null) {
                    nr = getGeneralService().getNodeRef(dropdownItem.getXPath());
                }
                for (NodeRef childItemRef : getMenuService().openTreeItem(dropdownItem, nr)) {
                    DropdownMenuItem childItem = new DropdownMenuItem();
                    getMenuService().setupTreeItem(childItem, childItemRef);
                    if (item.getSubItems() == null) {
                        item.setSubItems(new ArrayList<MenuItem>());
                    }
                    item.getSubItems().add(childItem);
                }
                dropdownItem.setExpanded(true);
                item = dropdownItem.getSubItems().get(Integer.parseInt(step));
            }
        }

        DropdownMenuItem dd = (DropdownMenuItem) item;
        // When XML configuration doesn't specify any children, this list will be null!
        if (dd.getSubItems() == null) {
            dd.setSubItems(new ArrayList<MenuItem>());
        }
        dd.getSubItems().clear();

        // Toggle the link
        if (dd.isExpanded()) {
            dd.setExpanded(false);
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
                getMenuService().setupTreeItem(ddChild, child);
                dd.getSubItems().add(ddChild);
            }
        }
        dd.setExpanded(true);

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
        if (getMenuService().getUpdateCount() != updateCount || this.menu == null) {
            log.debug("Fetching new menu structure from service.");
            reloadMenu(); // XXX - Somehow this makes it work... Although menu structure in service isn't modified.
            this.menu = getMenuService().getMenu();
            this.updateCount = getMenuService().getUpdateCount();
            if (lastLinkId != null && linkNodeRef != null) {
                updateTree();
            }
        }
        return this.menu;
    }

    public MenuItem getActiveMainMenuItem() {
        return menu.getSubItems().get(Integer.parseInt(activeItemId));
    }

    public String getActiveItemId() {
        return activeItemId;
    }

    public void setActiveItemId(String activeMenuId) {
        this.activeItemId = activeMenuId;
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

    // Shortcuts

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
        for (Iterator<String> i = getShortcuts().iterator(); i.hasNext(); ) {
            String shortcut = i.next();
            if (!generateAndAddShortcut(shortcut)) {
                i.remove();
            }
        }
    }

    private boolean generateAndAddShortcut(String shortcut) {
      FacesContext context = FacesContext.getCurrentInstance();
      String[] path = getPathFromShortcut(shortcut);

      List<MenuItem> subItems = menu.getSubItems();
      MenuItem item = null;
      int index;
      for (int i = 0; i < path.length; i++) {
          index = Integer.parseInt(path[i]);
          if (index >= subItems.size()) {
              return false;
          }
          item = subItems.get(index);
          subItems = item.getSubItems();
      }
      if (item == null) {
          return false;
      }
      MenuItemWrapper component = (MenuItemWrapper) item.createComponent(context, "shortcut-" + shortcutsPanelGroup.getChildCount(), getUserService(), false);
      component.setPlain(true);

      @SuppressWarnings("unchecked")
      List<UIComponent> children = shortcutsPanelGroup.getChildren();
      children.add(component);
      return true;
  }

    @SuppressWarnings("unchecked")
    public int getShortcutAddable() {
        if (!StringUtils.contains(clickedId, ":sm")) {
            return 0;
        }

        Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        Stack<String> viewStack = (Stack<String>) sessionMap.get(UIMenuComponent.VIEW_STACK);
        if (viewStack.size() > 1) {
            return 0;
        }

        String[] path = getPathFromClickedId();
        String shortcut = getShortcutFromPath(path);
        if (shortcuts.contains(shortcut)) {
            return -1;
        }

        List<MenuItem> subItems = menu.getSubItems();
        MenuItem item = null;
        int index;
        for (int i = 0; i < path.length; i++) {
            index = Integer.parseInt(path[i]);
            if (index >= subItems.size()) {
                return 0;
            }
            item = subItems.get(index);
            if (item instanceof DropdownMenuItem) {
                if (StringUtils.isNotBlank(((DropdownMenuItem) item).getXPath())) {
                    return 0;
                }
            }
            subItems = item.getSubItems();
        }
        if (item == null) {
            return 0;
        }

        return 1;
    }

    public void addShortcut(ActionEvent event) {
        String shortcut = getShortcutFromClickedId();
        if (shortcut == null || shortcuts.contains(shortcut)) {
            return;
        }
        if (generateAndAddShortcut(shortcut)) {
            getMenuService().addShortcut(shortcut);
            shortcuts.add(shortcut);
        }
    }

    public void removeShortcut(ActionEvent event) {
        String shortcut = getShortcutFromClickedId();
        if (shortcut == null || !shortcuts.contains(shortcut)) {
            return;
        }
        getMenuService().removeShortcut(shortcut);
        shortcuts.remove(shortcut);
        generateShortcutLinks();
    }

    private String getShortcutFromClickedId() {
        return StringUtils.join(getPathFromClickedId(), UIMenuComponent.VALUE_SEPARATOR);
    }

    private String getShortcutFromPath(String[] path) {
        return StringUtils.join(path, UIMenuComponent.VALUE_SEPARATOR);
    }

    private String[] getPathFromClickedId() {
        if (StringUtils.isBlank(clickedId)) {
            return null;
        }
        List<String> path = new ArrayList<String>();
        path.add(activeItemId);
        path.addAll(Arrays.asList(clickedId.replaceAll("^[^0-9]*", "").split(UIMenuComponent.VALUE_SEPARATOR)));
        return path.toArray(new String[path.size()]);
    }

    private String[] getPathFromShortcut(String shortcut) {
        return shortcut.replaceAll("^[^0-9]*", "").split(UIMenuComponent.VALUE_SEPARATOR);
    }

    public void resetClickedId() {
        clickedId = "";
    }

    public void resetClickedId(ActionEvent event) {
        resetClickedId();
    }

    public void setupUserConsole(ActionEvent event) {
        resetClickedId();
        UserDetailsDialog userDetailsDialog = (UserDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "UserDetailsDialog");
        userDetailsDialog.setupCurrentUser(event);
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

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    // END: getters / setters
}
