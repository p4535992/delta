package ee.webmedia.alfresco.common.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;

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

    ChildAssociationRef getLastChildAssocRef(String nodeRefXPath);

    /**
     * Returns the full name of the user.
     * 
     * @param userName
     * @return
     */
    String getPersonFullNameByUserName(String userName);

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
     */
    void setPropertiesIgnoringSystem(NodeRef nodeRef, Map<String, Object> nodeProps);

    /**
     * @return return and remove value from request map that was put there by
     *         {@link WMUIProperty#saveExistingValue4ComponentGenerator(FacesContext, Node, String)} for component generators to be able to use existing value
     */
    String getExistingRepoValue4ComponentGenerator();

    /**
     * Returns a map with the user's properties.
     * @param userName
     * @return
     */
    Map<QName, Serializable> getPersonProperties(String userName);
    
    /**
     * Puts two names together into one string with a separator between.
     * @param first
     * @param last
     * @return
     */
    String getFirstAndLastNames(String first, String last, String sep);

    /**
     * @param childRef - child of the parent being searched for
     * @param parentType - type that parent of given childRef is expected to have
     * @return dorect primary parent node if it has givent parentType, null otherwise
     */
    Node getParentWithType(NodeRef childRef, QName parentType);
}
