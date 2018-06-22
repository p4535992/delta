package ee.webmedia.alfresco.common.service;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomConstants.VALUE_INDEX_IN_MULTIVALUED_PROPERTY;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
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
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
//import org.postgresql.util.PGobject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.upload.UploadFileInput.FileWithContentType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.log.service.DocumentPropertiesChangeHolder;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.AdjustableSemaphore;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class GeneralServiceImpl implements GeneralService, BeanFactoryAware {
    private static final String REFRESH_MATERIALIZED_VIEWS = "refreshMaterializedViews";

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(GeneralServiceImpl.class);

    private StoreRef store;
    private StoreRef archivalsStore;
    private NodeService nodeService;
    private NamespaceService namespaceService;
    private DictionaryService dictionaryService;
    private SearchService searchService;
    private FileFolderService fileFolderService;
    private ContentService contentService;
    private MimetypeService mimetypeService;
    private TransactionService transactionService;
    private SimpleJdbcTemplate jdbcTemplate;

    private final AtomicLong backgroundThreadCounter = new AtomicLong();

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        // we have only one BeanFactory and it shouldn't change so let's make it available to static methods
        AppConstants.setBeanFactory(beanFactory);
        // set static field StoreRef so that it is available already for abstractModuleComponents that run before storeRef would normally be set
        ImporterBootstrap bootstrap = (ImporterBootstrap) beanFactory.getBean(Application.BEAN_IMPORTER_BOOTSTRAP);
        Repository.setStoreRef(bootstrap.getStoreRef());
    }

    @Override
    public StoreRef getStore() {
        return store;
    }

    private LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs;
    private LinkedHashSet<StoreRef> archivalsStoreRefs;
    private LinkedHashSet<StoreRef> allWithArchivalsStoreRefs;

    @Override
    public void setArchivalsStoreVOs(LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs) {
        this.archivalsStoreVOs = new LinkedHashSet<ArchivalsStoreVO>(archivalsStoreVOs);
    }

    @Override
    public LinkedHashSet<ArchivalsStoreVO> getArchivalsStoreVOs() {
        return archivalsStoreVOs;
    }

    @Override
    public LinkedHashSet<StoreRef> getAllWithArchivalsStoreRefs() {
        if (allWithArchivalsStoreRefs == null) {
            LinkedHashSet<StoreRef> stores = new LinkedHashSet<StoreRef>(getArchivalsStoreRefs());
            stores.add(store);
            allWithArchivalsStoreRefs = stores;
        }
        return allWithArchivalsStoreRefs;
    }

    @Override
    public LinkedHashSet<StoreRef> getAllStoreRefsWithTrashCan() {
        LinkedHashSet<StoreRef> allStoreRefs = new LinkedHashSet<StoreRef>(getAllWithArchivalsStoreRefs());
        allStoreRefs.add(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
        return allStoreRefs;
    }

    @Override
    public LinkedHashSet<StoreRef> getArchivalsStoreRefs() {
        if (archivalsStoreRefs == null) {
            LinkedHashSet<StoreRef> stores = new LinkedHashSet<StoreRef>();
            for (ArchivalsStoreVO storeVO : getArchivalsStoreVOs()) {
                stores.add(storeVO.getStoreRef());
            }
            archivalsStoreRefs = stores;
        }
        return archivalsStoreRefs;
    }

    @Override
    public NodeRef getPrimaryArchivalsNodeRef() {
        return archivalsStoreVOs.iterator().next().getNodeRef();
    }

    @Override
    public StoreRef getArchivalsStoreRef() {
        return archivalsStore;
    }

    @Override
    public boolean isArchivalsStoreRef(StoreRef storeRef) {
        if (storeRef == null) {
            return false;
        }
        if (storeRef.equals(archivalsStore)) {
            return true;
        }
        if (archivalsStoreVOs != null) {
            for (ArchivalsStoreVO storeVO : archivalsStoreVOs) {
                if (storeVO.getStoreRef().equals(storeRef)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasAdditionalArchivalStores() {
        return archivalsStoreVOs != null && archivalsStoreVOs.size() > 1;
    }

    @Override
    public String getStoreTitle(StoreRef storeRef) {
        if (store.equals(storeRef)) {
            return "Dokumentide loetelu";
        }
        if (archivalsStoreVOs != null) {
            for (ArchivalsStoreVO storeVO : archivalsStoreVOs) {
                if (storeVO.getStoreRef().equals(storeRef)) {
                    return storeVO.getTitle();
                }
            }
        }
        return null;
    }

    @Override
    public NodeRef getNodeRef(String nodeRefXPath) {
        return getNodeRef(nodeRefXPath, store);
    }

    @Override
    public NodeRef getNodeRef(String nodeRefXPath, StoreRef storeRef) {
        return getNodeRef(nodeRefXPath, nodeService.getRootNode(storeRef));
    }

    @Override
    public NodeRef getNodeRef(String nodeRefXPath, NodeRef root) {
        Assert.notNull(root, "rootRef is a mandatory parameter");
        NodeRef nodeRef = root;
        String[] xPathParts;

        if (nodeRefXPath.contains("{")) {
            xPathParts = StringUtils.split(StringUtils.replace(nodeRefXPath, "/{", "|{"), "|");
        } else {
            xPathParts = StringUtils.split(nodeRefXPath, "/");
        }

        int partNr = 0;
        for (String xPathPart : xPathParts) {
            if (xPathPart.startsWith("/")) {
                xPathPart = StringUtils.removeStart(xPathPart, "/");
            }

            QName qName = QName.resolveToQName(namespaceService, xPathPart);

            nodeRef = getChildByAssocName(nodeRef, qName, nodeRefXPath);
            if (++partNr < xPathParts.length && nodeRef == null) {
                throw new IllegalArgumentException("started to resolve xpath based on '" + nodeRefXPath
                        + "'\nxPathParts='" + xPathParts + "'\npart that is incorrect='" + xPathPart + "'");
            }
        }
        return nodeRef;
    }

    @Override
    public NodeRef getChildByAssocName(NodeRef parentRef, QNamePattern assocNamePattern) {
        return getChildByAssocName(parentRef, assocNamePattern, null);
    }

    private NodeRef getChildByAssocName(NodeRef parentRef, QNamePattern assocNamePattern, String nodeRefXPath) {
        Assert.notNull(parentRef, "parentRef is a mandatory parameter");
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentRef, RegexQNamePattern.MATCH_ALL, assocNamePattern);
        if (childAssocs.size() != 1) {
            StringBuilder msg = new StringBuilder("Expected 1, got ").append(childAssocs.size()).append(" childAssocs for assocName '")
                    .append(assocNamePattern instanceof QName ? ((QName) assocNamePattern).toPrefixString(namespaceService) : assocNamePattern).append("'");
            if (nodeRefXPath != null) {
                msg.append(" when searching for node with xPath '").append(nodeRefXPath).append("'");
            }
            if (childAssocs.size() == 0) {
                log.trace(msg);
                return null;
            }
            msg.append(".\nNodeRefs with same xPath:");
            for (ChildAssociationRef childAssociationRef : childAssocs) {
                msg.append("\n").append(childAssociationRef.getChildRef());
            }
            throw new RuntimeException(msg.toString());
        }
        parentRef = childAssocs.get(0).getChildRef();
        return parentRef;
    }

    @Override
    public Node getParentWithType(NodeRef childRef, final QName... parentType) {
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
        return getAncestorNodeRefWithType(childRef, ancestorType, false);
    }

    @Override
    public NodeRef getAncestorNodeRefWithType(NodeRef childRef, QName ancestorType, boolean checkSubTypes) {
        return getAncestorNodeRefWithType(childRef, ancestorType, checkSubTypes, true);
    }

    @Override
    public NodeRef getAncestorNodeRefWithType(NodeRef childRef, QName ancestorType, boolean checkSubTypes, boolean startFromParent) {
        final NodeRef parentRef;
        if (startFromParent) {
            parentRef = nodeService.getPrimaryParent(childRef).getParentRef();
        } else {
            parentRef = childRef;
        }
        final QName realParentType = nodeService.getType(parentRef);
        if (ancestorType.equals(realParentType) || (checkSubTypes && dictionaryService.isSubClass(realParentType, ancestorType))) {
            return parentRef;
        }
        if (realParentType.equals(ContentModel.TYPE_STOREROOT)) {
            return null;
        }
        return getAncestorNodeRefWithType(parentRef, ancestorType, checkSubTypes, true);
    }

    @Override
    public Node getAncestorWithType(NodeRef childRef, QName ancestorType) {
        NodeRef ancestorNodeRef = getAncestorNodeRefWithType(childRef, ancestorType);
        return (ancestorNodeRef == null) ? null : fetchNode(ancestorNodeRef);

    }

    @Override
    public int saveAddedAssocs(Node node) {
        Map<String, Map<String, AssociationRef>> addedAssocs = node.getAddedAssociations();
        int nrOfAddedAssocs = 0;
        for (Map<String, AssociationRef> typedAssoc : addedAssocs.values()) {
            for (AssociationRef assoc : typedAssoc.values()) {
                nodeService.createAssociation(assoc.getSourceRef(), assoc.getTargetRef(), assoc.getTypeQName());
                nrOfAddedAssocs++;
            }
        }
        return nrOfAddedAssocs;
    }

    @Override
    public void saveRemovedChildAssocs(Node node, DocumentPropertiesChangeHolder docPropsChangeHolder) {
        Map<String, Map<String, ChildAssociationRef>> removedChildAssocs = node.getRemovedChildAssociations();
        for (Map<String, ChildAssociationRef> typedAssoc : removedChildAssocs.values()) {
            for (ChildAssociationRef assoc : typedAssoc.values()) {
                final NodeRef childRef = assoc.getChildRef();

                if (RepoUtil.isSaved(childRef) && nodeService.exists(childRef)) {
                    for (ChildAssociationRef childAssoc : nodeService.getChildAssocs(childRef)) {
                        docPropsChangeHolder.addChange(childRef, childAssoc.getTypeQName(), childAssoc.getChildRef(), null);
                    }

                    docPropsChangeHolder.addChange(node.getNodeRef(), assoc.getTypeQName(), childRef, null);
                    nodeService.deleteNode(childRef);
                }
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
        return fetchNode(nodeRef, null);
    }

    @Override
    public Node fetchNode(NodeRef nodeRef, Map<Long, QName> propertyTypes) {
        final Node node = new Node(nodeRef);
        node.getAspects();
        node.getProperties(propertyTypes, null);
        return node;
    }

    /**
     * Create nodes from nodRefs and populate it with properties and aspects
     *
     * @param nodeRefs
     * @param propsToLoad
     * @param callback
     * @return
     */
    @Override
    public <T> List<T> fetchObjects(Collection<NodeRef> nodeRefs, Set<QName> propsToLoad, NodeBasedObjectCallback<T> callback) {
        List<T> result = new ArrayList<T>();
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (NodeRef nodeRef : nodeRefs) {
            result.add(fetchObject(nodeRef, propsToLoad, callback, propertyTypes));
        }
        return result;
    }

    @Override
    public <T> T fetchObject(NodeRef nodeRef, Set<QName> propsToLoad, NodeBasedObjectCallback<T> callback, Map<Long, QName> propertyTypes) {
        final Node node = loadPropertiesAndAspects(nodeRef, propsToLoad, propertyTypes);
        T resultObject = callback.create(node);
        return resultObject;
    }

    /**
     * Create nodes from nodRefs and populate it with properties and aspects
     *
     * @param childAssocs
     * @param propsToLoad
     * @param callback
     * @return
     */
    @Override
    public <T> List<T> fetchObjectsForChildAssocs(Collection<ChildAssociationRef> childAssocs, Set<QName> propsToLoad,
            NodeBasedObjectCallback<T> callback) {
        List<T> result = new ArrayList<T>();
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (ChildAssociationRef childAssoc : childAssocs) {
            final Node node = loadPropertiesAndAspects(childAssoc.getChildRef(), propsToLoad, propertyTypes);
            result.add(callback.create(node));
        }
        return result;
    }

    private Node loadPropertiesAndAspects(NodeRef nodeRef, Set<QName> propsToLoad, Map<Long, QName> propertyTypes) {
        final Node node = new Node(nodeRef);
        node.getAspects();
        node.getProperties(propertyTypes, propsToLoad);
        return node;
    }

    @Override
    public WmNode fetchObjectNode(NodeRef objectRef, QName objectType) {
        return fetchObjectNode(objectRef, objectType, null);
    }

    @Override
    public WmNode fetchObjectNode(NodeRef objectRef, QName objectType, Map<Long, QName> propertyTypes) {
        QName type = nodeService.getType(objectRef);
        // XXX quickfix, a caseFile object can sometimes be used as a volume
        // FIXME we should actually find all calls with type volume to this method and rewrite them properly
        if (!(objectType.equals(VolumeModel.Types.VOLUME) && type.equals(CaseFileModel.Types.CASE_FILE))) {
            Assert.isTrue(objectType.equals(type), objectRef + " is typed as " + type + " but was requested as " + objectType);
        }
        Set<QName> aspects = RepoUtil.getAspectsIgnoringSystem(nodeService.getAspects(objectRef));
        Map<QName, Serializable> props = RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(objectRef, null, propertyTypes), dictionaryService);

        return new WmNode(objectRef, type, aspects, props);
    }

    @Override
    public WmNode createNewUnSaved(QName type, Map<QName, Serializable> props) {
        Set<QName> aspects = getDefaultAspects(type);
        props = addDefaultValues(type, aspects, props);
        return new WmNode(/* unsaved */RepoUtil.createNewUnsavedNodeRef(), type, aspects, props);
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
    public boolean setAspectsIgnoringSystem(Node node) {
        return setAspectsIgnoringSystem(node.getNodeRef(), node.getAspects());
    }

    @Override
    public boolean setAspectsIgnoringSystem(NodeRef nodeRef, Set<QName> nodeAspects) {
        boolean modified = false;
        Set<QName> repoAspects = RepoUtil.getAspectsIgnoringSystem(nodeService.getAspects(nodeRef));
        nodeAspects = RepoUtil.getAspectsIgnoringSystem(nodeAspects);

        List<QName> aspectsToRemove = new ArrayList<QName>(repoAspects);
        aspectsToRemove.removeAll(nodeAspects);
        for (QName aspect : aspectsToRemove) {
            nodeService.removeAspect(nodeRef, aspect);
            modified = true;
        }
        List<QName> aspectsToAdd = new ArrayList<QName>(nodeAspects);
        aspectsToAdd.removeAll(repoAspects);
        for (QName aspect : aspectsToAdd) {
            nodeService.addAspect(nodeRef, aspect, null);
            modified = true;
        }
        return modified;
    }

    @Override
    public ChildAssociationRef getLastChildAssocRef(String nodeRefXPath) {
        ChildAssociationRef ref = null;
        String[] xPathParts = StringUtils.split(nodeRefXPath, '/');
        for (String xPathPart : xPathParts) {
            QName qName = QName.resolveToQName(namespaceService, xPathPart);
            NodeRef parent = (ref == null ? nodeService.getRootNode(store) : ref.getChildRef());
            try {
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
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to get children of node " + parent + " by xPathPart '" + xPathPart
                        + "'(qName=" + qName + "), when searching for node with xPath '" + nodeRefXPath + "'" + ", qName=" + qName, e);
            }
        }
        return ref;
    }

    @Override
    public Map<QName, Serializable> setPropertiesIgnoringSystem(Map<QName, Serializable> properties, NodeRef nodeRef) {
        properties = getPropertiesIgnoringSys(properties);
        nodeService.addProperties(nodeRef, properties);
        return properties;
    }

    @Override
    public void setPropertiesIgnoringSystem(NodeRef nodeRef, Map<String, Object> nodeProps) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        for (Entry<String, Object> entry : nodeProps.entrySet()) {
            QName qname = QName.createQName(entry.getKey());
            addToPropsIfNotSystem(qname, (Serializable) entry.getValue(), props);
        }
        nodeService.addProperties(nodeRef, getPropertiesIgnoringSystem(nodeProps));
    }

    @Override
    public Map<QName, Serializable> getPropertiesIgnoringSystem(Map<String, Object> nodeProps) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        for (Entry<String, Object> entry : nodeProps.entrySet()) {
            QName qname = QName.createQName(entry.getKey());
            addToPropsIfNotSystem(qname, (Serializable) entry.getValue(), props);
        }
        return props;
    }

    @Override
    public Map<QName, Serializable> getPropertiesIgnoringSys(Map<QName, Serializable> nodeProps) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        for (Entry<QName, Serializable> entry : nodeProps.entrySet()) {
            addToPropsIfNotSystem(entry.getKey(), entry.getValue(), props);
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
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            Pair<String, String> result = getMimetypeAndEncoding(is, fileName, mimetype);
            writer.setMimetype(result.getFirst());
            writer.setEncoding(result.getSecond());
            writer.putContent(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Pair<String, String> getMimetypeAndEncoding(InputStream is, String fileName, String mimetype) {
        // Always ignore user-provided mime-type
        String oldMimetype = StringUtils.lowerCase(mimetype);
        mimetype = mimetypeService.guessMimetype(fileName);
        if (log.isDebugEnabled() && !StringUtils.equals(oldMimetype, mimetype)) {
            log.debug("User provided mimetype '" + oldMimetype + "', but we are guessing mimetype based on filename '"
                    + fileName + "' => '" + mimetype + "'");
        }

        String encoding;
        try {
            Charset charset = mimetypeService.getContentCharsetFinder().getCharset(is, mimetype);
            encoding = charset.name();
            if ("windows-1257".equals(encoding)) {
                encoding = StandardCharsets.UTF_8.name();
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Pair<String, String> result = Pair.newInstance(mimetype, encoding);
        return result;
    }

    @Override
    public NodeRef getParentNodeRefWithType(NodeRef childRef, QName... parentType) {
        final NodeRef parentRef = nodeService.getPrimaryParent(childRef).getParentRef();
        final QName realParentType = nodeService.getType(parentRef);
        if (ArrayUtils.contains(parentType, realParentType)) {
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
            @SuppressWarnings("unchecked")
            List<Object> array = (List<Object>) value;
            if (!array.isEmpty()) {
                final Integer valueIndex = (Integer) requestMap.get(VALUE_INDEX_IN_MULTIVALUED_PROPERTY);
                value = array.get(valueIndex);
            } else {
                value = null;
            }
        }
        return DefaultTypeConverter.INSTANCE.convert(requiredClasss, value);
    }

    @Override
    public long writeZipFileFromFiles(OutputStream output, List<NodeRef> fileRefs) {
        ZipArchiveOutputStream out = new ZipArchiveOutputStream(output);
        long fileSize;
        try {
            out.setLevel(Deflater.DEFAULT_COMPRESSION);
            out.setEncoding("Cp437");
            out.setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy.NOT_ENCODEABLE);
            byte[] buffer = new byte[10240];
            for (NodeRef fileRef : fileRefs) {
                FileInfo fileInfo = fileFolderService.getFileInfo(fileRef);
                ZipArchiveEntry entry = new ZipArchiveEntry(fileInfo.getName());
                entry.setSize(fileInfo.getContentData().getSize());
                out.putArchiveEntry(entry);
                InputStream in = fileFolderService.getReader(fileInfo.getNodeRef()).getContentInputStream();
                try {
                    int length = 0;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                } finally {
                    in.close();
                }
                out.closeArchiveEntry();
            }
            out.finish();
        } catch (IOException e) {
            log.warn("Failed to zip up files.", e);
            throw new RuntimeException("Failed to zip up files.", e);
        } finally {
            fileSize = out.getBytesWritten();
            IOUtils.closeQuietly(out);
        }

        return fileSize;
    }

    @Override
    public long writeZipFileFromStream(OutputStream output, String fileName, InputStream in) {
        Assert.hasLength(fileName, "No name provided for ZIP archive");

        long bytesWritten = -1;
        CountingOutputStream counter = null;
        ZipArchiveOutputStream out = null;
        try {
            counter = new CountingOutputStream(output);
            out = new ZipArchiveOutputStream(counter);

            out.setLevel(Deflater.DEFAULT_COMPRESSION);
            out.setEncoding("Cp437");
            out.setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy.NOT_ENCODEABLE);
            byte[] buffer = new byte[10240];
            ZipArchiveEntry entry = new ZipArchiveEntry(fileName);
            CountingInputStream countingStream = new CountingInputStream(in);
            out.putArchiveEntry(entry);

            try {
                int length = 0;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } finally {
                in.close();
            }

            entry.setSize(countingStream.getByteCount());
            out.closeArchiveEntry();

            out.finish();
            bytesWritten = counter.getByteCount();
        } catch (IOException e) {
            log.warn("Failed to zip up stream.", e);
            throw new RuntimeException("Failed to zip up stream.", e);
        } finally {
            IOUtils.closeQuietly(counter);
            IOUtils.closeQuietly(out);
        }

        return bytesWritten;
    }

    @Override
    public String getUniqueFileName(NodeRef folder, String fileName) {
        return getUniqueFileName(fileName, null, folder);
    }

    @Override
    public String getUniqueFileName(String fileName, List<NodeRef> filesToCheck, NodeRef... parentRefs) {
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName);
        if (StringUtils.isBlank(extension)) {
            extension = MimetypeMap.EXTENSION_BINARY;
        }
        String suffix = "";
        int i = 1;
        List<NodeRef> existingFilesWithName = getExistingFilesWithName(baseName + suffix + "." + extension, parentRefs);
        while ((filesToCheck == null && !existingFilesWithName.isEmpty()) || (filesToCheck != null && CollectionUtils.containsAny(existingFilesWithName, filesToCheck))) {
            suffix = " (" + i + ")";
            i++;
            existingFilesWithName = getExistingFilesWithName(baseName + suffix + "." + extension, parentRefs);
        }
        return baseName + suffix + "." + extension;

    }

    private List<NodeRef> getExistingFilesWithName(String filenameToCheck, NodeRef... parentRefs) {
        List<NodeRef> fileRefs = new ArrayList<NodeRef>();
        for (NodeRef parentRef : parentRefs) {
            if (!nodeService.exists(parentRef)) {
                continue;
            }
            NodeRef fileRef = fileFolderService.searchSimple(parentRef, filenameToCheck);
            if (fileRef != null) {
                fileRefs.add(fileRef);
            }
        }
        return fileRefs;
    }

    @Override
    // TODO: current implementation doesn't worry about situation where more than one transaction tries to update documents count on same object
    // (but maybe alfresco prevents it?)
    public void updateParentContainingDocsCount(final NodeRef parentNodeRef, final QName propertyName, Boolean added, Integer count) {
        if (parentNodeRef == null) {
            return;
        }

        Serializable valueProperty = nodeService.getProperty(parentNodeRef, propertyName);
        if (valueProperty == null) { // first time, assign default value
            valueProperty = 0;
        }

        int value = Integer.parseInt(valueProperty.toString());
        count = (count == null) ? 1 : count;
        int newValue;
        if (added != null) {
            newValue = (added) ? value + count : value - count;
        } else {
            newValue = value + count;
        }
        if (newValue < 0) {
            newValue = 0;
        }
        final int finalNewValue = newValue;

        // Update property with elevated rights
        AuthenticationUtil.runAs(new RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                nodeService.setProperty(parentNodeRef, propertyName, finalNewValue);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    public void deleteNodeRefs(Collection<NodeRef> nodeRefs, boolean moveToTrashCan) {
        for (NodeRef nodeRef : nodeRefs) {
            try {
                if (nodeService.exists(nodeRef)) {
                    if (!moveToTrashCan) {
                        nodeService.addAspect(nodeRef, DocumentCommonModel.Aspects.DELETE_PERMANENT, null);
                    }
                    nodeService.deleteNode(nodeRef);
                }
            } catch (Exception e) {
                log.error("Error deleting " + nodeRef + " : " + e.getMessage(), e);
                // throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
            }
        }
    }

    @Override
    public <T> T runSemaphored(AdjustableSemaphore semaphore, ExecuteCallback<T> semaphoredCallback) {
        boolean semaphoreAcquired = false;
        try {
            long start = System.nanoTime();

            semaphore.acquire();
            semaphoreAcquired = true;

            long stop = System.nanoTime();
            long duration = CalendarUtil.duration(start, stop);
            if (duration > 20) {
                log.info("Semaphore wait " + duration + " ms, " + semaphore.getMaxPermits() + " max permits allowed");
            }

            log.debug("Semaphore aquired.");
            return semaphoredCallback.execute();
        } catch (InterruptedException e) {
            // TODO: is this correct? Should never happen under normal conditions
            return null;
        } finally {
            if (semaphoreAcquired) {
                semaphore.release();
                log.debug("Semaphore released.");
            }
        }
    }

    @Override
    public void runOnBackground(final RunAsWork<Void> work, final String threadNamePrefix, final boolean createTransaction) {
        runOnBackground(work, threadNamePrefix, createTransaction, null);
    }

    @Override
    public void runOnBackground(final RunAsWork<Void> work, final String threadNamePrefix, final boolean createTransaction, final RunAsWork<Void> workAfterCommit) {
        Assert.notNull(threadNamePrefix, "threadName");
        final String threadName = threadNamePrefix + "-" + backgroundThreadCounter.getAndIncrement();
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        log.info("Started new background thread: " + Thread.currentThread().getName());
                        long startTime = System.nanoTime();
                        try {
                            Pair<Long, Long> workTime;
                            if (createTransaction) {
                                RetryingTransactionHelper txHelper = transactionService.getRetryingTransactionHelper();
                                workTime = txHelper.doInTransaction(new RetryingTransactionCallback<Pair<Long, Long>>() {
                                    @Override
                                    public Pair<Long, Long> execute() throws Throwable {
                                        log.info("Started new transaction in background thread: " + Thread.currentThread().getName());
                                        long start = System.nanoTime();
                                        AuthenticationUtil.runAs(work, AuthenticationUtil.getSystemUserName());
                                        long stop = System.nanoTime();
                                        return new Pair<Long, Long>(start, stop);
                                    }
                                }, false, true);
                            } else {
                                long start = System.nanoTime();
                                AuthenticationUtil.runAs(work, AuthenticationUtil.getSystemUserName());
                                long stop = System.nanoTime();
                                workTime = new Pair<Long, Long>(start, stop);
                            }
                            long stopTime = System.nanoTime();
                            log.info("Finished " + (createTransaction ? "transaction and " : "") + "background thread: " + Thread.currentThread().getName() + " total time = "
                                    + CalendarUtil.duration(startTime, stopTime)
                                    + "ms, last work time = " + CalendarUtil.duration(workTime.getFirst(), workTime.getSecond())
                                    + " ms" + (createTransaction ? ", last transaction commit time = " + CalendarUtil.duration(workTime.getSecond(), stopTime) + " ms" : ""));
                        } catch (Exception e) {
                            long stopTime = System.nanoTime();
                            log.error("Exception in background thread: " + Thread.currentThread().getName() + " total time = " + CalendarUtil.duration(startTime, stopTime) + "ms",
                                    e);
                        }
                    }

                };
                log.info("Creating and starting a new background thread: " + threadName);
                Thread thread = new Thread(runnable, threadName);
                thread.start();

                if (workAfterCommit != null) {
                    try {
                        workAfterCommit.doWork();
                    } catch (Exception e) {
                        log.error("Exception in work after commit!", e);
                    }
                }
            }
        });
    }

    @Override
    public void runBeforeCommit(final RunAsWork<Void> work) {
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {

            @Override
            public void beforeCommit(boolean readOnly) {
                long start = System.currentTimeMillis();
                AuthenticationUtil.runAs(work, AuthenticationUtil.getSystemUserName());
                log.info("Run work before commit, total time: " + (System.currentTimeMillis() - start) + "ms");
            }

        });
    }

    @Override
    public void refreshMaterializedViews(QName... nodeTypeQName) {
        if (AlfrescoTransactionSupport.getResource(REFRESH_MATERIALIZED_VIEWS) == null) {
            final Set<QName> refreshMaterializedViews = new HashSet<QName>();
            refreshMaterializedViews.addAll(Arrays.asList(nodeTypeQName));
            AlfrescoTransactionSupport.bindResource(REFRESH_MATERIALIZED_VIEWS, refreshMaterializedViews);
            runOnBackground(new RunAsWork<Void>() {

                @Override
                public Void doWork() throws Exception {
                    for (QName viewToUpdate : refreshMaterializedViews) {
                        if (FunctionsModel.Types.FUNCTION.equals(viewToUpdate)) {
                            updateMaterializedView("delta_function");
                        } else if (SeriesModel.Types.SERIES.equals(viewToUpdate)) {
                            updateMaterializedView("delta_series");
                        } else if (VolumeModel.Types.VOLUME.equals(viewToUpdate) || CaseFileModel.Types.CASE_FILE.equals(viewToUpdate)) {
                            updateMaterializedView("delta_volume_casefile");
                        } else if (CaseModel.Types.CASE.equals(viewToUpdate)) {
                            updateMaterializedView("delta_case");
                        }
                    }
                    return null;
                }

            }, "refreshMaterializedViews", true, new RunAsWork<Void>() {

                @Override
                public Void doWork() throws Exception {
                    AlfrescoTransactionSupport.unbindResource(REFRESH_MATERIALIZED_VIEWS);
                    return null;
                }
            });
        } else {
            Set<QName> refreshMaterializedViews = AlfrescoTransactionSupport.getResource(REFRESH_MATERIALIZED_VIEWS);
            refreshMaterializedViews.addAll(Arrays.asList(nodeTypeQName));
        }
    }

    @SuppressWarnings("deprecation")
    private void updateMaterializedView(String viewName) {
        jdbcTemplate.update("REFRESH MATERIALIZED VIEW " + viewName);
    }

    @Override
    public NodeRef getExistingNodeRefAllStores(String id) {
        if (id == null) {
            return null;
        }
        for (StoreRef storeRef : getAllWithArchivalsStoreRefs()) {
            NodeRef nodeRef = new NodeRef(storeRef, id);
            if (nodeService.exists(nodeRef)) {
                return nodeRef;
            }
        }
        return null;
    }

    @Override
    public String getTsquery(String input) {
        return getTsquery(input, 3);
    }

    @Override
    public String getTsquery(String input, int minLexemLength) {
        if (input == null) {
            input = "";
        }
        // TODO: Remove postgresql object
        //PGobject res = jdbcTemplate.queryForObject("SELECT plainto_tsquery('simple', ?)", PGobject.class, input);
        //String originalTsquery = res.getValue();

        String originalTsquery = jdbcTemplate.queryForObject("SELECT plainto_tsquery('simple', ?)", String.class, input);
        List<String> lexems = new ArrayList<String>();
        Pattern pattern = Pattern.compile("'[^']+'");
        Matcher matcher = pattern.matcher(originalTsquery);
        while (matcher.find()) {
            String lexemValue = matcher.group().substring(1, matcher.group().length() - 1);
            if (lexemValue.length() < minLexemLength) {
                continue;
            }
            String lexem = "'" + lexemValue + "':*";
            if (!lexems.contains(lexem)) {
                lexems.add(lexem);
            }
        }
        String tsquery = StringUtils.join(lexems, " & ");
        if (log.isDebugEnabled()) {
            log.debug("Parsed:\n  input[" + input.length() + " chars]=" + input + "\n  original tsquery=" + originalTsquery + "\n  new tsquery[" + lexems.size() + " lexems]="
                    + tsquery);
        }
        return tsquery;
    }

    @Override
    public void explainQuery(String sqlQuery, Log traceLog, Object... args) {
        explainAnalyzeQuery(sqlQuery, traceLog, false, args);
    }

    @Override
    public void explainAnalyzeQuery(String sqlQuery, Log traceLog, boolean analyze, Object... args) {
        if (traceLog.isTraceEnabled()) {
            jdbcTemplate.getJdbcOperations().execute("SET enable_seqscan TO off");
            List<String> explanation = jdbcTemplate.query("EXPLAIN " + (analyze ? "ANALYZE " : "") + sqlQuery, new ParameterizedRowMapper<String>() {

                @Override
                public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getString(1);
                }
            }, args);
            StringBuffer sb = new StringBuffer("Explaining database query, slq='" + sqlQuery + "', args=\n");
            if (args != null && args.length > 0) {
                int argsCounter = 1;
                for (Object arg : args) {
                    sb.append(argsCounter++ + ") ").append(arg != null ? arg.toString() : arg).append("\n");
                }
            } else {
                sb.append("[]").append("\n");
            }
            sb.append(TextUtil.joinNonBlankStrings(explanation, "\n"));
            traceLog.trace(sb.toString());
        }
    }

    @Override
    public NodeRef deleteBootstrapNodeRef(String moduleName, String bootstrapName) {
        String bootstrap = getBootstrapXPath(moduleName, bootstrapName);
        StoreRef bootstrapStore = new StoreRef("system://system");
        NodeRef bootstrapRef = getNodeRef(bootstrap, bootstrapStore);
        if (bootstrapRef != null) {
            nodeService.deleteNode(bootstrapRef);
        }
        return bootstrapRef;
    }

    private String getBootstrapXPath(String moduleName, String bootstrapName) {
        return "/sys:system-registry/module:modules/module:" + moduleName + "/module:components/module:" + bootstrapName;
    }

    @Override
    public void setModifiedToNow(final NodeRef nodeRef) {
        // Change document modified time, so it would be detected on next ADR sync
        AuthenticationUtil.runAs(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                nodeService.setProperty(nodeRef, ContentModel.PROP_MODIFIED, new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    // START: getters / setters
    public void setDefaultStore(String store) {
        this.store = new StoreRef(store);
    }

    public void setArchivalsStore(String archivalsStore) {
        this.archivalsStore = new StoreRef(archivalsStore);
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

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // END: getters / setters

}
