package ee.webmedia.alfresco.menu.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.thoughtworks.xstream.XStream;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.menu.model.BrowseMenuItem;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;

/**
 * @author Kaarel Jõgeva
 */
public class MenuServiceImpl implements MenuService {

    private Menu menu;
    private String menuConfigLocation;
    private FileFolderService fileFolderService;
    private GeneralService generalService;
    private DocumentTypeService documentTypeService;

    private static Logger log = Logger.getLogger(MenuServiceImpl.class);

    @Override
    public Menu getMenu() {
        return this.menu;
    }

    @Override
    public NodeRef getNodeRefForXPath(FacesContext context, String XPath) {
        return generalService.getNodeRef(XPath);
    }

    @Override
    public void reload() {

        try {
            ResourceLoader resourceLoader = new DefaultResourceLoader();
            Resource resource = resourceLoader.getResource(menuConfigLocation);
            if (!resource.exists()) {
                throw new FileNotFoundException("UIMenuComponent configuration file does not exist: " + menuConfigLocation);
            }

            XStream xstream = new XStream();
            xstream.processAnnotations(Menu.class);
            xstream.processAnnotations(MenuItem.class);
            xstream.processAnnotations(DropdownMenuItem.class);
            xstream.processAnnotations(BrowseMenuItem.class);

            Menu loadedMenu = (Menu) xstream.fromXML(resource.getInputStream());
            process(loadedMenu.getSubItems());
            menu = loadedMenu;

        } catch (IOException e) {
            log.error("UIMenuComponent configuration loading failed: " + menuConfigLocation, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMenuXml() {
        XStream xstream = new XStream();
        xstream.processAnnotations(UIMenuComponent.class);
        xstream.processAnnotations(MenuItem.class);

        return xstream.toXML(menu);
    }

    @Override
    public int getNodeChildrenCount(NodeRef nodeRef) {
        return fileFolderService.listFolders(nodeRef).size();
    }

    // START: getters / setters

    public void setMenuConfigLocation(String menuConfigLocation) {
        this.menuConfigLocation = menuConfigLocation;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    // END: getters / setters

    protected boolean process(List<MenuItem> items) {
        if (items == null) {
            return true;
        }
        for (MenuItem item : items) {
            if ("documentTypes".equals(item.getId())) {
                processDocumentTypes(item.getSubItems());
                return false;
            }
            if (!process(item.getSubItems())) {
                return false;
            }
        }
        return true;
    }

    protected void processDocumentTypes(List<MenuItem> items) {
        if (items == null) {
            return;
        }
        for (Iterator<MenuItem> i = items.iterator(); i.hasNext();) {
            MenuItem item = i.next();
            if (!org.apache.commons.lang.StringUtils.isEmpty(item.getOutcome()) && !processDocumentType(item)) {
                i.remove();
            } else {
                processDocumentTypes(item.getSubItems());
            }
        }
    }

    protected boolean processDocumentType(MenuItem item) {
        DocumentType docType = documentTypeService.getDocumentType(item.getOutcome());
        if (docType == null) {
            log.warn("Document type not found: " + item.getOutcome());
            return false;
        }
        if (!docType.isUsed()) {
            return false;
        }
        item.setTitle(docType.getName());
        item.setOutcome("dialog:document");
        item.setActionListener("#{DocumentDialog.create}");
        Map<String, String> params = item.getParams();
        if (params == null) {
            params = new HashMap<String, String>();
            item.setParams(params);
        }
        params.put("documentType", docType.getId().toString());
        return true;
    }

}