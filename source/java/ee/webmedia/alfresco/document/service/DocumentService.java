package ee.webmedia.alfresco.document.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.register.model.RegNrHolder2;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SignatureChallenge;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.model.DeletionType;
import ee.webmedia.alfresco.workflow.model.TaskAndDocument;
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.service.Task;

public interface DocumentService {

    public abstract class TransientProps { // using abstract class instead of interface to be able to add/change constants without reDeploy
        public static final String FUNCTION_LABEL = RepoUtil.createTransientProp("function_Lbl").toString();
        public static final String SERIES_LABEL = RepoUtil.createTransientProp("series_Lbl").toString();
        public static final String VOLUME_LABEL = RepoUtil.createTransientProp("volume_Lbl").toString();
        public static final String CASE_LABEL = RepoUtil.createTransientProp("case_Lbl").toString();
        public static final String CASE_LABEL_EDITABLE = RepoUtil.createTransientProp("case_Lbl_Editable").toString();
        //
        public static final String FUNCTION_NODEREF = RepoUtil.createTransientProp("function").toString();
        public static final String SERIES_NODEREF = RepoUtil.createTransientProp("series").toString();
        public static final String VOLUME_NODEREF = RepoUtil.createTransientProp("volume").toString();
        public static final String CASE_NODEREF = RepoUtil.createTransientProp("case").toString();
        public static final QName TEMP_DOCUMENT_IS_DRAFT_OR_IMAP_OR_DVK_QNAME = RepoUtil.createTransientProp("isDraftOrImapOrDvk");
        public static final QName TEMP_DOCUMENT_IS_INCOMING_INVOICE_QNAME = RepoUtil.createTransientProp("isIncomingInvoice");
        public static final QName TEMP_DOCUMENT_IS_FROM_WEB_SERVICE_QNAME = RepoUtil.createTransientProp("isFromWebService");
        public static final QName TEMP_DOCUMENT_IS_DVK_QNAME = RepoUtil.createTransientProp("isDvk");
        public static final QName TEMP_DOCUMENT_IS_FORWARDED_DEC_DOCUMENT = RepoUtil.createTransientProp("forwardDecDocument");
        public static final QName TEMP_DOCUMENT_IS_DRAFT_QNAME = RepoUtil.createTransientProp("isDraft");
        public static final String TEMP_DOCUMENT_IS_DRAFT = TEMP_DOCUMENT_IS_DRAFT_QNAME.toString();
        public static final String TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED = "{temp}logging_disabled_docMetadataChanged";
        public static final QName TEMP_DOCUMENT_DISABLE_UPDATE_INITIAL_ACCESS_RESTRICTION_PROPS = RepoUtil.createTransientProp("disableUpdateInitialAccessRestrictionProps");
        public static final QName TEMP_DOCUMENT_ACCESS_RESTRICTION_PROPS_CHANGED = RepoUtil.createTransientProp("accessRestrictionPropsChanged");
        public static final QName TEMP_DOCUMENT_OLD_TYPE_ID = RepoUtil.createTransientProp("oldDocTypeId");
    }

    String BEAN_NAME = "DocumentService";
    String VOLUME_MARK_SEPARATOR = "/";

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

    void updateSearchableFiles(NodeRef document);

    void updateSearchableFiles(NodeRef document, Map<QName, Serializable> props);

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
    @Deprecated
    Node createFollowUp(QName docType, NodeRef nodeRef);

    List<NodeRef> getAllDocumentFromDvk();

    int getAllDocumentFromDvkCount();

    int getIncomingEmailsCount(int limit);

    int getSentEmailsCount(int limit);

    void deleteDocument(NodeRef nodeRef);

    void deleteDocument(NodeRef nodeRef, String comment);

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
     */
    public static abstract class PropertiesModifierCallback implements InitializingBean {
        private DocumentService documentService;

