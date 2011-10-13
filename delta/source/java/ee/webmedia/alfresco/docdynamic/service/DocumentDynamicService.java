package ee.webmedia.alfresco.docdynamic.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;

/**
 * @author Alar Kvell
 */
public interface DocumentDynamicService {

    String BEAN_NAME = "DocumentDynamicService";

    /**
     * Create a new document and set default property values according to fully authenticated user.
     * 
     * @param documentTypeId
     * @param parent
     * @return
     */
    NodeRef createNewDocument(String documentTypeId, NodeRef parent);

    /**
     * Create a new document in drafts and set default property values according to fully authenticated user.
     * 
     * @see #createNewDocument(String, NodeRef)
     */
    NodeRef createNewDocumentInDrafts(String documentTypeId);

    NodeRef copyDocument(DocumentDynamic document, Map<QName, Serializable> overriddenProperties, QName... ignoredProperty);

    DocumentDynamic getDocument(NodeRef docRef);

    void deleteDocumentIfDraft(NodeRef docRef);

    /**
     * @param document document to save; document object is cloned in this service, so that argument object is preserved.
     * @param saveListenerBeanNames save and validation listener bean names
     * @throws UnableToPerformException one error message if validation or save was unsuccessful.
     * @throws UnableToPerformMultiReasonException multiple error messages if validation or save was unsuccessful.
     */
    void updateDocument(DocumentDynamic document, List<String> saveListenerBeanNames);

    boolean isDraft(NodeRef docRef);

}
