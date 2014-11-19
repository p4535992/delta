<<<<<<< HEAD
package ee.webmedia.alfresco.menu.service;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemFilter;
import ee.webmedia.alfresco.substitute.web.SubstitutionBean;

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
        SubstitutionBean substitutionBean = (SubstitutionBean) FacesContextUtils.getRequiredWebApplicationContext( //
                FacesContext.getCurrentInstance()).getBean(SubstitutionBean.BEAN_NAME);

        if ("documentTypes".equalsIgnoreCase(menuItem.getId()) && substitutionBean.getSubstitutionInfo().isSubstituting()) {
            return false;
        }

        return true;
    }

}
=======
package ee.webmedia.alfresco.menu.service;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemFilter;
import ee.webmedia.alfresco.substitute.web.SubstitutionBean;

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
        SubstitutionBean substitutionBean = (SubstitutionBean) FacesContextUtils.getRequiredWebApplicationContext( //
                FacesContext.getCurrentInstance()).getBean(SubstitutionBean.BEAN_NAME);

        if ("documentTypes".equalsIgnoreCase(menuItem.getId()) && substitutionBean.getSubstitutionInfo().isSubstituting()) {
            return false;
        }

        return true;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
