package ee.webmedia.alfresco.document.search.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * @author Kaarel Jõgeva
 */
public class UserWorkingDocumentsMenuItemProcessor extends CountAddingMenuItemProcessor implements InitializingBean {
    
        private MenuService menuService;
        private DocumentSearchService documentSearchService;

        @Override
        public void afterPropertiesSet() throws Exception {
            menuService.addProcessor("userWorkingDocuments", this, false);
        }

        @Override
        protected int getCount(MenuItem menuItem) {
            return documentSearchService.searchUserWorkingDocumentsCount();
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