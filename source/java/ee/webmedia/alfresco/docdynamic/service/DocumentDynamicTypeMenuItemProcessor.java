package ee.webmedia.alfresco.docdynamic.service;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;

public class DocumentDynamicTypeMenuItemProcessor implements InitializingBean, MenuItemProcessor {

    public static final Comparator<MenuItem> COMPARATOR = new Comparator<MenuItem>() {

        @Override
        public int compare(MenuItem o1, MenuItem o2) {
            return AppConstants.getNewCollatorInstance().compare(o1.getTitle(), o2.getTitle());
        }
    };

    private MenuService menuService;
    private DocumentAdminService documentAdminService;
    private PrivilegeService privilegeService;

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        List<DocumentType> documentTypes = documentAdminService.getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
        processDynamicTypeMenuItem(privilegeService, Privilege.CREATE_DOCUMENT, documentTypes, menuItem, "#{DocumentDynamicDialog.createDraft}");
    }

    public static void processDynamicTypeMenuItem(PrivilegeService privilegeService, Privilege permission, List<? extends DynamicType> types, MenuItem menuItem,
            String actionListener) {
        Assert.notNull(permission, "permission is mandatory");
        Assert.isTrue(StringUtils.isNotBlank(actionListener), "action listener is mandatory");
        if (types.isEmpty()) {
            return;
        }
        String userName = AuthenticationUtil.getRunAsUser();
        boolean isDocManager = BeanHelper.getUserService().isDocumentManager();
        Map<String, List<String>> createDocumentPrivileges = null;
        Set<String> userAuthorities = null;
        if (!isDocManager) {
            Map<String, NodeRef> docTypeNodeRefs = new HashMap<String, NodeRef>();
            for (DynamicType documentType : types) {
                NodeRef nodeRef = documentType.getNodeRef();
                docTypeNodeRefs.put(nodeRef.getId(), nodeRef);
            }
            Set<String> nodeIds = docTypeNodeRefs.keySet();
            createDocumentPrivileges = Privilege.CREATE_DOCUMENT.equals(permission) ? privilegeService.getCreateDocumentPrivileges(nodeIds)
                    : privilegeService.getCreateCaseFilePrivileges(nodeIds);
            userAuthorities = new HashSet<String>(BeanHelper.getAuthorityService().getContainedAuthorities(null, userName, false));
            userAuthorities.add(userName);
        }
        List<DynamicType> subitems;
        Map<String, List<DynamicType>> structure = new HashMap<String, List<DynamicType>>();
        for (DynamicType documentType : types) {
            // Check if current user can create this type of item
            if (!isDocManager && !hasCreatePermission(createDocumentPrivileges, userName, userAuthorities, documentType.getNodeRef())) {
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

    public static boolean hasCreatePermission(Map<String, List<String>> existingPrivileges, String userName, Set<String> userAuthorities, NodeRef docTypeNodeRef) {
        String docTypeNodeId = docTypeNodeRef.getId();
        if (existingPrivileges.containsKey(docTypeNodeId)) {
            for (String authority : existingPrivileges.get(docTypeNodeId)) {
                if (userAuthorities.contains(authority)) {
                    return true;
                }
            }
            return false;
        }
        // if createDocument privilege is not set for any authority, creating documents of that type is allowed for everybody
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

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    // END: getters / setters

}
