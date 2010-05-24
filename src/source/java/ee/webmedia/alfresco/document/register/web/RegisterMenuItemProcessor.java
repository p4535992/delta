package ee.webmedia.alfresco.document.register.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * TODO: add comment
 *
 * @author Romet Aidla
 */
public class RegisterMenuItemProcessor extends CountAddingMenuItemProcessor implements InitializingBean {
    private MenuService menuService;
    private DocumentSearchService documentSearchService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("forRegisteringList", this, false);
    }

    @Override
    protected int getCount(MenuItem menuItem) {
        return documentSearchService.getCountOfDocumentsForRegistering();
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
