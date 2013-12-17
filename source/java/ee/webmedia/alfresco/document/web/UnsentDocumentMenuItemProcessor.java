package ee.webmedia.alfresco.document.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * @author Kaarel Jõgeva
 */
public class UnsentDocumentMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {

    private MenuService menuService;
    private DocumentSearchService documentSearchService;
    public static final String UNSENT_DOCUMENT = "unsentDocument";

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler(UNSENT_DOCUMENT, this);
    }

    @Override
    public int getCount(MenuItem menuItem) {
        return documentSearchService.searchRecipientFinishedDocumentsCount();
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