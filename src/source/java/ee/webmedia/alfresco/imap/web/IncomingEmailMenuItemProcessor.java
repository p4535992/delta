package ee.webmedia.alfresco.imap.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuService;

public class IncomingEmailMenuItemProcessor extends CountAddingMenuItemProcessor implements InitializingBean {

    private MenuService menuService;
    private DocumentService documentService;
    
    @Override
    protected int getCount(MenuItem menuItem) {
        return documentService.getIncomingEmailsCount();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("incomingEmails", this, false);
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
