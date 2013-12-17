package ee.webmedia.alfresco.document.scanned.web;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * @author Kaarel Jõgeva
 */
public class ScannedDocumentsMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {
    private MenuService menuService;
    private FileFolderService fileFolderService;
    private GeneralService generalService;
    private NodeService nodeService;
    private String scannedFilesPath;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("scannedDocuments", this);
    }

    @Override
    public int getCount(MenuItem menuItem) {
        NodeRef nodeRef = generalService.getNodeRef(scannedFilesPath);
        List<FileInfo> folders = fileFolderService.listFolders(nodeRef);
        int count = 0;
        for (FileInfo fileInfo : folders) {
            count += nodeService.getChildAssocs(fileInfo.getNodeRef(), ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL).size();
        }

        return count + fileFolderService.listFiles(nodeRef).size();
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setScannedFilesPath(String scannedFilesPath) {
        this.scannedFilesPath = scannedFilesPath;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    // END: getters / setters
}
