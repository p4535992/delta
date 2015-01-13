package ee.webmedia.alfresco.docdynamic.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.utils.TreeNode;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;

public interface DocumentDynamicService {

    String BEAN_NAME = "DocumentDynamicService";

    /**
     * Create a new document and set default property values according to fully authenticated user.
     * 
     * @param documentTypeId
     * @param parent
     * @return
     */
    Pair<DocumentDynamic, DocumentTypeVersion> createNewDocument(String documentTypeId, NodeRef parent);

    Pair<DocumentDynamic, DocumentTypeVersion> createNewDocument(DocumentTypeVersion docVer, NodeRef parent, boolean reallySetDefaultValues);

    /**
     * Create a new document in drafts and set default property values according to fully authenticated user.
     * 
     * @see #createNewDocument(String, NodeRef)
     */
    Pair<DocumentDynamic, DocumentTypeVersion> createNewDocumentInDrafts(String documentTypeId);

    NodeRef copyDocumentToDrafts(DocumentDynamic document, Map<QName, Serializable> overriddenProperties, QName... ignoredProperty);

    DocumentDynamic getDocument(NodeRef docRef);

    DocumentDynamic getDocumentWithInMemoryChangesForEditing(NodeRef docRef);

    void deleteDocumentIfDraft(NodeRef docRef);

    /**
     * Save properties and aspects of document node and all meta-data child-nodes and grand-child-nodes.
     * Also does the following:
     * <ul>
     * <li>calls save-listeners to perform validation and additional stuff</li>
     * <li>adds log messages about changed property values</li>
     * <li>updates searchable properties</li>
     * <li>updates generated files</li>
     * </ul>
     * 
     * @param document document to save; document object is cloned in this service, so that argument object is preserved.
     * @param saveListenerBeanNames save and validation listener bean names; can be {@code null}
     * @return cloned document, possibly modified by save listeners
     * @throws UnableToPerformException one error message if validation or save was unsuccessful.
     * @throws UnableToPerformMultiReasonException multiple error messages if validation or save was unsuccessful.
     */
    DocumentDynamic updateDocument(DocumentDynamic document, List<String> saveListenerBeanNames, boolean relocateAssocDocs);

    boolean isDraft(NodeRef docRef);

    boolean isDraftOrImapOrDvk(NodeRef docRef);

    boolean isImapOrDvk(NodeRef docRef);

    boolean isOutgoingLetter(NodeRef docRef);

    String getDocumentTypeName(NodeRef documentRef);

    void setOwner(NodeRef docRef, String ownerId, boolean retainPreviousOwnerId);

    void setOwner(Map<QName, Serializable> props, String ownerId, boolean retainPreviousOwnerId, Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs);

    void setOwner(Map<QName, Serializable> props, String ownerId, boolean retainPreviousOwnerId);

    boolean isOwner(NodeRef docRef, String ownerId);

    public void changeTypeInMemory(DocumentDynamic document, String newTypeId);

    void createChildNodesHierarchyAndSetDefaultPropertyValues(Node parentNode, QName[] childAssocTypeQNameHierarchy, DocumentTypeVersion docVer);

    boolean isShowMessageIfUnregistered();

    /** Same as updateDocument; return list of original nodeRefs that were updated by the method */
    Pair<DocumentDynamic, List<Pair<NodeRef, NodeRef>>> updateDocumentGetDocAndNodeRefs(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames,
            boolean relocateAssocDocs);

    String getDocumentType(NodeRef documentRef);

    TreeNode<QName> getChildNodeQNameHierarchy(QName[] hierarchy, TreeNode<QName> root);

}
