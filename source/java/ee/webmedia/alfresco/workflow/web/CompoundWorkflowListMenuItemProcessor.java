package ee.webmedia.alfresco.workflow.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * @author Riina Tens
 */
public class CompoundWorkflowListMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {

    private MenuService menuService;
    private DocumentSearchService documentSearchService;

    @Override
    public int getCount(MenuItem menuItem) {
        return documentSearchService.getCurrentUserCompoundWorkflowsCount();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.setCountHandler("userCompoundWorkflows", this);
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
