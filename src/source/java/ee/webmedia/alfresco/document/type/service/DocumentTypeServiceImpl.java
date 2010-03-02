package ee.webmedia.alfresco.document.type.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.model.DocumentTypeModel;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

/**
 * @author Alar Kvell
 */
public class DocumentTypeServiceImpl implements DocumentTypeService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentTypeServiceImpl.class);

    private GeneralService generalService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private MenuService menuService;

    private static final BeanPropertyMapper<DocumentType> documentTypeBeanPropertyMapper = BeanPropertyMapper.newInstance(DocumentType.class);

    @Override
    public List<DocumentType> getAllDocumentTypes() {
        return getAllDocumentTypes(null);
    }

    @Override
    public List<DocumentType> getAllDocumentTypes(boolean used) {
        return getAllDocumentTypes(Boolean.valueOf(used));
    }

    private List<DocumentType> getAllDocumentTypes(Boolean used) {
        String xPath = DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE;
        final NodeRef documentTypesRoot = generalService.getNodeRef(xPath);
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(documentTypesRoot);
        List<DocumentType> documentTypes = new ArrayList<DocumentType>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            DocumentType documentType =  documentTypeBeanPropertyMapper.toObject(nodeService.getProperties(childAssoc.getChildRef()));
            if (used != null && documentType.isUsed() != used) {
                continue;
            }
            documentType.setId(childAssoc.getQName());
            documentTypes.add(documentType);
        }
        return documentTypes;
    }

    @Override
    public void updateDocumentTypes(Collection<DocumentType> documentTypes) {
        for (DocumentType documentType : documentTypes) {
            String xPath = DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE + "/" + documentType.getId().toPrefixString(namespaceService);
            NodeRef nodeRef = generalService.getNodeRef(xPath);
            Map<QName, Serializable> props = documentTypeBeanPropertyMapper.toProperties(documentType);
            props.remove(DocumentTypeModel.Props.NAME);
            if (log.isDebugEnabled()) {
                log.debug("Updating documentType xPath=" + xPath + " nodeRef=" + nodeRef + " with properties:\n" + props);
            }
            nodeService.addProperties(nodeRef, props);
        }
        menuService.reload();
    }

    @Override
    public DocumentType getDocumentType(QName docTypeId) {
        String xPath = DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE + "/" + docTypeId.toPrefixString(namespaceService);
        ChildAssociationRef childAssoc = generalService.getLastChildAssocRef(xPath);
        if (childAssoc == null) {
            return null;
        }
        DocumentType documentType = documentTypeBeanPropertyMapper.toObject(nodeService.getProperties(childAssoc.getChildRef()));
        documentType.setId(childAssoc.getQName());
        return documentType;
    }

    @Override
    public DocumentType getDocumentType(String docTypeId) {
        final QName qName = QName.resolveToQName(namespaceService, docTypeId);
        if(qName==null) {
            throw new RuntimeException("docTypeId '"+docTypeId+"' was not resolved to QName");
        }
        final DocumentType documentType = getDocumentType(qName);
        if(documentType==null) {
            throw new RuntimeException("docType '"+qName+"' was not found in document types list");
        }
        return documentType;
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
    // END: getters / setters
}
