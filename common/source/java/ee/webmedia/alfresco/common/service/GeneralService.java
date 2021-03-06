package ee.webmedia.alfresco.common.service;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.logging.Log;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.log.service.DocumentPropertiesChangeHolder;
import ee.webmedia.alfresco.utils.AdjustableSemaphore;

public interface GeneralService {
    String BEAN_NAME = "GeneralService";

    /**
     * @return default store (where unarchived data is stored)
     */
    StoreRef getStore();

    void setArchivalsStoreVOs(LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs);

    /**
     * If you call this method from a bootstrap / module component, then you must use dependsOn = archivalsStoresBootstrap.
     *
     * @return
     */
    LinkedHashSet<ArchivalsStoreVO> getArchivalsStoreVOs();

    /**
     * If you call this method from a bootstrap / module component, then you must use dependsOn = archivalsStoresBootstrap.
     *
     * @return
     */
    LinkedHashSet<StoreRef> getAllWithArchivalsStoreRefs();

    /**
     * Return all storeRefs returned from getAllWithArchivalsStoreRefs call (active store + archived documents' stores)
     * and storeRef where deleted documents are stored
     */
    LinkedHashSet<StoreRef> getAllStoreRefsWithTrashCan();

    /**
     * @return store where archived documents are stored
     */
    StoreRef getArchivalsStoreRef();

    NodeRef getPrimaryArchivalsNodeRef();

    /**
     * Search for NodeRef with an XPath expression.
     *
     * @param nodeRefXPath child association names separated with forward slashes, in the form of <code>/foo:bar/{pingNamespaceUri}pong</code>
     * @return NodeRef, {@code null} if node not found
     * @throws RuntimeException if more than 1 node found
     */
    NodeRef getNodeRef(String nodeRefXPath);

    NodeRef getArchivalNodeRef(String nodeRefXPath);

    /**
     * Search for NodeRef with an XPath expression from given store root.
     *
     * @param nodeRefXPath child association names separated with forward slashes, in the form of <code>/foo:bar/{pingNamespaceUri}pong</code>
     * @param storeRef Reference to store
     * @return NodeRef, {@code null} if node not found
     * @throws RuntimeException if more than 1 node found
     */
    NodeRef getNodeRef(String nodeRefXPath, StoreRef storeRef);

    /**
     * Search for NodeRef with an XPath expression from given root.
     *
     * @param nodeRefXPath child association names separated with forward slashes, in the form of <code>/foo:bar/{pingNamespaceUri}pong</code>
     * @param root reference to root node, under which is searched
     * @return NodeRef, {@code null} if node not found
     * @throws RuntimeException if more than 1 node found
     */
    NodeRef getNodeRef(String nodeRefXPath, NodeRef root);

    NodeRef getChildByAssocName(NodeRef parentRef, QNamePattern assocNamePattern);

    ChildAssociationRef getLastChildAssocRef(String nodeRefXPath);

    /**
     * Sets nodeProps to given nodeRef excluding system and contentModel properties
     *
     * @param nodeRef
     * @param nodeProps
     * @see #setPropertiesIgnoringSystem(Map, NodeRef)
     */
    void setPropertiesIgnoringSystem(NodeRef nodeRef, Map<String, Object> nodeProps);

    /**
     * @param properties
     * @param nodeRef
     * @see #setPropertiesIgnoringSystem(NodeRef, Map)
     * @return properties without system properties
     */
    Map<QName, Serializable> setPropertiesIgnoringSystem(Map<QName, Serializable> properties, NodeRef nodeRef);

    Map<QName, Serializable> getPropertiesIgnoringSystem(Map<String, Object> nodeProps);

    /** the same as {@link #getPropertiesIgnoringSystem(Map)}, but different generic types */
    Map<QName, Serializable> getPropertiesIgnoringSys(Map<QName, Serializable> nodeProps);

    /**
     * For each property value of {@code FileWithContentType} class, save file content to repository and replace property value with {@link ContentData} object.
     */
    void savePropertiesFiles(Map<QName, Serializable> props);

