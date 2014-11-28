package ee.webmedia.alfresco.common.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

/**
 * This is limited version of BulkLoadNodeService backported from Delta 5.1 version
 * and should not be integrated to higher branches than current 3.6 branch.
 */
@Deprecated
public interface BulkLoadNodeService {

    String BEAN_NAME = "BulkLoadNodeService";
    String NON_TX_BEAN_NAME = "bulkLoadNodeService";

    List<NodeRef> loadChildDocNodeRefs(NodeRef parentNodeRef);

    <T extends Serializable> Map<T, NodeRef> loadChildElementsNodeRefs(NodeRef parentNodeRef, QName propName, QName... type);

    <T extends Serializable> Map<T, NodeRef> loadChildElementsNodeRefs(NodeRef parentNodeRef, final QName propName, boolean skipNodesWithMissingProp, QName... type);

    List<String> loadChildSearchableDocFieldNodeRefs(NodeRef fieldDefinitionsRoot);

    List<String> loadChildSearchableVolFieldNodeRefs(NodeRef fieldDefinitionsRoot);

    List<NodeRef> orderByDocumentCaseLabel(List<NodeRef> nodeRefs, boolean descending);

    List<NodeRef> orderByDocumentVolumeLabel(List<NodeRef> nodeRefs, boolean descending);

    List<NodeRef> orderByDocumentSeriesLabel(List<NodeRef> nodeRefs, boolean descending);

    List<NodeRef> orderByDocumentFunctionLabel(List<NodeRef> nodeRefs, boolean descending);

    Map<NodeRef, String> loadDocumentWorkflowStates(List<NodeRef> docRefs);

    List<NodeRef> orderByDocumentWorkflowStates(List<NodeRef> docRefs, boolean descending);

    Map<NodeRef, Node> loadNodes(List<NodeRef> nodesToLoad, Set<QName> propsToLoad);

    Set<NodeRef> loadChildRefs(NodeRef parentRef, final QName propName, String value, QName type);

    <T> Map<NodeRef, List<T>> loadChildNodes(List<NodeRef> parentNodes, Set<QName> propsToLoad, QName childNodeType, Map<Long, QName> propertyTypes,
            CreateObjectCallback<T> createObjectCallback);

    Map<NodeRef, Map<NodeRef, Map<QName, Serializable>>> loadChildNodes(List<NodeRef> parentNodes, Set<QName> propsToLoad);

    void getAssociatedDocRefs(NodeRef docRef, final Set<NodeRef> associatedDocs, Set<NodeRef> checkedDocs, final Set<NodeRef> currentAssociatedDocs, Set<Integer> checkedNodes);

    Map<NodeRef, Map<QName, Serializable>> loadPrimaryParentsProperties(List<NodeRef> taskRefs, Set<QName> parentType, Set<QName> propsToLoad, Map<Long, QName> propertyTypes);

    Map<NodeRef, Integer> getSearchableChildDocCounts(List<NodeRef> indpendentCompoundWorkflows);

    Map<NodeRef, Node> loadNodes(List<NodeRef> workflowRefs, Set<QName> singleton, Map<Long, QName> propertyTypes);

    /** Loads all given nodeRefs (can be used for checking if nodeRef returned from lucene search is present in database) */
    List<NodeRef> loadNodeRefs(List<NodeRef> nodeRefs);

    void fillQNameCache();

    int countChildNodes(NodeRef parentRef, QName childNodeType);

    Map<NodeRef, Integer> countChildNodes(List<NodeRef> parentRefs, QName childNodeType);

    boolean hasChildNodeOfType(NodeRef parentRef, QName childNodeType);

    /**
     * Note that this method may return null in case given qname is not present in db.
     * Currently this leads to expression in form "field = null", which, according to specification,
     * should always equal to false (even in case field value is actually null).
     * At present, this is desired behviour (if qname is not present in db, it means that it hasn't been used, so it cannot be present on node anyway)
     **/
    Long getQNameDbId(QName qname);

    Long getStoreRefDbId(StoreRef storeRef);

    String getNodeTableConditionalJoin(Collection<NodeRef> nodesToLoad, List<Object> arguments);

    StoreRef getStoreRefByDbId(Long dbId);

    Map<NodeRef, Integer> getSearchableTargetAssocsCount(List<NodeRef> sourceRefs, QName assocType);

    Map<NodeRef, List<NodeRef>> getSourceAssocs(List<NodeRef> targetRefs, QName assocType);

    Map<NodeRef, NodeRef> getPrimaryParentRefs(Collection<NodeRef> childRefs, Set<QName> parentNodeTypes);

    List<NodeRef> loadTaskRefByUuid(List<String> taskUuidSlice);

}
