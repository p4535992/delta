package ee.webmedia.alfresco.document.service;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Alar Kvell
 */
public class DocumentServiceImpl implements DocumentService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentServiceImpl.class);
    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private FileFolderService fileFolderService;
    private GeneralService generalService;
    private UserService userService;
    private AuthenticationService authenticationService;

    private String draftsPath;
    private Map<QName/* nodeType/nodeAspect */, PropertiesModifierCallback> creationPropertiesModifierCallbacks = new LinkedHashMap<QName, PropertiesModifierCallback>();

    @Override
    public Node getDocument(NodeRef nodeRef) {
        return RepoUtil.fetchNode(nodeRef);
    }

    @Override
    public NodeRef getDrafts() {
        return generalService.getNodeRef(draftsPath);
    }

    @Override
    public Node createDocument(QName documentTypeId) {
        // XXX do we need to check if document type is used?
        if (!dictionaryService.isSubClass(documentTypeId, DocumentCommonModel.Types.DOCUMENT)) {
            throw new RuntimeException("DocumentTypeId '" + documentTypeId.toPrefixString(namespaceService) + "' must be a subclass of '"
                    + DocumentCommonModel.Types.DOCUMENT.toPrefixString(namespaceService) + "'");
        }
        NodeRef parent = getDrafts();

        // random filename here, but will be changed below
        NodeRef document = fileFolderService.create(parent, GUID.generate(), documentTypeId).getNodeRef();
        try {
            fileFolderService.rename(document, document.getId()); // set cm:name and child-association name to the same value as sys:node-uuid
            // just for convenience sake, then we have the same random UUID :)
        } catch (FileExistsException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        Set<QName> docAspects = nodeService.getAspects(document);
        // first iterate over callbacks to be able to predict in which order callbacks will be called (that is registration order).
        for (QName callbackAspect : creationPropertiesModifierCallbacks.keySet()) {
            for (QName docAspect : docAspects) {
                if (dictionaryService.isSubClass(docAspect, callbackAspect)) {
                    PropertiesModifierCallback callback = creationPropertiesModifierCallbacks.get(docAspect);
                    Map<QName, Serializable> properties = nodeService.getProperties(document);
                    callback.doWithProperties(properties, document);
                    nodeService.setProperties(document, properties);
                }
            }
        }

        return getDocument(document);
    }

    @Override
    public Node updateDocument(final Node node) {
        NodeRef docNodeRef = node.getNodeRef();
        generalService.setPropertiesIgnoringSystem(docNodeRef, node.getProperties());
        final String volumeNodeRef = (String) node.getProperties().get(TransientProps.VOLUME_NODEREF);
        NodeRef targetParentRef = null;
        try {
            targetParentRef = new NodeRef(volumeNodeRef);
        } catch (Exception e) {
            // ignore, volumeNodeRef not inserted
        }
        if (targetParentRef != null) {
            final Node existingVolumeNode = getVolumeByDocument(docNodeRef);
            if(existingVolumeNode != null && targetParentRef == null) {
                throw new RuntimeException("Can't remove link from volume, once it has been assigned");
            }
            if (existingVolumeNode == null || !targetParentRef.equals(existingVolumeNode.getNodeRef())) {
                try {
                    return getDocument(nodeService.moveNode(docNodeRef, targetParentRef, DocumentCommonModel.Assocs.DOCUMENT,
                            DocumentCommonModel.Assocs.DOCUMENT).getChildRef());
                } catch (Exception e) {
                    final String msg = "Failed to move document to volumes folder";
                    log.error(msg, e);
                    throw new RuntimeException(msg, e);
                }
            }
        }
        return node;
    }

    @Override
    public void deleteDocument(NodeRef nodeRef) {
        log.debug("Deleting document: " + nodeRef);
        nodeService.deleteNode(nodeRef);
    }

    @Override
    public boolean isMetadataEditAllowed(NodeRef nodeRef) {
        if (userService.isDocumentManager()) {
            return true;
        }
        String ownerId = (String) nodeService.getProperty(nodeRef, DocumentCommonModel.Props.OWNER_ID);
        return authenticationService.getCurrentUserName().equals(ownerId);
    }

    @Override
    public void addPropertiesModifierCallback(QName qName, PropertiesModifierCallback propertiesModifierCallback) {
        this.creationPropertiesModifierCallbacks.put(qName, propertiesModifierCallback);
    }

    @Override
    public Node getVolumeByDocument(NodeRef nodeRef) {
        return generalService.getParentWithType(nodeRef, VolumeModel.Types.VOLUME);
    }

    @Override
    public Node[] getAncestorNodesByDocument(NodeRef nodeRef) {
        Node volumeNode = getVolumeByDocument(nodeRef);
        Node seriesNode = volumeNode != null ? getSeriesByVolume(volumeNode.getNodeRef()) : null;
        Node functionNode = seriesNode != null ? getFunctionBySeries(seriesNode.getNodeRef()): null;
        return new Node[] {volumeNode, seriesNode, functionNode};
    }

    private Node getFunctionBySeries(NodeRef seriesRef) {
        return seriesRef == null ? null : generalService.getParentWithType(seriesRef, FunctionsModel.Types.FUNCTION);
    }

    private Node getSeriesByVolume(NodeRef volumeRef) {
        return volumeRef == null ? null : generalService.getParentWithType(volumeRef, SeriesModel.Types.SERIES);
    }

    // START: getters / setters
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void setDraftsPath(String draftsPath) {
        this.draftsPath = draftsPath;
    }
    // END: getters / setters
}
