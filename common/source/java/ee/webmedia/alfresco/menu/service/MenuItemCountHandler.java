package ee.webmedia.alfresco.menu.service;

import ee.webmedia.alfresco.menu.model.MenuItem;

public interface MenuItemCountHandler extends MenuService.MenuItemProcessor {

    int getCount(MenuItem menuItem);

}
