package ee.webmedia.alfresco.casefile.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.service.FavoritesService;

public interface CaseFileFavoritesService extends FavoritesService {

    String BEAN_NAME = "CaseFileFavoritesService";

    List<CaseFile> getCaseFileFavorites(NodeRef containerNodeRef);

}