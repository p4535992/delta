package ee.webmedia.alfresco.workflow.service;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;

/**
 * @author Kaarel Jõgeva
 */
public class TaskMenuItemProcessor implements MenuItemProcessor, InitializingBean {

    private MenuService menuService;
    private DocumentSearchService documentSearchService;

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        QName taskType = QName.createQName(menuItem.getProcessor());

        int count = documentSearchService.getCurrentUsersTaskCount(taskType);
        if(menuItem.getTitle() == null) {
            menuItem.setTitle(I18NUtil.getMessage(menuItem.getTitleId()));
        }
        String title = menuItem.getTitle();
        int firstBrace = -1;
        if (title.endsWith(")")) {
            firstBrace = title.lastIndexOf('(');
        }
        String titleSuffix = "";

        if (count != 0) {
            titleSuffix += " (" + count + ")";
        }

        if (firstBrace > 0) {
            title = title.substring(0, firstBrace);
        }

        menuItem.setTitle(title + titleSuffix);
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
