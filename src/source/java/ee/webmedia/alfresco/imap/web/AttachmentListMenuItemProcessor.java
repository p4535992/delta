package ee.webmedia.alfresco.imap.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuService;

public class AttachmentListMenuItemProcessor extends CountAddingMenuItemProcessor implements InitializingBean {

    private MenuService menuService;
    private FileService fileService;
    private ImapServiceExt imapServiceExt;
    
    @Override
    protected int getCount(MenuItem menuItem) {
        return fileService.getAllFilesExcludingDigidocSubitems(imapServiceExt.getAttachmentRoot()).size();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("emailAttachments", this, false);
    }
    
    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setImapServiceExt(ImapServiceExt imapServiceExt) {
        this.imapServiceExt = imapServiceExt;
    }

    // END: getters / setters
}
