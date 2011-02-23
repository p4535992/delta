package ee.webmedia.alfresco.menu.service;

import javax.faces.context.FacesContext;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.web.app.servlet.FacesHelper;

import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.web.MenuItemCountBean;

/**
 * Menu item processor that can be used as a base class for menu items which need count after title.
 * Just subclass this class and implement {@link CountAddingMenuItemProcessor#getCount()}.
 *
 * @author Romet Aidla
 * @author Alar Kvell
 */
public abstract class CountAddingMenuItemProcessor implements MenuService.MenuItemProcessor {
    
    @Override
    final public void doWithMenuItem(MenuItem menuItem) {

        if(menuItem.getTitle() == null) {
            menuItem.setTitle(I18NUtil.getMessage(menuItem.getTitleId()));
            menuItem.getStyleClass().add("menuItemCount");
        }

        MenuItemCountBean menuItemCountBean = (MenuItemCountBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuItemCountBean.BEAN_NAME);
        Integer count = menuItemCountBean.getCount(menuItem.getId());

        int countValue = count == null ? 0 : count.intValue();

        String title = menuItem.getTitle();
        int firstBrace = -1;
        if (title.endsWith(")")) {
            firstBrace = title.lastIndexOf('(');
        }
        String titleSuffix = "";
        if (countValue != 0) {
            titleSuffix += " (" + countValue + ")";
        }
        if (firstBrace > 0) {
            title = title.substring(0, firstBrace);
        }
        menuItem.setTitle(title + titleSuffix);
    }

}
