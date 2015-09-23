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

import ee.webmedia.alfresco.document.file.model.SimpleFile;
import ee.webmedia.alfresco.document.file.model.SimpleFileWithOrder;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.substitute.model.Substitute;

public interface BulkLoadNodeService {

    String BEAN_NAME = "BulkLoadNodeService";
    String NON_TX_BEAN_NAME = "bulkLoadNodeService";

    Map<NodeRef, Document> loadDocuments(List<NodeRef> documentsToLoad, Set<QName> propsToLoad);

    Map<NodeRef, Document> loadDocumentsLimitingProperties(List<NodeRef> documentsToLoad, Set<QName> propsNotToLoad);

    List<NodeRef> loadChildDocNodeRefs(NodeRef parentNodeRef);

    <T extends Serializable> Map<T, NodeRef> loadChildElementsNodeRefs(NodeRef parentNodeRef, QName propName, QName... type);

    <T extends Serializable> Map<T, NodeRef> loadChildElementsNodeRefs(NodeRef parentNodeRef, final QName propName, boolean skipNodesWithMissingProp, QName... type);

    List<String> loadChildSearchableDocFieldNodeRefs(NodeRef fieldDefinitionsRoot);

    List<String> loadChildSearchableVolFieldNodeRefs(NodeRef fieldDefinitionsRoot);

    List<NodeRef> orderByDocumentCaseLabel(List<NodeRef> nodeRefs, boolean descending);

    List<NodeRef> orderByDocumentVolumeLabel(List<NodeRef> nodeRefs, boolean descending);

    List<NodeRef> orderByDocumentSeriesLabel(List<NodeRef> nodeRefs, boolean descending);

    List<NodeRef> orderByDocumentFunctionLabel(List<NodeRef> nodeRefs, boolean descending);

    Map<NodeRef, Document> loadDocumentsOrCases(List<NodeRef> documentsToLoad, Set<QName> propsToLoad);

    Map<NodeRef, String> loadDocumentWorkflowStates(List<NodeRef> docRefs);

    List<NodeRef> orderByDocumentWorkflowStates(List<NodeRef> docRefs, boolean descending);

    Map<NodeRef, Node> loadNodes(Collection<NodeRef> nodesToLoad, Set<QName> propsToLoad);

    List<NodeRef> loadChildRefs(NodeRef parentRef, QName type);

    Set<NodeRef> loadChildRefs(NodeRef parentRef, final QName propName, String value, QName type);

    <T> List<T> loadChildNodes(NodeRef parentNodeRef, Set<QName> propsToLoad, QName childNodeType, Map<Long, QName> propertyTypes,
            CreateObjectCallback<T> createObjectCallback);

    <T> Map<NodeRef, List<T>> loadChildNodes(Collection<NodeRef> parentNodes, Set<QName> propsToLoad, QName childNodeType, Map<Long, QName> propertyTypes,
            CreateObjectCallback<T> createObjectCallback);

    List<SimpleFile> loadActiveFiles(NodeRef nodeRef, Map<Long, QName> propertyTypes);

    <T extends SimpleFile> List<T> loadActiveFiles(NodeRef nodeRef, Map<Long, QName> propertyTypes, Set<QName> propsToLoad, CreateSimpleFileCallback<T> createFileCallback);

    List<SimpleFileWithOrder> loadInactiveFilesWithOrder(NodeRef parentRef);

    List<SimpleFileWithOrder> loadActiveFilesWithOrder(NodeRef parentRef);

    int countFiles(NodeRef parentNodeRef, Boolean active);

    List<SimpleFile> loadAllFiles(NodeRef parentRef);

    Map<NodeRef, List<SimpleFile>> loadActiveFiles(List<NodeRef> parentNodeRefs, Map<Long, QName> propertyTypes);

    Map<NodeRef, Map<NodeRef, Map<QName, Serializable>>> loadChildNodes(Collection<NodeRef> parentNodes, Set<QName> propsToLoad);

    Set<NodeRef> getAssociatedDocRefs(NodeRef docRef);

    Map<NodeRef, Map<QName, Serializable>> loadPrimaryParentsProperties(List<NodeRef> taskRefs, Set<QName> parentType, Set<QName> propsToLoad, Map<Long, QName> propertyTypes);

    Map<NodeRef, Map<QName, Serializable>> loadPrimaryParentsProperties(List<NodeRef> taskRefs, Set<QName> parentType, Set<QName> propsToLoad, Map<Long, QName> propertyTypes,
            boolean includeIntrinsicProps);

    Set<NodeRef> loadPrimaryParentNodeRefs(Set<NodeRef> childNodes, Set<QName> parentNodeTypes);

    Map<NodeRef, Integer> getSearchableChildDocCounts(List<NodeRef> indpendentCompoundWorkflows);

    Map<NodeRef, Node> loadNodes(Collection<NodeRef> workflowRefs, Set<QName> singleton, Map<Long, QName> propertyTypes);

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

    /** AVOID using this method unless the results are stored in cache */
    List<Substitute> loadUserSubstitutionDuties(String personName, NodeRef peopleContainer);

    Boolean getSubscriptionPropValue(NodeRef personRef, QName notificationType);

    List<Node> loadAssociatedTargetNodes(NodeRef parentRef, Set<QName> propsToLoad, QName assocType, CreateObjectCallback<Node> createNodeCallback);

}
