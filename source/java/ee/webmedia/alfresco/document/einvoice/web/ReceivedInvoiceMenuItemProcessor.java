package ee.webmedia.alfresco.document.einvoice.web;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.user.service.UserService;

public class ReceivedInvoiceMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {
    private MenuService menuService;
    private DocumentService documentService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("incomingEInvoice", this);
    }

    @Override
    public int getCount(MenuItem menuItem) {
        UserService userService = BeanHelper.getUserService();
        if (userService.isAdministrator() || userService.isDocumentManager() || userService.isInAccountantGroup()) {
            return documentService.getAllDocumentFromIncomingInvoiceCount();
        } else {
            return documentService.getUserDocumentFromIncomingInvoiceCount(AuthenticationUtil.getRunAsUser());
        }
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
