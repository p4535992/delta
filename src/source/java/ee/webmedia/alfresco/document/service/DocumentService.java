package ee.webmedia.alfresco.document.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.workflow.model.TaskAndDocument;
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * @author Alar Kvell
 */
public interface DocumentService {

    public abstract class TransientProps { // using abstract class instead of interface to be able to add/change constants without reDeploy
        public static final String FUNCTION_LABEL = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "function_Lbl").toString();
        public static final String SERIES_LABEL = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "series_Lbl").toString();
        public static final String VOLUME_LABEL = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "volume_Lbl").toString();
        public static final String CASE_LABEL = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "case_Lbl").toString();
        public static final String CASE_LABEL_EDITABLE = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "case_Lbl_Editable").toString();
        //
        public static final String FUNCTION_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "function").toString();
        public static final String SERIES_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "series").toString();
        public static final String VOLUME_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "volume").toString();
        public static final String CASE_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "case").toString();
        public static final String TEMP_DOCUMENT_IS_DRAFT = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "isDraft").toString();
        public static final String TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED = "{temp}logging_disabled_docMetadataChanged";
    }

    String BEAN_NAME = "DocumentService";

    NodeRef getDrafts();

    /**
     * Get the document from repository.
     * 
     * @param nodeRef document
     * @return document
     */
    Node getDocument(NodeRef nodeRef);

    /**
     * Create a new blank document in drafts folder.
     * 
     * @return created document
     */
    Node createDocument(QName documentTypeId);

    /**
     * Create a new blank document into <code>parentFolderRef</code>.
     * 
     * @param documentTypeId
     * @param parentFolderRef
     * @param props
     * @return created document
     */
    Node createDocument(QName documentTypeId, NodeRef parentFolderRef, Map<QName, Serializable> props);
    
    Node createPPImportDocument(QName documentTypeId, NodeRef parentRef, Map<QName, Serializable> properties);

    /**
     * Save new values for document properties to repository.
     * 
     * @param node document with new property values
     * @return new Node representing document if node had reference to volumeNodeRef
     */
    Node updateDocument(Node node);

    void updateSearchableFiles(NodeRef document);

    /**
     * Make a copy of document as a draft.
     * To make it permanent, it must be saved explicitly.
     * 
     * @param nodeRef Reference to document that is copied
     * @return copied document as draft
     */
    Node copyDocument(NodeRef nodeRef);

    /**
     * Create a node of the specified type from the properties
     * of the original node as a reply.
     * 
     * @param docType
     * @param nodeRef
     * @return
     */
    Node createReply(QName docType, NodeRef nodeRef);

    /**
     * Create a node of the specified type from the properties
     * of the original node as a follow up.
     * 
     * @param docType
     * @param nodeRef
     * @return
     */
    Node createFollowUp(QName docType, NodeRef nodeRef);

    /**
     * @param volumeRef
     * @return all documents that have been assigned under given volume
     */
    List<Document> getAllDocumentsByVolume(NodeRef volumeRef);

    List<Document> getAllDocumentsByCase(NodeRef caseRef);

    List<Document> getAllDocumentFromDvk();

    int getAllDocumentFromDvkCount();

    List<DocAssocInfo> getAssocInfos(Node document);

    /**
     * Deletes association between nodes
     * 
     * @param sourceNodeRef
     * @param targetNodeRef
     * @param assocQName if null, defaults to <code>DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT</code>
     */
    void deleteAssoc(NodeRef sourceNodeRef, NodeRef targetNodeRef, QName assocQName);

    DocAssocInfo getDocAssocInfo(AssociationRef assocRef, boolean isSourceAssoc);

    /**
     * Get list of incoming email.
     * 
     * @return list of documents
     */
    List<Document> getIncomingEmails();

    int getIncomingEmailsCount();

    /**
     * Get list of sent email.
     * 
     * @return list of documents
     */
    List<Document> getSentEmails();

    int getSentEmailsCount();

    void deleteDocument(NodeRef nodeRef);

    /**
     * Add callback to document creation phase, where default values for the properties could be created or overridden when creatable document has given
     * aspect(or the document has aspect that is one parent types of given aspect).<br>
     * Note that when more than one callback should be applied to document being created, then callback, that was registered before will be called first. <br>
     * Thus allowing callbacks that were registered later to change/override properties set by callbacks that were registered before.
     * 
     * @param aspectName
     * @param propertiesModifierCallback
     */
    void addPropertiesModifierCallback(QName aspectName, PropertiesModifierCallback propertiesModifierCallback);

    /**
     * You can change properties of creatable document by registering implementation of this callback interface.<br>
     * 
     * @see {@link DocumentService#addPropertiesModifierCallback(QName, PropertiesModifierCallback)}
     * @author Ats Uiboupin
     */
    public static abstract class PropertiesModifierCallback implements InitializingBean {
        private DocumentService documentService;

        /**
         * @param properties - initial properties for the node that is not jet constructed
         */
        public void doWithProperties(Map<QName, Serializable> properties) {
            // set default properties for document with type defined by getAspectName() before creation
        }

        /**
         * @param node - after node has been created
         */
        public void doWithNode(Node node, String phase) {
            // do whatever you like after document with aspect defined by getAspectName() has been created
        }

        /**
         * @param documentService - reference to the implementation of DocumentService where this Callback must be registered to
         */
        public void setDocumentService(DocumentService documentService) {
            this.documentService = documentService;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            documentService.addPropertiesModifierCallback(getAspectName(), this);
        }

        /**
         * @return QName of the aspect that the document must have for executing callbacks defined in this class
         */
        public abstract QName getAspectName();
        
    }

    /**
     * Executes the callback registered for docAspect to modify the properties.
     * 
     * @param docAspect
     * @param properties
     */
    void callbackAspectProperiesModifier(QName docAspect, Map<QName, Serializable> properties);

    /**
     * @param nodeRef
     * @return parent volume of given document
     */
    Node getVolumeByDocument(NodeRef nodeRef);

    Node getCaseByDocument(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return array of Node:
     *         <ol>
     *         <li>volumeNode</li>
     *         <li>seriesNode</li>
     *         <li>functionNode</li>
     *         </ol>
     */
    DocumentParentNodesVO getAncestorNodesByDocument(NodeRef nodeRef);

    void registerDocumentIfNotRegistered(NodeRef document, boolean logging);

    /**
     * @param documentNode
     * @return the same instance with updated values(regNumber, regDate)
     * @throws UnableToPerformException - Document can't be registered because of initial document is not registered.
     */
    Node registerDocument(Node documentNode) throws UnableToPerformException;

    /**
     * @param nodeRef
     * @return true if document is saved under case or volume
     */
    boolean isSaved(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return true if document is saved under DVK receive space
     */
    boolean isFromDVK(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return true if document is saved under incoming email space
     */
    boolean isFromIncoming(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return true if document is saved under sent email space
     */
    boolean isFromSent(NodeRef nodeRef);

    void setTransientProperties(Node document, DocumentParentNodesVO documentParentNodesVO);

    Document getDocumentByNodeRef(NodeRef document);

    public enum AssocType {
        INITIAL("alusdokument"),
        REPLY("vastusdokument"),
        FOLLOWUP("järgdokument"),
        DEFAULT("tavaline");

        String valueName;

        AssocType(String valueName) {
            this.valueName = valueName;
        }

        public String getValueName() {
            return valueName;
        }

    }

    /**
     * Changes the type of the repository node to the type of this node.
     * 
     * @param node
     * @return
     */
    void changeType(Node node);

    /**
     * Change the type of the node without writing to the repository,
     * add new aspects and fill some required default properties.
     * 
     * @param node
     * @param newType
     */
    void changeTypeInMemory(Node node, QName newType);

    /**
     * @param document
     * @param user
     * @return true when the user is the document's OWNER_ID property
     */
    boolean isDocumentOwner(NodeRef document, String user);

    void setDocumentOwner(NodeRef document, String userName);

    /**
     * @param docNode
     * @return true when docNode is registered.
     */
    boolean isRegistered(Node docNode);

    /**
     * Fetches document objects for tasks
     * 
     * @param tasks
     * @return
     */
    List<TaskAndDocument> getTasksWithDocuments(List<Task> tasks);

    /**
     * Updates the status of the document ant it's compound workflows to stopped.
     * 
     * @param nodeRef
     */
    void stopDocumentPreceedingAndUpdateStatus(NodeRef nodeRef);

    /**
     * Updates the status of the document ant it's compound workflows to working.
     * 
     * @param nodeRef
     */
    void continueDocumentPreceedingAndUpdateStatus(NodeRef nodeRef);

    /**
     * Ends document.
     * 
     * @param documentRef Reference to document to be ended
     */
    void endDocument(NodeRef documentRef);

    /**
     * Reopens document.
     * 
     * @param documentRef Reference to document to be reopened
     */
    void reopenDocument(NodeRef documentRef);

    void prepareDocumentSigning(NodeRef document);

    /**
     * @throws SignatureRuntimeException
     */
    void finishDocumentSigning(SignatureTask task, String signatureHex);

    SignatureDigest prepareDocumentDigest(NodeRef document, String certHex) throws SignatureException;

    List<Document> getFavorites();

    boolean isFavorite(NodeRef document);

    boolean isFavoriteAddable(NodeRef document);

    void addFavorite(NodeRef document);

    void removeFavorite(NodeRef document);

    List<Document> processExtendedSearchResults(List<Document> documents, Node filter);

    /**
     * @param base
     * @return list of documents that are reply or follow up to base
     */
    List<Document> getReplyOrFollowUpDocuments(NodeRef base);

    void updateParentNodesContainingDocsCount(NodeRef documentNodeRef, boolean documentAdded);

    /**
     * @param parentRef
     * @return number of documents under given volume or case
     */
    int getDocumentsCountByVolumeOrCase(NodeRef parentRef);
}
