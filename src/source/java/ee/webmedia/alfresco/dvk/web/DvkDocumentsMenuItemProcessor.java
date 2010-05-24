package ee.webmedia.alfresco.dvk.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuService;

public class DvkDocumentsMenuItemProcessor extends CountAddingMenuItemProcessor implements InitializingBean {
    private MenuService menuService;
    private DocumentService documentService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("dvkDocuments", this, false);
    }

    @Override
    protected int getCount(MenuItem menuItem) {
        return documentService.getAllDocumentFromDvkCount();
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    // END: getters / setters
}