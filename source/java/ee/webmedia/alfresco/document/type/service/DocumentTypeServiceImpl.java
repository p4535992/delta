package ee.webmedia.alfresco.document.type.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.model.DocumentTypeModel;
import ee.webmedia.alfresco.document.type.model.DocumentTypeModel.Assocs;
import ee.webmedia.alfresco.document.type.model.DocumentTypeModel.Types;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class DocumentTypeServiceImpl implements DocumentTypeService, BeanFactoryAware {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentTypeServiceImpl.class);
    private GeneralService generalService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private MenuService menuService;
    private AdrService _adrService;
    private BeanFactory beanFactory;
    private NodeRef documentTypesRef;

    @Override
    public List<DocumentType> getAllDocumentTypes() {
        return getAllDocumentTypes(null);
    }

    @Override
    public List<DocumentType> getAllDocumentTypes(boolean used) {
        return getAllDocumentTypes(Boolean.valueOf(used));
    }

    @Override
    public Set<QName> getPublicAdrDocumentTypeQNames() {
        List<ChildAssociationRef> childAssocs = getAllDocumentTypeChildAssocs();
        Set<QName> documentTypes = new HashSet<QName>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            Boolean publicAdr = (Boolean) nodeService.getProperty(childAssoc.getChildRef(), DocumentTypeModel.Props.PUBLIC_ADR);
            if (publicAdr != null && publicAdr) {
                documentTypes.add(childAssoc.getQName());
            }
        }
        return documentTypes;
    }

    @Override
    public List<DocumentType> getPublicAdrDocumentTypes() {
        List<DocumentType> documentTypes = getAllDocumentTypes();
        for (Iterator<DocumentType> i = documentTypes.iterator(); i.hasNext();) {
            DocumentType documentType = i.next();
            if (!documentType.isPublicAdr()) {
                i.remove();
            }
        }
        return documentTypes;
    }

    private List<ChildAssociationRef> getAllDocumentTypeChildAssocs() {
        String xPath = DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE;
        final NodeRef documentTypesRoot = generalService.getNodeRef(xPath);
        return nodeService.getChildAssocs(documentTypesRoot);
    }

    private List<DocumentType> getAllDocumentTypes(Boolean used) {
        List<ChildAssociationRef> childAssocs = getAllDocumentTypeChildAssocs();
        List<DocumentType> documentTypes = new ArrayList<DocumentType>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            DocumentType documentType = constructDocumentType(childAssoc.getChildRef(), childAssoc.getQName());
            if (used != null && documentType.isUsed() != used) {
                continue;
            }
            documentTypes.add(documentType);
        }
        return documentTypes;
    }

    @Override
    public void saveOrUpdateDocumentType(DocumentType documentType) {
        QName id = documentType.getId();
        boolean isNew = false;
        if (id == null) {
            String tmpId = documentType.getTmpId();
            if (StringUtils.isBlank(tmpId)) {
                throw new UnableToPerformException("document_type_error_id_mandatory");
            } else if (tmpId.length() > QName.MAX_LENGTH) {
                throw new UnableToPerformException("document_type_error_id_tooLong");
            }
            id = QName.createQName(DocumentTypeModel.URI, tmpId);
            isNew = true;
            documentType.setId(id);
        }
        String xPath = DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE + "/" + id.toPrefixString(namespaceService);
        NodeRef nodeRef = generalService.getNodeRef(xPath);
        if (isNew && nodeRef != null) {
            throw new UnableToPerformException("document_type_error_id_alreadyExists", id.getLocalName());
        }
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(documentType.getNode().getProperties());
        if (log.isDebugEnabled()) {
            log.debug("Updating documentType xPath=" + xPath + " nodeRef=" + nodeRef + " with properties:\n" + props);
        }

        Boolean oldPublicAdr = isNew ? false : (Boolean) nodeService.getProperty(nodeRef, DocumentTypeModel.Props.PUBLIC_ADR);
        if (oldPublicAdr == null) {
            oldPublicAdr = Boolean.FALSE;
        }

        Boolean newPublicAdr = (Boolean) props.get(DocumentTypeModel.Props.PUBLIC_ADR);
        if (newPublicAdr == null) {
            newPublicAdr = Boolean.FALSE;
        }

        if (oldPublicAdr.booleanValue() != newPublicAdr.booleanValue()) {
            if (log.isDebugEnabled()) {
                log.debug("Changing publicAdr of DocumentType " + id.toPrefixString(namespaceService) + " from "
                        + oldPublicAdr.toString().toUpperCase() + " to " + newPublicAdr.toString().toUpperCase());
            }
            if (newPublicAdr) {
                getAdrService().addDocumentType(id);
            } else {
                getAdrService().deleteDocumentType(id);
            }
        }
        props = generalService.getPropertiesIgnoringSys(props);
        if (isNew) {
            nodeService.createNode(getDocumentTypesRef(), Assocs.DOCUMENT_TYPE, id, Types.DOCUMENT_TYPE, props);
        } else {
            generalService.setPropertiesIgnoringSystem(props, nodeRef);
        }
        menuService.menuUpdated();
    }

    private NodeRef getDocumentTypesRef() {
        if (documentTypesRef == null) {
            String xPath = DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE;
            documentTypesRef = generalService.getNodeRef(xPath);
        }
        return documentTypesRef;
    }

    @Override
    public DocumentType getDocumentType(QName docTypeId) {
        String xPath = DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE + "/" + docTypeId.toPrefixString(namespaceService);
        ChildAssociationRef childAssoc = generalService.getLastChildAssocRef(xPath);
        if (childAssoc == null) {
            return null;
        }
        return constructDocumentType(childAssoc.getChildRef(), childAssoc.getQName());
    }

    private DocumentType constructDocumentType(NodeRef docTypeRef, QName id) {
        return new DocumentType(id, new WmNode(docTypeRef, DocumentTypeModel.Types.DOCUMENT_TYPE));
    }

    @Override
    public DocumentType getDocumentType(String docTypeId) {
        final QName qName = QName.resolveToQName(namespaceService, docTypeId);
        if (qName == null) {
            throw new RuntimeException("docTypeId '" + docTypeId + "' was not resolved to QName");
        }
        final DocumentType documentType = getDocumentType(qName);
        if (documentType == null) {
            throw new RuntimeException("docType '" + qName + "' was not found in document types list");
        }
        return documentType;
    }

    @Override
    public QName getIncomingLetterType() {
        return getUsedDocumentType(DocumentSubtypeModel.Types.INCOMING_LETTER, DocumentSubtypeModel.Types.INCOMING_LETTER_MV);
    }

    @Override
    public QName getOutgoingLetterType() {
        return getUsedDocumentType(DocumentSubtypeModel.Types.OUTGOING_LETTER, DocumentSubtypeModel.Types.OUTGOING_LETTER_MV);
    }

    private QName getUsedDocumentType(QName primaryTypeQName, QName secondaryTypeQName) {
        DocumentType primaryType = getDocumentType(primaryTypeQName);
        DocumentType secondaryType = getDocumentType(secondaryTypeQName);
        // One case returns secondary type
        // * if only secondary type is enabled
        if (secondaryType.isUsed() || !primaryType.isUsed()) {
            return secondaryType.getId();
        }
        // All other cases return primary type
        // * if only primary type is enabled
        // * if both types are enabled
        // * if neither type is enabled
        return primaryType.getId();
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * To break Circular dependency
     */
    private AdrService getAdrService() {
        if (_adrService == null) {
            _adrService = (AdrService) beanFactory.getBean(AdrService.BEAN_NAME);
        }
        return _adrService;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    // END: getters / setters

    @Override
    public DocumentType createNewUnSavedDocumentType() {
        return new DocumentType(generalService.createNewUnSaved(DocumentTypeModel.Types.DOCUMENT_TYPE, null));
    }

}
