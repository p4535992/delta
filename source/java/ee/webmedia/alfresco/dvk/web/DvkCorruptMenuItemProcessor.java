<<<<<<< HEAD
package ee.webmedia.alfresco.dvk.web;

import java.util.List;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

public class DvkCorruptMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {

    private MenuService menuService;
    private GeneralService generalService;
    private FileFolderService fileFolderService;
    private DvkService dvkService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("dvkCorrupt", this);
    }

    @Override
    public int getCount(MenuItem menuItem) {
        List<FileInfo> fileInfos = fileFolderService.listFiles(generalService.getNodeRef(dvkService.getCorruptDvkDocumentsPath()));
        return fileInfos != null ? fileInfos.size() : 0;
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    // END: getters / setters
=======
package ee.webmedia.alfresco.dvk.web;

import java.util.List;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

public class DvkCorruptMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {

    private MenuService menuService;
    private GeneralService generalService;
    private FileFolderService fileFolderService;
    private DvkService dvkService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("dvkCorrupt", this);
    }

    @Override
    public int getCount(MenuItem menuItem) {
        List<FileInfo> fileInfos = fileFolderService.listFiles(generalService.getNodeRef(dvkService.getCorruptDvkDocumentsPath()));
        return fileInfos != null ? fileInfos.size() : 0;
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    // END: getters / setters
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}