package ee.webmedia.alfresco.menu.service;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.web.MenuItemCountBean;

public class CountAddingMenuItemProcessor implements MenuService.MenuItemProcessor, InitializingBean {

    final public static char COUNT_SUFFIX_START = '(';
    final public static char COUNT_SUFFIX_END = ')';

    private MenuService menuService;

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
        Integer count = menuItemCountBean.getCount(menuItem.getId());

        String title = menuItem.getTitle();
        int firstBrace = -1;
        if (title.endsWith(String.valueOf(COUNT_SUFFIX_END))) {
            firstBrace = title.lastIndexOf(COUNT_SUFFIX_START);
        }
        String titleSuffix = "";
        if (count != null && count > 0) {
            int maxReslts = menuItemCountBean.getMaxSearchResultRows();
            titleSuffix += " " + COUNT_SUFFIX_START + (count > maxReslts ? (maxReslts + "+") : count) + COUNT_SUFFIX_END;
        }
        if (firstBrace > 0) {
            title = title.substring(0, firstBrace);
        }
        menuItem.setTitle(title + titleSuffix);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor(MenuItem.ASSIGNMENT_TASKS, this, false, true);
        menuService.addProcessor(MenuItem.ORDER_ASSIGNMENT_TASKS, this, false, true);
        menuService.addProcessor(MenuItem.INFORMATION_TASKS, this, false, true);
        menuService.addProcessor(MenuItem.OPINION_TASKS, this, false, true);
        menuService.addProcessor(MenuItem.REVIEW_TASKS, this, false, true);
        menuService.addProcessor(MenuItem.EXTERNAL_REVIEW_TASKS, this, false, true);
        menuService.addProcessor(MenuItem.SIGNATURE_TASKS, this, false, true);
        menuService.addProcessor(MenuItem.CONFIRMATION_TASKS, this, false, true);
        menuService.addProcessor(MenuItem.USER_WORKING_DOCUMENTS, this, false, true);
        menuService.addProcessor(MenuItem.DISCUSSIONS, this, false, true);
        menuService.addProcessor(MenuItem.FOR_REGISTERING_LIST, this, false, true);
        menuService.addProcessor(MenuItem.USER_COMPOUND_WORKFLOWS, this, false, true);
        menuService.addProcessor(MenuItem.USER_CASE_FILES, this, false, true);
        menuService.addProcessor(MenuItem.SEND_FAILURE_NOTIFICATION, this, false, true);
        menuService.addProcessor(MenuItem.SCANNED_DOCUMENTS, this, false, true);
        menuService.addProcessor(MenuItem.INCOMING_EINVOICE, this, false, true);
        menuService.addProcessor(MenuItem.OUTBOX_DOCUMENT, this, false, true);
        menuService.addProcessor(MenuItem.INCOMING_EMAILS, this, false, true);
        menuService.addProcessor(MenuItem.DVK_DOCUMENTS, this, false, true);
        menuService.addProcessor(MenuItem.DVK_CORRUPT, this, false, true);
        menuService.addProcessor(MenuItem.EMAIL_ATTACHMENTS, this, false, true);
        menuService.addProcessor(MenuItem.SENT_EMAILS, this, false, true);
        menuService.addProcessor(MenuItem.UNSENT_DOCUMENT, this, false, true);
        menuService.addProcessor(MenuItem.WEB_SERVICE_DOCUMENTS, this, false, true);
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

}