        /**
         * @param properties - initial properties for the node that is not yet constructed
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

    Node getVolumeByDocument(NodeRef docRef, Node caseNode);

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

    boolean registerDocumentIfNotRegistered(NodeRef document, boolean logging);

    void registerDocumentRelocating(Node docNode, Node previousVolume);

    /**
     * @param documentNode
     * @return the same instance with updated values(regNumber, regDate)
     * @throws UnableToPerformException - Document can't be registered because of initial document is not registered.
     */
    NodeRef registerDocument(Node documentNode) throws UnableToPerformException;

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
        /** alusdokument */
        INITIAL("alusdokument"),
        /** vastusdokument */
        REPLY("vastusdokument"),
        /** järgdokument */
        FOLLOWUP("järgdokument"),
        /** dokumendimenetlus */
        WORKFLOW("dokumendimenetlus"),
        /** tavaline */
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
     * Fetches document objects for tasks
     *
     * @param tasks
     * @param propertyTypes
     * @return
     */
    Map<NodeRef, TaskAndDocument> getTasksWithDocuments(Collection<Task> tasks, Map<Long, QName> propertyTypes);

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

    void prepareDocumentSigning(List<NodeRef> documents, boolean generatePdfs, boolean inactivateOriginalFiles);

    /**
     * @throws SignatureRuntimeException
     */
    void finishDocumentSigning(SignatureTask task, String signatureHex, NodeRef document, boolean signSeparately, boolean finishTask, Map<NodeRef, String> originalStatuses);

    SignatureDigest prepareDocumentDigest(NodeRef document, String certHex, NodeRef compoundWorkflowRef) throws SignatureException;

    SignatureChallenge prepareDocumentChallenge(NodeRef document, String phoneNo, String idCode, NodeRef compoundWorkflowRef) throws SignatureException;

    /**
     * @param base
     * @return list of documents that are reply or follow up to base
     */
    List<Document> getReplyOrFollowUpDocuments(NodeRef base);

    void updateParentNodesContainingDocsCount(NodeRef documentNodeRef, boolean documentAdded);

    void updateParentDocumentRegNumbers(NodeRef docRef, String removedRegNumber, String addedRegNumber);

    /**
     * @param parentRef
     * @return number of documents under given volume or case
     */
    int getDocumentsCountByVolumeOrCase(NodeRef parentRef);

    List<String> getSearchableFileNames(NodeRef document);

    ContentData getSearchableFileContents(NodeRef document);

    Map<QName, NodeRef> getDocumentParents(NodeRef documentRef);

    void setDocStatusFinished(final NodeRef docRef);

    void setPropertyAsSystemUser(final QName propName, final Serializable value, final NodeRef docRef);

    List<NodeRef> getIncomingEInvoices();

    int getAllDocumentFromIncomingInvoiceCount();

    boolean isIncomingInvoice(NodeRef nodeRef);

    List<Document> getIncomingEInvoicesForUser(String userFullName);

    int getUserDocumentFromIncomingInvoiceCount(String userFullName);

    // FIXME DLSeadist - selle meetodi peaks eemaldama igalt poolt pärast DLSeadist valmimist kui staatilised dokumendid on konverditud dünaamilisteks
    void throwIfNotDynamicDoc(Node docNode);

    List<NodeRef> getIncomingDocuments(NodeRef incomingNodeRef);

    NodeRef checkExistingBdoc(NodeRef document, NodeRef compoundWorkflowRef);

    boolean isDraft(NodeRef document);

    void setRegNrBasedOnPattern(final Series series, NodeRef volumeNodeRef, final Register docRegister, final Register volRegister, final RegNrHolder2 holder,
            final Date regDateTime, final String pattern, final boolean disableVolCounterIncrement);

    int getAllDocumentsFromFolderCount(NodeRef folder);

    List<NodeRef> getAllDocumentRefsByParentRefWithoutRestrictedAccess(NodeRef parentRef);

    void deleteDocument(NodeRef nodeRef, String comment, DeletionType deletionType);

    void deleteDocument(NodeRef nodeRef, String docDeletingComment, DeletionType disposition, String executingUser);
    
    void deleteDocument(NodeRef nodeRef, String comment, DeletionType deletionType, String executingUser, boolean isDisposeVolume);

    int countFilesInFolder(NodeRef parentRef, boolean countFilesInSubfolders, int limit);

}
