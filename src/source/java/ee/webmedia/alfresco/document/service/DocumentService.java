package ee.webmedia.alfresco.document.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public interface DocumentService {

    public interface TransientProps {
        String FUNCTION_LABEL = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "function_Lbl").toString();
        String SERIES_LABEL = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "series_Lbl").toString();
        String VOLUME_LABEL = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "volume_Lbl").toString();
        //
        String FUNCTION_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "function_fnSerVol").toString();
        String SERIES_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "series_fnSerVol").toString();
        String VOLUME_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "volume_fnSerVol").toString();
        String CASE_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "volume_fnSerVolCase").toString();
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
     * Save new values for document properties to repository.
     * 
     * @param node document with new property values
     * @return new Node representing document if node had reference to volumeNodeRef
     */
    Node updateDocument(Node node);

    /**
     * Make a copy of document as a draft.
     * To make it permanent, it must be saved explicitly.
     *
     * @param nodeRef Reference to document that is copied
     * @return copied document as draft
     */
    Node copyDocument(NodeRef nodeRef);
    
    /**
     * @param volumeRef
     * @return all documents that have been assigned under given volume
     */
    List<Document> getAllDocumentsByVolume(NodeRef volumeRef);

    List<Document> getAllDocumentsByCase(NodeRef caseRef);

    void deleteDocument(NodeRef nodeRef);

    boolean isMetadataEditAllowed(NodeRef nodeRef);

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
    public interface PropertiesModifierCallback {
        void doWithProperties(Map<QName, Serializable> properties);

        /**
         * @param documentService - reference to the implementation of DocumentService where this Callback must be registered to
         */
        void setDocumentService(DocumentService documentService);
    }

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

    /**
     * @param documentNode
     * @return the same instance with updated values(regNumber, regDate)
     */
    Node registerDocument(Node documentNode);

    /**
     * @param nodeRef
     * @return true if document is saved under case or volume
     */
    boolean isSaved(NodeRef nodeRef);

    /**
     * Searches for documents where:
     * + search string matches against any Document property value (supported types: text, int, long, float, double, date, datetime)
     * + or file name 
     * + or file content
     * 
     * It returns maximum of 100 entries. It is possible that the method returns less than 100 Documents even when there 
     * are more than 100 matches in the repository because we search for 200 matches and then filter out duplicate documents 
     * where multiple files under the same document matched the search criteria. 
     * 
     * @param searchString
     * @return list of matching documents (max 100 entries)
     */
    List<Document> searchDocumentsQuick(String searchString);

    void setTransientProperties(Node document, DocumentParentNodesVO documentParentNodesVO);

}
