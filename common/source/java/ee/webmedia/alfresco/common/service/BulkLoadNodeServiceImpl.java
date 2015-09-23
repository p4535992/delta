package ee.webmedia.alfresco.common.service;

import static ee.webmedia.alfresco.common.search.DbSearchUtil.getDbFieldNameFromCamelCase;
import static ee.webmedia.alfresco.common.search.DbSearchUtil.getQuestionMarks;
import static ee.webmedia.alfresco.common.service.ApplicationConstantsBean.SORT_ALLOWED_LIMIT;
import static ee.webmedia.alfresco.utils.WebUtil.exceedsLimit;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.LocaleDAO;
import org.alfresco.repo.domain.NodePropertyValue;
import org.alfresco.repo.domain.PropertyMapKey;
import org.alfresco.repo.domain.QNameDAO;
import org.alfresco.repo.domain.contentdata.ContentDataDAO;
import org.alfresco.repo.node.db.hibernate.HibernateNodeDaoServiceImpl;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.util.SerializationHelper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.model.SimpleFile;
import ee.webmedia.alfresco.document.file.model.SimpleFileWithOrder;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.model.FakeDocument;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.model.SubstituteModel;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class BulkLoadNodeServiceImpl implements BulkLoadNodeService {

    private static final String DOUBLE_VALUE_COL = "double_value";

    private static final String FLOAT_VALUE_COL = "float_value";

    private static final String LONG_VALUE_COL = "long_value";

    private static final String BOOLEAN_VALUE_COL = "boolean_value";

    private static final String STRING_VALUE_COL = "string_value";

    private static final String NODE_REF_QUERY_START = "select node.uuid, node.store_id ";
    private static final String NODE_NOT_DELETED_CONDITION = " alf_node.node_deleted = false";
    private static final Set<QName> SIMPLE_FILE_PROPS = new HashSet<>(Arrays.asList(FileModel.Props.DISPLAY_NAME, FileModel.Props.ACTIVE, DvkModel.Props.DVK_ID,
            ContentModel.PROP_NAME));
    private static final Set<QName> SIMPLE_FILE_PROPS_WITH_FILE_ORDER;
    private static final Map<String, String> PROP_TABLE_ALIASES = new ConcurrentHashMap<>();
    private static final Map<QName, Long> QNAME_TO_DB_ID = new ConcurrentHashMap<>();
    private static final Map<Long, QName> DB_ID_TO_QNAME = new ConcurrentHashMap<>();
    private static final Map<StoreRef, Long> STORE_REF_TO_DB_ID = new ConcurrentHashMap<>();
    private static final Map<Long, StoreRef> DB_ID_TO_STORE_REF = new ConcurrentHashMap<>();

    private static final Log LOG = LogFactory.getLog(BulkLoadNodeServiceImpl.class);

    static {
        Set<QName> tempProps = new HashSet<>(SIMPLE_FILE_PROPS);
        tempProps.add(FileModel.Props.FILE_ORDER_IN_LIST);
        tempProps.add(FileModel.Props.GENERATION_TYPE);
        tempProps.add(FileModel.Props.GENERATED_FROM_TEMPLATE);
        SIMPLE_FILE_PROPS_WITH_FILE_ORDER = tempProps;
    }

    private SimpleJdbcTemplate jdbcTemplate;
    private QNameDAO qnameDAO;
    private ContentDataDAO contentDataDAO;
    private LocaleDAO localeDAO;
    private DictionaryService dictionaryService;
    private WorkflowService _workflowService;

    @Override
    public Map<NodeRef, Document> loadDocuments(List<NodeRef> documentsToLoad, final Set<QName> propsToLoad) {
        List<List<NodeRef>> slicedDocRefs = RepoUtil.sliceList(documentsToLoad, SORT_ALLOWED_LIMIT);
        Map<NodeRef, Document> result = new HashMap<NodeRef, Document>();
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (List<NodeRef> docRefsSlice : slicedDocRefs) {
            result.putAll(loadNodes(docRefsSlice, propsToLoad, false, createDocumentCallback(), false, propertyTypes));
        }
        return result;
    }

    @Override
    public Map<NodeRef, Document> loadDocumentsLimitingProperties(List<NodeRef> documentsToLoad, final Set<QName> propsNotToLoad) {
        return loadNodes(documentsToLoad, propsNotToLoad, true, createDocumentCallback(), false, null);
    }

    @Override
    public Map<NodeRef, Document> loadDocumentsOrCases(List<NodeRef> documentsToLoad, final Set<QName> propsToLoad) {
        CreateNodeCallback<Document> createNodeCallback = new CreateNodeCallback<Document>() {

            @Override
            public Document create(NodeRef nodeRef, Map<String, Object> properties, QName typeQname) {
                if (!dictionaryService.isSubClass(typeQname, DocumentCommonModel.Types.DOCUMENT)) {
                    return new FakeDocument(nodeRef, properties);
                }
                return new Document(nodeRef, properties);
            }
        };
        return loadNodes(documentsToLoad, propsToLoad, false, createNodeCallback, true, null);
    }

    @Override
    public List<SimpleFile> loadActiveFiles(NodeRef nodeRef, Map<Long, QName> propertyTypes) {
        return loadActiveFiles(nodeRef, propertyTypes, SIMPLE_FILE_PROPS, getSimpleFileCallback());
    }

    private CreateSimpleFileCallback<SimpleFile> getSimpleFileCallback() {
        return new CreateSimpleFileCallback<SimpleFile>() {

            @Override
            public SimpleFile create(Map<QName, Serializable> fileProps, Serializable... objects) {

                String displayName = (String) fileProps.get(FileModel.Props.DISPLAY_NAME);
                if (StringUtils.isBlank(displayName)) {
                    displayName = (String) fileProps.get(ContentModel.PROP_NAME);
                }
                NodeRef fileRef = (NodeRef) fileProps.get(ContentModel.PROP_NODE_REF);
                String readOnlyUrl = DownloadContentServlet.generateDownloadURL(fileRef, displayName);
                SimpleFile simpleFile = new SimpleFile(displayName, readOnlyUrl);
                return simpleFile;
            }
        };
    }

    private CreateSimpleFileCallback<SimpleFileWithOrder> getSimpleFileWithOrderCallback() {
        return new CreateSimpleFileCallback<SimpleFileWithOrder>() {

            @Override
            public SimpleFileWithOrder create(Map<QName, Serializable> fileProps, Serializable... objects) {
                String displayName = (String) fileProps.get(FileModel.Props.DISPLAY_NAME);
                if (StringUtils.isBlank(displayName)) {
                    displayName = (String) fileProps.get(ContentModel.PROP_NAME);
                }
                Long fileOrderInList = (Long) fileProps.get(FileModel.Props.FILE_ORDER_IN_LIST);
                NodeRef fileRef = (NodeRef) fileProps.get(ContentModel.PROP_NODE_REF);
                String readOnlyUrl = DownloadContentServlet.generateDownloadURL(fileRef, displayName);
                boolean generated = fileProps.get(FileModel.Props.GENERATED_FROM_TEMPLATE) != null || fileProps.get(FileModel.Props.GENERATION_TYPE) != null;
                SimpleFileWithOrder simpleFile = new SimpleFileWithOrder(displayName, readOnlyUrl, fileOrderInList, fileRef, generated);
                boolean active = Boolean.TRUE.equals(fileProps.get(FileModel.Props.ACTIVE));
                simpleFile.setActive(active);
                return simpleFile;
            }
        };
    }

    @Override
    public Map<NodeRef, List<SimpleFile>> loadActiveFiles(List<NodeRef> parentNodeRefs, Map<Long, QName> propertyTypes) {
        return loadFiles(parentNodeRefs, propertyTypes, SIMPLE_FILE_PROPS, getSimpleFileCallback(), FilesLoadStrategy.ACTIVE);
    }

    @Override
    public <T extends SimpleFile> List<T> loadActiveFiles(NodeRef nodeRef, Map<Long, QName> propertyTypes, Set<QName> propsToLoad, CreateSimpleFileCallback<T> createFileCallback) {
        Map<NodeRef, List<T>> allDocumentsFiles = loadFiles(Arrays.asList(nodeRef), propertyTypes, propsToLoad, createFileCallback, FilesLoadStrategy.ACTIVE);
        return allDocumentsFiles.get(nodeRef);
    }

    @Override
    public List<SimpleFileWithOrder> loadInactiveFilesWithOrder(NodeRef parentRef) {
        return loadFilesWithOrder(parentRef, FilesLoadStrategy.INACTIVE);
    }

    @Override
    public List<SimpleFileWithOrder> loadActiveFilesWithOrder(NodeRef parentRef) {
        return loadFilesWithOrder(parentRef, FilesLoadStrategy.ACTIVE);
    }

    private List<SimpleFileWithOrder> loadFilesWithOrder(NodeRef parentRef, FilesLoadStrategy strategy) {
        Map<NodeRef, List<SimpleFileWithOrder>> allDocumentsFiles = loadFiles(Arrays.asList(parentRef), null, SIMPLE_FILE_PROPS_WITH_FILE_ORDER,
                getSimpleFileWithOrderCallback(), strategy);
        return allDocumentsFiles.get(parentRef);
    }

    @Override
    public List<SimpleFile> loadAllFiles(NodeRef parentRef) {
        Map<NodeRef, List<SimpleFile>> files = loadFiles(Arrays.asList(parentRef), null, SIMPLE_FILE_PROPS, getSimpleFileCallback(), FilesLoadStrategy.BOTH);
        return files.get(parentRef);
    }

    private enum FilesLoadStrategy {
        ACTIVE, INACTIVE, BOTH;
    }

    private <T extends SimpleFile> Map<NodeRef, List<T>> loadFiles(List<NodeRef> parentNodeRefs, Map<Long, QName> propertyTypes, Set<QName> propsToLoad,
            CreateSimpleFileCallback<T> createFileCallback, FilesLoadStrategy strategy) {
        Map<NodeRef, List<Map<QName, Serializable>>> fileNodesProps = loadChildNodes(parentNodeRefs, propsToLoad, ContentModel.TYPE_CONTENT,
                propertyTypes, new CreateObjectCallback<Map<QName, Serializable>>() {

            @Override
            public Map<QName, Serializable> create(NodeRef fileRef, Map<QName, Serializable> properties) {
                properties.put(ContentModel.PROP_NODE_REF, fileRef);
                return properties;
            }
        });
        Map<NodeRef, List<T>> allDocumentsFiles = new HashMap<>();
        boolean activeFilesOnly = FilesLoadStrategy.ACTIVE.equals(strategy);
        boolean inactiveFilesOnly = FilesLoadStrategy.INACTIVE.equals(strategy);
        for (NodeRef parentRef : parentNodeRefs) {
            List<T> files = new ArrayList<>();
            List<Map<QName, Serializable>> fileNodeProps = fileNodesProps.get(parentRef);
            if (fileNodeProps != null) {
                for (Map<QName, Serializable> fileProps : fileNodeProps) {
                    Serializable activePropValue = fileProps.get(FileModel.Props.ACTIVE);
                    boolean active = (activePropValue == null) ? true : Boolean.parseBoolean(activePropValue.toString());
                    if (activeFilesOnly && !active || fileProps.containsKey(DvkModel.Props.DVK_ID)) {
                        continue;
                    } else if (inactiveFilesOnly && active) {
                        continue;
                    }
                    files.add(createFileCallback.create(fileProps));
                }
            }
            allDocumentsFiles.put(parentRef, files);
        }
        return allDocumentsFiles;
    }

    @Override
    public int countFiles(NodeRef parentNodeRef, Boolean active) {
        List<Map<QName, Serializable>> fileNodesProps = loadChildNodes(Collections.singleton(parentNodeRef), Collections.singleton(FileModel.Props.ACTIVE),
                ContentModel.TYPE_CONTENT, null, new CreateObjectCallback<Map<QName, Serializable>>() {

            @Override
            public Map<QName, Serializable> create(NodeRef fileRef, Map<QName, Serializable> properties) {
                return properties;
            }
        }).get(parentNodeRef);

        int count = 0;
        if (CollectionUtils.isNotEmpty(fileNodesProps)) {
            for (Map<QName, Serializable> props : fileNodesProps) {
                if (active.equals(props.get(FileModel.Props.ACTIVE))) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int countChildNodes(NodeRef parentRef, QName childNodeType) {
        Map<NodeRef, Integer> counts = countChildNodes(Collections.singletonList(parentRef), childNodeType);
        return (counts.containsKey(parentRef) ? counts.get(parentRef) : 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<NodeRef, Integer> countChildNodes(List<NodeRef> parentRefs, QName childNodeType) {
        if (parentRefs == null || parentRefs.isEmpty()) {
            return new HashMap<>();
        }
        List<Object> arguments = new ArrayList<>();
        String sql = "SELECT node.store_id, node.uuid, count(1) as child_count "
                + " FROM " + getNodeTableConditionalJoin(parentRefs, arguments)
                + " JOIN alf_child_assoc child_assoc on child_assoc.parent_node_id = node.id "
                + " JOIN alf_node child on child_assoc.child_node_id = child.id ";

        if (childNodeType != null) {
            sql += " WHERE child.type_qname_id = " + getQNameDbId(childNodeType);
        }
        sql += " GROUP BY node.store_id, node.uuid";

        final Map<NodeRef, Integer> counts = new HashMap<>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                counts.put(getNodeRef(rs), rs.getInt("child_count"));
                return null;
            }

        }, arguments.toArray());
        return counts;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<NodeRef, Integer> getSearchableTargetAssocsCount(List<NodeRef> sourceRefs, QName assocType) {
        if (sourceRefs == null || sourceRefs.isEmpty()) {
            return new HashMap<>();
        }
        List<Object> arguments = new ArrayList<>();
        String sql = "SELECT node.store_id, node.uuid, count(1) as source_count "
                + " FROM " + getNodeTableConditionalJoin(sourceRefs, arguments)
                + " JOIN alf_node_assoc node_assoc on node_assoc.target_node_id = node.id "
                + " JOIN alf_node source on node_assoc.source_node_id = source.id "
                + " JOIN alf_node_aspects aspects on source.id = aspects.node_id and aspects.qname_id = " + getQNameDbId(DocumentCommonModel.Aspects.SEARCHABLE);

        if (assocType != null) {
            sql += " WHERE node_assoc.type_qname_id = " + getQNameDbId(assocType);
        }
        sql += " GROUP BY node.store_id, node.uuid";

        final Map<NodeRef, Integer> counts = new HashMap<>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                counts.put(getNodeRef(rs), rs.getInt("source_count"));
                return null;
            }

        }, arguments.toArray());
        return counts;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasChildNodeOfType(NodeRef parentRef, QName childNodeType) {
        List<Object> arguments = new ArrayList<>();
        String sql = "SELECT EXISTS (SELECT 1 "
                + " FROM " + getNodeTableConditionalJoin(Arrays.asList(parentRef), arguments)
                + " JOIN alf_child_assoc child_assoc on child_assoc.parent_node_id = node.id "
                + " JOIN alf_node child on child_assoc.child_node_id = child.id "
                + " WHERE child.type_qname_id = " + getQNameDbId(childNodeType) + ")";

        return jdbcTemplate.queryForObject(sql, Boolean.class, arguments.toArray());
    }

    @Override
    public Map<NodeRef, Node> loadNodes(Collection<NodeRef> nodesToLoad, final Set<QName> propsToLoad) {
        return loadNodes(nodesToLoad, propsToLoad, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<Substitute> loadUserSubstitutionDuties(String personName, NodeRef peopleContainer) {
        String sql =
                "WITH all_substitutes AS ("
                        + "SELECT people.qname_localname AS replaced_person_name, props.*, alf_node.store_id, alf_node.uuid AS node_ref_uuid "
                        + " FROM alf_child_assoc people "
                        + " JOIN alf_child_assoc substitutes_container ON substitutes_container.parent_node_id = people.child_node_id"
                        + " JOIN alf_child_assoc substitutes ON substitutes.parent_node_id = substitutes_container.child_node_id"
                        + " JOIN alf_node_properties props ON props.node_id = substitutes.child_node_id"
                        + " JOIN alf_node ON substitutes.child_node_id = alf_node.id"
                        + " WHERE people.parent_node_id = "
                        + "  (SELECT id FROM alf_node"
                        + "  WHERE store_id = " + getStoreRefDbId(peopleContainer.getStoreRef()) + " AND uuid = '" + peopleContainer.getId() + "')"
                        + " AND substitutes_container.qname_localname = 'substitutes'"
                        + " AND substitutes.qname_localname = 'substitute'"
                        + " )"
                        + " SELECT * FROM all_substitutes "
                        + " WHERE node_ref_uuid IN ( "
                        + "  SELECT node_ref_uuid FROM all_substitutes WHERE string_value = ? "
                        + " )";

        final Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> allProps = new HashMap<NodeRef, Map<PropertyMapKey, NodePropertyValue>>();
        final Map<NodeRef, String> replacedPersonNames = new HashMap<>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                NodeRef ref = getNodeRef(rs, "node_ref_uuid");
                Map<PropertyMapKey, NodePropertyValue> props = allProps.get(ref);
                if (props == null) {
                    String replacedPerson = rs.getString("replaced_person_name");
                    replacedPersonNames.put(ref, replacedPerson);
                    props = new HashMap<>();
                    allProps.put(ref, props);
                }

                PropertyMapKey key = getPropMapKey(rs);
                NodePropertyValue value = getPropertyValue(rs);
                props.put(key, value);

                return null;
            }

        }, personName);

        List<Substitute> result = new ArrayList<>();
        explainQuery(sql, personName);

        CreateObjectCallback<Substitute> createSubstituteCallback = new CreateObjectCallback<Substitute>() {

            @Override
            public Substitute create(NodeRef nodeRef, Map<QName, Serializable> properties) {
                Substitute s = new Substitute();
                s.setSubstitutionStartDate((Date) properties.get(SubstituteModel.Props.SUBSTITUTION_START_DATE));
                s.setSubstitutionEndDate((Date) properties.get(SubstituteModel.Props.SUBSTITUTION_END_DATE));
                s.setSubstituteId((String) properties.get(SubstituteModel.Props.SUBSTITUTE_ID));
                s.setSubstituteName((String) properties.get(SubstituteModel.Props.SUBSTITUTE_NAME));
                s.setNodeRef(nodeRef);
                s.setReplacedPersonUserName(replacedPersonNames.get(nodeRef));
                return s;
            }
        };

        for (Map.Entry<NodeRef, Map<PropertyMapKey, NodePropertyValue>> entry : allProps.entrySet()) {
            NodeRef nodeRef = entry.getKey();
            Map<QName, Serializable> properties = HibernateNodeDaoServiceImpl.convertToPublicProperties(entry.getValue(), qnameDAO, localeDAO, contentDataDAO, dictionaryService);
            result.add(createSubstituteCallback.create(nodeRef, properties));
        }
        return result;
    }

    @Override
    public Map<NodeRef, Node> loadNodes(Collection<NodeRef> nodesToLoad, final Set<QName> propsToLoad, Map<Long, QName> propertyTypes) {
        CreateNodeCallback<Node> createNodeCallback = new CreateNodeCallback<Node>() {
            @Override
            public Node create(NodeRef nodeRef, Map<String, Object> properties, QName typeQname) {
                return new WmNode(nodeRef, typeQname, properties, null);
            }
        };
        return loadNodes(nodesToLoad, propsToLoad, false, createNodeCallback, true, propertyTypes);
    }

    private CreateNodeCallback<Document> createDocumentCallback() {
        return new CreateNodeCallback<Document>() {
            @Override
            public Document create(NodeRef nodeRef, Map<String, Object> properties, QName typeQname) {
                return new Document(nodeRef, properties);
            }
        };
    }

    @Override
    public Map<NodeRef, Map<NodeRef, Map<QName, Serializable>>> loadChildNodes(Collection<NodeRef> parentNodes, Set<QName> propsToLoad) {
        long startTime = System.nanoTime();
        List<Object> arguments = new ArrayList<>();
        String sql = getChildNodeQuery(parentNodes, propsToLoad, null, arguments);

        final Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> allPropsMap = executeChildNodesQuery(arguments, sql);

        int childCount = 0;
        Map<NodeRef, Map<NodeRef, Map<QName, Serializable>>> result = new HashMap<NodeRef, Map<NodeRef, Map<QName, Serializable>>>();
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (Map.Entry<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> entry : allPropsMap.entrySet()) {
            NodeRef parentRef = entry.getKey();
            Map<NodeRef, Map<QName, Serializable>> childMap = new HashMap<NodeRef, Map<QName, Serializable>>();
            result.put(parentRef, childMap);
            for (Map.Entry<NodeRef, Map<PropertyMapKey, NodePropertyValue>> innerEntry : entry.getValue().entrySet()) {
                childMap.put(innerEntry.getKey(), HibernateNodeDaoServiceImpl.convertToPublicProperties(innerEntry.getValue(), qnameDAO, localeDAO, contentDataDAO,
                        dictionaryService, null, propertyTypes));
            }
            childCount += childMap.size();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("BulkLoadService.loadChildNodes total time " + CalendarUtil.duration(startTime, System.nanoTime()) + "ms, loaded " + childCount + " childNodes");
        }
        return result;
    }

    @Override
    public <T> List<T> loadChildNodes(NodeRef parentNodeRef, Set<QName> propsToLoad, QName childNodeType, Map<Long, QName> propertyTypes,
            CreateObjectCallback<T> createObjectCallback) {
        Map<NodeRef, List<T>> childNodes = loadChildNodes(Arrays.asList(parentNodeRef), propsToLoad, childNodeType, propertyTypes, createObjectCallback);

        if (childNodes.containsKey(parentNodeRef)) {
            return childNodes.get(parentNodeRef);
        }

        return Collections.emptyList();
    }

    @Override
    public <T> Map<NodeRef, List<T>> loadChildNodes(Collection<NodeRef> parentNodes, Set<QName> propsToLoad, QName childNodeType, Map<Long, QName> propertyTypes,
            CreateObjectCallback<T> createObjectCallback) {
        long startTime = System.nanoTime();
        List<Object> arguments = new ArrayList<>();
        String sql = getChildNodeQuery(parentNodes, propsToLoad, childNodeType, arguments);

        final Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> allPropsMap = executeChildNodesQuery(arguments, sql);
        Map<NodeRef, List<T>> result = new HashMap<>();
        int childCount = 0;
        if (propertyTypes == null) {
            propertyTypes = new HashMap<Long, QName>();
        }
        for (Map.Entry<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> entry : allPropsMap.entrySet()) {
            NodeRef parentRef = entry.getKey();
            List<T> objectList = new ArrayList<>();
            for (Map.Entry<NodeRef, Map<PropertyMapKey, NodePropertyValue>> innerEntry : entry.getValue().entrySet()) {
                Map<QName, Serializable> properties = HibernateNodeDaoServiceImpl.convertToPublicProperties(innerEntry.getValue(), qnameDAO, localeDAO, contentDataDAO,
                        dictionaryService, null, propertyTypes);
                objectList.add(createObjectCallback.create(innerEntry.getKey(), properties));
            }
            childCount += objectList.size();
            result.put(parentRef, objectList);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("BulkLoadService.loadChildNodes total time " + CalendarUtil.duration(startTime, System.nanoTime()) + "ms, loaded " + childCount + " childNodes");
        }

        return result;
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<Node> loadAssociatedTargetNodes(NodeRef sourceRef, Set<QName> propsToLoad, QName assocType, CreateObjectCallback<Node> createNodeCallback) {
        List<Object> arguments = new ArrayList<>();
        boolean filterProps = CollectionUtils.isNotEmpty(propsToLoad);
        String sql = "SELECT target.id, target.uuid, target.store_id, props.* FROM " + getNodeTableConditionalJoin(Collections.singleton(sourceRef), arguments, "id")
                + " JOIN alf_node_assoc assoc ON node.id = assoc.source_node_id"
                + " JOIN alf_node_properties props ON props.node_id = assoc.target_node_id"
                + " JOIN alf_node target ON target.id = assoc.target_node_id"
                + " WHERE assoc.type_qname_id = " + getQNameDbId(assocType)
                + (filterProps ? " AND props.qname_id in (" + createQnameIdListing(propsToLoad, false) + " )" : "");
        Object[] args = arguments.toArray();
        final Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> nodeProps = new HashMap<>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                NodeRef nodeRef = getNodeRef(rs);
                Map<PropertyMapKey, NodePropertyValue> props = nodeProps.get(nodeRef);
                if (props == null) {
                    props = new HashMap<>();
                    nodeProps.put(nodeRef, props);
                }
                PropertyMapKey propMapKey = getPropMapKey(rs);
                if (propMapKey.getQnameId() > 0) {
                    NodePropertyValue nodePropValue = getPropertyValue(rs);
                    props.put(propMapKey, nodePropValue);
                }
                return null;
            }

        }, args);

        List<Node> result = new ArrayList<>();
        for (Map.Entry<NodeRef, Map<PropertyMapKey, NodePropertyValue>> entry : nodeProps.entrySet()) {
            NodeRef nodeRef = entry.getKey();
            Map<QName, Serializable> properties = HibernateNodeDaoServiceImpl.convertToPublicProperties(entry.getValue(),
                    qnameDAO, localeDAO, contentDataDAO, dictionaryService, null, null);
            result.add(createNodeCallback.create(nodeRef, properties));
        }
        explainQuery(sql, args);
        return result;
    }

    @SuppressWarnings("deprecation")
    private Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> executeChildNodesQuery(List<Object> arguments, String sql) {
        Object[] paramArray = arguments.toArray();
        final Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> allPropsMap = new HashMap<>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> allChildProps = getPropsMap(getNodeRef(rs, "parent_uuid"), allPropsMap);
                Map<PropertyMapKey, NodePropertyValue> childProps = getPropsMap(getNodeRef(rs, "child_uuid"), allChildProps);
                PropertyMapKey propMapKey = getPropMapKey(rs);
                if (propMapKey.getQnameId() <= 0) {
                    return null;
                }
                NodePropertyValue nodePropValue = getPropertyValue(rs);
                childProps.put(propMapKey, nodePropValue);
                return null;
            }

        }, paramArray);
        explainQuery(sql, paramArray);
        return allPropsMap;
    }

    private String getChildNodeQuery(Collection<NodeRef> parentNodes, Set<QName> propsToLoad, QName childNodeType, List<Object> arguments) {
        String sql = "SELECT child_props.*, node.store_id as store_id, node.uuid AS parent_uuid, child.uuid AS child_uuid "
                + " FROM " + getNodeTableConditionalJoin(parentNodes, arguments)
                + " JOIN alf_child_assoc child_assoc on child_assoc.parent_node_id = node.id "
                + " JOIN alf_node child on child_assoc.child_node_id = child.id ";
        sql += " LEFT JOIN alf_node_properties child_props on child_props.node_id = child.id ";

        boolean hasPropCondition = propsToLoad != null && !propsToLoad.isEmpty();
        if (childNodeType != null) {
            sql += " WHERE child.type_qname_id = " + getQNameDbId(childNodeType) + (hasPropCondition ? " AND " : "");
        } else if (hasPropCondition) {
            sql += " WHERE ";
        }

        if (hasPropCondition) {
            sql += " child_props.qname_id in (" + createQnameIdListing(propsToLoad) + ") ";
        }
        return sql;
    }

    private <V, K> Map<K, V> getPropsMap(NodeRef nodeRef, Map<NodeRef, Map<K, V>> allDocPropsMap) {
        Map<K, V> allPropsMap = allDocPropsMap.get(nodeRef);
        if (allPropsMap == null) {
            allPropsMap = new HashMap<>();
            allDocPropsMap.put(nodeRef, allPropsMap);
        }
        return allPropsMap;
    }

    @SuppressWarnings("deprecation")
    private <T extends Node> Map<NodeRef, T> loadNodes(Collection<NodeRef> documentsToLoad, Set<QName> propsToLoad, boolean propsNotQuery,
            CreateNodeCallback<T> createDocumentCallback,
            final boolean loadType, Map<Long, QName> propertyTypes) {
        long startTime = System.nanoTime();
        final Map<NodeRef, T> result = new HashMap<NodeRef, T>();
        if (documentsToLoad == null || documentsToLoad.isEmpty()) {
            return result;
        }
        List<Object> arguments = new ArrayList<Object>();
        StringBuilder sqlQuery = new StringBuilder();
        boolean restrictByProps = propsToLoad != null && !propsToLoad.isEmpty();

        sqlQuery.append("select node.uuid as uuid, node.store_id, " + (loadType ? " node.type_qname_id, " : "")
                + " node.audit_creator as creator, node.audit_created as created, node.audit_modifier as modifier, node.audit_modified as modified, props.* "
                + " from " + getNodeTableConditionalJoin(documentsToLoad, arguments)
                + " left join alf_node_properties props on node.id = props.node_id");
        if (restrictByProps) {
            String qnameIds = createQnameIdListing(propsToLoad, !propsNotQuery);
            if (propsNotQuery && StringUtils.isNotBlank(qnameIds)) {
                sqlQuery.append(" AND qname_id not in (" + qnameIds + " )");
            } else if (!propsNotQuery) {
                sqlQuery.append(" AND qname_id in (" + qnameIds + " ) ");
            }
        }
        sqlQuery.append(" order by node.id, qname_id, locale_id, list_index");

        Object[] paramArray = arguments.toArray();
        final Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> allDocProps = new HashMap<>();
        final Map<NodeRef, Map<String, Object>> allDocIntrinsicProps = new HashMap<>();
        final Map<NodeRef, QName> nodeTypes = new HashMap<>();
        String query = sqlQuery.toString();
        jdbcTemplate.query(query,
                new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                NodeRef nodeRef = getNodeRef(rs);
                Map<PropertyMapKey, NodePropertyValue> docProps = allDocProps.get(nodeRef);
                if (docProps == null) {
                    docProps = new HashMap<PropertyMapKey, NodePropertyValue>();
                    allDocProps.put(nodeRef, docProps);
                }
                Map<String, Object> docIntrinsicProps = allDocIntrinsicProps.get(nodeRef);
                if (docIntrinsicProps == null) {
                    docIntrinsicProps = new HashMap<String, Object>();
                    docIntrinsicProps.put(ContentModel.PROP_CREATOR.toPrefixString(), rs.getString("creator"));
                    docIntrinsicProps.put(ContentModel.PROP_CREATED.toPrefixString(), DefaultTypeConverter.INSTANCE.convert(Date.class, rs.getString("created")));
                    docIntrinsicProps.put(ContentModel.PROP_MODIFIER.toPrefixString(), rs.getString("modifier"));
                    docIntrinsicProps.put(ContentModel.PROP_MODIFIED.toPrefixString(), DefaultTypeConverter.INSTANCE.convert(Date.class, rs.getString("modified")));
                    allDocIntrinsicProps.put(nodeRef, docIntrinsicProps);
                }
                PropertyMapKey propMapKey = getPropMapKey(rs);
                if (propMapKey.getQnameId() > 0) {
                    NodePropertyValue nodePropValue = getPropertyValue(rs);
                    docProps.put(propMapKey, nodePropValue);
                }
                if (loadType && !nodeTypes.containsKey(nodeRef)) {
                    nodeTypes.put(nodeRef, getTypeFromRs(rs));
                }
                return null;
            }

        }, paramArray);
        explainQuery(query, paramArray);
        if (propertyTypes == null) {
            propertyTypes = new HashMap<Long, QName>();
        }
        for (Map.Entry<NodeRef, Map<PropertyMapKey, NodePropertyValue>> entry : allDocProps.entrySet()) {
            NodeRef docRef = entry.getKey();
            Map<QName, Serializable> properties = HibernateNodeDaoServiceImpl.convertToPublicProperties(entry.getValue(), qnameDAO, localeDAO, contentDataDAO, dictionaryService,
                    null, propertyTypes);
            Map<String, Object> stringProperties = RepoUtil.toStringProperties(properties);
            Map<String, Object> docIntrinsicProps = allDocIntrinsicProps.get(docRef);
            if (docIntrinsicProps != null) {
                stringProperties.putAll(docIntrinsicProps);
            }
            result.put(docRef, createDocumentCallback.create(docRef, stringProperties, nodeTypes.get(docRef)));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("BulkLoadService.loadDocuments db total time " + CalendarUtil.duration(startTime, System.nanoTime()) + "ms, loaded " + result.size() + " documents");
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<NodeRef> loadNodeRefs(List<NodeRef> nodeRefs) {
        List<Object> arguments = new ArrayList<>();
        List<List<NodeRef>> slicedNodeRefList = RepoUtil.sliceList(nodeRefs, ApplicationConstantsBean.SORT_ALLOWED_LIMIT);
        List<NodeRef> result = new ArrayList<>();
        for (List<NodeRef> slice : slicedNodeRefList) {
            String sql = "select node.uuid as uuid, node.store_id from " + getNodeTableConditionalJoin(slice, arguments);
            result.addAll(jdbcTemplate.query(sql, new NodeRefRowMapper(), arguments.toArray()));
            arguments.clear();
        }
        return result;
    }

    private QName getTypeFromRs(ResultSet rs) throws SQLException {
        Long typeId = rs.getLong("type_qname_id");
        QName qname = DB_ID_TO_QNAME.get(typeId);
        if (typeId != null && qname == null && !DB_ID_TO_QNAME.containsKey(typeId)) {
            qname = qnameDAO.getQName(typeId).getSecond();
            DB_ID_TO_QNAME.put(typeId, qname);
        }
        return qname;
    }

    @Override
    public Map<NodeRef, Map<QName, Serializable>> loadPrimaryParentsProperties(List<NodeRef> childNodes, Set<QName> parentTypes, Set<QName> propsToLoad,
            Map<Long, QName> propertyTypes) {
        return loadPrimaryParentsProperties(childNodes, parentTypes, propsToLoad, propertyTypes, false);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<NodeRef, Map<QName, Serializable>> loadPrimaryParentsProperties(List<NodeRef> childNodes, Set<QName> parentTypes, Set<QName> propsToLoad,
            Map<Long, QName> propertyTypes, final boolean includeIntrinsicProps) {
        long startTime = System.nanoTime();
        if (childNodes == null || childNodes.isEmpty()) {
            return new HashMap<>();
        }
        List<Object> arguments = new ArrayList<>();
        String sql = getParentNodeQuery(childNodes, propsToLoad, parentTypes, arguments, includeIntrinsicProps);

        Object[] paramArray = arguments.toArray();
        final Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> allPropsMap = new HashMap<>();
        final Map<NodeRef, Map<QName, Serializable>> allIntrinsicProps = new HashMap<>();

        jdbcTemplate.query(sql, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> allParentProps = getPropsMap(getNodeRef(rs, "child_uuid"), allPropsMap);
                NodeRef parentRef = getNodeRef(rs, "parent_uuid");
                Map<PropertyMapKey, NodePropertyValue> parentProps = getPropsMap(parentRef, allParentProps);
                PropertyMapKey propMapKey = getPropMapKey(rs);
                if (propMapKey.getQnameId() > 0) {
                    NodePropertyValue nodePropValue = getPropertyValue(rs);
                    parentProps.put(propMapKey, nodePropValue);
                }
                if (includeIntrinsicProps) {
                    Map<QName, Serializable> intrinsicProps = allIntrinsicProps.get(parentRef);
                    if (intrinsicProps == null) {
                        intrinsicProps = new HashMap<QName, Serializable>();
                        intrinsicProps.put(ContentModel.PROP_CREATOR, rs.getString("creator"));
                        intrinsicProps.put(ContentModel.PROP_CREATED, DefaultTypeConverter.INSTANCE.convert(Date.class, rs.getString("created")));
                        intrinsicProps.put(ContentModel.PROP_MODIFIER, rs.getString("modifier"));
                        intrinsicProps.put(ContentModel.PROP_MODIFIED, DefaultTypeConverter.INSTANCE.convert(Date.class, rs.getString("modified")));
                        allIntrinsicProps.put(parentRef, intrinsicProps);
                    }
                }
                return null;
            }

        }, paramArray);
        explainQuery(sql, paramArray);

        Map<NodeRef, Map<QName, Serializable>> result = new HashMap<>();
        if (propertyTypes == null) {
            propertyTypes = new HashMap<>();
        }
        for (NodeRef childRef : childNodes) {
            Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> parentProps = allPropsMap.get(childRef);
            if (parentProps == null) {
                continue;
            }
            for (Map.Entry<NodeRef, Map<PropertyMapKey, NodePropertyValue>> innerEntry : parentProps.entrySet()) {
                Map<QName, Serializable> parentProperties = HibernateNodeDaoServiceImpl.convertToPublicProperties(innerEntry.getValue(), qnameDAO, localeDAO, contentDataDAO,
                        dictionaryService, null, propertyTypes);
                NodeRef parentRef = innerEntry.getKey();
                parentProperties.put(ContentModel.PROP_NODE_REF, parentRef);
                if (includeIntrinsicProps) {
                    Map<QName, Serializable> intrinsicProps = allIntrinsicProps.get(parentRef);
                    if (intrinsicProps != null) {
                        parentProperties.putAll(intrinsicProps);
                    }
                }
                result.put(childRef, parentProperties);
                break;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("BulkLoadService.loadPrimaryParents total time "
                    + CalendarUtil.duration(startTime, System.nanoTime()) + "ms, loaded parents for " + result.size() + " childNodes");
        }
        return result;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Set<NodeRef> loadPrimaryParentNodeRefs(Set<NodeRef> childNodes, Set<QName> parentNodeTypes) {
        if (childNodes == null || childNodes.isEmpty()) {
            return new HashSet<>();
        }
        List<Object> arguments = new ArrayList<>();
        String sql = getParentNodeQuery(childNodes, arguments, parentNodeTypes, false, false);
        Object[] paramArray = arguments.toArray();
        final Set<NodeRef> parentRefs = new HashSet<>();

        jdbcTemplate.query(sql, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                parentRefs.add(getNodeRef(rs, "parent_uuid"));
                return null;
            }

        }, paramArray);
        explainQuery(sql, paramArray);
        return parentRefs;
    }

    private String getParentNodeQuery(List<NodeRef> childNodes, Set<QName> propsToLoad, Set<QName> parentNodeTypes, List<Object> arguments, boolean includeIntrinsicProps) {
        String sql = "SELECT * FROM (" + getParentNodeQuery(childNodes, arguments, parentNodeTypes, true, includeIntrinsicProps);
        boolean hasPropCondition = propsToLoad != null && !propsToLoad.isEmpty();
        sql += ") temp ";
        if (hasPropCondition) {
            sql += " WHERE qname_id in (" + createQnameIdListing(propsToLoad) + ") OR qname_id is null";
        }
        return sql;
    }

    private String getParentNodeQuery(Collection<NodeRef> childNodes, List<Object> arguments, Set<QName> parentNodeTypes, boolean includeProps, boolean includeIntrinsicProps) {
        String sql = "SELECT " + (includeProps ? "parent_props.*, " : "") + "node.store_id as store_id, node.uuid AS child_uuid, parent.uuid AS parent_uuid "
                + (includeIntrinsicProps
                        ? ", parent.audit_creator AS creator, parent.audit_created AS created, parent.audit_modifier AS modifier, parent.audit_modified AS modified " : "")
                + " FROM " + getNodeTableConditionalJoin(childNodes, arguments)
                + " JOIN alf_child_assoc parent_assoc on parent_assoc.child_node_id = node.id "
                + " JOIN alf_node parent on parent_assoc.parent_node_id = parent.id ";
        if (includeProps) {
            sql += " LEFT JOIN alf_node_properties parent_props on parent_props.node_id = parent.id ";
        }
        sql += " WHERE parent_assoc.is_primary = true ";
        if (parentNodeTypes != null && !parentNodeTypes.isEmpty()) {
            sql += " AND parent.type_qname_id in (" + createQnameIdListing(parentNodeTypes) + ") ";
        }
        return sql;
    }

    @Override
    public Map<NodeRef, Integer> getSearchableChildDocCounts(List<NodeRef> targetNodes) {
        List<Object> arguments = new ArrayList<Object>();
        String sql = "SELECT node.store_id as store_id, node.uuid, count(*) as document_count "
                + " FROM " + getNodeTableConditionalJoin(targetNodes, arguments)
                + " JOIN alf_node_assoc doc_assoc on doc_assoc.target_node_id = node.id "
                + " JOIN alf_node source on doc_assoc.source_node_id = source.id "
                + " JOIN alf_node_aspects source_aspects on source_aspects.node_id = source.id "
                + " WHERE source.type_qname_id = " + getQNameDbId(DocumentCommonModel.Types.DOCUMENT)
                + " AND source_aspects.qname_id = " + getQNameDbId(DocumentCommonModel.Aspects.SEARCHABLE)
                + " AND doc_assoc.type_qname_id = " + getQNameDbId(DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT)
                + " group by node.store_id, node.uuid ";

        Object[] paramArray = arguments.toArray();

        final Map<NodeRef, Integer> result = new HashMap<NodeRef, Integer>();

        jdbcTemplate.query(sql, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                result.put(getNodeRef(rs), rs.getInt("document_count"));
                return null;
            }

        }, paramArray);
        return result;
    }

    @Override
    public Long getQNameDbId(QName qname) {
        Long qnameId = QNAME_TO_DB_ID.get(qname);
        if (qnameId == null) {
            Pair<Long, QName> qnamePair = qnameDAO.getQName(qname);
            if (qnamePair != null) {
                qnameId = qnamePair.getFirst();
                QNAME_TO_DB_ID.put(qname, qnameId);
            }
        }
        return qnameId;
    }

    private String getNodeQueryWithPropsFromAlfNode(List<NodeRef> nodeRefs, List<Object> params, String... typeIdPropTableAlias) {
        return getNodeQueryWithProps(typeIdPropTableAlias) + " from " + getNodeTableConditionalJoin(nodeRefs, params);
    }

    private String getNodeQueryWithProps(String... propTableFields) {
        String query = NODE_REF_QUERY_START;
        if (propTableFields != null) {
            for (String propName : propTableFields) {
                query += ", " + propName + (!propName.contains(".") ? ".* " : "");
            }
        }
        return query;
    }

    @Override
    public List<NodeRef> orderByDocumentCaseLabel(List<NodeRef> nodeRefs, boolean descending) {
        return orderByDocListUnitLable(nodeRefs, descending, DocumentCommonModel.Props.CASE, "delta_case");
    }

    @Override
    public List<NodeRef> orderByDocumentVolumeLabel(List<NodeRef> nodeRefs, boolean descending) {
        return orderByDocListUnitLable(nodeRefs, descending, DocumentCommonModel.Props.VOLUME, "delta_volume_casefile");
    }

    @Override
    public List<NodeRef> orderByDocumentSeriesLabel(List<NodeRef> nodeRefs, boolean descending) {
        return orderByDocListUnitLable(nodeRefs, descending, DocumentCommonModel.Props.SERIES, "delta_series");
    }

    @Override
    public List<NodeRef> orderByDocumentFunctionLabel(List<NodeRef> nodeRefs, boolean descending) {
        return orderByDocListUnitLable(nodeRefs, descending, DocumentCommonModel.Props.FUNCTION, "delta_function");
    }

    private List<NodeRef> orderByDocListUnitLable(List<NodeRef> nodeRefs, boolean descending, QName labelNodeRefProp, String labelView) {
        long startTime = System.nanoTime();
        if (exceedsLimit(nodeRefs, LOG)) {
            return null;
        }
        List<Object> params = new ArrayList<Object>();
        String labelNodeRefPropTableAlias = getPropTableAlias(labelNodeRefProp);
        String sqlQuery = getNodeQueryWithPropsFromAlfNode(nodeRefs, params, labelNodeRefPropTableAlias)
                + getPropertyTableLeftJoin(params, null, labelNodeRefProp)
                + " left join " + labelView + " on " + labelNodeRefPropTableAlias + ".string_value = " + labelView + ".node_ref_str "
                + " order by " + labelView + ".label " + getDescSort(descending);
        Object[] paramArray = params.toArray();
        @SuppressWarnings("deprecation")
        List<NodeRef> sortedNodeRefs = jdbcTemplate.query(sqlQuery, new NodeRefRowMapper(), paramArray);
        if (LOG.isDebugEnabled()) {
            LOG.debug("BulkLoadService.orderByDocListUnitLable db total time " + CalendarUtil.duration(startTime, System.nanoTime()) + "ms, loaded " + sortedNodeRefs.size()
                    + " documents");
        }
        explainQuery(sqlQuery, paramArray);
        return sortedNodeRefs;
    }

    private String getDescSort(boolean descending) {
        return descending ? " desc " : "";
    }

    private boolean isAuditableProp(QName prop) {
        return ContentModel.PROP_CREATOR.equals(prop) || ContentModel.PROP_CREATED.equals(prop) || ContentModel.PROP_MODIFIER.equals(prop)
                || ContentModel.PROP_MODIFIED.equals(prop);
    }

    private PropertyMapKey getPropMapKey(ResultSet rs) throws SQLException {
        PropertyMapKey propMapKey = new PropertyMapKey();
        propMapKey.setQnameId(rs.getLong("qname_id"));
        propMapKey.setLocaleId(rs.getLong("locale_id"));
        propMapKey.setListIndex(rs.getInt("list_index"));
        return propMapKey;
    }

    private NodePropertyValue getPropertyValue(ResultSet rs) throws SQLException {
        NodePropertyValue nodePropValue = new NodePropertyValue();
        nodePropValue.setActualType(rs.getInt("actual_type_n"));
        nodePropValue.setPersistedType(rs.getInt("persisted_type_n"));
        nodePropValue.setBooleanValue(rs.getBoolean(BOOLEAN_VALUE_COL));
        nodePropValue.setDoubleValue(rs.getDouble(DOUBLE_VALUE_COL));
        nodePropValue.setFloatValue(rs.getFloat(FLOAT_VALUE_COL));
        nodePropValue.setLongValue(rs.getLong(LONG_VALUE_COL));
        // TODO investigate if this is adequate convresion
        byte[] serializableValue = rs.getBytes("serializable_value");
        if (serializableValue != null && serializableValue.length > 0) {
            Serializable deserializedValue = (Serializable) (serializableValue != null && serializableValue.length > 0 ? SerializationHelper
                    .deserialize(serializableValue) : null);
            nodePropValue.setSerializableValue(deserializedValue);
        }
        nodePropValue.setStringValue(rs.getString(STRING_VALUE_COL));
        return nodePropValue;
    }

    private String createQnameIdListing(Set<QName> propsToLoad) {
        return createQnameIdListing(propsToLoad, true);
    }

    private String createQnameIdListing(Set<QName> propsToLoad, boolean includeNulls) {
        Assert.isTrue(propsToLoad != null && !propsToLoad.isEmpty(), "Cannot create properties condition without arguments!");
        StringBuilder sb = new StringBuilder();
        for (QName prop : propsToLoad) {
            Long qNameDbId = getQNameDbId(prop);
            if (qNameDbId == null && !includeNulls) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(qNameDbId);
        }
        return sb.toString();
    }

    @Override
    public String getNodeTableConditionalJoin(Collection<NodeRef> nodesToLoad, List<Object> arguments) {
        return getNodeTableConditionalJoin(nodesToLoad, arguments, "*");
    }

    private String getNodeTableConditionalJoin(Collection<NodeRef> nodesToLoad, List<Object> arguments, String selectColumns) {
        Assert.isTrue(nodesToLoad != null && !nodesToLoad.isEmpty(), "Cannot create node query without arguments!");
        Map<StoreRef, Set<String>> nodeIdsByStoreRef = getNodeRefsByStore(nodesToLoad);
        List<String> queryParts = new ArrayList<>();
        for (Map.Entry<StoreRef, Set<String>> entry : nodeIdsByStoreRef.entrySet()) {
            Set<String> value = entry.getValue();
            queryParts.add("(select " + selectColumns + " from alf_node where alf_node.store_id = " + getStoreRefDbId(entry.getKey()) + " and alf_node.uuid IN ("
                    + getQuestionMarks(value.size()) + ") AND " + NODE_NOT_DELETED_CONDITION + ")");
            arguments.addAll(value);
        }
        return "(" + TextUtil.joinNonBlankStrings(queryParts, " UNION ") + ") as node ";
    }

    @Override
    public Long getStoreRefDbId(StoreRef storeRef) {
        if (STORE_REF_TO_DB_ID.isEmpty()) {
            List<Pair<Long, StoreRef>> stores = BeanHelper.getNodeDaoService().getStores();
            for (Pair<Long, StoreRef> store : stores) {
                STORE_REF_TO_DB_ID.put(store.getSecond(), store.getFirst());
            }
        }
        return STORE_REF_TO_DB_ID.get(storeRef);
    }

    @Override
    public StoreRef getStoreRefByDbId(Long dbId) {
        if (DB_ID_TO_STORE_REF.isEmpty()) {
            List<Pair<Long, StoreRef>> stores = BeanHelper.getNodeDaoService().getStores();
            for (Pair<Long, StoreRef> store : stores) {
                DB_ID_TO_STORE_REF.put(store.getFirst(), store.getSecond());
            }
        }
        return DB_ID_TO_STORE_REF.get(dbId);
    }

    private Map<StoreRef, Set<String>> getNodeRefsByStore(Collection<NodeRef> nodesToLoad) {
        Map<StoreRef, Set<String>> nodeIdsByStoreRef = new HashMap<StoreRef, Set<String>>();
        for (NodeRef nodeRef : nodesToLoad) {
            StoreRef storeRef = nodeRef.getStoreRef();
            Set<String> storeNodeRefs = nodeIdsByStoreRef.get(storeRef);
            if (storeNodeRefs == null) {
                storeNodeRefs = new HashSet<String>();
                nodeIdsByStoreRef.put(storeRef, storeNodeRefs);
            }
            storeNodeRefs.add(nodeRef.getId());
        }
        return nodeIdsByStoreRef;
    }

    @Override
    public Map<NodeRef, String> loadDocumentWorkflowStates(List<NodeRef> docRefs) {
        long startTime = System.nanoTime();
        List<Object> arguments = new ArrayList<>();
        String sqlQuery = getWorkflowStateQuery(docRefs, arguments, true);
        final Map<NodeRef, String> workflowStates = new HashMap<>();
        jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                NodeRef nodeRef = getNodeRef(rs);
                String workflowState = rs.getString("document_workflow_state");
                workflowStates.put(nodeRef, workflowState);
                return null;
            }

        }, arguments.toArray());
        if (LOG.isDebugEnabled()) {
            LOG.debug("BulkLoadService.loadDocumentWorkflowStates db total time " + CalendarUtil.duration(startTime, System.nanoTime()) + "ms, loaded "
                    + workflowStates.size() + " documents");
        }
        return workflowStates;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<NodeRef> orderByDocumentWorkflowStates(List<NodeRef> docRefs, boolean descending) {
        long startTime = System.nanoTime();
        if (exceedsLimit(docRefs, LOG)) {
            return null;
        }
        List<Object> arguments = new ArrayList<>();
        String sqlQuery = getWorkflowStateQuery(docRefs, arguments, false);
        sqlQuery += "order by string_agg(workflow_state, ';') " + getDescSort(descending);
        List<NodeRef> orderedWorkflowStates = jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                return getNodeRef(rs);
            }

        }, arguments.toArray());
        Collection<NodeRef> missingFromOrdered = CollectionUtils.subtract(docRefs, orderedWorkflowStates);
        List<NodeRef> result;
        if (descending) {
            result = new ArrayList<>(missingFromOrdered);
            result.addAll(orderedWorkflowStates);
        } else {
            result = orderedWorkflowStates;
            result.addAll(missingFromOrdered);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("BulkLoadService.orderByDocumentWorkflowStates db total time " + CalendarUtil.duration(startTime, System.nanoTime()) + "ms, loaded "
                    + result.size() + " documents");
        }
        return result;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Set<NodeRef> getAssociatedDocRefs(NodeRef docRef) {
        Set<NodeRef> associatedDocs = new HashSet<>();
        if (docRef == null) {
            return associatedDocs;
        }

        List<Object> arguments = new ArrayList<>();

        Set<QName> assocQNames = new HashSet<QName>();
        assocQNames.add(DocumentCommonModel.Assocs.DOCUMENT_REPLY);
        assocQNames.add(DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);

        String sql = "with recursive cte (id, store_id, uuid, source_node_id, target_node_id, processed) as ("
                + " select node.id, store_id, uuid, source_node_id, target_node_id, "
                + " ARRAY[node.id] from " + getNodeTableConditionalJoin(Collections.singletonList(docRef), arguments)
                + " join alf_node_assoc assoc on node.id = assoc.source_node_id or node.id = assoc.target_node_id "
                + " where assoc.type_qname_id in (" + createQnameIdListing(assocQNames) + ")"
                + " union all "
                + " select node.id, node.store_id, node.uuid, assoc.source_node_id, assoc.target_node_id, "
                + " cte.processed || ARRAY[cte.source_node_id] || ARRAY[cte.target_node_id] from "
                + " alf_node  node, cte, alf_node_assoc assoc "
                + " where assoc.type_qname_id in (" + createQnameIdListing(assocQNames) + ")"
                + " and (node.id = any(ARRAY[cte.source_node_id]) or node.id = any(ARRAY[cte.target_node_id])) "
                + " and not node.id = ANY(cte.processed) "
                + " and (node.id = assoc.source_node_id or node.id = assoc.target_node_id) "
                + " ) "
                + " select distinct store_id, uuid from cte ";

        associatedDocs.addAll(jdbcTemplate.query(sql, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                return getNodeRef(rs);
            }
        }, arguments.toArray()));
        return associatedDocs;
    }

    private String getWorkflowStateQuery(List<NodeRef> docRefs, List<Object> arguments, boolean returnStateStr) {
        String inProgressStatus = Status.IN_PROGRESS.getName();
        String sqlQuery = "select uuid, store_id " + (returnStateStr ? ", string_agg(workflow_state, '; ') as document_workflow_state" : "") + " from " +
                "( " +
                "select uuid, store_id, workflow_id, concat(workflow_label, ' (', string_agg(task_owner_name, ', '), ')') as workflow_state from " +
                "( " +
                "select node.uuid as uuid, node.store_id, delta_task.workflow_id as workflow_id,  " +
                "    workflow_label.name as workflow_label, delta_task.wfc_owner_name as task_owner_name "
                + " from " + getNodeTableConditionalJoin(docRefs, arguments) +
                "join alf_child_assoc doc_assoc on node.id = doc_assoc.parent_node_id " +
                "join alf_node compound_workflow on compound_workflow.id = doc_assoc.child_node_id " +
                "join alf_child_assoc workflow_assoc on compound_workflow.id = workflow_assoc.parent_node_id " +
                "join alf_node workflow on workflow.id = workflow_assoc.child_node_id " +
                "join alf_node_properties status_prop on status_prop.node_id = workflow.id " +
                "left join delta_workflow_type_name workflow_label on workflow_label.type_qname_id = workflow.type_qname_id " +
                "left join delta_task on workflow.uuid = delta_task.workflow_id " +
                "where " +
                " delta_task.wfc_status = '" + inProgressStatus + "'" +
                " and status_prop.qname_id = " + getQNameDbId(WorkflowCommonModel.Props.STATUS) +
                "and status_prop.string_value = '" + inProgressStatus + "'" +
                "order by compound_workflow.id, workflow_assoc.assoc_index, delta_task.index_in_workflow " +
                ") as all_tasks " +
                "group by uuid, store_id, workflow_id, workflow_label " +
                ") as all_tasks_by_workflow " +
                "group by uuid, store_id ";
        return sqlQuery;
    }

    @Override
    public List<NodeRef> loadChildDocNodeRefs(NodeRef parentNodeRef) {
        List<Object> params = new ArrayList<Object>();
        String sqlQuery = getChildNodeQueryWithProps(parentNodeRef, params, Collections.singleton(DocumentCommonModel.Types.DOCUMENT));

        Object[] paramArray = params.toArray();

        @SuppressWarnings("deprecation")
        List<NodeRef> documents = jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                return getNodeRef(rs);
            }

        }, paramArray);
        return documents;
    }

    private NodeRef getNodeRef(ResultSet rs, String nodeUuidName) throws SQLException {
        String nodeUuid = rs.getString(nodeUuidName);
        StoreRef storeRef = getStoreRefByDbId(rs.getLong("store_id"));
        return new NodeRef(storeRef, nodeUuid);
    }

    private NodeRef getNodeRef(ResultSet rs) throws SQLException {
        return getNodeRef(rs, "uuid");
    }

    @Override
    public <T extends Serializable> Map<T, NodeRef> loadChildElementsNodeRefs(NodeRef parentNodeRef, final QName propName, QName... type) {
        return loadChildElementsNodeRefs(parentNodeRef, propName, false, type);
    }

    @SuppressWarnings("deprecation")
    @Override
    public <T extends Serializable> Map<T, NodeRef> loadChildElementsNodeRefs(NodeRef parentNodeRef, final QName propName, final boolean skipNodesWithMissingProp, QName... type) {
        List<Object> params = new ArrayList<Object>();

        String sqlQuery = getChildNodeQueryWithProps(parentNodeRef, params, new HashSet<QName>(Arrays.asList(type)), propName);

        Object[] paramArray = params.toArray();
        final Map<T, NodeRef> values = new HashMap<>();

        final Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                PropertyMapKey propertyKey = getPropMapKey(rs);
                if (skipNodesWithMissingProp && allZeroesOrNulls(propertyKey)) {
                    return null;
                }
                Map<QName, Serializable> props = new HashMap<>();
                if (propertyKey.getQnameId() > 0) {
                    NodePropertyValue propertyValue = getPropertyValue(rs);
                    Map<PropertyMapKey, NodePropertyValue> rawPropValues = new HashMap<PropertyMapKey, NodePropertyValue>();
                    rawPropValues.put(propertyKey, propertyValue);
                    props = HibernateNodeDaoServiceImpl.convertToPublicProperties(rawPropValues, qnameDAO, localeDAO, contentDataDAO,
                            dictionaryService, null, propertyTypes);
                }
                @SuppressWarnings("unchecked")
                T value = (T) props.get(propName);
                if (values.containsKey(value)) {
                    throw new RuntimeException("Duplicate value: " + value);
                }
                values.put(value, getNodeRef(rs));
                return null;
            }
        }, paramArray);
        return values;
    }

    private boolean allZeroesOrNulls(PropertyMapKey key) {
        return (key.getListIndex() == null || key.getListIndex() == 0)
                && (key.getLocaleId() == null || key.getLocaleId() == 0)
                && (key.getQnameId() == null || key.getQnameId() == 0);
    }

    @Override
    public List<NodeRef> loadChildRefs(NodeRef parentRef, QName type) {
        List<NodeRef> result = new ArrayList<>();
        result.addAll(loadChildRefs(parentRef, null, null, type));
        return result;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Set<NodeRef> loadChildRefs(NodeRef parentRef, final QName propName, String requiredValue, QName type) {
        final Set<NodeRef> result = new HashSet<>();
        List<Object> params = new ArrayList<Object>();

        Set<QName> typeArray = type != null ? Collections.singleton(type) : null;
        boolean checkPropValue = requiredValue != null;
        String sqlQuery = checkPropValue ? getChildNodeQueryWithProps(parentRef, params, typeArray, propName) : getChildNodeQueryWithProps(parentRef, params, typeArray);
        if (checkPropValue) {
            sqlQuery += (typeArray == null ? " where " : " and ") + getPropTableAlias(propName) + ".string_value=?";
            params.add(requiredValue);
        }
        Object[] paramArray = params.toArray();
        jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                result.add(getNodeRef(rs));
                return null;
            }
        }, paramArray);
        return result;
    }

    private String getPropTableAlias(final QName propName) {
        String localName = propName.getLocalName();
        String alias = PROP_TABLE_ALIASES.get(localName);
        if (alias == null) {
            alias = getDbFieldNameFromCamelCase(localName) + "_prop";
        }
        return alias;
    }

    @Override
    public Boolean getSubscriptionPropValue(NodeRef personRef, QName notificationType) {
        long startTime = System.nanoTime();
        List<Object> arguments = new ArrayList<>();
        String sql = "SELECT grand_child_props.* "
                + " FROM " + getNodeTableConditionalJoin(Arrays.asList(personRef), arguments)
                + " JOIN alf_child_assoc child_assoc on child_assoc.parent_node_id = node.id "
                + " JOIN alf_node child on child_assoc.child_node_id = child.id and child.type_qname_id = " + getQNameDbId(ApplicationModel.TYPE_CONFIGURATIONS)
                + " JOIN alf_child_assoc grand_child_assoc on grand_child_assoc.parent_node_id = child.id and grand_child_assoc.qname_localname = 'preferences' "
                + " JOIN alf_node grand_child on grand_child_assoc.child_node_id = grand_child.id "
                + " JOIN alf_node_properties grand_child_props on grand_child_props.node_id = grand_child.id "
                + " WHERE grand_child_props.qname_id = " + getQNameDbId(notificationType);

        List<Map<QName, Serializable>> props = jdbcTemplate.query(sql, new ParameterizedRowMapper<Map<QName, Serializable>>() {

            @Override
            public Map<QName, Serializable> mapRow(ResultSet rs, int rowNum) throws SQLException {
                PropertyMapKey propertyKey = getPropMapKey(rs);
                Map<QName, Serializable> props = new HashMap<>();
                if (propertyKey.getQnameId() > 0) {
                    NodePropertyValue propertyValue = getPropertyValue(rs);
                    Map<PropertyMapKey, NodePropertyValue> rawPropValues = new HashMap<>();
                    rawPropValues.put(propertyKey, propertyValue);
                    props = HibernateNodeDaoServiceImpl.convertToPublicProperties(rawPropValues, qnameDAO, localeDAO, contentDataDAO,
                            dictionaryService, null, null);
                }
                return props;
            }

        }, arguments.toArray());
        Boolean result = null;
        if (!props.isEmpty()) {
            result = (Boolean) props.get(0).get(notificationType);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("BulkLoadService.getSubscriptionPropValue total time " + CalendarUtil.duration(startTime, System.nanoTime()) + "ms");
        }
        return result;
    }

    @Override
    public List<String> loadChildSearchableDocFieldNodeRefs(NodeRef fieldDefinitionsRoot) {
        return loadChildSearchableFieldNodeRefs(fieldDefinitionsRoot, DocumentAdminModel.Props.IS_PARAMETER_IN_DOC_SEARCH);
    }

    @Override
    public List<String> loadChildSearchableVolFieldNodeRefs(NodeRef fieldDefinitionsRoot) {
        return loadChildSearchableFieldNodeRefs(fieldDefinitionsRoot, DocumentAdminModel.Props.IS_PARAMETER_IN_VOL_SEARCH);
    }

    private List<String> loadChildSearchableFieldNodeRefs(NodeRef fieldDefinitionsRoot, final QName isParamInSearchProp) {
        List<Object> params = new ArrayList<Object>();
        String searchPropTableAlias = getPropTableAlias(isParamInSearchProp);
        final QName idProp = DocumentAdminModel.Props.FIELD_ID;

        String sqlQuery = getChildNodeQueryWithProps(fieldDefinitionsRoot, params, Collections.singleton(DocumentAdminModel.Types.FIELD_DEFINITION), idProp, isParamInSearchProp)
                + " AND " + searchPropTableAlias + ".boolean_value=true";

        Object[] paramArray = params.toArray();
        @SuppressWarnings("deprecation")
        List<String> fieldDefinitionIds = jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                PropertyMapKey propertyKey = getPropMapKey(rs);
                if (propertyKey.getQnameId() <= 0) {
                    return null;
                }
                NodePropertyValue propertyValue = getPropertyValue(rs);
                Map<PropertyMapKey, NodePropertyValue> rawPropValues = new HashMap<PropertyMapKey, NodePropertyValue>();
                rawPropValues.put(propertyKey, propertyValue);
                Map<QName, Serializable> classificatorProps = HibernateNodeDaoServiceImpl.convertToPublicProperties(rawPropValues, qnameDAO, localeDAO, contentDataDAO,
                        dictionaryService);
                return (String) classificatorProps.get(idProp);
            }
        }, paramArray);
        return fieldDefinitionIds;
    }

    private String getChildNodeQueryWithProps(NodeRef parentRef, List<Object> params, Set<QName> childNodeTypes, QName... propName) {
        String propTableAlias = null;
        boolean hasProp = propName != null && propName.length > 0;
        if (hasProp) {
            propTableAlias = getPropTableAlias(propName[0]);
        }
        String sql = "SELECT child.store_id, child.uuid as uuid " + (hasProp ? (", " + propTableAlias + ".*") : "")
                + " FROM " + getNodeTableConditionalJoin(Collections.singletonList(parentRef), params)
                + " JOIN alf_child_assoc child_assoc on child_assoc.parent_node_id = node.id "
                + " JOIN alf_node child on child_assoc.child_node_id = child.id ";
        if (hasProp) {
            sql += getPropertyTableLeftJoin(params, "child", propName);
        }

        if (childNodeTypes != null && !childNodeTypes.isEmpty()) {
            sql += " WHERE child.type_qname_id in (" + createQnameIdListing(childNodeTypes) + ")";
        }
        return sql;
    }

    private String getPropertyTableLeftJoin(List<Object> params, String nodeTableAlias, QName... propNames) {
        return getPropertyTableJoin(params, true, nodeTableAlias, propNames);
    }

    private String getPropertyTableJoin(List<Object> params, boolean leftJoin, String nodeTableAlias, QName... propNames) {
        Assert.isTrue(propNames != null && propNames.length > 0, "Cannot join tables without table arguments!");
        String sql = "";
        String left = leftJoin ? " LEFT " : "";
        if (nodeTableAlias == null) {
            nodeTableAlias = "node";
        }
        for (QName propQName : propNames) {
            if (isAuditableProp(propQName)) {
                continue;
            }
            String propTableAlias = getPropTableAlias(propQName);
            sql += left + "JOIN alf_node_properties " + propTableAlias + " ON (" + nodeTableAlias + ".id = " + propTableAlias + ".node_id and " + propTableAlias + ".qname_id = "
                    + getQNameDbId(propQName) + ")";
        }
        return sql;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void fillQNameCache() {
        jdbcTemplate.query("select id from alf_qname", new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                qnameDAO.getQName(rs.getLong("id"));
                return null;
            }
        });
    }

    private final class NodeRefRowMapper implements ParameterizedRowMapper<NodeRef> {
        @Override
        public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
            return getNodeRef(rs);
        }
    }

    private void explainQuery(String sqlQuery, Object... args) {
        // generalService.explainQuery(sqlQuery, LOG, false, args);
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setGeneralService(GeneralService generalService) {
    }

    public void setQnameDAO(QNameDAO qnameDAO) {
        this.qnameDAO = qnameDAO;
    }

    public void setContentDataDAO(ContentDataDAO contentDataDAO) {
        this.contentDataDAO = contentDataDAO;
    }

    public void setLocaleDAO(LocaleDAO localeDAO) {
        this.localeDAO = localeDAO;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public WorkflowService getWorkflowService() {
        if (_workflowService == null) {
            _workflowService = BeanHelper.getWorkflowService();
        }
        return _workflowService;
    }

    public void setWorkflowConstantsBean(WorkflowConstantsBean workflowConstantsBean) {
    }
}
