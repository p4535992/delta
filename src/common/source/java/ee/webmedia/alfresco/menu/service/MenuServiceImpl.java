package ee.webmedia.alfresco.menu.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
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
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;

/**
 * @author Kaarel Jõgeva
 */
public class MenuServiceImpl implements MenuService {
    private static Logger log = Logger.getLogger(MenuServiceImpl.class);

    private String menuConfigLocation;
    private FileFolderService fileFolderService;
    private GeneralService generalService;
    private int updateCount;

    private Menu menu;
    private List<ProcessorWrapper> processors = new ArrayList<ProcessorWrapper>(); // doesn't need to be synchronized, because it is not modified after spring initialization
    private TreeItemProcessor treeItemProcessor;
    
    private static class ProcessorWrapper {

        public String menuItemId;
        public MenuItemProcessor processor;

        public ProcessorWrapper(String menuItemId, MenuItemProcessor processor) {
            this.menuItemId = menuItemId;
            this.processor = processor;
        }
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
            process(loadedMenu);
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
    public void addProcessor(String menuItemId, MenuItemProcessor processor) {
        processors.add(new ProcessorWrapper(menuItemId, processor));
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

    private void process(Menu loadedMenu) {
        for (ProcessorWrapper processorWrapper : processors) {
            if (processorWrapper.menuItemId == null) {
                for (MenuItem item : loadedMenu.getSubItems()) {
                    processorWrapper.processor.doWithMenuItem(item);
                }
            } else {
                process(processorWrapper, loadedMenu.getSubItems());
            }
        }
    }

    private void process(ProcessorWrapper processorWrapper, List<MenuItem> items) {
        if (items == null) {
            return;
        }
        for (MenuItem item : items) {
            if (processorWrapper.menuItemId.equals(item.getId())) {
                processorWrapper.processor.doWithMenuItem(item);
            } else {
                process(processorWrapper, item.getSubItems());
            }
        }
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
    
    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    
    // END: getters / setters

}