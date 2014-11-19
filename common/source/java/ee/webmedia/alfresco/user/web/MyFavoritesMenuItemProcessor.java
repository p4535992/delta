<<<<<<< HEAD
package ee.webmedia.alfresco.user.web;

import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;

public class MyFavoritesMenuItemProcessor implements MenuItemProcessor, InitializingBean {
    private MenuService menuService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("myFavourites", this, false, true);
    }

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        Node userNode = BeanHelper.getUserService().getUser(AuthenticationUtil.getRunAsUser());
        if (userNode == null) {
            return;
        }
        NodeRef user = userNode.getNodeRef();
        NodeService nodeService = BeanHelper.getNodeService();
        List<MenuItem> subItems = menuItem.getSubItems();
        subItems.clear();
        for (ChildAssociationRef favDir : nodeService.getChildAssocs(user, DocumentCommonModel.Assocs.FAVORITE_DIRECTORY, RegexQNamePattern.MATCH_ALL)) {
            MenuItem item = new MenuItem();
            item.setOutcome("dialog:favoritesDocumentListDialog");
            item.getParams().put("nodeRef", favDir.getChildRef().toString());
            String dirName = favDir.getQName().getLocalName();
            item.getParams().put("dirName", dirName);
            item.setActionListener("#{FavoritesDocumentListDialog.setup}");
            item.setTitle(dirName);
            subItems.add(item);
        }

    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }
}
=======
package ee.webmedia.alfresco.user.web;

import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;

public class MyFavoritesMenuItemProcessor implements MenuItemProcessor, InitializingBean {
    private MenuService menuService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("myFavourites", this, false, true);
    }

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        Node userNode = BeanHelper.getUserService().getUser(AuthenticationUtil.getRunAsUser());
        if (userNode == null) {
            return;
        }
        NodeRef user = userNode.getNodeRef();
        NodeService nodeService = BeanHelper.getNodeService();
        List<MenuItem> subItems = menuItem.getSubItems();
        subItems.clear();
        for (ChildAssociationRef favDir : nodeService.getChildAssocs(user, DocumentCommonModel.Assocs.FAVORITE_DIRECTORY, RegexQNamePattern.MATCH_ALL)) {
            MenuItem item = new MenuItem();
            item.setOutcome("dialog:favoritesDocumentListDialog");
            item.getParams().put("nodeRef", favDir.getChildRef().toString());
            String dirName = favDir.getQName().getLocalName();
            item.getParams().put("dirName", dirName);
            item.setActionListener("#{FavoritesDocumentListDialog.setup}");
            item.setTitle(dirName);
            subItems.add(item);
        }

    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
