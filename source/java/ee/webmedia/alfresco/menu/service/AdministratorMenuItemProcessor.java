package ee.webmedia.alfresco.menu.service;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;
import ee.webmedia.alfresco.user.service.UserService;

public class AdministratorMenuItemProcessor implements MenuItemProcessor, InitializingBean {

    private MenuService menuService;
    private UserService userService;
    private EInvoiceService einvoiceService;
    private ApplicationConstantsBean applicationConstantsBean;
    private final List<String> invoiceRelated = Arrays.asList("dimensions", "transactionTemplates", "transactionDescParameters");

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        List<MenuItem> subItems = menuItem.getSubItems();

        // Remove invoice related menu items
        if (!applicationConstantsBean.isEinvoiceEnabled()) {
            for (Iterator<MenuItem> iterator = subItems.iterator(); iterator.hasNext();) {
                MenuItem item = iterator.next();
                if (invoiceRelated.contains(item.getId())) {
                    iterator.remove();
                }
            }
        }

        // Administrators and document manager have no further restrictions
        if (userService.isDocumentManager()) {
            return;
        }

        if (userService.isInAccountantGroup()) {
            boolean applicableItemFound = false;
            for (MenuItem item : menuItem.getSubItems()) {
                if (item.isAccountant()) {
                    applicableItemFound = setupMenuItem(menuItem, item);
                    break;
                }
            }
            if (!applicableItemFound) {
                menuItem.setRenderingDisabled(true); // Disable entire administrator menu if no applicable items found
            }
            return;
        }

        if (userService.isInSupervisionGroup()) {
            boolean applicableItemFound = false;
            for (MenuItem item : menuItem.getSubItems()) {
                if (item.isSupervisor()) {
                    applicableItemFound = setupMenuItem(menuItem, item);
                    break;
                }
            }
            if (!applicableItemFound) {
                menuItem.setRenderingDisabled(true); // Disable entire administrator menu if no applicable items found
            }
            return;
        }
    }

    private boolean setupMenuItem(MenuItem menuItem, MenuItem item) {
        menuItem.setActionListener(item.getActionListener());
        menuItem.setOutcome(item.getOutcome());
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("administrator", this, false, true);
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setEinvoiceService(EInvoiceService einvoiceService) {
        this.einvoiceService = einvoiceService;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

}
