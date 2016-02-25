package ee.webmedia.alfresco.menu.ui;

import static org.alfresco.web.bean.dialog.BaseDialogBean.getCloseOutcome;
import static org.apache.commons.lang.StringUtils.remove;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.IDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.DialogsConfigElement.DialogConfig;
import org.alfresco.web.config.WizardsConfigElement.WizardConfig;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIOutputText;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.ShortcutMenuItem;
import ee.webmedia.alfresco.menu.service.ShortcutMenuItemOutcome;
import ee.webmedia.alfresco.menu.ui.component.MenuItemWrapper;
import ee.webmedia.alfresco.menu.ui.component.MenuRenderer;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;

public class MenuBean implements Serializable {

    public static final String SHORTCUT_MENU_ITEM_PREFIX = "shortcut-";
    public static final String SHORTCUT_OUTCOME_MENU_ITEM_PREFIX = SHORTCUT_MENU_ITEM_PREFIX + "outcome-";
    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MenuBean.class);

    public static final String BEAN_NAME = "MenuBean";
    public static final String UPDATE_TREE_ACTTIONLISTENER = "#{MenuBean.updateTree}";
    public static final String COUNT = "count";

    // NB! this order defined in menu-structure.xml
    // If you change menu item order in menu-structure.xml, also update constants below!
    public static final int MY_TASKS_AND_DOCUMENTS_ID = 0;
    public static final int DOCUMENT_REGISTER_ID = 1;
    public static final int MY_DOCUMENTS_ID = 3;
    public static final int CREATE_NEW_DOCUMENT = 8;
    public static final int CREATE_NEW = 9;

    private static final String ACTION_KEY_OUTCOME = "outcome";
    private static final String ACTION_KEY_ACTION = "action";
    private static final String ACTION_KEY_NODE_REF = "nodeRef";
    private static final String ACTION_KEY_MENU_ITEM_NODE_REF = "menuItemNodeRef";

    public static final List<String> HIDDEN_WHEN_EMPTY = Arrays.asList(
            MenuItem.ASSIGNMENT_TASKS,
            MenuItem.INFORMATION_TASKS,
            MenuItem.ORDER_ASSIGNMENT_TASKS,
            MenuItem.OPINION_TASKS,
            MenuItem.DISCUSSIONS,
            MenuItem.REVIEW_TASKS,
            MenuItem.EXTERNAL_REVIEW_TASKS,
            MenuItem.CONFIRMATION_TASKS,
            MenuItem.SIGNATURE_TASKS,
            MenuItem.FOR_REGISTERING_LIST,
            MenuItem.USER_WORKING_DOCUMENTS,
            MenuItem.USER_CASE_FILES
            );

    public static final List<String> HIDDEN_TO_OTHER_STRUCT_UNIT_PEOPLE = Arrays.asList(
            "documentRegister"
            , "contact"
            , "me"
            , "search"
            , "restrictedDelta"
            , "regularDelta"
            , "documentDynamicTypes"
            , "newCaseFileOrWorkflow"
            , "menu_my_responsibility"
            , "departmentDocuments"
            , "myDocuments"
            );
    
    public static final List<String> HIDDEN_TO_GUESTS = Arrays.asList(
            "documentRegister"
            , "contact"
            , "menuReports"
            , "executedReports"
            , "restrictedDelta"
            , "regularDelta"
            , "documentDynamicTypes"
            , "newCaseFileOrWorkflow"
            , "menu_my_responsibility"
            , "departmentDocuments"
            );

    public static final List<String> HIDDEN_FROM_SUBSTITUTOR = Arrays.asList("documentDynamicTypes");

    private transient HtmlPanelGroup shortcutsPanelGroup;
    private transient HtmlPanelGroup breadcrumb;

    private transient MenuService menuService;
    private transient GeneralService generalService;
    private transient UserService userService;
    private transient WorkflowConstantsBean workflowConstantsBean;
    private transient EInvoiceService einvoiceService;

    private Menu menu;
    private int updateCount = 0;
    private String lastLinkId;
    private NodeRef linkNodeRef;
    private List<ShortcutMenuItem> shortcuts;
    private String activeItemId = "0";
    private String clickedId = "";
    private Stack<String> stateList = new Stack<String>();
    private String scrollToAnchor;
    private String scrollToY;

    private List<String> leftMenuTitles;

    /**
     * Watch out for a gotcha moment! If event comes from ActionLink or CommandButton,
     * handleNavigation is called for a second time and it resets this setting!
     *
     * @param anchor ID of the HTML element in the form of "#my-panel"
     */
    public void scrollToAnchor(String anchor) {
        WebUtil.navigateTo(anchor);
    }

    public void resetBreadcrumb() {
        stateList.clear();
        BeanHelper.getBeanCleanupHelper().cleanAll();
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

    public void addBreadcrumbItem(String title) {
        if (getLeftMenuTitles().contains(title)) {
            int i = 0;
            for (String listTitle : stateList) {
                if (listTitle.equals(title)) { // left side menu provides functionality for backwards navigation.
                    stateList.setSize(i);
                    @SuppressWarnings("rawtypes")
                    Stack viewStack = (Stack) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get(UIMenuComponent.VIEW_STACK);
                    if (viewStack != null) {
                        viewStack.setSize(i + 1);
                    }
                    break;
                }
                i++;
            }
        }
        stateList.push(title);
    }

    private List<String> getLeftMenuTitles() {
        if (leftMenuTitles == null) {
            leftMenuTitles = Collections.unmodifiableList(Arrays.asList(
                    MessageUtil.getMessage("volume_list"),
                    MessageUtil.getMessage("series_list"),
                    MessageUtil.getMessage("document_list")));
        }
        return leftMenuTitles;
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
        context.getApplication().getNavigationHandler().handleNavigation(context, "closeBreadcrumbItem", getCloseOutcome(closeCount));
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

        // NOTE: In XML nodes are referenced by xPath, but since all child association names are with the same name(function, series etc)
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

        String lastLinkPath = lastLinkId.substring(MenuRenderer.SECONDARY_MENU_PREFIX.length());
        String[] path = lastLinkPath.split(UIMenuComponent.VALUE_SEPARATOR);

        // If we toggle browse items to collapse, we don't need further processing
        String menuItemFullPath = getActiveItemId() + UIMenuComponent.VALUE_SEPARATOR + lastLinkPath;
        MenuItem activeMenuItem = MenuBean.getMenuItemFromShortcut(menuItemFullPath, getMenu());

        if (activeMenuItem instanceof DropdownMenuItem && ((DropdownMenuItem) activeMenuItem).isBrowse() && ((DropdownMenuItem) activeMenuItem).isExpanded()) {
            ((DropdownMenuItem) activeMenuItem).setExpanded(false);
            return;
        }

        MenuItem item = getActiveMainMenuItem();
        if (Integer.parseInt(activeItemId) == DOCUMENT_REGISTER_ID) {
            collapseMenuItems(item);
        } else if (Integer.parseInt(activeItemId) == MY_TASKS_AND_DOCUMENTS_ID) {
            collapseMenuItems(item.getSubItems().get(MY_DOCUMENTS_ID));
        }

        // Let's go to the clicked link
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
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
                    getMenuService().setupTreeItem(childItem, childItemRef, propertyTypes);
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
                    getMenuService().setupTreeItem(ddChild, child, propertyTypes);
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
        int newUpdateCount = getMenuService().getUpdateCount();
        if (newUpdateCount != updateCount || menu == null) {
            log.debug("Fetching new menu structure from service.");
            reloadMenu(); // XXX - Somehow this makes it work... Although menu structure in service isn't modified.
            menu = getMenuService().getMenu();
            getMenuService().process(menu, false, true);
            menuService.logMenu(menu, "After MenuBean.getMenu.process: ");
            updateCount = newUpdateCount;
            if (lastLinkId != null && linkNodeRef != null) {
                updateTree();
                menuService.logMenu(menu, "After MenuBean.getMenu.updateTree: ");
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

    public String getWebServiceDocumentsMenuItemTitle() {
        return BeanHelper.getAddDocumentService().getWebServiceDocumentsMenuItemTitle();
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
        sessionMap.put(UIMenuComponent.VIEW_STACK, new Stack<String>());

        MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
        menuBean.setMenuItemId(primaryId, secondaryId);
        menuBean.resetBreadcrumb();

        // let the ClearStateNotificationHandler notify all the interested listeners
        BeanHelper.getClearStateNotificationHandler().notifyClearStateListeners();
    }

    public void clearViewStack(ActionEvent event) {
        String primaryId = ActionUtil.getParam(event, "primaryId");
        clearViewStack(primaryId, null);
    }

    public void toggle(@SuppressWarnings("unused") ActionEvent event) {
        MenuItem menuItem = getMenuItemFromShortcut(getShortcutFromClickedId(), getMenu());
        if (menuItem instanceof DropdownMenuItem) {
            ((DropdownMenuItem) menuItem).toggle();
        }
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

    public List<ShortcutMenuItem> getShortcuts() {
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
        for (Iterator<ShortcutMenuItem> i = getShortcuts().iterator(); i.hasNext();) {
            ShortcutMenuItem menuItemId = i.next();
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

    private boolean generateAndAddShortcut(ShortcutMenuItem shortcutMenuItem) {
        if (shortcutMenuItem == null || (!shortcutMenuItem.isOutcomeShortcut() && StringUtils.isBlank(shortcutMenuItem.getMenuItemId()))) {
            return false;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        MenuItem item;
        Pair<MenuItem, String[]> menuItemAndPath = null;
        String idPrefix;
        if (shortcutMenuItem.isOutcomeShortcut()) {
            item = new MenuItem();
            idPrefix = SHORTCUT_OUTCOME_MENU_ITEM_PREFIX;
            NodeRef actionNodeRef = shortcutMenuItem.getActionNodeRef();
            item.setOutcome(shortcutMenuItem.getOutcome().getOutcome());
            item.setActionListener(shortcutMenuItem.getOutcome().getAction());
            item.setId(SHORTCUT_OUTCOME_MENU_ITEM_PREFIX + "item-" + shortcutsPanelGroup.getChildCount());
            Map<String, String> params = item.getParams();
            if (actionNodeRef == null && requiresNodeRef(shortcutMenuItem)) {
                return false;
            }
            if (actionNodeRef != null) {
                if (!BeanHelper.getNodeService().exists(actionNodeRef)) {
                    return false;
                }
                Map<QName, Serializable> props = BeanHelper.getNodeService().getProperties(actionNodeRef);
                StringBuffer sb = new StringBuffer("");
                for (QName titlePropQName : shortcutMenuItem.getOutcome().getTitlePropQNames()) {
                    sb.append(props.get(titlePropQName)).append(" ");
                }
                item.setTitle(sb.toString());
                params.put("nodeRef", actionNodeRef.toString());
            }
        } else {
            idPrefix = SHORTCUT_MENU_ITEM_PREFIX;
            menuItemAndPath = getMenuItemAndPathFromMenuItemId(shortcutMenuItem.getMenuItemId());
            if (menuItemAndPath == null) {
                return false;
            }
            item = menuItemAndPath.getFirst();
        }
        MenuItemWrapper wrapper = (MenuItemWrapper) item.createComponent(context, idPrefix + shortcutsPanelGroup.getChildCount()
                , getUserService(), getWorkflowConstantsBean(), BeanHelper.getRSService(), false);
        if (wrapper == null) {
            return false; // no permissions or for some other reason wrapper is not created
        }
        wrapper.setPlain(true);

        if (!shortcutMenuItem.isOutcomeShortcut()) {
            UIActionLink link = (UIActionLink) wrapper.getChildren().get(0);
            String shortcut = getShortcutFromPath(menuItemAndPath.getSecond());
            link.addActionListener(new ShortcutClickedActionListener(shortcut));

            String title = (String) link.getValue();
            if (title.endsWith(")")) {
                link.setValue(title.substring(0, title.lastIndexOf('(')));
            }
        } else {
            List<UIComponent> children = ((UIActionLink) wrapper.getChildren().get(0)).getChildren();
            if (!children.isEmpty()) {
                ((UIParameter) children.get(0)).setId(SHORTCUT_MENU_ITEM_PREFIX + "param-" + shortcutsPanelGroup.getChildCount());
            }
        }

        // All shortcut items should be visible and we don't need the AJAX counter updater.
        // (NB! Don't modify item variable since it is linked to the actual menu where items have to be hidden sometimes)
        @SuppressWarnings("unchecked")
        Map<String, String> attr = wrapper.getAttributes();
        String styleClass = attr.get("styleClass");
        if (StringUtils.isNotBlank(styleClass)) {
            attr.put("styleClass", remove(remove(styleClass, MenuItem.HIDDEN_MENU_ITEM), "menuItemCount"));
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = shortcutsPanelGroup.getChildren();
        children.add(wrapper);
        return true;
    }

    private boolean requiresNodeRef(ShortcutMenuItem shortcutMenuItem) {
        ShortcutMenuItemOutcome outcome = shortcutMenuItem.getOutcome();
        return ShortcutMenuItemOutcome.CASE_FILE == outcome || ShortcutMenuItemOutcome.VOLUME == outcome;
    }

    public static ShortcutMenuItemOutcome getOutcome(String outcomeStr) {
        try {
            return ShortcutMenuItemOutcome.valueOf(outcomeStr);
        } catch (IllegalArgumentException e) {
            return ShortcutMenuItemOutcome.VOLUME;
        }
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
        if (isExistingShortcut(new ShortcutMenuItem(menuItemId))) {
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
        ShortcutMenuItem shortcutMenuItem = new ShortcutMenuItem(menuItemId);
        if (menuItemId == null || isExistingShortcut(shortcutMenuItem)) {
            return;
        }
        if (generateAndAddShortcut(shortcutMenuItem)) {
            getMenuService().addShortcut(shortcutMenuItem);
            shortcuts.add(shortcutMenuItem);
        }
    }

    public void addVolumeOutcomeShortcut(ActionEvent event) {
        ShortcutMenuItemOutcome outcome = getOutcome(ActionUtil.getParam(event, ACTION_KEY_OUTCOME));
        NodeRef nodeRef = null;
        if (ActionUtil.hasParam(event, ACTION_KEY_NODE_REF)) {
            nodeRef = new NodeRef(ActionUtil.getParam(event, ACTION_KEY_NODE_REF));
        }
        ShortcutMenuItem shortcutMenuItem = new ShortcutMenuItem(null, outcome, nodeRef);
        if (isExistingShortcut(shortcutMenuItem)) {
            return;
        }
        if (generateAndAddShortcut(shortcutMenuItem)) {
            shortcutMenuItem = getMenuService().addShortcut(shortcutMenuItem);
            shortcuts.add(shortcutMenuItem);
        }
    }

    public void removeShortcut(@SuppressWarnings("unused") ActionEvent event) {
        String shortcut = getShortcutFromClickedId();
        String menuItemId = getMenuItemIdFromShortcut(shortcut);
        ShortcutMenuItem shortcutMenuItem = new ShortcutMenuItem(menuItemId);
        if (menuItemId == null || !isExistingShortcut(shortcutMenuItem)) {
            return;
        }
        getMenuService().removeShortcut(shortcutMenuItem);
        removeShortcutFromList(shortcutMenuItem, shortcuts);
        generateShortcutLinks();
    }

    public static void removeShortcutFromList(ShortcutMenuItem shortcut, List<ShortcutMenuItem> shortcuts) {
        for (Iterator<ShortcutMenuItem> i = shortcuts.iterator(); i.hasNext();) {
            ShortcutMenuItem item = i.next();
            if (item.equals(shortcut)) {
                i.remove();
            }
        }
    }

    public void removeOutcomeShortcut(ActionEvent event) {
        NodeRef volumeNodeRef = new NodeRef(ActionUtil.getParam(event, ACTION_KEY_MENU_ITEM_NODE_REF));
        ShortcutMenuItem shortcutToRemove = null;
        for (ShortcutMenuItem shortcutMenuItem : shortcuts) {
            if (volumeNodeRef.equals(shortcutMenuItem.getActionNodeRef())) {
                shortcutToRemove = shortcutMenuItem;
                break;
            }
        }
        if (shortcutToRemove == null) {
            return;
        }
        getMenuService().removeShortcut(shortcutToRemove);
        shortcuts.remove(shortcutToRemove);
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

    public boolean isMenuItemHidden(String menuItemId) {
        if (StringUtils.isBlank(menuItemId)) {
            return false;
        }
        
        if (HIDDEN_TO_GUESTS.contains(menuItemId)) {
        	if (getUserService().isGuest()) {
        		return true;
        	} 
        }

        if (HIDDEN_WHEN_EMPTY.contains(menuItemId)) {
            Boolean showEmpty = (Boolean) getUserService().getUserProperties(AuthenticationUtil.getRunAsUser()).get(ContentModel.SHOW_EMPTY_TASK_MENU);
            boolean result = showEmpty == null || !showEmpty;
            if (result && MenuItem.MY_TASK_MENU_ITEMS.contains(menuItemId)) {
                log.debug("Setting menu_my_tasks subitem " + menuItemId + " hidden");
            }
            return result;
        }

        if (HIDDEN_FROM_SUBSTITUTOR.contains(menuItemId)) {
            return BeanHelper.getSubstitutionBean().getSubstitutionInfo().isSubstituting();
        }

        if (HIDDEN_TO_OTHER_STRUCT_UNIT_PEOPLE.contains(menuItemId)) {
            return !BeanHelper.getSubstitutionBean().isCurrentStructUnitUser();
        }
        
        if ("volSearch".equals(menuItemId)) {
            return !BeanHelper.getApplicationConstantsBean().isCaseVolumeEnabled();
        }
        return false;
    }

    public boolean isShowAddCaseFileVolumeShortcut() {
        if (!isCaseFileVolumeDialog()) {
            return false;
        }
        return isNotExistingShortcut();
    }

    public boolean isShowAddVolumeShortcut() {
        if (!isVolumeDialog()) {
            return false;
        }
        return isNotExistingShortcut();
    }

    private boolean isNotExistingShortcut() {
        NodeRef currentVolumeRef = getCurrentVolumeRef();
        return currentVolumeRef != null && !isExistingShortcut(currentVolumeRef);
    }

    private boolean isCaseFileVolumeDialog() {
        try {
            return "caseFileDialog".equals(BeanHelper.getDialogManager().getCurrentDialog().getName());
        } catch (NullPointerException e) {
            return false;
        }
    }

    private boolean isVolumeDialog() {
        try {
            return "caseDocListDialog".equals(BeanHelper.getDialogManager().getCurrentDialog().getName());
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean isShowRemoveVolumeOutcomeShortcut() {
        NodeRef currentVolumeRef = getCurrentVolumeRef();
        if (currentVolumeRef == null) {
            return false;
        }
        return isExistingShortcut(currentVolumeRef);
    }

    private boolean isExistingShortcut(NodeRef currentVolumeRef) {
        if (currentVolumeRef == null) {
            return false;
        }
        for (ShortcutMenuItem shortcut : shortcuts) {
            if (shortcut.isOutcomeShortcut() && currentVolumeRef.equals(shortcut.getActionNodeRef())) {
                return true;
            }
        }
        return false;
    }

    private boolean isExistingShortcut(ShortcutMenuItem shortcutMenuItem) {
        return isExistingShortcut(shortcutMenuItem, shortcuts);
    }

    public static boolean isExistingShortcut(ShortcutMenuItem shortcutMenuItem, List<ShortcutMenuItem> shortcuts) {
        for (ShortcutMenuItem shortcut : shortcuts) {
            if (shortcut.equals(shortcutMenuItem)) {
                return true;
            }
        }
        return false;
    }

    public NodeRef getCurrentVolumeRef() {
        NodeRef currentVolumeRef = null;
        if (isCaseFileVolumeDialog()) {
            WmNode caseFile = BeanHelper.getCaseFileDialog().getNode();
            currentVolumeRef = caseFile != null ? caseFile.getNodeRef() : null;
        } else if (isVolumeDialog()) {
            Volume volume = BeanHelper.getCaseDocumentListDialog().getParent();
            if (volume != null) {
                Node volumeNode = volume.getNode();
                currentVolumeRef = volumeNode != null ? volumeNode.getNodeRef() : null;
            }
        }
        return currentVolumeRef;
    }

    public String getVolumeOpenOutcome() {
        return ShortcutMenuItemOutcome.VOLUME.toString();
    }

    public String getCaseFileOpenOutcome() {
        return ShortcutMenuItemOutcome.CASE_FILE.toString();
    }

    public String getRestrictedDeltaName() {
        return BeanHelper.getRSService().getRestrictedDeltaName();
    }

    public String getDeltaName() {
        return BeanHelper.getRSService().getDeltaName();
    }

    public String getRestrictedDeltaUrl() {
        return BeanHelper.getRSService().getRestrictedDeltaUrl();
    }

    public String getDeltaUrl() {
        return BeanHelper.getRSService().getDeltaUrl();
    }

    public String getWorkingDocumentsAddress() {
        return BeanHelper.getParametersService().getStringParameter(Parameters.WORKING_DOCUMENTS_ADDRESS);
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

    protected WorkflowConstantsBean getWorkflowConstantsBean() {
        if (workflowConstantsBean == null) {
            workflowConstantsBean = BeanHelper.getWorkflowConstantsBean();
        }
        return workflowConstantsBean;
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
                return AppConstants.getNewCollatorInstance().compare(((DropdownMenuItem) o1).getTransientOrderString(), ((DropdownMenuItem) o2).getTransientOrderString());
            }
            return 0;
        }
    }

}