    /**
     * @return return and remove value from request map that was put there by {@link WMUIProperty#saveExistingValue4ComponentGenerator(FacesContext, Node, String)} for component
     *         generators to be able to use existing value
     */
    String getExistingRepoValue4ComponentGenerator();

    /**
     * @param childRef - child of the parent being searched for
     * @param parentType - type that parent of given childRef is expected to have
     * @return dorect primary parent node if it has givent parentType, null otherwise
     */
    Node getParentWithType(NodeRef childRef, QName... parentType);

    Node getPrimaryParent(NodeRef nodeRef);

    /**
     * Return default property values defined in model.
     *
     * @param className type or aspect
     * @return
     */
    Map<QName, Serializable> getDefaultProperties(QName className);

    LinkedHashSet<QName> getDefaultAspects(QName className);

    /**
     * Construct an anonymous type that combines all definitions of the specified type, aspects and mandatory aspects
     *
     * @param type the type to start with
     * @return the anonymous type definition
     */
    TypeDefinition getAnonymousType(QName type);

    TypeDefinition getAnonymousType(Node node);

    /**
     * Add and remove aspects in repository according to node aspects.
     *
     * @param node
     * @return if node was modified in repository
     */
    boolean setAspectsIgnoringSystem(Node node);

    boolean setAspectsIgnoringSystem(NodeRef nodeRef, Set<QName> nodeAspects);

    /**
     * @param node
     * @return number of new associations created to repository
     */
    int saveAddedAssocs(Node node);

    /**
     * Remove all child association removed in the UI
     *
     * @param node - node that previously had some childAssociations in repository that should be removed
     * @return number of associations that were deleted from repository
     */
    public void saveRemovedChildAssocs(Node node, DocumentPropertiesChangeHolder docPropsChangeHolder);

    /**
     * @param nodeRef
     * @return node according to nodeRef from repo, filling properties and aspects
     */
    Node fetchNode(NodeRef nodeRef);

    /**
     * Fetches WmNode from repository
     *
     * @param objectRef NodeRef of the desired object
     * @param objectType Type of the object
     * @return WmNode representation of the repository object
     */
    WmNode fetchObjectNode(NodeRef objectRef, QName objectType);

    /**
     * @param type - node type
     * @param props - initial properties or null
     * @return new WmNode with given type and properties (aspects are set based on model)
     */
    WmNode createNewUnSaved(QName type, Map<QName, Serializable> props);

    /**
     * Searches ancestor with specified type. Will go up in hierarchy until found.
     *
     * @param childRef
     * @param ancestorType
     * @return return ancestor node or null if none found
     */
    Node getAncestorWithType(NodeRef childRef, QName ancestorType);

    /**
     * Zip-s up given files.
     *
     * @param output
     * @param fileRefs selected file nodeRefs.
     */
    long writeZipFileFromFiles(OutputStream output, List<NodeRef> fileRefs);

    long writeZipFileFromStream(OutputStream output, String fileName, InputStream in);

    String getUniqueFileName(NodeRef folder, String fileName);

    NodeRef getAncestorNodeRefWithType(NodeRef childRef, QName ancestorType);

    /**
     * @param childRef
     * @param ancestorType
     * @param checkSubTypes - if true dictionaryService is used to determine if ancestor is subType of expected <code>ancestorType</code>
     * @return nodeRef of the first parent of childRef that has type <code>ancestorType</code>
     */
    public NodeRef getAncestorNodeRefWithType(NodeRef childRef, QName ancestorType, boolean checkSubTypes);

    /**
     * @param nodeRef
     * @param ancestorType
     * @param checkSubTypes - if true dictionaryService is used to determine if ancestor is subType of expected <code>ancestorType</code>
     * @param startFromParent - if false, then nodeRef is returned if nodeRef type matches ancestorType
     * @return nodeRef of the first parent of childRef that has type <code>ancestorType</code>
     */
    NodeRef getAncestorNodeRefWithType(NodeRef nodeRef, QName ancestorType, boolean checkSubTypes, boolean startFromParent);

