package ee.webmedia.alfresco.addressbook.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

/**
 * @author Keit Tehvan
 */
public interface AddressbookService {

    String BEAN_NAME = "AddressbookService";

    /**
     * If current user has necessary permissions to manage addressbook (is an admin or belongs to ADDRESSBOOK group)
     */
    boolean hasManagePermission();

    /**
     * @return list of nodes of type ab:organization
     */
    List<Node> listOrganization();

    /**
     * updates a node
     * 
     * @param node - the node with new properties to update
     */
    void updateNode(Node node);

    /**
     * deletes a node
     * 
     * @param nodeRef - the nodeRef of the node to delete
     */
    void deleteNode(NodeRef nodeRef);

    /**
     * @return list of nodes of type ab:privPerson
     */
    List<Node> listPerson();

    /**
     * @param organization - the organization nodeRef whose underlings to show
     * @return list of nodes of type ab:orgPerson
     */
    List<Node> listPerson(NodeRef organization);

    /**
     * creates a new node in the repository,
     * if the node has the nodeRef of an existing node, then calls updateNode(node);
     * 
     * @param node - the transient node with set properties to create
     * @param parent - the parent nodeRef of the node to be created, can be null
     * @return the nodeRef of the newly created node
     */
    NodeRef addOrUpdateNode(Node node, NodeRef parent);

    /**
     * returns a node from the repository
     * 
     * @param node the nodeRef of the node to be retrieved
     * @return the node
     */
    Node getNode(NodeRef node);

    /**
     * returns an empty node of a certain type
     * 
     * @param type the type of the empty node
     * @return a non-existing TransientNode with no connection to the repository
     */
    Node getEmptyNode(QName type);

    /**
     * searches the addressbook
     * 
     * @param searchCriteria - the search string
     * @return list of node results
     */
    List<Node> search(String searchCriteria);

    /**
     * returns the parent of the given node
     * 
     * @param ref - person node ref
     * @return org node ref
     */
    NodeRef getOrgOfPerson(NodeRef ref);

}
