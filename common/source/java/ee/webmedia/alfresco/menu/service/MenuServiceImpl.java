package ee.webmedia.alfresco.menu.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.thoughtworks.xstream.XStream;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.model.MenuModel;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.parameters.service.ParametersService.ParameterChangedCallback;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;

public class MenuServiceImpl implements MenuService, InitializingBean {
    private static Logger log = Logger.getLogger(MenuServiceImpl.class);

    private String menuConfigLocation;
    private FileFolderService fileFolderService;
    private GeneralService generalService;
    private NodeService nodeService;
    private UserService userService;
    private ParametersService parametersService;

    private int updateCount;

    private Menu menu;
    // doesn't need to be synchronized, because it is not modified after spring initialization
    private final List<ProcessorWrapper> processors = new ArrayList<ProcessorWrapper>();
    private TreeItemProcessor treeItemProcessor;
    private Map<String, MenuItemFilter> menuItemFilters;

    private static class ProcessorWrapper {

        public String menuItemId;
        public MenuItemProcessor processor;
        public boolean runOnce;
        public boolean isExecutable;
        public boolean isSessionScoped;

        public ProcessorWrapper(String menuItemId, MenuItemProcessor processor, boolean runOnce, boolean isSessionScoped) {
            this.menuItemId = menuItemId;
            this.processor = processor;
            this.runOnce = runOnce;
            isExecutable = true;
            this.isSessionScoped = isSessionScoped;

        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        parametersService.addParameterChangeListener(Parameters.WORKING_DOCUMENTS_ADDRESS.getParameterName(), new ParameterChangedCallback() {
            @Override
            public void doWithParameter(Serializable value) {
                menuUpdated();
            }
        });
    }

    @Override
    public void processTasks(Menu menu) {
        processTasks(menu, null);
    }

    @Override
    public void processTasks(Menu menu, Collection<String> onlyMenuItemIds) {
        long start = System.currentTimeMillis();
        process(menu, false, onlyMenuItemIds, false);
    }

    @Override
    public int getUpdateCount() {
        return updateCount;
    }

    @Override
    public void menuUpdated() {
        updateCount++;
    }

    @Override
    public Menu getMenu() {
        return menu;
    }

    @Override
    public NodeRef getNodeRefForXPath(FacesContext context, String XPath) {
        return generalService.getNodeRef(XPath);
    }

