package ee.webmedia.alfresco.casefile.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.document.service.DocumentFavoritesServiceImpl;

/**
 * @author Kaarel Jõgeva
 */
public class CaseFileFavoritesServiceImpl extends DocumentFavoritesServiceImpl implements CaseFileFavoritesService {

    private CaseFileService caseFileService;

    @Override
    protected QName getFavoriteAssocQName() {
        return CaseFileModel.Assocs.FAVORITE;
    }

    @Override
    public List<CaseFile> getCaseFileFavorites(NodeRef containerNodeRef) {
        List<NodeRef> favouriteRefs = getFavorites(containerNodeRef);
        List<CaseFile> favorites = new ArrayList<CaseFile>(favouriteRefs.size());
        for (NodeRef caseFileRef : favouriteRefs) {
            favorites.add(caseFileService.getCaseFile(caseFileRef));
        }
        return favorites;
    }

    public void setCaseFileService(CaseFileService caseFileService) {
        this.caseFileService = caseFileService;
    }
}