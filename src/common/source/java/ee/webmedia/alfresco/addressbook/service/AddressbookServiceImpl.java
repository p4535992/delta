package ee.webmedia.alfresco.addressbook.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

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
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.QueryParser;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Assocs;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Keit Tehvan
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
        if (nodeService.exists(node.getNodeRef())) {
            updateNode(node);
            return node.getNodeRef();
        }
        NodeRef output = null;
        if (node.getType().equals(Types.ORGANIZATION)) {
            output = createOrganization(convertProps(node.getProperties()));
        } else if (node.getType().equals(Types.PRIV_PERSON)
                || node.getType().equals(Types.ORGPERSON)) {
            output = createPerson(parent, convertProps(node.getProperties()));
        } else if (node.getType().equals(Types.CONTACT_GROUP)) {
            output = createContactGroup(convertProps(node.getProperties()));
        }
        return output;
    }
    
    @Override
    public void addToGroup(NodeRef groupNodeRef, NodeRef memberNodeRef) {
        QName type = nodeService.getType(memberNodeRef);
        if (Types.ORGANIZATION.equals(type)) {
            nodeService.createAssociation(groupNodeRef, memberNodeRef, Assocs.CONTACT_ORGANIZATION);
        } else {
            nodeService.createAssociation(groupNodeRef, memberNodeRef, Assocs.CONTACT_PERSON_BASE);
        }
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
        return executeSearch(searchCriteria, searchFields);
    }

    @Override
    public List<Node> searchContactGroups(String searchCriteria) {
        return executeSearch(searchCriteria, contactGroupSearchFields);
    }
    
    private List<Node> executeSearch(String searchCriteria, Set<QName> fields) {
        List<NodeRef> nodeRefs = null;
        final ResultSet searchResult;
        if (StringUtils.isNotBlank(searchCriteria)) {

            StringBuilder query = new StringBuilder();
            for (StringTokenizer t = new StringTokenizer(searchCriteria.trim(), " "); t.hasMoreTokens(); /**/) {
                String term = QueryParser.escape(t.nextToken());
                for (QName field : fields) {
                    String fieldPrefixed = field.toPrefixString(namespaceService);
                    String fieldEscaped = StringUtils.replace(fieldPrefixed, "" + QName.NAMESPACE_PREFIX, "\\" + QName.NAMESPACE_PREFIX);
                    query.append("@").append(fieldEscaped).append(":\"*").append(term).append("*\" ");
                }
            }
            searchResult = searchService.query(store, SearchService.LANGUAGE_LUCENE, query.toString());
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
        for (NodeRef nodeRef : nodeRefs) {
            result.add(getNode(nodeRef));
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
    public Node getRoot() {
        return new Node(getRootNodeRef());
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

    private NodeRef createOrganization(Map<QName, Serializable> data) {
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

    private NodeRef getAddressbookNodeRef() {
        return nodeService.getChildAssocs(getRootNodeRef(), RegexQNamePattern.MATCH_ALL, Assocs.ADDRESSBOOK)
                .get(0).getChildRef();
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
