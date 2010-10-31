package ee.webmedia.alfresco.menu.service;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.SessionContext;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemFilter;

public class CreateDocumentMenuItemFilter implements MenuItemFilter {

    @Override
    public String openItemActionsForType(DropdownMenuItem dd, NodeRef nodeRef, QName type) {
        dd.getSubItems().clear();
        MenuItem tmpItem = new MenuItem();
        tmpItem.setTitleId("menu_substituting_no_new_doc");
        dd.getSubItems().add(tmpItem);

        return "done";
    }

    @Override
    public boolean passesFilter(MenuItem menuItem, NodeRef childNodeRef) {
        SessionContext sessionContext = (SessionContext) FacesContextUtils.getRequiredWebApplicationContext( //
                FacesContext.getCurrentInstance()).getBean(SessionContext.BEAN_NAME);        
        if ("documentTypes".equalsIgnoreCase(menuItem.getId()) && sessionContext.getSubstitutionInfo().isSubstituting())
            return false;

        return true;
    }

}
