package ee.webmedia.alfresco.addressbook.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.addressbook.web.dialog.ContactGroupAddDialog.UserDetails;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public interface AddressbookService {

    String BEAN_NAME = "AddressbookService";

    String ADDRESSBOOK_GROUP = "ADDRESSBOOK";

    /**
     * If current user has necessary permissions to manage addressbook (is an admin or belongs to ADDRESSBOOK group)
     */
    boolean hasManagePermission();

    /**
     * @return list of nodes of type ab:organization and ab:privPerson
     */
    List<Node> listOrganizationAndPerson();

    /**
     * @return list of nodes of type ab:organization
     */
    List<Node> listOrganization();

    List<AddressbookEntry> listAddressbookEntries(QName type);

    List<AddressbookEntry> listOrganizationPeople(NodeRef organizationRef);
    
    List<AddressbookEntry> listOrganizationCertificates(final NodeRef organizationRef);

    /**
     * @return list of nodes of type ab:contactGroups
     */
    List<Node> listContactGroups();

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
     * @param node
     * @return List of message keys and value holders for duplicate contacts
     */
    List<Pair<String, String>> checkIfContactExists(Node node) throws UnableToPerformException;

    /**
     * creates a new node in the repository,
     * if the node has the nodeRef of an existing node, then calls updateNode(node);
     * 
     * @param node - the transient node with set properties to create
     * @param parent - the parent nodeRef of the node to be created, can be null
     * @return the nodeRef of the newly created node
     * @throws UnableToPerformException when contact group name exists
     */
    NodeRef addOrUpdateNode(Node node, NodeRef parent);

    /**
     * Add a member to the group by creating an association from groupNodeRef to memberNodeRef.
     * 
     * @param groupNodeRef
     * @param usersForGroup
     * @return feedback about items that already existed in the group
     */
    MessageDataWrapper addToGroup(NodeRef groupNodeRef, List<UserDetails> usersForGroup, boolean createAddedToGroupInfo);

    /**
     * Remove a member from the group by deleting the association from groupNodeRef to memberNodeRef.
     * 
     * @param groupNodeRef
     * @param memberNodeRef
     */
    void deleteFromGroup(NodeRef groupNodeRef, NodeRef memberNodeRef);

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
     * searches the addressbook for private contacts, organizations and organization contacts
     * 
     * @param searchCriteria - the search string
     * @param limit
     * @return list of node results
     */
    List<Node> search(String searchCriteria, int limit);

    /**
     * searches the addressbook for private contacts, organizations and organization contacts
     * 
     * @param searchCriteria - the search string
     * @param limit
     * @param onlyActive returns inactive contacts when false
     * @return list of node results
     */
    List<Node> search(String searchCriteria, int i, boolean onlyActive);

    /**
     * Searches the addressbook for contact groups only
     * 
     * @param searchCriteria - the search string
     * @param limit
     * @return list of node results
     */
    List<Node> searchTaskCapableContactGroups(String searchCriteria, boolean orgOnly, boolean dvkCapableOnly, String institutionToRemove, int limit);

    /**
     * returns the parent of the given node
     * 
     * @param ref - person node ref
     * @return org node ref
     */
    NodeRef getOrgOfPerson(NodeRef ref);

    /**
     * Returns a list of nodes of a given type that nodeRef if associated with.
     * 
     * @param type
     * @param nodeRef
     * @return
     */
    List<Node> getContactsByType(QName type, NodeRef nodeRef);

    /**
     * Returns a list of all nodeRef's associations.
     * 
     * @param nodeRef
     * @return
     */
    List<Node> getContacts(NodeRef nodeRef);

    List<Node> searchTaskCapableContacts(String searchCriteria, boolean orgOnly, boolean dvkCapableOnly, String institutionToRemove, int limit);

    List<Node> searchDecDocumentForwardCapableContacts(String searchCriteria, int limit);

    List<Node> getDvkCapableOrgs();

    List<String> getDvkCapableOrgNames();

    /**
     * Find organization nodeRef based on e-mail and organization name
     */
    NodeRef getOrganizationNodeRef(String orgEmail, String orgName);

    List<Node> searchContactGroups(String searchCriteria, boolean showAdminManageable, boolean excludeTaskCapable, int limit);

    /**
     * @param regNumber
     * @return contacts of type ORGANIZATION where ORGANIZATION_CODE = regNumber
     *         and contacts of type PRIV_PERSON where PERSON_ID = regNumber
     */
    List<Node> getContactsByRegNumber(String regNumber);

    List<Node> searchPersonContacts(String searchCriteria, int limit);

    List<Node> getPersonContactsByCode(String code);

    List<Node> searchOrgContacts(String searchCriteria, int limit);

    boolean isTaskCapableGroupMember(NodeRef contactRef);

    NodeRef createOrganization(Map<QName, Serializable> data);

    List<Node> getContactsWithSapAccount();

    List<Node> getContactsGroups(NodeRef contactNodeRef);

    boolean hasCreatePermission();

    List<NodeRef> getContactGroupContents(NodeRef contactGroupRef);

    List<Pair<String, String>> searchTaskCapableContacts(String param, int limit);

    List<Pair<String, String>> searchTaskCapableContactGroups(String param, int limit);

    List<Pair<String, String>> listAllGroupMembers(NodeRef groupRef);
}
