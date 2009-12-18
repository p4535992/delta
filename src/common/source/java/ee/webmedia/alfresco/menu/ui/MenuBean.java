package ee.webmedia.alfresco.menu.ui;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.service.MenuService;

/**
 * @author Kaarel Jõgeva
 */
public class MenuBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "MenuBean";

    private transient MenuService menuService;

    private String activeItemId = "0";

    public Menu getMenu() {
        return menuService.getMenu();
    }

    public String getActiveItemId() {
        return activeItemId;
    }

    public void setActiveItemId(String activeMenuId) {
        this.activeItemId = activeMenuId;
    }
    
    public void reloadMenu() {
        getMenuService().reload();
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public MenuService getMenuService() {
        if (menuService == null) {
            menuService = (MenuService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(MenuService.BEAN_NAME);
        }
        return menuService;
    }

    // END: getters / setters
}
