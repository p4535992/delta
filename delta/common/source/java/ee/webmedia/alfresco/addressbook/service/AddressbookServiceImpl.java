package ee.webmedia.alfresco.addressbook.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.QueryParser;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Assocs;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookAddEditDialog;
import ee.webmedia.alfresco.addressbook.web.dialog.ContactGroupAddDialog.UserDetails;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * @author Keit Tehvan 29.09.2009
 */
public class AddressbookServiceImpl implements AddressbookService {

    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private SearchService searchService;
    private NamespaceService namespaceService;
    private AuthorityService authorityService;

    private Set<QName> searchFields; // doesn't need to be synchronized, because it is not modified during runtime
    private Set<QName> contactGroupSearchFields; // doesn't need to be synchronized, because it is not modified during runtime
    private StoreRef store;

    // ---------- interface methods

    @Override
    public boolean hasManagePermission() {
        Set<String> auths = authorityService.getAuthorities();
        return auths.contains(PermissionService.ADMINISTRATOR_AUTHORITY) || auths.contains(authorityService.getName(AuthorityType.GROUP, ADDRESSBOOK_GROUP));
    }

    @Override
    public List<Node> listOrganization() {
        return listAddressbookChildren(Types.ORGANIZATION);
    }

    @Override
    public List<Node> listContactGroups() {
        return listAddressbookChildren(Types.CONTACT_GROUP);
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
            output = createOrganization(convertProps(node.getProperties()));
        } else if (node.getType().equals(Types.PRIV_PERSON)
                || node.getType().equals(Types.ORGPERSON)) {
            output = createPerson(parent, convertProps(node.getProperties()));
        } else if (node.getType().equals(Types.CONTACT_GROUP)) {
            List<Node> contactGroups = listContactGroups();
            for (Node contactGroup : contactGroups) {
                String contactGroupName = (String) contactGroup.getProperties().get(AddressbookModel.Props.GROUP_NAME);
                String newContactGroupName = (String) node.getProperties().get(AddressbookModel.Props.GROUP_NAME);
                if (StringUtils.equalsIgnoreCase(contactGroupName, newContactGroupName)) {
                    throw new UnableToPerformException("addressbook_group_name_exists");
                }
            }
            output = createContactGroup(convertProps(node.getProperties()));
        }
        return output;
    }

    @Override
    public List<Pair<String, String>> checkIfContactExists(Node contactNode) {
        List<Pair<String, String>> duplicateMessages = new ArrayList<Pair<String, String>>();
        final NodeRef contactRef = contactNode.getNodeRef();
        if (contactNode.getType().equals(Types.PRIV_PERSON)) {
            final String messageKey = "addressbook_save_person_error_nameExists";
            String fullName = getFullName(contactNode);
            String code = (String) contactNode.getProperties().get(AddressbookModel.Props.PERSON_ID);
            final List<Node> persons = listPerson();
            for (Node person : persons) {
                final String otherfullName = getFullName(person);
                String otherCode = (String) person.getProperties().get(AddressbookModel.Props.PERSON_ID);
                if (isDuplicate(code, otherCode, contactRef, person)) {
                    duplicateMessages.add(new Pair<String, String>(AddressbookAddEditDialog.PERSON_CODE_EXISTS_ERROR, otherCode));
                }
                if (isDuplicate(fullName, otherfullName, contactRef, person)) {
                    duplicateMessages.add(new Pair<String, String>(messageKey, fullName));
                }
            }
        }
        if (contactNode.getType().equals(Types.ORGANIZATION)) {
            final String duplicateMessageKey = "addressbook_save_organization_error_nameExists";
            final List<Node> orgs = listOrganization();
            final String orgName = (String) contactNode.getProperties().get(AddressbookModel.Props.ORGANIZATION_NAME);
            String code = (String) contactNode.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE);
            for (Node org : orgs) {
                final String otherOrgName = (String) org.getProperties().get(AddressbookModel.Props.ORGANIZATION_NAME);
                String otherCode = (String) org.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE);
                if (isDuplicate(code, otherCode, contactRef, org)) {
                    duplicateMessages.add(new Pair<String, String>(AddressbookAddEditDialog.ORG_CODE_EXISTS_ERROR, otherCode));
                }
                if (isDuplicate(orgName, otherOrgName, contactRef, org)) {
                    duplicateMessages.add(new Pair<String, String>(duplicateMessageKey, orgName));
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

    private String getFullName(Node node) {
        final String pFirstName = (String) node.getProperties().get(AddressbookModel.Props.PERSON_FIRST_NAME);
        final String pLastName = (String) node.getProperties().get(AddressbookModel.Props.PERSON_LAST_NAME);
        String fullName = pFirstName + " " + pLastName;
        return fullName;
    }

    private void addToGroup(NodeRef groupNodeRef, NodeRef memberNodeRef) {
        QName type = nodeService.getType(memberNodeRef);
        if (Types.ORGANIZATION.equals(type)) {
            nodeService.createAssociation(groupNodeRef, memberNodeRef, Assocs.CONTACT_ORGANIZATION);
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
                addToGroup(groupNodeRef, new NodeRef(wrapper.getNodeRef()));
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
        return listAddressbookChildren(Types.PRIV_PERSON);
    }

    @Override
    public List<Node> listPerson(NodeRef org) {
        return listNodeChildren(Types.ORGPERSON, org);
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
        queryPartsOrg.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGANIZATION));
        queryPartsOrg.add(SearchUtil.generateStringExactQuery(regNumber, AddressbookModel.Props.ORGANIZATION_CODE));
        queryParts.add(SearchUtil.joinQueryPartsAnd(queryPartsOrg));
        queryParts.add(generatePersonByCodeQuery(regNumber));
        String query = SearchUtil.joinQueryPartsOr(queryParts);
        ResultSet searchResult = searchService.query(store, SearchService.LANGUAGE_LUCENE, query);
        try {
            List<NodeRef> nodeRefs = searchResult.getNodeRefs();
            List<Node> contactNodes = new ArrayList<Node>(nodeRefs.size());
            for (NodeRef nodeRef : nodeRefs) {
                contactNodes.add(getNode(nodeRef));
            }
            return contactNodes;
        } finally {
            searchResult.close();
        }
    }

    @Override
    public List<Node> getContactsWithSapAccount() {
        List<String> queryParts = new ArrayList<String>(3);
        queryParts.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGANIZATION));
        queryParts.add(SearchUtil.generatePropertyNotNullQuery(AddressbookModel.Props.ORGANIZATION_CODE));
        queryParts.add(SearchUtil.generatePropertyNotNullQuery(AddressbookModel.Props.SAP_ACCOUNT));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        ResultSet searchResult = searchService.query(store, SearchService.LANGUAGE_LUCENE, query);
        try {
            List<NodeRef> nodeRefs = searchResult.getNodeRefs();
            List<Node> contactNodes = new ArrayList<Node>(nodeRefs.size());
            for (NodeRef nodeRef : nodeRefs) {
                contactNodes.add(getNode(nodeRef));
            }
            return contactNodes;
        } finally {
            searchResult.close();
        }
    }

    @Override
    public List<Node> getPersonContactsByCode(String code) {
        String query = generatePersonByCodeQuery(code);
        ResultSet searchResult = searchService.query(store, SearchService.LANGUAGE_LUCENE, query);
        try {
            List<NodeRef> nodeRefs = searchResult.getNodeRefs();
            List<Node> contactNodes = new ArrayList<Node>(nodeRefs.size());
            for (NodeRef nodeRef : nodeRefs) {
                contactNodes.add(getNode(nodeRef));
            }
            return contactNodes;
        } finally {
            searchResult.close();
        }
    }

    private String generatePersonByCodeQuery(String regNumber) {
        List<String> queryPartsPerson = new ArrayList<String>(2);
        List<String> queryPartsPersonType = new ArrayList<String>(2);
        queryPartsPersonType.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.PRIV_PERSON));
        queryPartsPersonType.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGPERSON));
        queryPartsPerson.add(SearchUtil.joinQueryPartsOr(queryPartsPersonType));
        queryPartsPerson.add(SearchUtil.generateStringExactQuery(regNumber, AddressbookModel.Props.PERSON_ID));
        return SearchUtil.joinQueryPartsAnd(queryPartsPerson);
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
        if (node.getType().equals(Types.ORGANIZATION)) {
            node.getProperties().put(ContentModel.PROP_NAME.toString(), node.getProperties().get(AddressbookModel.Props.ORGANIZATION_NAME));
        } else if (node.getType().equals(Types.PRIV_PERSON) || node.getType().equals(Types.ORGPERSON)) {
            node.getProperties().put(ContentModel.PROP_NAME.toString(), node.getProperties().get(AddressbookModel.Props.PERSON_FIRST_NAME));
        }
        nodeService.setProperties(node.getNodeRef(), convertProps(node.getProperties()));
    }

    @Override
    public List<Node> search(String searchCriteria) {
        return executeSearch(searchCriteria, searchFields, false, false, false, null);
    }

    @Override
    public List<Node> searchContactGroups(String searchCriteria) {
        return executeSearch(searchCriteria, contactGroupSearchFields, false, false, false, null);
    }

    @Override
    public List<Node> searchTaskCapableContacts(String searchCriteria, boolean orgOnly, String institutionToRemove) {
        return executeSearch(searchCriteria, searchFields, true, orgOnly, false, institutionToRemove);
    }

    @Override
    public List<Node> searchPersonContacts(String searchCriteria) {
        return executeSearch(searchCriteria, searchFields, false, false, true, null);
    }

    @Override
    public List<Node> searchOrgContacts(String searchCriteria) {
        return executeSearch(searchCriteria, searchFields, false, true, false, null);
    }

    @Override
    public List<Node> searchTaskCapableContactGroups(String searchCriteria, boolean orgOnly, String institutionToRemove) {
        return executeSearch(searchCriteria, contactGroupSearchFields, true, orgOnly, false, institutionToRemove);
    }

    private List<Node> executeSearch(String searchCriteria, Set<QName> fields, boolean taskCapableOnly, boolean orgOnly, boolean personOnly, String institutionToRemove) {
        List<NodeRef> nodeRefs = null;
        final ResultSet searchResult;
        if (StringUtils.isNotBlank(searchCriteria) || taskCapableOnly || (orgOnly && fields == searchFields)) {

            StringBuilder query = new StringBuilder();
            if (searchCriteria != null) {
                for (StringTokenizer t = new StringTokenizer(searchCriteria.trim(), " "); t.hasMoreTokens(); /**/) {
                    String term = QueryParser.escape(t.nextToken());
                    for (QName field : fields) {
                        String fieldPrefixed = field.toPrefixString(namespaceService);
                        String fieldEscaped = StringUtils.replace(fieldPrefixed, "" + QName.NAMESPACE_PREFIX, "\\" + QName.NAMESPACE_PREFIX);
                        query.append("@").append(fieldEscaped).append(":\"*").append(term).append("*\" ");
                    }
                }
            }
            if (taskCapableOnly) {
                addTaskCapableCondition(query, fields);
            }
            String queryString = query.toString();
            if (orgOnly && fields == searchFields) {
                queryString = SearchUtil.joinQueryPartsAnd(Arrays.asList(queryString, SearchUtil.generateAspectQuery(AddressbookModel.Aspects.ORGANIZATION_PROPERTIES)));
            }
            if (personOnly && fields == searchFields) {
                List<String> personQueryParts = new ArrayList<String>(2);
                personQueryParts.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGPERSON));
                personQueryParts.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.PRIV_PERSON));
                String personQuery = SearchUtil.joinQueryPartsOr(personQueryParts);
                queryString = SearchUtil.joinQueryPartsAnd(Arrays.asList(queryString, personQuery));
            }
            searchResult = searchService.query(store, SearchService.LANGUAGE_LUCENE, queryString);
        } else {
            if (fields == contactGroupSearchFields) {
                // get all contact groups under addressBook
                final List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(getAddressbookNodeRef(), new HashSet<QName>(Arrays
                        .asList(Types.CONTACT_GROUP)));
                nodeRefs = new ArrayList<NodeRef>(childAssocs.size());
                for (ChildAssociationRef childAssociationRef : childAssocs) {
                    nodeRefs.add(childAssociationRef.getChildRef());
                }
                searchResult = null;
            } else if (fields == searchFields) {
                // search for persons(directly under addressBook) and organizations and persons under organizations
                final String personsQuery = SearchUtil.generateQuery(searchCriteria, AddressbookModel.Types.PRIV_PERSON, Collections.<QName> emptySet());
                final String orgQuery = SearchUtil.generateQuery(searchCriteria, AddressbookModel.Types.ORGANIZATION, Collections.<QName> emptySet());
                final String orgPersonQuery = SearchUtil.generateQuery(searchCriteria, AddressbookModel.Types.ORGPERSON, Collections.<QName> emptySet());
                searchResult = searchService.query(store, SearchService.LANGUAGE_LUCENE, SearchUtil.joinQueryPartsOr(Arrays.asList(personsQuery, orgQuery,
                        orgPersonQuery)));
            } else {
                throw new IllegalArgumentException("searchCriteria can't be blank when searching from following addressbook fields:\n\t" + fields);
            }
        }
        if (searchResult != null) {
            try {
                nodeRefs = searchResult.getNodeRefs();
            } finally {
                searchResult.close();
            }
        }
        @SuppressWarnings("null")
        // nodeRefs shouldn't be null here as it is initialized based on searchResult when searching
        // or directly set in when getting all contact groups under addressBook
        List<Node> result = new ArrayList<Node>(nodeRefs.size());
        boolean filterContactGroups = orgOnly && fields == contactGroupSearchFields;
        filterAndAddResults(nodeRefs, result, filterContactGroups, orgOnly, institutionToRemove);
        return result;
    }

    public void addTaskCapableCondition(StringBuilder query, Set<QName> fields) {
        if (query.length() > 0) {
            query.insert(0, "(");
            query.append(") AND ");
        } else {
            // add type condition if field condition is not specified
            if (fields == searchFields) {
                query.append("NOT ");
            }
            query.append(SearchUtil.generateTypeQuery(AddressbookModel.Types.CONTACT_GROUP)).append(" AND ");
        }
        String fieldPrefixed = AddressbookModel.Props.TASK_CAPABLE.toPrefixString(namespaceService);
        String fieldEscaped = StringUtils.replace(fieldPrefixed, "" + QName.NAMESPACE_PREFIX, "\\" + QName.NAMESPACE_PREFIX);
        query.append("@").append(fieldEscaped).append(":true");
    }

    private void filterAndAddResults(List<NodeRef> nodeRefs, List<Node> result, boolean filterContactGroups, boolean orgOnly, String institutionToRemove) {
        if (filterContactGroups) {
            for (NodeRef nodeRef : nodeRefs) {
                for (Node contact : getContacts(nodeRef)) {
                    if ((!orgOnly || contact.getType().equals(AddressbookModel.Types.ORGANIZATION))
                                && !isInstitution(institutionToRemove, contact)) {
                        result.add(getNode(nodeRef));
                        break;
                    }
                }
            }
        } else {
            for (NodeRef nodeRef : nodeRefs) {
                Node contact = getNode(nodeRef);
                if ((!orgOnly || contact.getType().equals(AddressbookModel.Types.ORGANIZATION))
                        && !isInstitution(institutionToRemove, contact)) {
                    result.add(getNode(nodeRef));
                }
            }
        }
    }

    private boolean isInstitution(String institutionToRemove, Node contact) {
        if (institutionToRemove == null) {
            return false;
        }
        if (!contact.getType().equals(AddressbookModel.Types.ORGANIZATION)) {
            return false;
        }
        return institutionToRemove.equalsIgnoreCase((String) nodeService.getProperty(contact.getNodeRef(), AddressbookModel.Props.ORGANIZATION_CODE));
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
    public Node getRoot() {
        return new Node(getRootNodeRef());
    }

    @Override
    public boolean isTaskCapableGroupMember(NodeRef contactRef) {
        List<AssociationRef> assocs = nodeService.getSourceAssocs(contactRef, RegexQNamePattern.MATCH_ALL);
        for (AssociationRef assocRef : assocs) {
            NodeRef sourceRef = assocRef.getSourceRef();
            if (AddressbookModel.Types.CONTACT_GROUP.equals(nodeService.getType(sourceRef))) {
                if (Boolean.TRUE.equals(nodeService.getProperty(sourceRef, AddressbookModel.Props.TASK_CAPABLE))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------- utility methods

    private Map<QName, Serializable> convertProps(Map<String, Object> properties) {
        Map<QName, Serializable> data = new HashMap<QName, Serializable>();
        Set<Entry<String, Object>> set = properties.entrySet();
        for (Entry<String, Object> setEntry1 : set) {
            if (setEntry1.getValue() instanceof Serializable) {
                data.put(QName.createQName(setEntry1.getKey()), (Serializable) setEntry1.getValue());
            }
        }
        return data;
    }

    @Override
    public NodeRef createOrganization(Map<QName, Serializable> data) {
        data.put(ContentModel.PROP_NAME, data.get(AddressbookModel.Props.ORGANIZATION_NAME));
        return createNode(null, Assocs.ORGANIZATIONS, Types.ORGANIZATION, data);
    }

    private NodeRef createPerson(NodeRef organization, Map<QName, Serializable> data) {
        data.put(ContentModel.PROP_NAME, data.get(AddressbookModel.Props.PERSON_FIRST_NAME));
        return createNode(organization,
                organization == null ? Assocs.ABPEOPLE : Assocs.ORGPEOPLE,
                organization == null ? Types.PRIV_PERSON : Types.ORGPERSON, data);
    }

    private NodeRef createContactGroup(Map<QName, Serializable> data) {
        return createNode(null, Assocs.CONTACT_GROUPS, Types.CONTACT_GROUP, data);
    }

    private NodeRef createNode(NodeRef parent, QName assoc, QName type, Map<QName, Serializable> data) {
        QName randomqname = QName.createQName(AddressbookModel.URI, GUID.generate());
        ChildAssociationRef a = nodeService.createNode(parent == null ? getAddressbookNodeRef() : parent, assoc,
                randomqname, type, data);
        return a.getChildRef();
    }

    private NodeRef getRootNodeRef() {
        return nodeService.getRootNode(store);
    }

    @Override
    public NodeRef getAddressbookNodeRef() {
        return nodeService.getChildAssocs(getRootNodeRef(), RegexQNamePattern.MATCH_ALL, Assocs.ADDRESSBOOK)
                .get(0).getChildRef();
    }

    @Override
    public List<Node> getDvkCapableOrgs() {
        List<Node> dvkCapableOrgs = new ArrayList<Node>();
        for (Node organization : listOrganization()) {
            Object dvkObj = organization.getProperties().get(AddressbookModel.Props.DVK_CAPABLE);
            if (dvkObj != null && (Boolean) dvkObj) {
                dvkCapableOrgs.add(organization);
            }
        }
        return dvkCapableOrgs;
    }

    private List<Node> listNodeChildren(QName type, NodeRef parent) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(parent);
        List<Node> entryNodes = null;
        entryNodes = new ArrayList<Node>(childRefs.size());
        for (ChildAssociationRef ref : childRefs) {
            NodeRef nodeRef = ref.getChildRef();
            if (nodeService.getType(nodeRef).equals(type)) {
                entryNodes.add(getNode(nodeRef));
            }
        }
        return entryNodes;
    }

    private List<Node> listAddressbookChildren(QName type) {
        return listNodeChildren(type, getAddressbookNodeRef());
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

    // ---------- service getters and setters

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
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

    public void setStore(String store) {
        this.store = new StoreRef(store);
    }

}