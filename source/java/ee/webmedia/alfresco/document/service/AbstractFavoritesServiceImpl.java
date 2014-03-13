package ee.webmedia.alfresco.document.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 *         Refactored from DocumentServiceImpl.
 */
public abstract class AbstractFavoritesServiceImpl implements FavoritesService {
    protected NodeService nodeService;
    protected UserService userService;
    protected MenuService menuService;
    protected static List<QName> allFavoriteAssocQNames = Arrays.asList(DocumentCommonModel.Assocs.FAVORITE, WorkflowCommonModel.Assocs.FAVORITE, CaseFileModel.Assocs.FAVORITE);

    @Override
    public List<NodeRef> getFavorites(NodeRef containerNodeRef) {
        if (containerNodeRef == null) {
            containerNodeRef = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        }
        if (!nodeService.hasAspect(containerNodeRef, DocumentCommonModel.Aspects.FAVORITE_CONTAINER)) {
            return Collections.emptyList();
        }
        List<AssociationRef> assocs = nodeService.getTargetAssocs(containerNodeRef, getAssocQName());
        List<NodeRef> favorites = new ArrayList<NodeRef>(assocs.size());
        for (AssociationRef assoc : assocs) {
            favorites.add(assoc.getTargetRef());
        }
        return favorites;
    }

    @Override
    public List<String> getFavoriteDirectoryNames() {
        NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        List<ChildAssociationRef> dirs = nodeService.getChildAssocs(user, DocumentCommonModel.Assocs.FAVORITE_DIRECTORY, RegexQNamePattern.MATCH_ALL);
        List<String> names = new ArrayList<String>(dirs.size());
        for (ChildAssociationRef dirAssoc : dirs) {
            names.add(dirAssoc.getQName().getLocalName());
        }

        return names;
    }

    @Override
    public boolean isFavoriteAddable(NodeRef nodeRef) {
        return isFavorite(nodeRef) == null;
    }

    protected abstract QName getFavoriteAssocQName();

    @Override
    public NodeRef isFavorite(NodeRef docRef) {
        NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        if (!nodeService.hasAspect(user, DocumentCommonModel.Aspects.FAVORITE_CONTAINER)
                && !nodeService.hasAspect(user, DocumentCommonModel.Aspects.FAVORITE_DIRECTORY_ASPECT)) {
            return null;
        }
        for (AssociationRef assoc : nodeService.getTargetAssocs(user, getAssocQName())) {
            if (assoc.getTargetRef().equals(docRef)) {
                return assoc.getSourceRef();
            }
        }
        for (ChildAssociationRef dirAssoc : nodeService.getChildAssocs(user, DocumentCommonModel.Assocs.FAVORITE_DIRECTORY, RegexQNamePattern.MATCH_ALL)) {
            for (AssociationRef docAssoc : nodeService.getTargetAssocs(dirAssoc.getChildRef(), getAssocQName())) {
                if (docAssoc.getTargetRef().equals(docRef)) {
                    return docAssoc.getSourceRef();
                }
            }
        }
        return null;
    }

    @Override
    public boolean addFavorite(NodeRef nodeRef, String favDirName, boolean updateMenu) {
        return addFavorite(nodeRef, favDirName, updateMenu, getAssocQName());
    }

    private boolean addFavorite(NodeRef nodeRef, String favDirName, boolean updateMenu, QName favoriteAssocQName) {
        if (isFavorite(nodeRef) != null) {
            return false;
        }
        NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        if (!nodeService.hasAspect(user, DocumentCommonModel.Aspects.FAVORITE_CONTAINER)) {
            nodeService.addAspect(user, DocumentCommonModel.Aspects.FAVORITE_CONTAINER, null);
            nodeService.addAspect(user, CaseFileModel.Aspects.FAVORITE, null);
        }
        if (StringUtils.isNotBlank(favDirName)) {
            favDirName = StringUtils.trim(favDirName);
            if (!nodeService.hasAspect(user, DocumentCommonModel.Aspects.FAVORITE_DIRECTORY_ASPECT)) {
                nodeService.addAspect(user, DocumentCommonModel.Aspects.FAVORITE_DIRECTORY_ASPECT, null);
                nodeService.addAspect(user, CaseFileModel.Aspects.FAVORITE, null);
            }
            QName assocName = QName.createQName(DocumentCommonModel.URI, QName.createValidLocalName(favDirName));
            List<ChildAssociationRef> favDirs = nodeService.getChildAssocs(user, DocumentCommonModel.Assocs.FAVORITE_DIRECTORY, assocName);
            NodeRef favDir;
            if (favDirs.isEmpty()) {
                favDir = nodeService.createNode(user, DocumentCommonModel.Assocs.FAVORITE_DIRECTORY, assocName, DocumentCommonModel.Types.FAVORITE_DIRECTORY).getChildRef();
                nodeService.addAspect(favDir, CaseFileModel.Aspects.FAVORITE, null);
            } else {
                favDir = favDirs.get(0).getChildRef();
            }
            nodeService.createAssociation(favDir, nodeRef, favoriteAssocQName);
            if (updateMenu) {            
                menuService.process(BeanHelper.getMenuBean().getMenu(), false, true);
            }
        } else {
            nodeService.createAssociation(user, nodeRef, favoriteAssocQName);
        }
        return true;
    }

    @Override
    public void removeFavorite(NodeRef nodeRef) {
        NodeRef favorite = isFavorite(nodeRef);
        if (favorite != null) {
            nodeService.removeAssociation(favorite, nodeRef, getAssocQName());
            if (nodeService.getType(favorite).equals(DocumentCommonModel.Types.FAVORITE_DIRECTORY) && favoritesFolderIsEmpty(favorite)) {
                nodeService.removeChildAssociation(nodeService.getParentAssocs(favorite).get(0));
                menuService.process(BeanHelper.getMenuBean().getMenu(), false, true);
            }
        }
    }

    private QName getAssocQName() {
        QName assocQName = getFavoriteAssocQName();
        Assert.isTrue(allFavoriteAssocQNames.contains(assocQName));
        return assocQName;
    }

    private boolean favoritesFolderIsEmpty(NodeRef favorite) {
        for (QName assocQname : allFavoriteAssocQNames) {
            if (!nodeService.getTargetAssocs(favorite, assocQname).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

}
