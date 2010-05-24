package ee.webmedia.alfresco.common.service;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.web.WmNode;

/**
 * @author Ats Uiboupin
 * @author Alar Kvell
 */
public interface GeneralService {
    String BEAN_NAME = "GeneralService";

    StoreRef getStore();

    /**
     * Search for NodeRef with an XPath expression.
     * 
     * @param nodeRefXPath child association names separated with forward slashes, in the form of <code>/foo:bar/{pingNamespaceUri}pong</code>
     * @return NodeRef, {@code null} if node not found
     * @throws RuntimeException if more than 1 node found
     */
    NodeRef getNodeRef(String nodeRefXPath);

    /**
     * Search for NodeRef with an XPath expression from given store.
     * 
     * @param nodeRefXPath child association names separated with forward slashes, in the form of <code>/foo:bar/{pingNamespaceUri}pong</code>
     * @param storeRef Reference to store
     * @return NodeRef, {@code null} if node not found
     * @throws RuntimeException if more than 1 node found
     */
    NodeRef getNodeRef(String nodeRefXPath, StoreRef storeRef);

    ChildAssociationRef getLastChildAssocRef(String nodeRefXPath);

    /**
     * Search for nodes by property values. Search results are limited by default limit.
     * 
     * @see #searchNodes(String, QName, Set, int)
     */
    List<NodeRef> searchNodes(String input, QName type, Set<QName> props);

    /**
     * Search for nodes by property values. All special characters are ignored in input string. If input string is too short, then search is not
     * performed, and {@code null} is returned to distinguish this condition from empty result. Input string is tokenized by space and search query is
     * constructed so that all tokens must be present in any node property (AND search).
     * 
     * @param input search string
     * @param type node type, may be {@code null}
     * @param props node properties that are searched
     * @param limit limit the total number of search results returned after pruning by permissions
     * @return search results or {@code null} if search was not performed
     */
    List<NodeRef> searchNodes(String input, QName type, Set<QName> props, int limit);

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
     */
    void setPropertiesIgnoringSystem(Map<QName, Serializable> properties, NodeRef nodeRef);

    Map<QName, Serializable> getPropertiesIgnoringSystem(Map<String, Object> nodeProps);

    /** the same as {@link #getPropertiesIgnoringSystem(Map)}, but different generic types */
    Map<QName, Serializable> getPropertiesIgnoringSys(Map<QName, Serializable> nodeProps);

    /**
     * For each property value of {@code FileWithContentType} class, save file content to repository and replace property value with {@link ContentData} object.
     */
    void savePropertiesFiles(Map<QName, Serializable> props);

    /**
     * @return return and remove value from request map that was put there by
     *         {@link WMUIProperty#saveExistingValue4ComponentGenerator(FacesContext, Node, String)} for component generators to be able to use existing value
     */
    String getExistingRepoValue4ComponentGenerator();

    /**
     * @param childRef - child of the parent being searched for
     * @param parentType - type that parent of given childRef is expected to have
     * @return dorect primary parent node if it has givent parentType, null otherwise
     */
    Node getParentWithType(NodeRef childRef, QName parentType);

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

    void saveAddedAssocs(Node node);

    /**
     * Remove all child association removed in the UI
     * 
     * @param node - node that previously had some childAssociations in repository that should be removed
     */
    public void saveRemovedChildAssocs(Node node);

    /**
     * @param nodeRef
     * @return node according to nodeRef from repo, filling properties and aspects
     */
    Node fetchNode(NodeRef nodeRef);

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
     * Zip-s up those files which are attached to given document, and where the id is in fileIds list.
     * 
     * @param document document node ref
     * @param fileNodeRefs selected file nodeRefs as strings (from all the files assosiated to this given document).
     * @return ByteArrayOutputStream with zip file bytes
     */
    ByteArrayOutputStream getZipFileFromFiles(NodeRef document, List<String> fileNodeRefs);

    String getUniqueFileName(NodeRef folder, String fileName);

    NodeRef getAncestorNodeRefWithType(NodeRef childRef, QName ancestorType);
    
    /**
     * Updates parent node containingDocsCount property
     *
     * @param parentNodeRef parent to update
     * @param propertyName property name to update
     * @param added should we increase or decrease
     * @param count how many docs were added/removed
     */
    void updateParentContainingDocsCount(NodeRef parentNodeRef, QName propertyName, boolean added, Integer count);
}
