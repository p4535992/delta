package ee.webmedia.alfresco.archivals.service;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class DocumentRegisterMenuItemProcessor implements MenuItemProcessor, InitializingBean {

    private MenuService menuService;
    private GeneralService generalService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("documentRegister", this, true, false);
    }

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        for (ArchivalsStoreVO archivalsStoreVO : generalService.getArchivalsStoreVOs()) {
            DropdownMenuItem childMenuItem = new DropdownMenuItem();

            // <dropdown title-id="menu_archivals_list" browse="true">
            // <xpath>/{http://alfresco.webmedia.ee/model/functions/1.0}archivals</xpath>
            // <store>workspace://ArchivalsStore</store>
            // <submenu-id>document-register</submenu-id>
            // <outcome>dialog:archivedFunctionsListDialog</outcome>
            // <action-listener>#{MenuBean.updateTree}</action-listener>
            // </dropdown>

            childMenuItem.setTitle(archivalsStoreVO.getTitle());
            childMenuItem.setBrowse(true);
            childMenuItem.setXPath(archivalsStoreVO.getPrimaryPath());
            childMenuItem.setStore(archivalsStoreVO.getStoreRef().toString());
            childMenuItem.setSubmenuId("document-register");
            childMenuItem.setActionListener("#{ArchivedFunctionsListDialog.setup}");
            childMenuItem.getParams().put("nodeRef", archivalsStoreVO.getNodeRef().toString());

            menuItem.getSubItems().add(childMenuItem);
        }
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
    // END: getters / setters

}
