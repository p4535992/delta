package ee.webmedia.alfresco.document.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public interface DocumentService {

    public interface TransientProps {
        String FUNCTION_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "function_fnSerVol").toString();
        String SERIES_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "series_fnSerVol").toString();
        String VOLUME_NODEREF = QName.createQName(RepoUtil.TRANSIENT_PROPS_NAMESPACE, "volume_fnSerVol").toString();
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
     * @param volumeRef
     * @return all documents that have been assigned under given volume
     */
    List<Document> getAllDocumentsByVolume(NodeRef volumeRef);

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
        void doWithProperties(Map<QName, Serializable> properties, NodeRef document);

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

    /**
     * @param nodeRef
     * @return array of Node:
     *         <ol>
     *         <li>volumeNode</li>
     *         <li>seriesNode</li>
     *         <li>functionNode</li>
     *         </ol>
     */
    Node[] getAncestorNodesByDocument(NodeRef nodeRef);

}
