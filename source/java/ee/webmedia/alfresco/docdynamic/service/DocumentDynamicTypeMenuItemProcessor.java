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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;

/**
 * @author Kaarel Jõgeva
 */
public class DocumentDynamicTypeMenuItemProcessor implements InitializingBean, MenuItemProcessor {

    public static final Comparator<MenuItem> COMPARATOR = new Comparator<MenuItem>() {

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
        processDynamicTypeMenuItem(permissionService, CREATE_DOCUMENT, documentTypes, menuItem, "#{DocumentDynamicDialog.createDraft}");
    }

    public static void processDynamicTypeMenuItem(PermissionService permissionService, String permission, List<? extends DynamicType> types, MenuItem menuItem,
            String actionListener) {
        Assert.isTrue(StringUtils.isNotBlank(permission), "permission is mandatory");
        Assert.isTrue(StringUtils.isNotBlank(actionListener), "action listener is mandatory");
        if (types.isEmpty()) {
            return;
        }

        Map<String, List<DynamicType>> structure = new HashMap<String, List<DynamicType>>();
        List<DynamicType> subitems;

        for (DynamicType documentType : types) {
            // Check if current user can create this type of item
            if (!hasPermission(permissionService, permission, documentType)) {
                continue;
            }

            String menuGroupName = documentType.getMenuGroupName();
            if (isBlank(menuGroupName)) {
                menuGroupName = "";
            }

            subitems = structure.get(menuGroupName);
            if (subitems == null) {
                structure.put(menuGroupName, new ArrayList<DynamicType>(Arrays.asList(documentType)));
            } else {
                subitems.add(documentType);
            }
        }

        // Create menu items from structure
        List<MenuItem> children;
        List<MenuItem> firstLevelSubItems = menuItem.getSubItems();
        for (Entry<String, List<DynamicType>> entry : structure.entrySet()) {
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

            for (DynamicType type : entry.getValue()) {
                MenuItem item = new MenuItem();
                item.setTitle(type.getName());
                item.setActionListener(actionListener);
                item.getParams().put("typeId", type.getId());
                children.add(item);
            }
        }
        Collections.sort(firstLevelSubItems, COMPARATOR);
        for (MenuItem subItem : firstLevelSubItems) {
            if (subItem.hasSubItems()) {
                Collections.sort(subItem.getSubItems(), COMPARATOR);
            }
        }
    }

    public static boolean hasPermission(PermissionService permissionService, String permission, DynamicType documentType) {
        boolean restricted = false;
        for (AccessPermission accessPermission : permissionService.getAllSetPermissions(documentType.getNodeRef())) {
            if (permission.equals(accessPermission.getPermission())) {
                restricted = true;
                break;
            }
        }
        if (restricted && AccessStatus.DENIED == permissionService.hasPermission(documentType.getNodeRef(), permission)) {
            return false;
        }
        return true;
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