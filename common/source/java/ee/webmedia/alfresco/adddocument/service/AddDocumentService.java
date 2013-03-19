package ee.webmedia.alfresco.adddocument.service;

import java.io.IOException;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.adddocument.generated.AddDocumentRequest;
import ee.webmedia.alfresco.adddocument.generated.AddDocumentResponse;
import ee.webmedia.alfresco.document.model.Document;

/**
 * @author Riina Tens
 */
public interface AddDocumentService {

    String BEAN_NAME = "AddDocumentService";

    List<Document> getAllDocumentFromWebService();

    NodeRef getWebServiceDocumentsRoot();

    String getWebServiceDocumentsMenuItemTitle();

    String getWebServiceDocumentsListTitle();

    int getAllDocumentFromWebServiceCount();

    AddDocumentResponse importDocument(AddDocumentRequest request) throws IOException;

}
