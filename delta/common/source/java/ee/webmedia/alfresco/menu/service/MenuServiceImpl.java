package ee.webmedia.alfresco.menu.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.thoughtworks.xstream.XStream;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.menu.model.BrowseMenuItem;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.model.MenuModel;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * @author Kaarel Jõgeva
 */
public class MenuServiceImpl implements MenuService {
    private static Logger log = Logger.getLogger(MenuServiceImpl.class);

    private String menuConfigLocation;
    private FileFolderService fileFolderService;
    private GeneralService generalService;
    private NodeService nodeService;
    private UserService userService;

    private int updateCount;

    private Menu menu;
    // doesn't need to be synchronized, because it is not modified after spring initialization
    private List<ProcessorWrapper> processors = new ArrayList<ProcessorWrapper>();
    private Map<String, MenuItemCountHandler> countHandlers = new HashMap<String, MenuItemCountHandler>();
    private TreeItemProcessor treeItemProcessor;
    private Map<String, MenuItemFilter> menuItemFilters;

    private static class ProcessorWrapper {

        public String menuItemId;
        public MenuItemProcessor processor;
        public boolean runOnce;
        public boolean isExecutable;

        public ProcessorWrapper(String menuItemId, MenuItemProcessor processor, boolean runOnce) {
            this.menuItemId = menuItemId;
            this.processor = processor;
            this.runOnce = runOnce;
            this.isExecutable = true;

        }
    }

    @Override
    public void processTasks(Menu menu) {
        processTasks(menu, null);
    }

    @Override
    public void processTasks(Menu menu, Collection<String> onlyMenuItemIds) {
        long start = System.currentTimeMillis();
        System.out.println("PERFORMANCE: MENU PROCESS TASK START");
        process(menu, false, onlyMenuItemIds);
        System.out.println("PERFORMANCE: MENU PROCESS TASK END: " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public int getUpdateCount() {
        return updateCount;
    }

    @Override
    public void menuUpdated() {
        this.updateCount++;
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
            xstream.processAnnotations(BrowseMenuItem.class);
            Menu loadedMenu = (Menu) xstream.fromXML(resource.getInputStream());
            process(loadedMenu, true);
            menu = loadedMenu; // this is performed here at the end, atomically

        } catch (IOException e) {
            log.error("Menu configuration loading failed: " + menuConfigLocation, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<NodeRef> openTreeItem(DropdownMenuItem menuItem, NodeRef nodeRef) {
        return treeItemProcessor.openTreeItem(menuItem, nodeRef);
    }

    @Override
    public void setupTreeItem(DropdownMenuItem dd, NodeRef nodeRef) {
        treeItemProcessor.setupTreeItem(dd, nodeRef);
    }

    @Override
    public void setTreeItemProcessor(TreeItemProcessor processor) {
        treeItemProcessor = processor;
    }

    @Override
    public void addProcessor(String menuItemId, MenuItemProcessor processor, boolean runOnce) {
        processors.add(new ProcessorWrapper(menuItemId, processor, runOnce));
    }

    @Override
    public void setCountHandler(String menuItemId, MenuItemCountHandler countHandler) {
        countHandlers.put(menuItemId, countHandler);
        addProcessor(menuItemId, countHandler, false);
    }

    @Override
    public MenuItemCountHandler getCountHandler(String menuItemId) {
        return countHandlers.get(menuItemId);
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
        process(loadedMenu, reloaded, null);
    }

    private void process(Menu loadedMenu, boolean reloaded, Collection<String> onlyMenuItemIds) {
        for (ProcessorWrapper processorWrapper : processors) {
            if (processorWrapper.isExecutable || reloaded) {
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
    public ArrayList<String> getShortcuts() {
        NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        @SuppressWarnings("unchecked")
        ArrayList<String> shortcuts = (ArrayList<String>) nodeService.getProperty(user, MenuModel.Props.SHORTCUTS);
        if (shortcuts == null) {
            return new ArrayList<String>();
        }
        return shortcuts;
    }

    @Override
    public void addShortcut(String shortcut) {
        ArrayList<String> shortcuts = getShortcuts();
        shortcuts.add(shortcut);
        saveShortcuts(shortcuts);
    }

    @Override
    public void removeShortcut(String shortcut) {
        ArrayList<String> shortcuts = getShortcuts();
        shortcuts.remove(shortcut);
        saveShortcuts(shortcuts);
    }

    private void saveShortcuts(ArrayList<String> shortcuts) {
        NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        nodeService.setProperty(user, MenuModel.Props.SHORTCUTS, shortcuts);
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

    // END: getters / setters

}