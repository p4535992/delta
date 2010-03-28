package ee.webmedia.alfresco.menu.service;

import org.alfresco.i18n.I18NUtil;

import ee.webmedia.alfresco.menu.model.MenuItem;

/**
 * Menu item processor that can be used as a base class for menu items which need count after title.
 * Just subclass this class and implement {@link CountAddingMenuItemProcessor#getCount()}.
 *
 * @author Romet Aidla
 */
public abstract class CountAddingMenuItemProcessor implements MenuService.MenuItemProcessor {
    @Override
    final public void doWithMenuItem(MenuItem menuItem) {
        if(menuItem.getTitle() == null) {
            menuItem.setTitle(I18NUtil.getMessage(menuItem.getTitleId()));
        }
        String title = menuItem.getTitle();

        int firstBrace = -1;
        if (title.endsWith(")")) {
            firstBrace = title.lastIndexOf('(');
        }

        int count = getCount();

        String titleSuffix = "";
        if (count != 0) {
            titleSuffix += " (" + count + ")";
        }

        if (firstBrace > 0) {
            title = title.substring(0, firstBrace);
        }

        menuItem.setTitle(title + titleSuffix);
    }

    abstract protected int getCount();
}
