package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges.CREATE_DOCUMENT;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;

/**
 * @author Kaarel Jõgeva
 */
public class DocumentDynamicTypeMenuItemProcessor implements InitializingBean, MenuItemProcessor {

    private static final Comparator<MenuItem> COMPARATOR = new Comparator<MenuItem>() {

        @Override
        public int compare(MenuItem o1, MenuItem o2) {
            return AppConstants.DEFAULT_COLLATOR.compare(o1.getTitle(), o2.getTitle());
        }
    };

    private MenuService menuService;
    private DocumentAdminService documentAdminService;
    private PermissionService permissionService;

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        List<DocumentType> documentTypes = documentAdminService.getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
        if (documentTypes.isEmpty()) {
            return;
        }

        Map<String, List<DocumentType>> structure = new HashMap<String, List<DocumentType>>();
        List<DocumentType> subitems;
        /*
         * TODO ALSeadist asjatoimiku liikide menüü loomine, õiguste kontrolli puhul kasutada
         * DocumentCommonModel.Privileges.CREATE_DOCUMENT
         * asemel õigust:
         * public static final String CREATE_CASE_FILE = "createCaseFile";
         */
        for (DocumentType documentType : documentTypes) {
            // Check if current user can create this type of document
            boolean restricted = false;
            for (AccessPermission accessPermission : permissionService.getAllSetPermissions(documentType.getNodeRef())) {

                if (CREATE_DOCUMENT.equals(accessPermission.getPermission())) {
                    restricted = true;
                    break;
                }
            }

            if (restricted && AccessStatus.DENIED == permissionService.hasPermission(documentType.getNodeRef(), CREATE_DOCUMENT)) {
                continue;
            }

            String menuGroupName = documentType.getMenuGroupName();
            if (isBlank(menuGroupName)) {
                menuGroupName = "";
            }

            subitems = structure.get(menuGroupName);
            if (subitems == null) {
                structure.put(menuGroupName, new ArrayList<DocumentType>(Arrays.asList(documentType)));
            } else {
                subitems.add(documentType);
            }
        }

        // Create menu items from structure
        List<MenuItem> children;
        List<MenuItem> firstLevelSubItems = menuItem.getSubItems();
        for (Entry<String, List<DocumentType>> entry : structure.entrySet()) {
            String groupName = entry.getKey();
            if (isNotBlank(groupName)) {
                DropdownMenuItem group = new DropdownMenuItem();
                group.setTitle(groupName);
                group.setSubmenuId(groupName + "-submenu");
                firstLevelSubItems.add(group);
                children = group.getSubItems();
            } else {
                children = firstLevelSubItems;
            }

            for (DocumentType type : entry.getValue()) {
                MenuItem item = new MenuItem();
                item.setTitle(type.getName());
                item.setActionListener("#{DocumentDynamicDialog.createDraft}");
                item.getParams().put("documentTypeId", type.getId());
                children.add(item);
            }
        }
        Collections.sort(firstLevelSubItems, COMPARATOR);
        for (MenuItem subItem : firstLevelSubItems) {
            if(subItem.hasSubItems()){
                Collections.sort(subItem.getSubItems(), COMPARATOR);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("documentDynamicTypes", this, true, true);
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // END: getters / setters

}
