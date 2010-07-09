package ee.webmedia.alfresco.common.service;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomConstants.VALUE_INDEX_IN_MULTIVALUED_PROPERTY;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.Deflater;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
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
import org.alfresco.util.GUID;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.upload.UploadFileInput.FileWithContentType;
import ee.webmedia.alfresco.common.web.WmNode;
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
    private ContentService contentService;
    private MimetypeService mimetypeService;

    @Override
    public StoreRef getStore() {
        return store;
    }

    @Override
    public NodeRef getNodeRef(String nodeRefXPath) {
        return getNodeRef(nodeRefXPath, store);
    }

    public NodeRef getNodeRef(String nodeRefXPath, StoreRef storeRef) {
        NodeRef nodeRef = nodeService.getRootNode(storeRef);
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
        final NodeRef parentRef = getParentNodeRefWithType(childRef, parentType);
        return parentRef != null ? fetchNode(parentRef) : null;
    }

    @Override
    public Node getPrimaryParent(NodeRef nodeRef) {
        final NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        return fetchNode(parentRef);
    }

    @Override
    public NodeRef getAncestorNodeRefWithType(NodeRef childRef, QName ancestorType) {
        final NodeRef parentRef = nodeService.getPrimaryParent(childRef).getParentRef();
        final QName realParentType = nodeService.getType(parentRef);
        if (ancestorType.equals(realParentType)) {
            return parentRef;
        }
        if (realParentType.equals(ContentModel.TYPE_STOREROOT)) {
            return null;
        }
        return getAncestorNodeRefWithType(parentRef, ancestorType);
    }

    @Override
    public Node getAncestorWithType(NodeRef childRef, QName ancestorType) {
        NodeRef ancestorNodeRef = getAncestorNodeRefWithType(childRef, ancestorType);
        return (ancestorNodeRef == null) ? null : fetchNode(ancestorNodeRef);

    }

    @Override
    public void saveAddedAssocs(Node node) {
        Map<String, Map<String, AssociationRef>> addedAssocs = node.getAddedAssociations();
        for (Map<String, AssociationRef> typedAssoc : addedAssocs.values()) {
            for (AssociationRef assoc : typedAssoc.values()) {
                nodeService.createAssociation(assoc.getSourceRef(), assoc.getTargetRef(), assoc.getTypeQName());
            }
        }
    }

    @Override
    public void saveRemovedChildAssocs(Node node) {
        Map<String, Map<String, ChildAssociationRef>> removedChildAssocs = node.getRemovedChildAssociations();
        for (Map<String, ChildAssociationRef> typedAssoc : removedChildAssocs.values()) {
            for (ChildAssociationRef assoc : typedAssoc.values()) {
                nodeService.removeChild(assoc.getParentRef(), assoc.getChildRef());
            }
        }
    }

    /**
     * Create node from nodRef and populate it with properties and aspects
     * 
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
    public WmNode createNewUnSaved(QName type, Map<QName, Serializable> props) {
        Set<QName> aspects = getDefaultAspects(type);
        props = addDefaultValues(type, aspects, props);
        return new WmNode(/* unsaved */new NodeRef(WmNode.NOT_SAVED_STORE, GUID.generate()), type, aspects, props);
    }

    /**
     * @param type - default values of this type are added to <code>props</code> map
     * @param aspects
     * @param props map of properties
     * @return new map if <code>props == null</code>, otherwise the same map. Result contains also default properties of given type
     */
    private Map<QName, Serializable> addDefaultValues(QName type, Set<QName> aspects, Map<QName, Serializable> props) {
        return addMissingValues(props, getDefaultProperties(type));
    }

    private Map<QName, Serializable> addMissingValues(Map<QName, Serializable> primaryValues, final Map<QName, Serializable> defaultValues) {
        if (defaultValues.size() != 0) {
            if (primaryValues == null) {
                primaryValues = new HashMap<QName, Serializable>();
            }
            for (Entry<QName, Serializable> entry : defaultValues.entrySet()) {
                final QName key = entry.getKey();
                if (!primaryValues.containsKey(key)) {
                    primaryValues.put(key, entry.getValue());
                }
            }
        }
        return primaryValues;
    }

    @Override
    public Map<QName, Serializable> getDefaultProperties(QName className) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        // Aspects are supposed to be in the order of most-parent ones first
        // So children's property-overrides should overwrite parent ones
        // Haven't tested it (we did not have any property-overrides in aspects)
        for (QName aspect : getDefaultAspects(className)) {
            props.putAll(getDefaultPropertiesForSingleClass(aspect));
        }
        props.putAll(getDefaultPropertiesForSingleClass(className));
        return props;
    }

    private Map<QName, Serializable> getDefaultPropertiesForSingleClass(QName className) {
        Map<QName, PropertyDefinition> propDefs = dictionaryService.getPropertyDefs(className);
        if (propDefs == null) {
            throw new RuntimeException("Class not found: " + className);
        }
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        for (Map.Entry<QName, PropertyDefinition> entry : propDefs.entrySet()) {
            PropertyDefinition propDef = entry.getValue();
            Serializable value = (Serializable) DefaultTypeConverter.INSTANCE.convert(propDef.getDataType(), propDef.getDefaultValue());
            if (value != null && propDef.isMultiValued()) {
                ArrayList<Serializable> values = new ArrayList<Serializable>();
                values.add(value);
                value = values;
            }
            props.put(entry.getKey(), value);
        }
        return props;
    }

    @Override
    public LinkedHashSet<QName> getDefaultAspects(QName className) {
        ClassDefinition classDef = dictionaryService.getClass(className);
        if (classDef == null) {
            throw new RuntimeException("Class not found: " + className);
        }
        List<AspectDefinition> aspectDefs = classDef.getDefaultAspects();
        LinkedHashSet<QName> aspects = new LinkedHashSet<QName>();
        for (AspectDefinition aspectDef : aspectDefs) {
            RepoUtil.getMandatoryAspects(aspectDef, aspects);
            aspects.add(aspectDef.getName());
        }
        return aspects;
    }

    @Override
    public TypeDefinition getAnonymousType(QName type) {
        return dictionaryService.getAnonymousType(type, getDefaultAspects(type));
    }

    @Override
    public TypeDefinition getAnonymousType(Node node) {
        return dictionaryService.getAnonymousType(node.getType(), node.getAspects());
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
        Map<QName, Serializable> props = getPropertiesIgnoringSys(properties);
        nodeService.addProperties(nodeRef, props);
    }

    @Override
    public void setPropertiesIgnoringSystem(NodeRef nodeRef, Map<String, Object> nodeProps) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        for (String key : nodeProps.keySet()) {
            QName qname = QName.createQName(key);
            addToPropsIfNotSystem(qname, (Serializable) nodeProps.get(key), props);
        }
        nodeService.addProperties(nodeRef, getPropertiesIgnoringSystem(nodeProps));
    }

    @Override
    public Map<QName, Serializable> getPropertiesIgnoringSystem(Map<String, Object> nodeProps) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        for (String key : nodeProps.keySet()) {
            QName qname = QName.createQName(key);
            addToPropsIfNotSystem(qname, (Serializable) nodeProps.get(key), props);
        }
        return props;
    }

    @Override
    public Map<QName, Serializable> getPropertiesIgnoringSys(Map<QName, Serializable> nodeProps) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        for (QName key : nodeProps.keySet()) {
            addToPropsIfNotSystem(key, nodeProps.get(key), props);
        }
        return props;
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
    public void savePropertiesFiles(Map<QName, Serializable> props) {
        for (Entry<QName, Serializable> entry : props.entrySet()) {
            Serializable value = entry.getValue();
            if (value instanceof FileWithContentType) {
                FileWithContentType container = (FileWithContentType) value;

                String mimetype = container.contentType.toLowerCase(); // Alfresco keeps mimetypes lowercase
                String fileName = container.fileName;
                File file = container.file;
                ContentWriter writer = contentService.getWriter(null, null, false);
                writeFile(writer, file, fileName, mimetype);
                ContentData contentData = writer.getContentData();
                if (log.isDebugEnabled()) {
                    log.debug("Saved file: " + contentData);
                }
                entry.setValue(contentData);
            }
        }
    }

    @Override
    public NodeRef addFileOrFolder(File fileOrFolder, NodeRef parentNodeRef, boolean flatten) {
        if (fileOrFolder.isDirectory()) {
            if (!flatten) {
                final FileInfo folderInfo = fileFolderService.create(parentNodeRef, fileOrFolder.getName(), ContentModel.TYPE_FOLDER);
                parentNodeRef = folderInfo.getNodeRef();
            }
            final File[] listFiles = fileOrFolder.listFiles();
            for (File file2 : listFiles) {
                addFileOrFolder(file2, parentNodeRef, flatten);
            }
            return parentNodeRef;
        }
        return addFile(fileOrFolder, parentNodeRef);
    }

    @Override
    public NodeRef addFile(File file, NodeRef parentNodeRef) {
        String fileName = getUniqueFileName(parentNodeRef, file.getName());
        final FileInfo fileInfo = fileFolderService.create(parentNodeRef, fileName, ContentModel.TYPE_CONTENT);
        final NodeRef fileRef = fileInfo.getNodeRef();
        final ContentWriter writer = fileFolderService.getWriter(fileRef);
        writeFile(writer, file, fileName, null);
        return fileRef;
    }

    @Override
    public void writeFile(ContentWriter writer, File file, String fileName, String mimetype) {
        // use container.mimeType; if our mimetype map has it, then use it; otherwise guess based on filename
        if (MimetypeMap.MIMETYPE_BINARY.equals(mimetype) || !mimetypeService.getExtensionsByMimetype().containsKey(mimetype)) {
            String oldMimetype = mimetype;
            mimetype = mimetypeService.guessMimetype(fileName);
            if (log.isDebugEnabled() && !StringUtils.equals(oldMimetype, mimetype)) {
                log.debug("User provided mimetype '" + oldMimetype + "', but we are guessing mimetype based on filename '" + fileName + "' => '"
                        + mimetype + "'");
            }
        }

        String encoding;
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            Charset charset = mimetypeService.getContentCharsetFinder().getCharset(is, mimetype);
            encoding = charset.name();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // Do nothing
            }
        }

        writer.setMimetype(mimetype);
        writer.setEncoding(encoding);
        writer.putContent(file);
    }

    private NodeRef getParentNodeRefWithType(NodeRef childRef, QName parentType) {
        final NodeRef parentRef = nodeService.getPrimaryParent(childRef).getParentRef();
        final QName realParentType = nodeService.getType(parentRef);
        if (parentType.equals(realParentType)) {
            return parentRef;
        }
        return null;
    }

    @Override
    public String getExistingRepoValue4ComponentGenerator() {
        return getExistingRepoValue4ComponentGenerator(String.class);
    }

    private <T> T getExistingRepoValue4ComponentGenerator(final Class<T> requiredClasss) {
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
        Object value = properties.get(qName.toString());
        if (value != null && StringUtils.equals(value.getClass().getCanonicalName(), ArrayList.class.getCanonicalName())) {
            final Integer valueIndex = (Integer) requestMap.get(VALUE_INDEX_IN_MULTIVALUED_PROPERTY);
            @SuppressWarnings("unchecked")
            List<Object> array = (List<Object>) value;
            value = array.get(valueIndex);
        }
        return DefaultTypeConverter.INSTANCE.convert(requiredClasss, value);
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

    @Override
    public String getUniqueFileName(NodeRef folder, String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName);
        if (StringUtils.isBlank(extension)) {
            extension = MimetypeMap.EXTENSION_BINARY;
        }
        String suffix = "";
        int i = 1;
        while (fileFolderService.searchSimple(folder, baseName + suffix + "." + extension) != null) {
            suffix = " (" + i + ")";

            i++;
        }
        return baseName + suffix + "." + extension;

    }

    @Override
    public String limitFileNameLength(String filename, int maxLength, String marker) {
        marker = (marker == null) ? "...." : marker;

        if (filename != null && filename.length() > maxLength) {
            String baseName = FilenameUtils.getBaseName(filename);
            String extension = FilenameUtils.getExtension(filename);
            baseName = baseName.substring(0, maxLength - extension.length() - marker.length());
            filename = baseName + marker + extension;
        }
        return filename;
    }

    @Override
    public void updateParentContainingDocsCount(final NodeRef parentNodeRef, final QName propertyName, boolean added, Integer count) {
        if (parentNodeRef == null)
            return;

        Serializable valueProperty = nodeService.getProperty(parentNodeRef, propertyName);
        if (valueProperty == null) { // first time, assign default value
            valueProperty = 0;
        }

        int value = Integer.parseInt(valueProperty.toString());
        count = (count == null) ? 1 : count;
        final int newValue = (added) ? value + count : value - count;

        // Update property with elevated rights
        AuthenticationUtil.runAs(new RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                nodeService.setProperty(parentNodeRef, propertyName, newValue);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    public void deleteNodeRefs(Collection<NodeRef> nodeRefs) {
        for (NodeRef nodeRef : nodeRefs) {
            nodeService.deleteNode(nodeRef);
        }
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

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    // END: getters / setters

}
