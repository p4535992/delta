package ee.webmedia.alfresco.menu.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuItem;

/**
 * @author Kaarel Jõgeva
 */
public interface MenuService {

    String BEAN_NAME = "MenuService";

    Menu getMenu();

    void reload();

    String getMenuXml();

    NodeRef getNodeRefForXPath(FacesContext context, String XPath);

    int getNodeChildrenCount(NodeRef nodeRef);

    /**
     * Add a processor that gets executed after (re-)loading the menu. Processors are executed in registration order. More than one processor can be registered
     * with the same {@code menuItemId}.
     * 
     * @param menuItemId the id of menuitem that gets passed to processor callback, or {@code null} if all root menuitems are passed to processor callback
     * @param processor
     */
    void addProcessor(String menuItemId, MenuItemProcessor processor, boolean runOnce);

    public interface MenuItemProcessor {

        void doWithMenuItem(MenuItem menuItem);

    }

    void setTreeItemProcessor(TreeItemProcessor processor);

    public interface TreeItemProcessor {

        List<NodeRef> openTreeItem(DropdownMenuItem dd, NodeRef nodeRef);

        void setupTreeItem(DropdownMenuItem dd, NodeRef nodeRef);

    }

    public interface MenuItemFilter {

        boolean passesFilter(MenuItem menuItem, NodeRef childNodeRef);

        /**
         * Provides ability to execute third party actions when opening a MenuItem. For example call some dialog's method
         * 
         * @param nodeRef NodeRef this item represents
         * @param dd DropdownMenuItem that is being opened
         * @param type QName on item that is being opened
         * @return returns null if further processing is not needed
         */
        String openItemActionsForType(DropdownMenuItem dd, NodeRef nodeRef, QName type);
    }

    List<NodeRef> openTreeItem(DropdownMenuItem menuItem, NodeRef nodeRef);

    void setupTreeItem(DropdownMenuItem dd, NodeRef nodeRef);

    /**
     * Measure to check if bean has the latest menu configuration
     * 
     * @return number of updates since last deploy
     */
    int getUpdateCount();

    /**
     * When a menu update is needed, this method invalidates beans menu configurations so it's reloaded from MenuService
     */
    void menuUpdated();

    void processTasks(Menu menu);

    void processTasks(Menu menu, Collection<String> onlyMenuItemIds);

    List<String> getShortcuts();

    void addShortcut(String shortcut);

    void removeShortcut(String shortcut);

    Map<String, MenuItemFilter> getMenuItemFilters();

}
