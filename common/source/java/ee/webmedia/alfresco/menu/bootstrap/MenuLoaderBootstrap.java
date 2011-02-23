package ee.webmedia.alfresco.menu.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.menu.service.MenuService;

public class MenuLoaderBootstrap extends AbstractModuleComponent {

    private MenuService menuService;

    @Override
    protected void executeInternal() throws Throwable {
        menuService.reload();
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

}