    /**
     * Updates parent node containingDocsCount property
     * If {@code added} is null, then {@code count} is added to current value (negative or positive);
     *
     * @param parentNodeRef parent to update
     * @param propertyName property name to update
     * @param added should we increase or decrease
     * @param count how many docs were added/removed
     */
    void updateParentContainingDocsCount(NodeRef parentNodeRef, QName propertyName, Boolean added, Integer count);

    /**
     * Sets up the writer (mimetype, encoding) and writes contents of the file
     *
     * @param writer ContentWriter where to write
     * @param file file from where content is read
     * @param fileName full file name with extension
     * @param mimetype
     * @return
     */
    void writeFile(ContentWriter writer, File file, String fileName, String mimetype);

    Pair<String, String> getMimetypeAndEncoding(InputStream is, String fileName, String mimetype);

    NodeRef addFileOrFolder(File file, NodeRef parentNodeRef, boolean flatten);

    NodeRef addFile(File file, NodeRef parentNodeRef);

    void deleteNodeRefs(Collection<NodeRef> nodeRefs, boolean moveToTrashCan);

    NodeRef getParentNodeRefWithType(NodeRef childRef, QName... parentType);

    /**
     * Perform work in a background thread. Background thread is created and started after current transaction commit successfully completes; if current transaction is rolled back,
     * then work never executes. Work is executed as System user and in a separate transaction ({@link RetryingTransactionHelper} is used).
     *
     * @param work work to execute in a background thread after current transaction commit completes
     * @param threadName name to give the new thread that is created for executing work
     */
    void runOnBackground(final RunAsWork<Void> work, final String threadName, boolean createTransaction);

    void runOnBackground(RunAsWork<Void> work, String threadNamePrefix, boolean createTransaction, RunAsWork<Void> workAfterCommit);

    /** Return true if storeRef is primary or additional archivals storeRef */
    boolean isArchivalsStoreRef(StoreRef storeRef);

    NodeRef getExistingNodeRefAllStores(String id);

    /** Run semaphoreCallback code quarded by given semaphore */
    <T> T runSemaphored(AdjustableSemaphore adjustableSemaphore, ExecuteCallback<T> searchCallback);

    LinkedHashSet<StoreRef> getArchivalsStoreRefs();

    String getUniqueFileName(String fileName, List<NodeRef> filesToCheck, NodeRef... parentRefs);

    String getTsquery(String input);

    String getTsquery(String input, int minLexemLength);

    String getStoreTitle(StoreRef storeRef);

    boolean hasAdditionalArchivalStores();

    void explainQuery(String sqlQuery, Log log, Object... args);

    NodeRef deleteBootstrapNodeRef(String moduleName, String bootstrapName);

    /**
     * Set node modified time to current time. Use this on document, so changes would be detected on next ADR sync.
     */
    void setModifiedToNow(NodeRef nodeRef);

    void explainAnalyzeQuery(String sqlQuery, Log traceLog, boolean analyze, Object[] args);

    void refreshMaterializedViews(QName... nodeTypeQName);

    <T> List<T> fetchObjectsForChildAssocs(Collection<ChildAssociationRef> childAssocs, Set<QName> propsToLoad, NodeBasedObjectCallback<T> callback);

    <T> List<T> fetchObjects(Collection<NodeRef> nodeRefs, Set<QName> propsToLoad, NodeBasedObjectCallback<T> callback);

    <T> T fetchObject(NodeRef nodeRef, Set<QName> propsToLoad, NodeBasedObjectCallback<T> callback, Map<Long, QName> propertryTypes);

    Node fetchNode(NodeRef nodeRef, Map<Long, QName> propertyTypes);

    WmNode fetchObjectNode(NodeRef objectRef, QName objectType, Map<Long, QName> propertyTypes);
    
    void runBeforeCommit(final RunAsWork<Void> work);

}
