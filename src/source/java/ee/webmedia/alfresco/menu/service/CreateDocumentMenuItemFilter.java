package ee.webmedia.alfresco.menu.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemFilter;
import ee.webmedia.alfresco.substitute.SubstitutionInfoHolder;

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
        if ("documentTypes".equalsIgnoreCase(menuItem.getId()) && SubstitutionInfoHolder.getSubstitutionInfo().isSubstituting())
            return false;

        return true;
    }

}
