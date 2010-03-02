package ee.webmedia.alfresco.document.register.web;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuService;
import org.springframework.beans.factory.InitializingBean;

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
    protected int getCount() {
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
