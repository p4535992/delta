package ee.webmedia.alfresco.menu.service;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.menu.model.Menu;

/**
 * @author Kaarel Jõgeva
 */
public interface MenuService {

    String BEAN_NAME = "MenuService";

    Menu getMenu();

    void reload();

    String getMenuXml();

    NodeRef getNodeRefForXPath(FacesContext context, String XPath);

    int getNodeChildrenCount(NodeRef nodeRef);
}