package ee.webmedia.alfresco.document.type.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.NamespaceService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;

public class DocumentTypeMenuItemProcessor implements MenuItemProcessor, InitializingBean {

    private static Logger log = Logger.getLogger(DocumentTypeMenuItemProcessor.class);

    private MenuService menuService;
    private DocumentTypeService documentTypeService;
    private NamespaceService namespaceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("documentTypes", this, true);
    }

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        List<DocumentType> allDocumentTypes = documentTypeService.getAllDocumentTypes();
        traverse(menuItem, allDocumentTypes);
    }

    private void traverse(MenuItem menuItem, List<DocumentType> allDocumentTypes) {
        List<MenuItem> items = menuItem.getSubItems();
        if (items == null) {
            return;
        }
        List<MenuItem> emptyItems = new ArrayList<MenuItem>();
        for (Iterator<MenuItem> i = items.iterator(); i.hasNext();) {
            MenuItem item = i.next();
            if (StringUtils.isNotEmpty(item.getOutcome()) && !process(item, allDocumentTypes)) {
                i.remove();
            } else {
                traverse(item, allDocumentTypes);
                // if submenu is emty, remove menu item
                if (StringUtils.isEmpty(item.getOutcome()) && (item.getSubItems() == null || item.getSubItems().size() == 0)) {
                    emptyItems.add(item);
                }
            }
        }
        items.removeAll(emptyItems);
    }

    private boolean process(MenuItem item, List<DocumentType> allDocumentTypes) {
        DocumentType docType = null;
        for (DocumentType tmpType : allDocumentTypes) {
            if (tmpType.getId().toPrefixString(namespaceService).equalsIgnoreCase(item.getOutcome())) {
                docType = tmpType;
                break;
            }
        }
        if (docType == null) {
            log.warn("Document type not found: " + item.getOutcome());
            return false;
        }
        if (!docType.isUsed()) {
            return false;
        }
        item.setTitle(docType.getName());
        item.setOutcome("#{DocumentDialog.action}");
        item.setActionListener("#{DocumentDialog.create}");
        Map<String, String> params = item.getParams();
        if (params == null) {
            params = new HashMap<String, String>();
            item.setParams(params);
        }
        params.put("documentType", docType.getId().toString());
        return true;
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    // END: getters / setters

}
