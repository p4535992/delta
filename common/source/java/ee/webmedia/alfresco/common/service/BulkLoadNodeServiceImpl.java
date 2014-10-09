package ee.webmedia.alfresco.common.service;

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
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.formula.functions.T;
import org.hibernate.util.SerializationHelper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * This is limited version of BulkLoadNodeServiceImpl backported from Delta 5.1 version
 * and should not be integrated to higher branches than current 3.6 branch.
 */
@Deprecated
public class BulkLoadNodeServiceImpl implements BulkLoadNodeService {

    private static final String DOUBLE_VALUE_COL = "double_value";

    private static final String FLOAT_VALUE_COL = "float_value";

    private static final String LONG_VALUE_COL = "long_value";

    private static final String BOOLEAN_VALUE_COL = "boolean_value";

    private static final String STRING_VALUE_COL = "string_value";

    private static final int SORT_ALLOWED_LIMIT = 10000;

    private static final String NODE_REF_QUERY_START = "select node.uuid, node.store_id ";
    private static final String NODE_NOT_DELETED_CONDITION = " alf_node.node_deleted = false";
    private static final Set<QName> SIMPLE_FILE_PROPS = new HashSet<QName>(Arrays.asList(FileModel.Props.DISPLAY_NAME, FileModel.Props.ACTIVE, DvkModel.Props.DVK_ID,
            ContentModel.PROP_NAME));
    private static final Map<String, String> PROP_TABLE_ALIASES = new ConcurrentHashMap<String, String>();
    private static final Map<QName, Long> QNAME_TO_DB_ID = new ConcurrentHashMap<QName, Long>();
    private static final Map<Long, QName> DB_ID_TO_QNAME = new ConcurrentHashMap<Long, QName>();
    private static final Map<StoreRef, Long> STORE_REF_TO_DB_ID = new ConcurrentHashMap<StoreRef, Long>();
    private static final Map<Long, StoreRef> DB_ID_TO_STORE_REF = new ConcurrentHashMap<Long, StoreRef>();

    private static final Log LOG = LogFactory.getLog(BulkLoadNodeServiceImpl.class);

    private SimpleJdbcTemplate jdbcTemplate;
    private QNameDAO qnameDAO;
    private ContentDataDAO contentDataDAO;
    private LocaleDAO localeDAO;
    private DictionaryService dictionaryService;
    private WorkflowService _workflowService;