    @Override
    public void reload() {
        try {
            ResourceLoader resourceLoader = new DefaultResourceLoader();
            Resource resource = resourceLoader.getResource(menuConfigLocation);
            if (!resource.exists()) {
                throw new FileNotFoundException("Menu configuration file does not exist: " + menuConfigLocation);
            }

            XStream xstream = new XStream();
            xstream.processAnnotations(Menu.class);
            xstream.processAnnotations(MenuItem.class);
            xstream.processAnnotations(DropdownMenuItem.class);
            Menu loadedMenu = (Menu) xstream.fromXML(resource.getInputStream());

            logMenu(loadedMenu, "After xml load: ");
            process(loadedMenu, true);
            menu = loadedMenu; // this is performed here at the end, atomically
            logMenu(menu, "After xml load and process: ");

        } catch (IOException e) {
            log.error("Menu configuration loading failed: " + menuConfigLocation, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void logMenu(Menu loadedMenu, String logPrefix) {
        if (loadedMenu.getSubItems() != null && loadedMenu.getSubItems().size() > 0) {
            MenuItem menuItem = loadedMenu.getSubItems().get(0);
            String titleId = menuItem.getTitleId();
            if ("menu_my_tasks_and_documents".equals(titleId)) {
                if (menuItem.getSubItems() != null && menuItem.getSubItems().size() > 0) {
                    MenuItem subMenuItem = menuItem.getSubItems().get(0);
                    String subMenuItemId = subMenuItem.getId();
                    if (!"menu_my_tasks".equals(subMenuItemId)) {
                        log.debug(logPrefix + "Menu error; item menu_my_tasks_and_documents submenu first child id=" + subMenuItemId);
                    } else {
                        List<MenuItem> submenuItems = subMenuItem.getSubItems();
                        if (submenuItems == null || submenuItems.size() == 0) {
                            log.debug(logPrefix + "Menu error; item menu_my_tasks_and_documents submenu menu_my_tasks subitems is null or empty");
                        } else {
                            log.debug(logPrefix + "Loading menu_my_tasks with " + submenuItems.size() + " subitems");
                        }
                    }
                } else {
                    log.debug(logPrefix + "Menu error; item menu_my_tasks_and_documents submenu is empty");
                }
            }
        }
    }

    @Override
    public List<NodeRef> openTreeItem(DropdownMenuItem menuItem, NodeRef nodeRef) {
        return treeItemProcessor.openTreeItem(menuItem, nodeRef);
    }

    @Override
    public void setupTreeItem(DropdownMenuItem dd, NodeRef nodeRef, Map<Long, QName> propertyTypes) {
        treeItemProcessor.setupTreeItem(dd, nodeRef, propertyTypes);
    }

    @Override
    public void setTreeItemProcessor(TreeItemProcessor processor) {
        treeItemProcessor = processor;
    }

    @Override
    public void addProcessor(String menuItemId, MenuItemProcessor processor, boolean runOnce, boolean sessionScope) {
        processors.add(new ProcessorWrapper(menuItemId, processor, runOnce, sessionScope));
    }

    @Override
    public void addProcessor(String menuItemId, MenuItemProcessor processor, boolean runOnce) {
        addProcessor(menuItemId, processor, runOnce, false);
    }

    @Override
    public String getMenuXml() {
        XStream xstream = new XStream();
        xstream.processAnnotations(UIMenuComponent.class);
        xstream.processAnnotations(MenuItem.class);
        return xstream.toXML(menu);
    }

    @Override
    public int getNodeChildrenCount(NodeRef nodeRef) {
        return fileFolderService.listFolders(nodeRef).size();
    }

    private void process(Menu loadedMenu, boolean reloaded) {
        process(loadedMenu, reloaded, null, false);
    }

    @Override
    public void process(Menu loadedMenu, boolean reloaded, boolean sessionScope) {
        process(loadedMenu, reloaded, null, sessionScope);
    }

    private void process(Menu loadedMenu, boolean reloaded, Collection<String> onlyMenuItemIds, boolean sessionScope) {
        if (loadedMenu == null) {
            // may happen in mDelta context
            return;
        }
        for (ProcessorWrapper processorWrapper : processors) {
            if (reloaded || (processorWrapper.isExecutable && processorWrapper.isSessionScoped == sessionScope)) {
                if (processorWrapper.runOnce) {
                    processorWrapper.isExecutable = false;
                }
                if (processorWrapper.menuItemId == null) {
                    for (MenuItem item : loadedMenu.getSubItems()) {
                        processorWrapper.processor.doWithMenuItem(item);
                    }
                } else {
                    process(processorWrapper, loadedMenu.getSubItems(), onlyMenuItemIds);
                }
            }
        }
    }

    private void process(ProcessorWrapper processorWrapper, List<MenuItem> items, Collection<String> onlyMenuItemIds) {
        if (items == null) {
            return;
        }
        for (MenuItem item : items) {
            final String itemId = item.getId();
            boolean process = onlyMenuItemIds == null || onlyMenuItemIds.contains(itemId);
            if (process && processorWrapper.menuItemId.equals(itemId)) {
                processorWrapper.processor.doWithMenuItem(item);
            } else {
                process(processorWrapper, item.getSubItems(), onlyMenuItemIds);
            }
        }
    }

    @Override
    public ArrayList<ShortcutMenuItem> getShortcuts() {
        return getShortcuts(userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef());
    }

    private ArrayList<ShortcutMenuItem> getShortcuts(NodeRef user) {
        ArrayList<ShortcutMenuItem> shortcuts = new ArrayList<ShortcutMenuItem>();
        @SuppressWarnings("unchecked")
        ArrayList<String> menuShortcuts = (ArrayList<String>) nodeService.getProperty(user, MenuModel.Props.SHORTCUTS);
        if (menuShortcuts != null) {
            for (String shortcut : menuShortcuts) {
                shortcuts.add(new ShortcutMenuItem(shortcut));
            }
        }
        shortcuts.addAll(getOutcomeShortcuts(user));
        return shortcuts;
    }

    private ArrayList<ShortcutMenuItem> getOutcomeShortcuts(NodeRef user) {
        ArrayList<ShortcutMenuItem> shortcuts = new ArrayList<ShortcutMenuItem>();
        for (ChildAssociationRef childAssocRef : nodeService.getChildAssocs(retrieveUserOutcomeShortcutsRoot(user), Collections.singleton(MenuModel.TYPES.OUTCOME_SHORTCUT))) {
            NodeRef menuItemRef = childAssocRef.getChildRef();
            Map<QName, Serializable> menuItemProps = nodeService.getProperties(menuItemRef);
            ShortcutMenuItemOutcome outcome = MenuBean.getOutcome((String) menuItemProps.get(MenuModel.Props.OUTCOME));
            shortcuts.add(new ShortcutMenuItem(menuItemRef, outcome, (NodeRef) menuItemProps.get(MenuModel.Props.ACTION_NODE_REF)));
        }
        return shortcuts;
    }

    @Override
    public ShortcutMenuItem addShortcut(ShortcutMenuItem shortcut) {
        return addShortcut(shortcut, userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef());
    }

    @Override
    public ShortcutMenuItem addShortcut(ShortcutMenuItem shortcut, NodeRef user) {
        if (shortcut.isOutcomeShortcut()) {
            createOutcomeShortcut(shortcut, user);
        } else {
            ArrayList<ShortcutMenuItem> shortcuts = getShortcuts(user);
            shortcuts.add(shortcut);
            saveMenuShortcuts(shortcuts, user);
        }
        return shortcut;
    }

    private void createOutcomeShortcut(ShortcutMenuItem shortcut, NodeRef user) {
        NodeRef parentRef = retrieveUserOutcomeShortcutsRoot(user);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(MenuModel.Props.OUTCOME, shortcut.getOutcome().toString());
        props.put(MenuModel.Props.ACTION_NODE_REF, shortcut.getActionNodeRef());
        NodeRef shortcutRef = nodeService.createNode(parentRef, MenuModel.Assocs.OUTCOME_SHORTCUT, MenuModel.Assocs.OUTCOME_SHORTCUT, MenuModel.TYPES.OUTCOME_SHORTCUT, props)
                .getChildRef();
        shortcut.setMenuItemNodeRef(shortcutRef);
    }

    private NodeRef retrieveUserOutcomeShortcutsRoot(NodeRef user) {
        List<ChildAssociationRef> root = nodeService.getChildAssocs(user, Collections.singleton(MenuModel.Assocs.OUTCOME_SHORTCUTS_ROOT));
        if (root.size() > 1) {
            throw new UnableToPerformException("multiple_shortcut_roots");
        }
        NodeRef rootNodeRef;
        if (root.size() == 0) {
            rootNodeRef = nodeService.createNode(user, MenuModel.Assocs.OUTCOME_SHORTCUTS_ROOT, MenuModel.Assocs.OUTCOME_SHORTCUTS_ROOT,
                    MenuModel.TYPES.OUTCOME_SHORTCUTS_ROOT).getChildRef();
            nodeService.addAspect(user, MenuModel.Aspects.OUTCOME_SHORTCUTS_CONTAINER, null);
        } else {
            rootNodeRef = root.get(0).getChildRef();
        }
        return rootNodeRef;
    }

    @Override
    public void removeShortcut(ShortcutMenuItem shortcut) {
        if (shortcut.isOutcomeShortcut()) {
            nodeService.deleteNode(shortcut.getMenuItemNodeRef());
        } else {
            NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
            ArrayList<ShortcutMenuItem> shortcuts = getShortcuts(user);
            MenuBean.removeShortcutFromList(shortcut, shortcuts);
            saveMenuShortcuts(shortcuts, user);
        }
    }

    private void saveMenuShortcuts(ArrayList<ShortcutMenuItem> shortcuts, NodeRef user) {
        List<String> menuShortcuts = new ArrayList<String>();
        for (ShortcutMenuItem shortcut : shortcuts) {
            if (!shortcut.isOutcomeShortcut()) {
                menuShortcuts.add(shortcut.getMenuItemId());
            }
        }
        nodeService.setProperty(user, MenuModel.Props.SHORTCUTS, (Serializable) menuShortcuts);
    }

    @Override
    public void addAllFunctionVolumeShortcuts(NodeRef userRef) {
        addFunctionVolumeShortcutsForUsers(Collections.singletonList(userRef), BeanHelper.getFunctionsService().getAllLimitedActivityFunctions());
    }

    @Override
    public void addFunctionVolumeShortcuts(NodeRef functionNodeRef) {
        addFunctionVolumeShortcutsForUsers(BeanHelper.getPersonService().getAllUserRefs(), Collections.singletonList(functionNodeRef));
    }

    @Override
    public void addVolumeShortcuts(NodeRef volumeRef, boolean isCaseFile) {
        ShortcutMenuItemOutcome outcome = isCaseFile ? ShortcutMenuItemOutcome.CASE_FILE : ShortcutMenuItemOutcome.VOLUME;
        ShortcutMenuItem shortcutMenuItem = new ShortcutMenuItem(null, outcome, volumeRef);
        for (NodeRef userRef : BeanHelper.getPersonService().getAllUserRefs()) {
            List<ShortcutMenuItem> userOutcomeShortcuts = getOutcomeShortcuts(userRef);
            if (!MenuBean.isExistingShortcut(shortcutMenuItem, userOutcomeShortcuts)) {
                addShortcut(shortcutMenuItem, userRef);
            }
        }
    }

    private void addFunctionVolumeShortcutsForUsers(List<NodeRef> userRefs, List<NodeRef> functionRefs) {
        List<ShortcutMenuItem> shortcutMenuItems = new ArrayList<ShortcutMenuItem>();
        for (NodeRef functionRef : functionRefs) {
            for (UnmodifiableSeries series : BeanHelper.getSeriesService().getAllSeriesByFunction(functionRef, true)) {
                for (UnmodifiableVolume volume : BeanHelper.getVolumeService().getAllVolumesBySeries(series.getSeriesRef())) {
                    ShortcutMenuItemOutcome outcome = volume.isDynamic() ? ShortcutMenuItemOutcome.CASE_FILE : ShortcutMenuItemOutcome.VOLUME;
                    shortcutMenuItems.add(new ShortcutMenuItem(null, outcome, volume.getNodeRef()));
                }
            }
        }
        for (NodeRef userRef : userRefs) {
            List<ShortcutMenuItem> userOutcomeShortcuts = getOutcomeShortcuts(userRef);
            for (ShortcutMenuItem shortcutMenuItem : shortcutMenuItems) {
                if (MenuBean.isExistingShortcut(shortcutMenuItem, userOutcomeShortcuts)) {
                    continue;
                }
                addShortcut(shortcutMenuItem, userRef);
            }
        }
    }

    @Override
    public Map<String, MenuItemFilter> getMenuItemFilters() {
        return menuItemFilters;
    }

    // START: getters / setters

    public void setMenuConfigLocation(String menuConfigLocation) {
        this.menuConfigLocation = menuConfigLocation;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public void setMenuItemFilters(Map<String, MenuItemFilter> menuItemFilters) {
        this.menuItemFilters = menuItemFilters;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    // END: getters / setters

}