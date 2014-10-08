package ee.webmedia.alfresco.adddocument.web;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.CountAddingMenuItemProcessor;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;

public class WebServiceDocumentsMenuItemProcessor extends CountAddingMenuItemProcessor implements MenuItemCountHandler, InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        BeanHelper.getMenuService().setCountHandler("webServiceDocuments", this);
    }

    @Override
    public int getCount(MenuItem menuItem) {
        return BeanHelper.getAddDocumentService().getAllDocumentFromWebServiceCount();
    }

}