    @Override
    public int countChildNodes(NodeRef parentRef, QName childNodeType) {
        Map<NodeRef, Integer> counts = countChildNodes(Collections.singletonList(parentRef), childNodeType);
        return (counts.containsKey(parentRef) ? counts.get(parentRef) : 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<NodeRef, Integer> countChildNodes(List<NodeRef> parentRefs, QName childNodeType) {
        if (parentRefs == null || parentRefs.isEmpty()) {
            return new HashMap<NodeRef, Integer>();
        }
        List<Object> arguments = new ArrayList<Object>();
        String sql = "SELECT node.store_id, node.uuid, count(1) as child_count "
                + " FROM " + getNodeTableConditionalJoin(parentRefs, arguments)
                + " JOIN alf_child_assoc child_assoc on child_assoc.parent_node_id = node.id "
                + " JOIN alf_node child on child_assoc.child_node_id = child.id ";

        if (childNodeType != null) {
            sql += " WHERE child.type_qname_id = " + getQNameDbId(childNodeType);
        }
        sql += " GROUP BY node.store_id, node.uuid";

        final Map<NodeRef, Integer> counts = new HashMap<NodeRef, Integer>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                setDefaultFetchSize(rs);
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
            return new HashMap<NodeRef, Integer>();
        }
        List<Object> arguments = new ArrayList<Object>();
        String sql = "SELECT node.store_id, node.uuid, count(1) as source_count "
                + " FROM " + getNodeTableConditionalJoin(sourceRefs, arguments)
                + " JOIN alf_node_assoc node_assoc on node_assoc.target_node_id = node.id "
                + " JOIN alf_node source on node_assoc.source_node_id = source.id "
                + " JOIN alf_node_aspects aspects on source.id = aspects.node_id and aspects.qname_id = " + getQNameDbId(DocumentCommonModel.Aspects.SEARCHABLE);

        if (assocType != null) {
            sql += " WHERE node_assoc.type_qname_id = " + getQNameDbId(assocType);
        }
        sql += " GROUP BY node.store_id, node.uuid";

        final Map<NodeRef, Integer> counts = new HashMap<NodeRef, Integer>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                setDefaultFetchSize(rs);
                counts.put(getNodeRef(rs), rs.getInt("source_count"));
                return null;
            }

        }, arguments.toArray());
        return counts;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Map<NodeRef, List<NodeRef>> getSourceAssocs(List<NodeRef> targetNodeRefs, QName assocType) {
        if (targetNodeRefs == null || targetNodeRefs.isEmpty()) {
            return new HashMap<NodeRef, List<NodeRef>>();
        }
        List<Object> arguments = new ArrayList<Object>();
        String sql = "SELECT node.store_id, node.uuid, target.store_id as target_store_id, target.uuid as target_uuid "
                + " FROM " + getNodeTableConditionalJoin(targetNodeRefs, arguments)
                + " JOIN alf_node_assoc node_assoc on node_assoc.source_node_id = node.id "
                + " JOIN alf_node target on node_assoc.target_node_id = target.id ";

        if (assocType != null) {
            sql += " WHERE node_assoc.type_qname_id = " + getQNameDbId(assocType);
        }

        final Map<NodeRef, List<NodeRef>> result = new HashMap<NodeRef, List<NodeRef>>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<T>() {

            @Override
            public T mapRow(ResultSet rs, int arg1) throws SQLException {
                NodeRef sourceRef = getNodeRef(rs, "uuid", "store_id");
                List<NodeRef> targetRefs = result.get(sourceRef);
                if (targetRefs == null) {
                    targetRefs = new ArrayList<NodeRef>();
                    result.put(sourceRef, targetRefs);
                }
                targetRefs.add(getNodeRef(rs, "target_uuid", "target_store_id"));
                return null;
            }
        }, arguments.toArray());

        return result;
    }

    private void setDefaultFetchSize(ResultSet rs) throws SQLException {
        // TODO: this could impact performance significantly,
        // by making retrieving large amounts of data much quicker, but at the same time consuming much more memory
        // Needs thorough testing before use
        // rs.setFetchSize(1000);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasChildNodeOfType(NodeRef parentRef, QName childNodeType) {
        List<Object> arguments = new ArrayList<Object>();
        String sql = "SELECT EXISTS (SELECT 1 "
                + " FROM " + getNodeTableConditionalJoin(Arrays.asList(parentRef), arguments)
                + " JOIN alf_child_assoc child_assoc on child_assoc.parent_node_id = node.id "
                + " JOIN alf_node child on child_assoc.child_node_id = child.id "
                + " WHERE child.type_qname_id = " + getQNameDbId(childNodeType) + ")";

        return jdbcTemplate.queryForObject(sql, Boolean.class, arguments.toArray());
    }

    @Override
    public Map<NodeRef, Node> loadNodes(List<NodeRef> nodesToLoad, final Set<QName> propsToLoad) {
        return loadNodes(nodesToLoad, propsToLoad, null);
    }

    @Override
    public Map<NodeRef, Node> loadNodes(List<NodeRef> nodesToLoad, final Set<QName> propsToLoad, Map<Long, QName> propertyTypes) {
        CreateNodeCallback<Node> createNodeCallback = new CreateNodeCallback<Node>() {
            @Override
            public Node create(NodeRef nodeRef, Map<String, Object> properties, QName typeQname) {
                return new WmNode(nodeRef, typeQname, properties, null);
            }
        };
        return loadNodes(nodesToLoad, propsToLoad, false, createNodeCallback, true, propertyTypes);
    }

    @Override
    public Map<NodeRef, Map<NodeRef, Map<QName, Serializable>>> loadChildNodes(List<NodeRef> parentNodes, Set<QName> propsToLoad) {
        long startTime = System.nanoTime();
        List<Object> arguments = new ArrayList<Object>();
        String sql = getChildNodeQuery(parentNodes, propsToLoad, null, arguments, false);

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
    @SuppressWarnings("deprecation")
    public <T> Map<NodeRef, List<T>> loadChildNodes(List<NodeRef> parentNodes, Set<QName> propsToLoad, QName childNodeType, Map<Long, QName> propertyTypes,
            CreateObjectCallback<T> createObjectCallback) {
        long startTime = System.nanoTime();
        List<Object> arguments = new ArrayList<Object>();
        String sql = getChildNodeQuery(parentNodes, propsToLoad, childNodeType, arguments, false);

        final Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> allPropsMap = executeChildNodesQuery(arguments, sql);
        Map<NodeRef, List<T>> result = new HashMap<NodeRef, List<T>>();
        int childCount = 0;
        if (propertyTypes == null) {
            propertyTypes = new HashMap<Long, QName>();
        }
        for (Map.Entry<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> entry : allPropsMap.entrySet()) {
            NodeRef parentRef = entry.getKey();
            List<T> objectList = new ArrayList<T>();
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

    @SuppressWarnings("deprecation")
    private Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> executeChildNodesQuery(List<Object> arguments, String sql) {
        Object[] paramArray = arguments.toArray();
        final Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> allPropsMap = new HashMap<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                setDefaultFetchSize(rs);
                Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> allChildProps = getPropsMap(getNodeRef(rs, "parent_uuid"), allPropsMap);
                Map<PropertyMapKey, NodePropertyValue> childProps = getPropsMap(getNodeRef(rs, "child_uuid"), allChildProps);
                PropertyMapKey propMapKey = getPropMapKey(rs);
                NodePropertyValue nodePropValue = getPropertyValue(rs);
                childProps.put(propMapKey, nodePropValue);
                return null;
            }

        }, paramArray);
        explainQuery(sql, paramArray);
        return allPropsMap;
    }

    private String getChildNodeQuery(List<NodeRef> parentNodes, Set<QName> propsToLoad, QName childNodeType, List<Object> arguments, boolean ordered) {
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
        if (ordered) {
            sql += " order by node.id, child_assoc.assoc_index ";
        }
        return sql;
    }

    private <V, K> Map<K, V> getPropsMap(NodeRef nodeRef, Map<NodeRef, Map<K, V>> allDocPropsMap) {
        Map<K, V> allPropsMap = allDocPropsMap.get(nodeRef);
        if (allPropsMap == null) {
            allPropsMap = new HashMap<K, V>();
            allDocPropsMap.put(nodeRef, allPropsMap);
        }
        return allPropsMap;
    }

    @SuppressWarnings("deprecation")
    private <T extends Node> Map<NodeRef, T> loadNodes(List<NodeRef> documentsToLoad, Set<QName> propsToLoad, boolean propsNotQuery, CreateNodeCallback<T> createDocumentCallback,
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
        final Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> allDocProps = new HashMap<NodeRef, Map<PropertyMapKey, NodePropertyValue>>();
        final Map<NodeRef, Map<String, Object>> allDocIntrinsicProps = new HashMap<NodeRef, Map<String, Object>>();
        final Map<NodeRef, QName> nodeTypes = new HashMap<NodeRef, QName>();
        String query = sqlQuery.toString();
        jdbcTemplate.query(query,
                new ParameterizedRowMapper<String>() {

                    @Override
                    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                        setDefaultFetchSize(rs);
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
        List<Object> arguments = new ArrayList<Object>();
        List<List<NodeRef>> slicedNodeRefList = sliceList(nodeRefs, SORT_ALLOWED_LIMIT);
        List<NodeRef> result = new ArrayList<NodeRef>();
        for (List<NodeRef> slice : slicedNodeRefList) {
            String sql = "select node.uuid as uuid, node.store_id from " + getNodeTableConditionalJoin(slice, arguments);
            result.addAll(jdbcTemplate.query(sql, new NodeRefRowMapper(), arguments.toArray()));
            arguments.clear();
        }
        return result;
    }

    @SuppressWarnings("null")
    public static <T> List<List<T>> sliceList(List<T> list, int sliceSize) {
        Assert.isTrue(sliceSize > 0);
        if (list == null || list.isEmpty()) {
            return new ArrayList<List<T>>();
        }
        List<List<T>> slicedList = new ArrayList<List<T>>();
        int sliceElem = 0;
        List<T> slice = null;
        for (T obj : list) {
            if (sliceElem == 0) {
                slice = new ArrayList<T>();
                slicedList.add(slice);
            }
            slice.add(obj);
            if (slice.size() >= sliceSize) {
                sliceElem = 0;
            } else {
                sliceElem++;
            }
        }
        return slicedList;
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
        long startTime = System.nanoTime();
        if (childNodes == null || childNodes.isEmpty()) {
            return new HashMap<NodeRef, Map<QName, Serializable>>();
        }
        List<Object> arguments = new ArrayList<Object>();
        String sql = getParentNodeQuery(childNodes, propsToLoad, parentTypes, arguments);

        final Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> allPropsMap = executeParentNodesQuery(arguments, sql);

        Map<NodeRef, Map<QName, Serializable>> result = new HashMap<NodeRef, Map<QName, Serializable>>();
        if (propertyTypes == null) {
            propertyTypes = new HashMap<Long, QName>();
        }
        for (NodeRef childRef : childNodes) {
            Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> parentProps = allPropsMap.get(childRef);
            if (parentProps == null) {
                continue;
            }
            for (Map.Entry<NodeRef, Map<PropertyMapKey, NodePropertyValue>> innerEntry : parentProps.entrySet()) {
                Map<QName, Serializable> parentProperties = HibernateNodeDaoServiceImpl.convertToPublicProperties(innerEntry.getValue(), qnameDAO, localeDAO, contentDataDAO,
                        dictionaryService, null, propertyTypes);
                parentProperties.put(ContentModel.PROP_NODE_REF, innerEntry.getKey());
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
    public Map<NodeRef, NodeRef> getPrimaryParentRefs(Collection<NodeRef> childRefs, Set<QName> parentNodeTypes) {
        List<Object> arguments = new ArrayList<Object>();
        String sql = "SELECT node.store_id as store_id, node.uuid AS child_uuid, parent.uuid AS parent_uuid "
                + " FROM " + getNodeTableConditionalJoin(childRefs, arguments)
                + " JOIN alf_child_assoc parent_assoc on parent_assoc.child_node_id = node.id "
                + " JOIN alf_node parent on parent_assoc.parent_node_id = parent.id "
                + " WHERE parent_assoc.is_primary = true ";
        if (parentNodeTypes != null && !parentNodeTypes.isEmpty()) {
            sql += " AND parent.type_qname_id in (" + createQnameIdListing(parentNodeTypes) + ") ";
        }
        final Map<NodeRef, NodeRef> result = new HashMap<NodeRef, NodeRef>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int i) throws SQLException {
                result.put(getNodeRef(rs, "child_uuid", "store_id"), getNodeRef(rs, "parent_uuid", "store_id"));
                return null;
            }
        }, arguments.toArray());
        return result;
    }

    private String getParentNodeQuery(List<NodeRef> childNodes, Set<QName> propsToLoad, Set<QName> parentNodeTypes, List<Object> arguments) {
        String sql = "SELECT * FROM ("
                + "SELECT parent_props.*, node.store_id as store_id, node.uuid AS child_uuid, parent.uuid AS parent_uuid "
                + " FROM " + getNodeTableConditionalJoin(childNodes, arguments)
                + " JOIN alf_child_assoc parent_assoc on parent_assoc.child_node_id = node.id "
                + " JOIN alf_node parent on parent_assoc.parent_node_id = parent.id ";
        sql += " LEFT JOIN alf_node_properties parent_props on parent_props.node_id = parent.id ";

        sql += " WHERE parent_assoc.is_primary = true ";
        boolean hasPropCondition = propsToLoad != null && !propsToLoad.isEmpty();
        if (parentNodeTypes != null && !parentNodeTypes.isEmpty()) {
            sql += " AND parent.type_qname_id in (" + createQnameIdListing(parentNodeTypes) + ") ";
        }
        sql += ") temp ";
        if (hasPropCondition) {
            sql += " WHERE qname_id in (" + createQnameIdListing(propsToLoad) + ") OR qname_id is null";
        }
        return sql;
    }

    @SuppressWarnings("deprecation")
    private Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> executeParentNodesQuery(List<Object> arguments, String sql) {
        Object[] paramArray = arguments.toArray();
        final Map<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>> allPropsMap = new HashMap<NodeRef, Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>>>();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                setDefaultFetchSize(rs);
                Map<NodeRef, Map<PropertyMapKey, NodePropertyValue>> allParentProps = getPropsMap(getNodeRef(rs, "child_uuid"), allPropsMap);
                Map<PropertyMapKey, NodePropertyValue> parentProps = getPropsMap(getNodeRef(rs, "parent_uuid"), allParentProps);
                PropertyMapKey propMapKey = getPropMapKey(rs);
                if (propMapKey.getQnameId() > 0) {
                    NodePropertyValue nodePropValue = getPropertyValue(rs);
                    parentProps.put(propMapKey, nodePropValue);
                }
                return null;
            }

        }, paramArray);
        explainQuery(sql, paramArray);
        return allPropsMap;
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

                + " group by node.store_id, node.uuid ";

        Object[] paramArray = arguments.toArray();

        final Map<NodeRef, Integer> result = new HashMap<NodeRef, Integer>();

        jdbcTemplate.query(sql, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                setDefaultFetchSize(rs);
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
        // if (exceedsLimit(nodeRefs, LOG)) {
        // return null;
        // }
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
        Assert.isTrue(nodesToLoad != null && !nodesToLoad.isEmpty(), "Cannot create node query without arguments!");
        Map<StoreRef, Set<String>> nodeIdsByStoreRef = getNodeRefsByStore(nodesToLoad);
        List<String> queryParts = new ArrayList<String>();
        for (Map.Entry<StoreRef, Set<String>> entry : nodeIdsByStoreRef.entrySet()) {
            Set<String> value = entry.getValue();
            queryParts.add("(select * from alf_node where alf_node.store_id = " + getStoreRefDbId(entry.getKey()) + " and alf_node.uuid IN ("
                    + getQuestionMarks(value.size()) + ") AND " + NODE_NOT_DELETED_CONDITION + ")");
            arguments.addAll(value);
        }
        return "(" + TextUtil.joinNonBlankStrings(queryParts, " UNION ") + ") as node ";
    }

    private String getQuestionMarks(int size) {
        Assert.isTrue(size > 0, "At least one question mark should be returned.");
        String questionMarks = StringUtils.repeat("?, ", size);
        questionMarks = questionMarks.substring(0, questionMarks.length() - 2);
        return questionMarks;
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
        List<Object> arguments = new ArrayList<Object>();
        String sqlQuery = getWorkflowStateQuery(docRefs, arguments, true);
        final Map<NodeRef, String> workflowStates = new HashMap<NodeRef, String>();
        jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                setDefaultFetchSize(rs);
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
        // if (exceedsLimit(docRefs, LOG)) {
        // return null;
        // }
        List<Object> arguments = new ArrayList<Object>();
        String sqlQuery = getWorkflowStateQuery(docRefs, arguments, false);
        sqlQuery += "order by string_agg(workflow_state, ';') " + getDescSort(descending);
        List<NodeRef> orderedWorkflowStates = jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                setDefaultFetchSize(rs);
                return getNodeRef(rs);
            }

        }, arguments.toArray());
        Collection<NodeRef> missingFromOrdered = CollectionUtils.subtract(docRefs, orderedWorkflowStates);
        List<NodeRef> result;
        if (descending) {
            result = new ArrayList<NodeRef>(missingFromOrdered);
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
    public void getAssociatedDocRefs(NodeRef docRef, final Set<NodeRef> associatedDocs, Set<NodeRef> checkedDocs, final Set<NodeRef> currentAssociatedDocs,
            final Set<Integer> checkedNodes) {
        if (checkedDocs.contains(docRef) || docRef == null) {
            return;
        }
        checkedDocs.add(docRef);

        List<Object> arguments = new ArrayList<Object>();

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
                setDefaultFetchSize(rs);
                return getNodeRef(rs);
            }
        }, arguments.toArray()));

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
                setDefaultFetchSize(rs);
                return getNodeRef(rs);
            }

        }, paramArray);
        return documents;
    }

    private NodeRef getNodeRef(ResultSet rs, String nodeUuidName) throws SQLException {
        String storeIdName = "store_id";
        return getNodeRef(rs, nodeUuidName, storeIdName);
    }

    private NodeRef getNodeRef(ResultSet rs, String nodeUuidName, String storeIdName) throws SQLException {
        String nodeUuid = rs.getString(nodeUuidName);
        StoreRef storeRef = getStoreRefByDbId(rs.getLong(storeIdName));
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
        final Map<T, NodeRef> values = new HashMap<T, NodeRef>();

        final Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                setDefaultFetchSize(rs);
                PropertyMapKey propertyKey = getPropMapKey(rs);
                if (skipNodesWithMissingProp && allZeroesOrNulls(propertyKey)) {
                    return null;
                }
                NodePropertyValue propertyValue = getPropertyValue(rs);
                Map<PropertyMapKey, NodePropertyValue> rawPropValues = new HashMap<PropertyMapKey, NodePropertyValue>();
                rawPropValues.put(propertyKey, propertyValue);
                Map<QName, Serializable> props = HibernateNodeDaoServiceImpl.convertToPublicProperties(rawPropValues, qnameDAO, localeDAO, contentDataDAO,
                        dictionaryService, null, propertyTypes);
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

    @Override
    public List<NodeRef> loadNodeRefByUuid(List<String> taskUuidSlice) {
        String sql = " select uuid, store_id from alf_node node where uuid in (" + getQuestionMarks(taskUuidSlice.size()) + ")";
        List<NodeRef> nodeRefs = jdbcTemplate.query(sql, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                return getNodeRef(rs);
            }
        }, taskUuidSlice.toArray());
        return nodeRefs;
    }

    private boolean allZeroesOrNulls(PropertyMapKey key) {
        return (key.getListIndex() == null || key.getListIndex() == 0)
                && (key.getLocaleId() == null || key.getLocaleId() == 0)
                && (key.getQnameId() == null || key.getQnameId() == 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Set<NodeRef> loadChildRefs(NodeRef parentRef, final QName propName, String requiredValue, QName type) {
        final Set<NodeRef> result = new HashSet<NodeRef>();
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
                setDefaultFetchSize(rs);
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

    private String getDbFieldNameFromCamelCase(String localName) {
        return getDbFieldNameFromCamelCase(null, localName);
    }

    private String getDbFieldNameFromCamelCase(String prefix, String localName) {
        Assert.isTrue(StringUtils.isNotBlank(localName));
        StringBuilder sb = new StringBuilder(StringUtils.isNotBlank(prefix) ? (prefix + "_") : "");
        for (int i = 0; i < localName.length(); i++) {
            char c = localName.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append("_" + Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
                setDefaultFetchSize(rs);
                PropertyMapKey propertyKey = getPropMapKey(rs);
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
                setDefaultFetchSize(rs);
                qnameDAO.getQName(rs.getLong("id"));
                return null;
            }
        });
    }

    private final class NodeRefRowMapper implements ParameterizedRowMapper<NodeRef> {
        @Override
        public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
            setDefaultFetchSize(rs);
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

}
