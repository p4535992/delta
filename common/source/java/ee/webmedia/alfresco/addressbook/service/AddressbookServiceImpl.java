package ee.webmedia.alfresco.addressbook.service;

import static ee.webmedia.alfresco.addressbook.model.AddressbookModel.URI;
import static ee.webmedia.alfresco.addressbook.util.AddressbookUtil.getContactFullName;
import static ee.webmedia.alfresco.utils.RepoUtil.toQNameProperties;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNotNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Assocs;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Props;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookAddEditDialog;
import ee.webmedia.alfresco.addressbook.web.dialog.ContactGroupAddDialog.UserDetails;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.search.service.AbstractSearchServiceImpl;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * @author Keit Tehvan 29.09.2009
 */
public class AddressbookServiceImpl extends AbstractSearchServiceImpl implements AddressbookService {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AddressbookServiceImpl.class);

    private NamespaceService namespaceService;
    private AuthorityService authorityService;
    private PermissionService permissionService;

    private Set<QName> allContactTypes; // doesn't need to be synchronized, because it is not modified during runtime
    private Set<QName> searchFields; // doesn't need to be synchronized, because it is not modified during runtime
    private Set<QName> contactGroupSearchFields; // doesn't need to be synchronized, because it is not modified during runtime
    private StoreRef store;
    private NodeRef addressbookRoot;

    // ---------- interface methods

    @Override
    public boolean hasManagePermission() {
        Set<String> auths = authorityService.getAuthorities();
        return auths.contains(PermissionService.ADMINISTRATOR_AUTHORITY) || auths.contains(authorityService.getName(AuthorityType.GROUP, ADDRESSBOOK_GROUP));
    }

    @Override
    public List<Node> listOrganizationAndPerson() {
        return listAddressbookChildren(RegexQNamePattern.MATCH_ALL);
    }

    @Override
    public List<Node> listOrganization() {
        return listAddressbookChildren(AddressbookModel.Assocs.ORGANIZATIONS);
    }

    @Override
    public List<AddressbookEntry> listAddressbookEntries(QName type) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(getAddressbookRoot(), type, RegexQNamePattern.MATCH_ALL);
        List<AddressbookEntry> addressbooks = new ArrayList<AddressbookEntry>(childRefs.size());
        for (ChildAssociationRef ref : childRefs) {
            try {
                addressbooks.add(new AddressbookEntry(getNode(ref.getChildRef())));
            } catch (InvalidNodeRefException e) {
                // This can happen if two users modify the addressbook simultaneously and one of them deletes a contact/organization
                // We can ignore this situation since the node doesn't exist any more and therefore it can be excluded from the listing.
                LOG.error("Unable to get addressbook entrys properties with type " + type, e);
                continue;
            }
        }
        return addressbooks;
    }

    @Override
    public List<Node> listContactGroups() {
        return listAddressbookChildren(AddressbookModel.Assocs.CONTACT_GROUPS);
    }

    @Override
    public NodeRef addOrUpdateNode(Node node, NodeRef parent) {
        final NodeRef nodeRef = node.getNodeRef();
        if (nodeService.exists(nodeRef)) {
            updateNode(node);
            return nodeRef;
        }
        NodeRef output = null;
        if (node.getType().equals(Types.ORGANIZATION)) {
            output = createOrganization(toQNameProperties(node.getProperties()));
        } else if (node.getType().equals(Types.PRIV_PERSON)
                || node.getType().equals(Types.ORGPERSON)) {
            output = createPerson(parent, toQNameProperties(node.getProperties()));
        } else if (node.getType().equals(Types.CONTACT_GROUP)) {
            String newContactGroupName = (String) node.getProperties().get(Props.GROUP_NAME);
            newContactGroupName = StringUtils.strip(newContactGroupName);
            node.getProperties().put(Props.GROUP_NAME.toString(), newContactGroupName);
            List<Node> contactGroups = listContactGroups();
            for (Node contactGroup : contactGroups) {
                String contactGroupName = (String) contactGroup.getProperties().get(Props.GROUP_NAME);
                if (StringUtils.equalsIgnoreCase(contactGroupName, newContactGroupName)) {
                    throw new UnableToPerformException("addressbook_group_name_exists");
                }
            }
            output = createContactGroup(toQNameProperties(node.getProperties()));
        }
        return output;
    }

    // XXX this method probably takes too much time to execute and could be optimized
    @Override
    public List<Pair<String, String>> checkIfContactExists(Node contactNode) {
        List<Pair<String, String>> duplicateMessages = new ArrayList<Pair<String, String>>();
        if (!(contactNode instanceof TransientNode)) {
            return duplicateMessages;
        }
        final NodeRef contactRef = contactNode.getNodeRef();
        if (contactNode.getType().equals(Types.PRIV_PERSON)) {
            String fullName = getContactFullName(RepoUtil.toQNameProperties(contactNode.getProperties()), contactNode.getType());
            String code = (String) contactNode.getProperties().get(Props.PERSON_ID);
            final List<Node> persons = listPerson();
            for (Node person : persons) {
                final String otherfullName = getContactFullName(person.getNodeRef());
                String otherCode = (String) person.getProperties().get(Props.PERSON_ID);
                if (isDuplicate(code, otherCode, contactRef, person)) {
                    duplicateMessages.add(new Pair<String, String>(AddressbookAddEditDialog.PERSON_CODE_EXISTS_ERROR, otherCode));
                }
                if (isDuplicate(fullName, otherfullName, contactRef, person)) {
                    duplicateMessages.add(new Pair<String, String>(AddressbookAddEditDialog.PERSON_NAME_EXISTS_ERROR, fullName));
                }
            }
        }
        if (contactNode.getType().equals(Types.ORGANIZATION)) {
            final List<Node> orgs = listOrganization();
            final String orgName = (String) contactNode.getProperties().get(Props.ORGANIZATION_NAME);
            String code = (String) contactNode.getProperties().get(Props.ORGANIZATION_CODE);
            for (Node org : orgs) {
                final String otherOrgName = (String) org.getProperties().get(Props.ORGANIZATION_NAME);
                String otherCode = (String) org.getProperties().get(Props.ORGANIZATION_CODE);
                if (isDuplicate(code, otherCode, contactRef, org)) {
                    duplicateMessages.add(new Pair<String, String>(AddressbookAddEditDialog.ORG_CODE_EXISTS_ERROR, otherCode));
                }
                if (isDuplicate(orgName, otherOrgName, contactRef, org)) {
                    duplicateMessages.add(new Pair<String, String>(AddressbookAddEditDialog.ORG_NAME_EXISTS_ERROR, orgName));
                }
            }
        }
        return duplicateMessages;
    }

    private boolean isDuplicate(final String value, final String otherValue, final NodeRef contactRef, Node otherContactRef) {
        if (StringUtils.isBlank(value) && StringUtils.isBlank(otherValue)) {
            return false;
        }
        if (!contactRef.equals(otherContactRef.getNodeRef())) {
            return StringUtils.equalsIgnoreCase(value, otherValue);
        }
        return false;
    }

    private void addToGroup(NodeRef groupNodeRef, NodeRef memberNodeRef) {
        QName type = nodeService.getType(memberNodeRef);
        if (Types.ORGANIZATION.equals(type)) {
            nodeService.createAssociation(groupNodeRef, memberNodeRef, Assocs.CONTACT_ORGANIZATION);
            boolean groupTaskCapable = Boolean.TRUE.equals(nodeService.getProperty(groupNodeRef, AddressbookModel.Props.TASK_CAPABLE));
            if (groupTaskCapable) {
                nodeService.setProperty(memberNodeRef, AddressbookModel.Props.TASK_CAPABLE, groupTaskCapable);
            }
        } else {
            nodeService.createAssociation(groupNodeRef, memberNodeRef, Assocs.CONTACT_PERSON_BASE);
        }
    }

    @Override
    public MessageDataWrapper addToGroup(NodeRef groupNodeRef, List<UserDetails> usersForGroup) {
        List<AssociationRef> assocRefs = nodeService.getTargetAssocs(groupNodeRef, RegexQNamePattern.MATCH_ALL);
        final HashSet<String> existingRefs = new HashSet<String>();
        for (AssociationRef assocRef : assocRefs) {
            existingRefs.add(assocRef.getTargetRef().toString());
        }
        MessageDataWrapper feedBack = new MessageDataWrapper();
        for (UserDetails wrapper : usersForGroup) {
            if (existingRefs.contains(wrapper.getNodeRef())) {
                feedBack.addFeedbackItem(new MessageDataImpl(MessageSeverity.INFO, "addressbook_contactgroup_add_contactExisted", wrapper.getName()));
            } else {
                Boolean groupTaskCapable = Boolean.TRUE.equals(nodeService.getProperty(groupNodeRef, AddressbookModel.Props.TASK_CAPABLE));
                NodeRef contactNodeRef = new NodeRef(wrapper.getNodeRef());
                String contactEmail = (String) nodeService.getProperty(contactNodeRef, AddressbookModel.Props.EMAIL);
                if (groupTaskCapable && StringUtils.isBlank(contactEmail)) {
                    feedBack.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "addressbook_contactgroup_task_capable_org_missing_email"));
                } else {
                    addToGroup(groupNodeRef, contactNodeRef);
                    feedBack.addFeedbackItem(new MessageDataImpl(MessageSeverity.INFO, "addressbook_added_to_contactgroup"));
                }
            }
        }
        return feedBack;
    }

    @Override
    public void deleteFromGroup(NodeRef groupNodeRef, NodeRef memberNodeRef) {
        QName type = nodeService.getType(memberNodeRef);
        if (Types.ORGANIZATION.equals(type)) {
            nodeService.removeAssociation(groupNodeRef, memberNodeRef, Assocs.CONTACT_ORGANIZATION);
        } else {
            nodeService.removeAssociation(groupNodeRef, memberNodeRef, Assocs.CONTACT_PERSON_BASE);
        }
    }

    @Override
    public List<Node> listPerson() {
        return listAddressbookChildren(AddressbookModel.Assocs.ABPEOPLE);
    }

    @Override
    public List<Node> listPerson(NodeRef org) {
        return listNodeChildren(AddressbookModel.Assocs.ORGPEOPLE, org);
    }

    @Override
    public Node getNode(NodeRef ref) {
        // create our Node representation
        MapNode node = new MapNode(ref);
        // this will also force initialization of the props now during the UserTransaction
        // it is much better for performance to do this now rather than during page bind
        node.getProperties();
        return node;
    }

    @Override
    public List<Node> getContactsByRegNumber(String regNumber) {
        List<String> queryParts = new ArrayList<String>(2);
        List<String> queryPartsOrg = new ArrayList<String>(2);
        queryPartsOrg.add(generateTypeQuery(Types.ORGANIZATION));
        queryPartsOrg.add(generateStringExactQuery(regNumber, Props.ORGANIZATION_CODE));
        queryParts.add(joinQueryPartsAnd(queryPartsOrg));
        queryParts.add(generatePersonByCodeQuery(regNumber));
        String query = joinQueryPartsOr(queryParts);
        return toNodeList(searchNodes(query, -1, "contactsByRegNumber", Collections.singletonList(store)));
    }

    @Override
    public List<Node> getContactsWithSapAccount() {
        List<String> queryParts = new ArrayList<String>(3);
        queryParts.add(generateTypeQuery(Types.ORGANIZATION));
        queryParts.add(generatePropertyNotNullQuery(Props.ORGANIZATION_CODE));
        queryParts.add(generatePropertyNotNullQuery(Props.SAP_ACCOUNT));
        String query = joinQueryPartsAnd(queryParts);
        return toNodeList(searchNodes(query, -1, "contactsWithSapAccount", Collections.singletonList(store)));
    }

    @Override
    public List<Node> getPersonContactsByCode(String code) {
        String query = generatePersonByCodeQuery(code);
        return toNodeList(searchNodes(query, -1, "personContactsByCode", Collections.singletonList(store)));
    }

    private List<Node> toNodeList(Pair<List<NodeRef>, Boolean> results) {
        List<NodeRef> nodeRefs = results.getFirst();
        List<Node> contactNodes = new ArrayList<Node>(nodeRefs.size());
        for (NodeRef nodeRef : nodeRefs) {
            contactNodes.add(getNode(nodeRef));
        }
        return contactNodes;
    }

    private String generatePersonByCodeQuery(String regNumber) {
        List<String> queryPartsPerson = new ArrayList<String>(2);
        List<String> queryPartsPersonType = new ArrayList<String>(2);
        queryPartsPersonType.add(generateTypeQuery(Types.PRIV_PERSON));
        queryPartsPersonType.add(generateTypeQuery(Types.ORGPERSON));
        queryPartsPerson.add(joinQueryPartsOr(queryPartsPersonType));
        queryPartsPerson.add(generateStringExactQuery(regNumber, Props.PERSON_ID));
        return joinQueryPartsAnd(queryPartsPerson);
    }

    @Override
    public Node getEmptyNode(QName type) {
        return TransientNode.createNew(dictionaryService, dictionaryService.getType(type), null, null);
    }

    @Override
    public void deleteNode(NodeRef nodeRef) {
        nodeService.deleteNode(nodeRef);
    }

    @Override
    public void updateNode(Node node) {
        nodeService.setProperties(node.getNodeRef(), toQNameProperties(node.getProperties()));
    }

    @Override
    public List<Node> search(String searchCriteria, int limit) {
        return search(searchCriteria, limit, true);
    }

    @Override
    public List<Node> search(String searchCriteria, int limit, boolean onlyActive) {
        return executeSearch(searchCriteria, searchFields, false, false, allContactTypes, null, limit, onlyActive);
    }

    @Override
    public List<Node> searchContactGroups(String searchCriteria, boolean showAdminManageable, boolean excludeTaskCapable, int limit) {
        List<Node> result = null;
        boolean emptySearch = StringUtils.isBlank(searchCriteria);
        if (emptySearch) {
            result = listContactGroups();
        } else {
            result = executeSearch(searchCriteria, contactGroupSearchFields, false, false, Collections.<QName> emptySet(), null, limit);
        }
        if (excludeTaskCapable) { // should be done during search
            List<Node> notTaskCapable = new ArrayList<Node>(result.size());
            for (Node node : result) {
                if (!Boolean.TRUE.equals(node.getProperties().get(AddressbookModel.Props.TASK_CAPABLE))) {
                    notTaskCapable.add(node);
                }
            }
            result = notTaskCapable;
        }
        if (BeanHelper.getUserService().isDocumentManager() || showAdminManageable) {
            if (result.size() > limit) {
                result = result.subList(0, limit);
            }
            return result;
        }
        List<Node> filtered = new ArrayList<Node>(result.size());
        for (Node node : result) {
            if (!Boolean.TRUE.equals(node.getProperties().get(AddressbookModel.Props.MANAGEABLE_FOR_ADMIN))) {
                filtered.add(node);
            }
            if (emptySearch && filtered.size() == limit) {
                break;
            }
        }
        return filtered;
    }

    @Override
    public List<Node> searchTaskCapableContacts(String searchCriteria, boolean orgOnly, boolean dvkCapableOnly, String institutionToRemove, int limit) {
        return executeSearch(
                searchCriteria,
                searchFields,
                true,
                dvkCapableOnly,
                orgOnly ? Collections.singleton(Types.ORGANIZATION) : allContactTypes, // actually only organizations have the taskCapable property...
                institutionToRemove,
                limit);
    }

    @Override
    public List<Node> searchPersonContacts(String searchCriteria, int limit) {
        Set<QName> types = new HashSet<QName>();
        types.add(Types.ORGPERSON);
        types.add(Types.PRIV_PERSON);
        return executeSearch(searchCriteria, searchFields, false, false, types, null, limit);
    }

    @Override
    public List<Node> searchOrgContacts(String searchCriteria, int limit) {
        return executeSearch(searchCriteria, searchFields, false, false, Collections.singleton(Types.ORGANIZATION), null, limit);
    }

    @Override
    public List<Node> searchTaskCapableContactGroups(String searchCriteria, boolean orgOnly, boolean dvkCapableOnly, String institutionToRemove, int limit) {
        return executeSearch(
                searchCriteria,
                contactGroupSearchFields,
                true,
                dvkCapableOnly,
                orgOnly ? Collections.singleton(Types.ORGANIZATION) : Collections.<QName> emptySet(),
                institutionToRemove,
                limit,
                false); // Contact groups don't have AddressbookModel.Props.ACTIVESTATUS property
    }

    private List<Node> executeSearch(String searchCriteria, Set<QName> fields, boolean taskCapableOnly, boolean dvkCapableOnly, Set<QName> types, String institutionToRemove,
            int limit) {
        return executeSearch(searchCriteria, fields, taskCapableOnly, dvkCapableOnly, types, institutionToRemove, limit, true);
    }

    private List<Node> executeSearch(String searchCriteria, Set<QName> fields, boolean taskCapableOnly, boolean dvkCapableOnly, Set<QName> types, String institutionToRemove,
            int limit, boolean onlyActive) {
        List<String> queryPartsAnd = new ArrayList<String>(4);
        if (StringUtils.isNotBlank(searchCriteria)) {
            queryPartsAnd.add(SearchUtil.generateStringWordsWildcardQuery(parseQuickSearchWords(searchCriteria, 1), true, true, fields.toArray(new QName[0])));
        }
        if (taskCapableOnly) {
            if (fields == contactGroupSearchFields) {
                queryPartsAnd.add(generateTypeQuery(Types.CONTACT_GROUP));
            }
            queryPartsAnd.add(generatePropertyBooleanQuery(Props.TASK_CAPABLE, true));
        }
        if (dvkCapableOnly && fields != contactGroupSearchFields) {
            queryPartsAnd.add(generatePropertyBooleanQuery(Props.DVK_CAPABLE, true));
        }
        if (onlyActive) {
            queryPartsAnd.add(generatePropertyBooleanQuery(Props.ACTIVESTATUS, true));
        }
        if (fields == searchFields) {
            queryPartsAnd.add(generateTypeQuery(types));
        } else if (queryPartsAnd.isEmpty()) {
            throw new IllegalArgumentException("searchCriteria can't be blank when searching from following addressbook fields:\n\t" + fields);
        }

        if (StringUtils.isNotBlank(institutionToRemove)) {
            // we do not want to see this organization in the search results
            queryPartsAnd.add(SearchUtil.generatePropertyExactNotQuery(Props.ORGANIZATION_CODE, institutionToRemove));
        }

        if (types.contains(Types.ORGANIZATION) && fields == contactGroupSearchFields) {
            List<NodeRef> nodeRefs = searchNodes(joinQueryPartsAnd(queryPartsAnd), -1, "addressbookSearch", Collections.singletonList(store)).getFirst();
            // here we only want the contact groups that contain organizations?
            List<Node> filteredList = filterContactGroupsThatContainOrganizations(nodeRefs, dvkCapableOnly);
            if (filteredList.size() > limit) {
                filteredList = filteredList.subList(0, limit);
            }
            return filteredList;
        }
        return toNodeList(searchNodes(joinQueryPartsAnd(queryPartsAnd), limit, "addressbookSearch", Collections.singletonList(store)));
    }

    private List<Node> filterContactGroupsThatContainOrganizations(List<NodeRef> nodeRefs, boolean dvkCapableOnly) {
        List<Node> result = new ArrayList<Node>();
        for (NodeRef nodeRef : nodeRefs) {
            for (Node contact : getContacts(nodeRef)) {
                if ((contact.getType().equals(Types.ORGANIZATION)) && (!dvkCapableOnly || Boolean.TRUE.equals(contact.getProperties().get(Props.DVK_CAPABLE)))) {
                    result.add(getNode(nodeRef));
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public NodeRef getOrgOfPerson(NodeRef ref) {
        return nodeService.getPrimaryParent(ref).getParentRef();
    }

    @Override
    public List<Node> getContactsByType(QName type, NodeRef nodeRef) {
        List<Node> allNodes = getContacts(nodeRef);
        List<Node> nodes = new ArrayList<Node>(allNodes.size());
        for (Node node : allNodes) {
            if (dictionaryService.isSubClass(node.getType(), type)) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    @Override
    public List<Node> getContacts(NodeRef nodeRef) {
        List<AssociationRef> assocRefs = nodeService.getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
        List<Node> nodes = new ArrayList<Node>(assocRefs.size());
        for (AssociationRef assocRef : assocRefs) {
            nodes.add(getNode(assocRef.getTargetRef()));
        }
        return nodes;
    }

    @Override
    public boolean isTaskCapableGroupMember(NodeRef contactRef) {
        List<AssociationRef> assocs = nodeService.getSourceAssocs(contactRef, RegexQNamePattern.MATCH_ALL);
        for (AssociationRef assocRef : assocs) {
            NodeRef sourceRef = assocRef.getSourceRef();
            if (Types.CONTACT_GROUP.equals(nodeService.getType(sourceRef))) {
                if (Boolean.TRUE.equals(nodeService.getProperty(sourceRef, Props.TASK_CAPABLE))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------- utility methods

    @Override
    public NodeRef createOrganization(Map<QName, Serializable> data) {
        return createNode(null, Assocs.ORGANIZATIONS, Types.ORGANIZATION, data);
    }

    private NodeRef createPerson(NodeRef organization, Map<QName, Serializable> data) {
        return createNode(organization,
                organization == null ? Assocs.ABPEOPLE : Assocs.ORGPEOPLE,
                organization == null ? Types.PRIV_PERSON : Types.ORGPERSON, data);
    }

    private NodeRef createContactGroup(Map<QName, Serializable> data) {
        return createNode(null, Assocs.CONTACT_GROUPS, Types.CONTACT_GROUP, data);
    }

    private NodeRef createNode(NodeRef parent, QName assoc, QName type, Map<QName, Serializable> data) {
        QName randomqname = QName.createQName(URI, GUID.generate());
        ChildAssociationRef a = nodeService.createNode(parent == null ? getAddressbookRoot() : parent, assoc,
                randomqname, type, data);
        return a.getChildRef();
    }

    @Override
    public NodeRef getAddressbookRoot() {
        if (addressbookRoot == null) {
            String xPath = AddressbookModel.Repo.ADDRESSBOOK_SPACE;
            addressbookRoot = generalService.getNodeRef(xPath);
        }
        return addressbookRoot;
    }

    @Override
    public List<Node> getDvkCapableOrgs() {
        List<Node> dvkCapableOrgs = new ArrayList<Node>();
        for (Node organization : listOrganization()) {
            if (Boolean.TRUE.equals(organization.getProperties().get(Props.DVK_CAPABLE))) {
                dvkCapableOrgs.add(organization);
            }
        }
        return dvkCapableOrgs;
    }

    private List<Node> listNodeChildren(QNamePattern type, NodeRef parent) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(parent, type, RegexQNamePattern.MATCH_ALL);
        List<Node> entryNodes = new ArrayList<Node>(childRefs.size());
        for (ChildAssociationRef ref : childRefs) {
            try {
                entryNodes.add(getNode(ref.getChildRef()));
            } catch (InvalidNodeRefException e) {
                // This can happen if two users modify the addressbook simultaneously and one of them deletes a contact/organization
                // We can ignore this situation since the node doesn't exist any more and therefore it can be excluded from the listing.
                LOG.error("Unable to get " + parent + " childs properties with type " + type, e);
                continue;
            }
        }
        return entryNodes;
    }

    private List<Node> listAddressbookChildren(QNamePattern type) {
        return listNodeChildren(type, getAddressbookRoot());
    }

    @Override
    public List<Node> getContactsGroups(NodeRef memberNodeRef) {
        QName type = nodeService.getType(memberNodeRef);
        List<AssociationRef> groupRefs = new ArrayList<AssociationRef>();
        if (Types.ORGANIZATION.equals(type)) {
            groupRefs = nodeService.getSourceAssocs(memberNodeRef, Assocs.CONTACT_ORGANIZATION);
        } else {
            groupRefs = nodeService.getSourceAssocs(memberNodeRef, Assocs.CONTACT_PERSON_BASE);
        }

        List<Node> groups = new ArrayList<Node>(groupRefs.size());
        for (AssociationRef associationRef : groupRefs) {
            groups.add(getNode(associationRef.getSourceRef()));
        }

        return groups;
    }

    @Override
    public List<NodeRef> getContactGroupContents(NodeRef contactGroupRef) {
        List<AssociationRef> assocs = nodeService.getTargetAssocs(contactGroupRef, RegexQNamePattern.MATCH_ALL);
        List<NodeRef> contacts = new ArrayList<NodeRef>(assocs.size());
        for (AssociationRef associationRef : assocs) {
            contacts.add(associationRef.getTargetRef());
        }
        return contacts;
    }

    // ---------- service getters and setters

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setSearchFields(Set<String> fields) {
        searchFields = new HashSet<QName>(fields.size());
        for (String qname : fields) {
            searchFields.add(QName.createQName(qname, namespaceService));
        }
    }

    public void setContactGroupSearchFields(Set<String> fields) {
        contactGroupSearchFields = new HashSet<QName>(fields.size());
        for (String qname : fields) {
            contactGroupSearchFields.add(QName.createQName(qname, namespaceService));
        }
    }

    public void setAllContactTypes(Set<String> fields) {
        allContactTypes = new HashSet<QName>(fields.size());
        for (String qname : fields) {
            allContactTypes.add(QName.createQName(qname, namespaceService));
        }
    }

    public void setStore(String store) {
        this.store = new StoreRef(store);
    }

    @Override
    public boolean hasCreatePermission() {
        return permissionService.hasPermission(getAddressbookRoot(), PermissionService.CREATE_CHILDREN) == AccessStatus.ALLOWED;
    }

}
