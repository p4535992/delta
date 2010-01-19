package ee.webmedia.alfresco.menu.service;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;

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
    void addProcessor(String menuItemId, MenuItemProcessor processor);

    public interface MenuItemProcessor {

        void doWithMenuItem(MenuItem menuItem);

    }

}
