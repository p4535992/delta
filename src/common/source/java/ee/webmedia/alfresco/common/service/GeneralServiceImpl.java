package ee.webmedia.alfresco.common.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Ats Uiboupin
 */
public class GeneralServiceImpl implements GeneralService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(GeneralServiceImpl.class);

    private StoreRef store;
    private NodeService nodeService;
    private NamespaceService namespaceService;
    private DictionaryService dictionaryService;
    private SearchService searchService;
    private FileFolderService fileFolderService;

    @Override
    public StoreRef getStore() {
        return store;
    }

    @Override
    public NodeRef getNodeRef(String nodeRefXPath) {
        NodeRef nodeRef = nodeService.getRootNode(store);
        String[] xPathParts;

        if (nodeRefXPath.contains("{")) {
            xPathParts = StringUtils.split(StringUtils.replace(nodeRefXPath, "/{", "|{"), "|");
        } else {
            xPathParts = StringUtils.split(nodeRefXPath, "/");
        }

        for (String xPathPart : xPathParts) {
            if (xPathPart.startsWith("/")) {
                xPathPart = StringUtils.removeStart(xPathPart, "/");
            }

            QName qName = QName.resolveToQName(namespaceService, xPathPart);

            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, RegexQNamePattern.MATCH_ALL, qName);
            if (childAssocs.size() != 1) {
                String msg = "Expected 1, got " + childAssocs.size() + " childAssocs for xPathPart '"
                        + xPathPart + "' when searching for node with xPath '" + nodeRefXPath + "'";
                if (childAssocs.size() == 0) {
                    log.trace(msg);
                    return null;
                }
                throw new RuntimeException(msg);
            }
            nodeRef = childAssocs.get(0).getChildRef();
        }
        return nodeRef;
    }

    @Override
    public Node getParentWithType(NodeRef childRef, final QName parentType) {
        final NodeRef parentRef = nodeService.getPrimaryParent(childRef).getParentRef();
        final QName realParentType = nodeService.getType(parentRef);
        if (parentType.equals(realParentType)) {
            return fetchNode(parentRef);
        }
        return null;
    }
    
    @Override
    public Node getAncestorWithType(NodeRef childRef, QName ancestorType) {
        final NodeRef parentRef = nodeService.getPrimaryParent(childRef).getParentRef();
        final QName realParentType = nodeService.getType(parentRef);
        if (ancestorType.equals(realParentType)) {
            return fetchNode(parentRef);
        }
        if(realParentType.equals(ContentModel.TYPE_STOREROOT)) {
            return null;
        }
        return getAncestorWithType(parentRef, ancestorType);
        
    }

    @Override
    public boolean isExistingPropertyValueEqualTo(Node currentNode, final QName property, final Object equalityTestValue) {
        final Object realValue = currentNode.getProperties().get(property.toString());
        return equalityTestValue == null ? realValue == null : equalityTestValue.equals(realValue);
    }

    /**
     * Create node from nodRef and populate it with properties and aspects
     * @param nodeRef
     * @return
     */
    @Override
    public Node fetchNode(NodeRef nodeRef) {
        final Node node = new Node(nodeRef);
        node.getAspects();
        node.getProperties();
        return node;
    }

    @Override
    public ChildAssociationRef getLastChildAssocRef(String nodeRefXPath) {
        ChildAssociationRef ref = null;
        String[] xPathParts = StringUtils.split(nodeRefXPath, '/');
        for (String xPathPart : xPathParts) {
            QName qName = QName.resolveToQName(namespaceService, xPathPart);
            NodeRef parent = (ref == null ? nodeService.getRootNode(store) : ref.getChildRef());
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent, RegexQNamePattern.MATCH_ALL, qName);
            if (childAssocs.size() != 1) {
                String msg = "Expected 1, got " + childAssocs.size() + " childAssocs for xPathPart '"
                        + xPathPart + "' when searching for node with xPath '" + nodeRefXPath + "'";
                if (childAssocs.size() == 0) {
                    log.trace(msg);
                    return null;
                }
                throw new RuntimeException(msg);
            }
            ref = childAssocs.get(0);
        }
        return ref;
    }

    @Override
    public List<NodeRef> searchNodes(String input, QName type, Set<QName> props) {
        return searchNodes(input, type, props, 100);
    }

    @Override
    public List<NodeRef> searchNodes(String input, QName type, Set<QName> props, int limit) {
        if (input == null) {
            return null;
        }
        String parsedInput = SearchUtil.replace(input, " ").trim();
        if (parsedInput.length() < 1) {
            return null;
        }

        String query = SearchUtil.generateQuery(parsedInput, type, props);
        log.debug("Query: " + query);

        SearchParameters sp = new SearchParameters();
        sp.addStore(store);
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setLimit(limit);
        sp.setLimitBy(LimitBy.FINAL_SIZE);
        // sp.addSort("@" + OrganizationStructureModel.Props.NAME, true); // XXX why doesn't lucene sorting work?
        sp.setQuery(query);

        ResultSet resultSet = searchService.query(sp);
        try {
            log.debug("Found " + resultSet.length() + " nodes");
            return resultSet.getNodeRefs();
        } finally {
            resultSet.close();
        }
    }

    @Override
    public void setPropertiesIgnoringSystem(Map<QName, Serializable> properties, NodeRef nodeRef) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        for (QName qName : properties.keySet()) {
            addToPropsIfNotSystem(qName, properties.get(qName), props);
        }
        nodeService.addProperties(nodeRef, props);
    }

    @Override
    public void setPropertiesIgnoringSystem(NodeRef nodeRef, Map<String, Object> nodeProps) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        for (String key : nodeProps.keySet()) {
            QName qname = QName.createQName(key);
            addToPropsIfNotSystem(qname, (Serializable) nodeProps.get(key), props);
        }
        nodeService.addProperties(nodeRef, props);
    }

    private void addToPropsIfNotSystem(QName qname, Serializable value, Map<QName, Serializable> props) {
        // ignore system and contentModel properties
        if (RepoUtil.isSystemProperty(qname)) {
            return;
        }
        // check for empty strings when using number types, set to null in this case
        if ((value != null) && (value instanceof String) && (value.toString().length() == 0)) {
            PropertyDefinition propDef = dictionaryService.getProperty(qname);
            if (propDef != null) {
                if (propDef.getDataType().getName().equals(DataTypeDefinition.DOUBLE) ||
                        propDef.getDataType().getName().equals(DataTypeDefinition.FLOAT) ||
                        propDef.getDataType().getName().equals(DataTypeDefinition.INT) ||
                        propDef.getDataType().getName().equals(DataTypeDefinition.LONG)) {
                    value = null;
                }
            }
        }
        props.put(qname, value);
    }

    @Override
    public String getExistingRepoValue4ComponentGenerator() {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        final Object[] nodeAndPropName = (Object[]) requestMap.get(WMUIProperty.REPO_NODE);
        requestMap.remove(WMUIProperty.REPO_NODE); // remove nodeAndPropName from request map
        if (nodeAndPropName == null) {
            return null;
        }
        Node node = (Node) nodeAndPropName[0];
        String propName = (String) nodeAndPropName[1];
        QName qName = QName.createQName(propName, namespaceService);
        final Map<String, Object> properties = node.getProperties();
        final Object value = properties.get(qName.toString());
        return DefaultTypeConverter.INSTANCE.convert(String.class, value);
    }

    @Override
    public ByteArrayOutputStream getZipFileFromFiles(NodeRef document, List<String> fileNodeRefs) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ZipArchiveOutputStream out = new ZipArchiveOutputStream(byteStream);
        InputStream in = null;
        try {
            out.setLevel(Deflater.DEFAULT_COMPRESSION);
            out.setEncoding("Cp437");
            out.setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy.NOT_ENCODEABLE);
            byte[] buffer = new byte[10240];
            for (FileInfo fileInfo : fileFolderService.listFiles(document)) {
                if (fileNodeRefs.contains(fileInfo.getNodeRef().toString())) {
                    ZipArchiveEntry entry = new ZipArchiveEntry(fileInfo.getName());
                    entry.setSize(fileInfo.getContentData().getSize());
                    out.putArchiveEntry(entry);
                    in = fileFolderService.getReader(fileInfo.getNodeRef()).getContentInputStream();
                    int length = 0;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                    out.closeArchiveEntry();
                    in.close();
                }
            }
            out.finish();
        } catch (IOException e) {
            log.warn("Failed to zip up files.", e);
            throw new RuntimeException("Failed to zip up files.", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
            try {
                in.close();
            } catch (Exception e) {
            }
        }
        return byteStream;
    }

    // START: getters / setters
    public void setDefaultStore(String store) {
        this.store = new StoreRef(store);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }
    
    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }
    // END: getters / setters

}
