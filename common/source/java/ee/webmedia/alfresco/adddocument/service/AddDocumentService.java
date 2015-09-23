package ee.webmedia.alfresco.adddocument.service;

import java.io.IOException;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.adddocument.generated.AddDocumentRequest;
import ee.webmedia.alfresco.adddocument.generated.AddDocumentResponse;

public interface AddDocumentService {

    String BEAN_NAME = "AddDocumentService";

    List<NodeRef> getAllDocumentFromWebService();

    String getWebServiceDocumentsMenuItemTitle();

    String getWebServiceDocumentsListTitle();

    AddDocumentResponse importDocument(AddDocumentRequest request) throws IOException;

}
