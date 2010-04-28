package ee.webmedia.alfresco.imap.web;

import org.alfresco.web.bean.repository.Node;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuService;

public class AttachmentListMenuItemProcessor extends CountAddingMenuItemProcessor implements InitializingBean {

    private MenuService menuService;
    private GeneralService generalService;
    private FileService fileService;
    private ImapServiceExt imapServiceExt;
    
    @Override
    protected int getCount() {
        Node node = generalService.fetchNode(imapServiceExt.getAttachmentRoot());
        Assert.notNull(node, "Attachment root not found");
        return fileService.getAllFilesExcludingDigidocSubitems(node.getNodeRef()).size();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("emailAttachments", this, false);
    }
    
    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setImapServiceExt(ImapServiceExt imapServiceExt) {
        this.imapServiceExt = imapServiceExt;
    }

    // END: getters / setters
}
