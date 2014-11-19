<<<<<<< HEAD
package ee.webmedia.alfresco.imap.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

public class AttachmentListMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {

    private MenuService menuService;
    private ImapServiceExt imapServiceExt;

    @Override
    public int getCount(MenuItem menuItem) {
        return imapServiceExt.getAllFilesCount(imapServiceExt.getAttachmentRoot(), true);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("emailAttachments", this);
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setImapServiceExt(ImapServiceExt imapServiceExt) {
        this.imapServiceExt = imapServiceExt;
    }

    // END: getters / setters
}
=======
package ee.webmedia.alfresco.imap.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

public class AttachmentListMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {

    private MenuService menuService;
    private ImapServiceExt imapServiceExt;

    @Override
    public int getCount(MenuItem menuItem) {
        return imapServiceExt.getAllFilesCount(imapServiceExt.getAttachmentRoot(), true);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("emailAttachments", this);
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setImapServiceExt(ImapServiceExt imapServiceExt) {
        this.imapServiceExt = imapServiceExt;
    }

    // END: getters / setters
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
