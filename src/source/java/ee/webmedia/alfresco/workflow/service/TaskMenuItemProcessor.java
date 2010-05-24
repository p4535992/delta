package ee.webmedia.alfresco.workflow.service;

import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * @author Kaarel Jõgeva
 */
public class TaskMenuItemProcessor extends CountAddingMenuItemProcessor implements InitializingBean {

    private MenuService menuService;
    private DocumentSearchService documentSearchService;

    @Override
    protected int getCount(MenuItem menuItem) {
        QName taskType = QName.createQName(menuItem.getProcessor());
        return documentSearchService.getCurrentUsersTaskCount(taskType);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("assignmentTasks", this, false);
        menuService.addProcessor("informationTasks", this, false);
        menuService.addProcessor("opinionTasks", this, false);
        menuService.addProcessor("reviewTasks", this, false);
        menuService.addProcessor("signatureTasks", this, false);
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
