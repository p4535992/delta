package ee.webmedia.alfresco.docdynamic.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;

/**
 * @author Alar Kvell
 */
public interface DocumentDynamicService {

    String BEAN_NAME = "DocumentDynamicService";

    NodeRef createDraft(String documentTypeId);

    DocumentDynamic getDocument(NodeRef docRef);

    void deleteDocument(NodeRef docRef);

    /**
     * @param document document to save; document object is cloned in this service, so that argument object is preserved.
     * @param saveListenerBeanNames save and validation listener bean names
     * @throws UnableToPerformException one error message if validation or save was unsuccessful.
     * @throws UnableToPerformMultiReasonException multiple error messages if validation or save was unsuccessful.
     */
    void updateDocument(DocumentDynamic document, List<String> saveListenerBeanNames);

}
