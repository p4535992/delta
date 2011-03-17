package ee.webmedia.alfresco.workflow.service;

import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * @author Kaarel Jõgeva
 */
public class TaskMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {

    private MenuService menuService;
    private DocumentSearchService documentSearchService;

    @Override
    public int getCount(MenuItem menuItem) {
        QName taskType = QName.createQName(menuItem.getProcessor());
        return documentSearchService.getCurrentUsersTaskCount(taskType);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("assignmentTasks", this);
        menuService.setCountHandler("informationTasks", this);
        menuService.setCountHandler("opinionTasks", this);
        menuService.setCountHandler("reviewTasks", this);
        menuService.setCountHandler("externalReviewTasks", this);
        menuService.setCountHandler("signatureTasks", this);
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    // END: getters / setters

}
