package ee.webmedia.alfresco.document.forum.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * CountHandler for discussions menu item
 * 
 * @author Kaarel Jõgeva
 */
public class DiscussionsMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {
    private MenuService menuService;
    private DocumentSearchService documentSearchService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("discussions", this);
    }

    @Override
    public int getCount(MenuItem menuItem) {
        return documentSearchService.searchDiscussionDocuments(false).size();
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    // END: getters / setters
}
