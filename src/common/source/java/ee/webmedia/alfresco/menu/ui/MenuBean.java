package ee.webmedia.alfresco.menu.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.component.MenuRenderer;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;

/**
 * @author Kaarel Jõgeva
 */
public class MenuBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MenuBean.class);

    public static final String BEAN_NAME = "MenuBean";
    public static final String UPDATE_TREE_ACTTIONLISTENER = "#{MenuBean.updateTree}";
    public static final int DOCUMENT_REGISTER_ID = 1; // XXX: this order defined in menu-structure.xml

    private transient MenuService menuService;
    private transient GeneralService generalService;
    private Menu menu;
    private int updateCount = 0;
    private String lastLinkId;
    private NodeRef linkNodeRef;

    private String activeItemId = "0";
    
    public void updateTree(ActionEvent event) {
        final UIComponent link = event.getComponent();
        setLastLinkId(((UIActionLink)link).getId());
        
        // NOTE: In XML nodes are referenced by xPath, but since all child association names are with the same (function, series etc)
        // Therefore items generated at runtime should be referenced by NodeRef
        if (link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_NODEREF) != null) {
            linkNodeRef = (NodeRef) link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_NODEREF);
        } else if (link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_XPATH) != null) {
            linkNodeRef = getGeneralService().getNodeRef((String) link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_XPATH));
        } else {
            log.error("NodeRef and xPath cannot be null at the same time on DropdownMenuItem!");
            throw new RuntimeException();
        }
        updateTree();
    }

    public void updateTree() {
        if(lastLinkId == null || linkNodeRef == null) {
            String msg = "MenuBean.updateTree() called, but info from last UIActionLink ActionEvent is missing!";
            log.error(msg);
            throw new RuntimeException(msg);
        }
        
        String[] path = lastLinkId.substring(MenuRenderer.SECONDARY_MENU_PREFIX.length()).split(UIMenuComponent.VALUE_SEPARATOR);
        MenuItem item = getActiveMainMenuItem();
        collapseMenuItems(item);

        // Let's go to the clicked link
        for (String step : path) {
            if (item.getSubItems() != null) {
                if(item instanceof DropdownMenuItem) {
                    ((DropdownMenuItem) item).setExpanded(true); // Mark our trail
                }
                item = item.getSubItems().get(Integer.parseInt(step));
            } else if (path.length > 1) { // if necessary, fetch children
                DropdownMenuItem dropdownItem = ((DropdownMenuItem) item);
                NodeRef nr = dropdownItem.getNodeRef();
                if(nr == null) {
                    nr = getGeneralService().getNodeRef(dropdownItem.getXPath());
                }
                for(NodeRef childItemRef : getMenuService().openTreeItem(dropdownItem, nr)) {
                    DropdownMenuItem childItem = new DropdownMenuItem();
                    getMenuService().setupTreeItem(childItem, childItemRef);
                    if(item.getSubItems() == null) {
                        item.setSubItems(new ArrayList<MenuItem>());
                    }
                    item.getSubItems().add(childItem);
                }
                dropdownItem.setExpanded(true);
                item = dropdownItem.getSubItems().get(Integer.parseInt(step));
            }
        }

        DropdownMenuItem dd = (DropdownMenuItem) item;
        // When XML configuration  doesn't specify any children, this list will be null!
        if (dd.getSubItems() == null) {
            dd.setSubItems(new ArrayList<MenuItem>());
        }
        dd.getSubItems().clear();

        // Toggle the link
        if (dd.isExpanded()) {
            dd.setExpanded(false);
            return; // When hiding, we don't need to refresh children
        }

        log.debug("Fetching children for: " + dd.getTitle());
        // Decide what outcome is needed for children and load proper data
        List<NodeRef> children = getMenuService().openTreeItem(dd, linkNodeRef);
        if (children != null) {
            for (NodeRef child : children) {
                DropdownMenuItem ddChild = new DropdownMenuItem();
                ddChild.setActionListener(UPDATE_TREE_ACTTIONLISTENER);
                ddChild.setNodeRef(child);
                ddChild.setBrowse(true);
                ddChild.setSubmenuId(lastLinkId);
                getMenuService().setupTreeItem(ddChild, child);
                dd.getSubItems().add(ddChild);
            }
        }
        dd.setExpanded(true);
        
    }

    /**
     * Collapses other items, so user can understand where the heck he/she is...
     * @param item
     */
    public void collapseMenuItems(MenuItem item) {
        if(item == null) {
            item = menu.getSubItems().get(DOCUMENT_REGISTER_ID);
        }
        
        if (item.getSubItems() != null) {
            for(MenuItem dmi : item.getSubItems()) {
                if(dmi instanceof DropdownMenuItem) {
                    final DropdownMenuItem dropdownMenuItem = (DropdownMenuItem) dmi;
                    dropdownMenuItem.setExpanded(false);
                }
                collapseMenuItems(dmi);
            }
        }
    }

    public Menu getMenu() {
        if (getMenuService().getUpdateCount() != updateCount || this.menu == null) {
            log.debug("Fetching new menu structure from service.");
            reloadMenu(); // XXX - Somehow this makes it work... Although menu structure in service isn't modified. 
            this.menu = getMenuService().getMenu();
            this.updateCount = getMenuService().getUpdateCount();
            if(lastLinkId != null && linkNodeRef != null) {
                updateTree();
            }
        }
        return this.menu;
    }
    
    public MenuItem getActiveMainMenuItem() {
        return menu.getSubItems().get(Integer.parseInt(activeItemId));
    }
    

    public String getActiveItemId() {
        return activeItemId;
    }

    public void setActiveItemId(String activeMenuId) {
        this.activeItemId = activeMenuId;
    }
    
    public void reloadMenu() {
        getMenuService().reload();
    }
    
    public String getLastLinkId() {
        return lastLinkId;
    }

    public void setLastLinkId(String lastLinkId) {
        this.lastLinkId = lastLinkId;
    }
    
    public NodeRef getLinkNodeRef() {
        return linkNodeRef;
    }

    public void setLinkNodeRef(NodeRef linkNodeRef) {
        this.linkNodeRef = linkNodeRef;
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public MenuService getMenuService() {
        if (menuService == null) {
            menuService = (MenuService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(MenuService.BEAN_NAME);
        }
        return menuService;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    // END: getters / setters
}
