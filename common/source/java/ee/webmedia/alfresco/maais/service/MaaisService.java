package ee.webmedia.alfresco.maais.service;

import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.maais.generated.server.CatalogStructureElement;
import ee.webmedia.alfresco.maais.generated.server.RegisterDocumentRequest;
import ee.webmedia.alfresco.maais.generated.server.RegisterDocumentResponse;

/**
 * @author Keit Tehvan
 */
public interface MaaisService {
    String BEAN_NAME = "MaaisService";

    Date updateAuth(String userId);

    void notifyAssoc(NodeRef documentRef);

    boolean isServiceAvailable();

    String getMaaisName();

    String getUserUrl(String userId);

    Date getUserSessionExpiry(String userId);

    List<CatalogStructureElement> generateCatalogStructureFromTemplateName(String templateName);

    int updateMaaisCases();

    void addMaaisChangedAspectIfNecessary(NodeRef docRef, boolean checkForMaaisAssocs);

    RegisterDocumentResponse registerMaaisDocument(RegisterDocumentRequest request);

    int notifyFailedAssocs();
}
