package ee.webmedia.alfresco.imap.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * @author Riina Tens
 */
public class SendFailureNotificationListMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {

    private MenuService menuService;
    private ImapServiceExt imapServiceExt;

    @Override
    public int getCount(MenuItem menuItem) {
        return imapServiceExt.getAllFilesCount(imapServiceExt.getSendFailureNoticeRoot(), true);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("sendFailureNotification", this);
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
