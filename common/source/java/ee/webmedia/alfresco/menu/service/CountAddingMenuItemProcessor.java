package ee.webmedia.alfresco.menu.service;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.web.MenuItemCountBean;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.menu.web.MenuItemCountBean.MenuItemCountVO;
>>>>>>> develop-5.1

/**
 * Menu item processor that can be used as a base class for menu items which need count after title.
 * Just subclass this class and implement {@link CountAddingMenuItemProcessor#getCount()}.
<<<<<<< HEAD
 * 
 * @author Romet Aidla
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
 */
public abstract class CountAddingMenuItemProcessor implements MenuService.MenuItemProcessor {

    final public static char COUNT_SUFFIX_START = '(';
    final public static char COUNT_SUFFIX_END = ')';

    @Override
    final public void doWithMenuItem(MenuItem menuItem) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            // If this is called from MenuService.reload which is called from an updater script which runs at deploy time,
            // then FacesContext is not initialized
            return;
        }

        String itemTitle = menuItem.getTitle();
        boolean isValueBinding = StringUtils.startsWith(itemTitle, "#{");
        if (itemTitle == null || isValueBinding) {
            if (isValueBinding) {
                ValueBinding vb = facesContext.getApplication().createValueBinding(itemTitle);
                if (vb != null) {
                    menuItem.setTitle((String) vb.getValue(facesContext));
                }
            } else {
                menuItem.setTitle(I18NUtil.getMessage(menuItem.getTitleId()));
            }
            menuItem.getStyleClass().add("menuItemCount");
        }

        MenuItemCountBean menuItemCountBean = (MenuItemCountBean) FacesHelper.getManagedBean(facesContext, MenuItemCountBean.BEAN_NAME);
<<<<<<< HEAD
        Integer count = menuItemCountBean.getCount(menuItem.getId());

        int countValue = count == null ? 0 : count.intValue();
=======
        MenuItemCountVO countVO = menuItemCountBean.getCount(menuItem.getId());
>>>>>>> develop-5.1

        String title = menuItem.getTitle();
        int firstBrace = -1;
        if (title.endsWith(String.valueOf(COUNT_SUFFIX_END))) {
            firstBrace = title.lastIndexOf(COUNT_SUFFIX_START);
        }
        String titleSuffix = "";
<<<<<<< HEAD
        if (countValue != 0) {
            titleSuffix += " " + COUNT_SUFFIX_START + countValue + COUNT_SUFFIX_END;
=======
        if (countVO.count != null && countVO.count > 0) {
            titleSuffix += " " + COUNT_SUFFIX_START + countVO.count + (countVO.exceedsMaxSearchResultRows ? "+" : "") + COUNT_SUFFIX_END;
>>>>>>> develop-5.1
        }
        if (firstBrace > 0) {
            title = title.substring(0, firstBrace);
        }
        menuItem.setTitle(title + titleSuffix);
    }

}
