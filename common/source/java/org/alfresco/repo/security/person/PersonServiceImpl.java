/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * and Open Source Software ("FLOSS") applications as described in Alfresco's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.security.person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.PermissionServiceSPI;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyCheck;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class PersonServiceImpl extends TransactionListenerAdapter implements PersonService, NodeServicePolicies.OnCreateNodePolicy, NodeServicePolicies.BeforeDeleteNodePolicy
{
    private static Log s_logger = LogFactory.getLog(PersonServiceImpl.class);

    private static final String DELETE = "DELETE";

    private static final String SPLIT = "SPLIT";

    private static final String LEAVE = "LEAVE";

    public static final String SYSTEM_FOLDER_SHORT_QNAME = "sys:system";

    public static final String PEOPLE_FOLDER_SHORT_QNAME = "sys:people";

    // IOC

    private StoreRef storeRef;

    private TransactionService transactionService;

    private NodeService nodeService;

    private TenantService tenantService;

    private SearchService searchService;

    private AuthorityService authorityService;

    private DictionaryService dictionaryService;

    private PermissionServiceSPI permissionServiceSPI;

    private NamespacePrefixResolver namespacePrefixResolver;

    private HomeFolderManager homeFolderManager;

    private PolicyComponent policyComponent;

    private boolean createMissingPeople;

    private static Set<QName> mutableProperties;

    private String defaultHomeFolderProvider;

    private boolean processDuplicates = true;

    private String duplicateMode = LEAVE;

    private boolean lastIsBest = true;

    private boolean includeAutoCreated = false;

    private PersonDao personDao;

    private BulkLoadNodeService bulkLoadNodeService;

    /** a transactionally-safe cache to be injected */
    private SimpleCache<String, NodeRef> personCache;

    /**
     * A transactionally-safe cache to be injected.
     * NB! Currently it is assumed that when querying this cache,
     * it has been verified that person exists (for example search has returned user nodeRef).
     * It may occur in concurrent transactions that personPropertiesCache contains data for person that doesn't exist any more.
     */
    private SimpleCache<String, CachedUser> personNodesCache;

    private UserNameMatcher userNameMatcher;

    private static NodeRef peopleContainerNodeRef;

    static
    {
        Set<QName> props = new HashSet<QName>();
        props.add(ContentModel.PROP_HOMEFOLDER);
        props.add(ContentModel.PROP_FIRSTNAME);
        // Middle Name
        props.add(ContentModel.PROP_LASTNAME);
        props.add(ContentModel.PROP_EMAIL);
        props.add(ContentModel.PROP_ORGID);
        mutableProperties = Collections.unmodifiableSet(props);
    }

    public static ThreadLocal<Boolean> validCreatePersonCall = new ThreadLocal<Boolean>();

    @Override
    public boolean equals(Object obj)
    {
        return this == obj;
    }

    @Override
    public int hashCode()
    {
        return 1;
    }

    /**
     * Spring bean init method
     */
    public void init()
    {
        PropertyCheck.mandatory(this, "storeUrl", storeRef);
        PropertyCheck.mandatory(this, "transactionService", transactionService);
        PropertyCheck.mandatory(this, "nodeService", nodeService);
        PropertyCheck.mandatory(this, "searchService", searchService);
        PropertyCheck.mandatory(this, "permissionServiceSPI", permissionServiceSPI);
        PropertyCheck.mandatory(this, "authorityService", authorityService);
        PropertyCheck.mandatory(this, "namespacePrefixResolver", namespacePrefixResolver);
        PropertyCheck.mandatory(this, "policyComponent", policyComponent);
        PropertyCheck.mandatory(this, "personCache", personCache);
        PropertyCheck.mandatory(this, "personNodesCache", personNodesCache);
        PropertyCheck.mandatory(this, "personDao", personDao);

        policyComponent
                .bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"), ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onCreateNode"));
        policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "beforeDeleteNode"), ContentModel.TYPE_PERSON, new JavaBehaviour(this,
                "beforeDeleteNode"));

    }
    
    public void clearPersonCache() {
    	personCache.clear();
    }
    
    public void clearPersonNodesCache() {
    	personNodesCache.clear();
    }

    public UserNameMatcher getUserNameMatcher()
    {
        return userNameMatcher;
    }

    public void setUserNameMatcher(UserNameMatcher userNameMatcher)
    {
        this.userNameMatcher = userNameMatcher;
    }

    void setDefaultHomeFolderProvider(String defaultHomeFolderProvider)
    {
        this.defaultHomeFolderProvider = defaultHomeFolderProvider;
    }

    public void setDuplicateMode(String duplicateMode)
    {
        this.duplicateMode = duplicateMode;
    }

    public void setIncludeAutoCreated(boolean includeAutoCreated)
    {
        this.includeAutoCreated = includeAutoCreated;
    }

    public void setLastIsBest(boolean lastIsBest)
    {
        this.lastIsBest = lastIsBest;
    }

    public void setProcessDuplicates(boolean processDuplicates)
    {
        this.processDuplicates = processDuplicates;
    }

    public void setHomeFolderManager(HomeFolderManager homeFolderManager)
    {
        this.homeFolderManager = homeFolderManager;
    }

    public void setPersonDao(PersonDao personDao)
    {
        this.personDao = personDao;
    }

    /**
     * Set the username to person cache.
     *
     * @param personCache
     *            a transactionally safe cache
     */
    public void setPersonCache(SimpleCache<String, NodeRef> personCache)
    {
        this.personCache = personCache;
    }

    /**
     * Set the username to person nodes cache.
     *
     * @param personNodesCache
     *            a transactionally safe cache
     */
    public void setPersonNodesCache(SimpleCache<String, CachedUser> personNodesCache)
    {
        this.personNodesCache = personNodesCache;
    }

    /**
     * Retrieve the person NodeRef for a username key. Depending on configuration missing people will be created if not
     * found, else a NoSuchPersonException exception will be thrown.
     *
     * @param userName
     *            of the person NodeRef to retrieve
     * @return NodeRef of the person as specified by the username
     * @throws NoSuchPersonException
     */
    private NodeRef getPersonInternal(final String userName)
    {
        // MT share - for activity service system callback
        if (tenantService.isEnabled() && (AuthenticationUtil.SYSTEM_USER_NAME.equals(AuthenticationUtil.getRunAsUser())) && tenantService.isTenantUser(userName))
        {
            final String tenantDomain = tenantService.getUserDomain(userName);

            return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<NodeRef>()
            {
                @Override
                public NodeRef doWork() throws Exception
                {
                    return getPersonImpl(userName);
                }
            }, tenantService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenantDomain));
        }
        else
        {
            return getPersonImpl(userName);
        }
    }

    private NodeRef getPersonImpl(String userName)
    {
        NodeRef personNode = getPersonOrNull(userName);
        if (personNode == null)
        {
            TxnReadState txnReadState = AlfrescoTransactionSupport.getTransactionReadState();
            if (createMissingPeople() && txnReadState == TxnReadState.TXN_READ_WRITE)
            {
                // We create missing people AND are in a read-write txn
                return createMissingPerson(userName);
            }
            else
            {
                throw new NoSuchPersonException(userName);
            }
        }
        else
        {
            return personNode;
        }
    }

    @Override
    public boolean personExists(String caseSensitiveUserName)
    {
        return getPersonOrNull(caseSensitiveUserName) != null;
    }

    @Override
    public List<Node> getPersonNodeList(int limit) {
        Map<String, NodeRef> people = getBulkLoadNodeService().loadChildElementsNodeRefs(BeanHelper.getPersonService().getPeopleContainer(), ContentModel.PROP_USERNAME,
                ContentModel.TYPE_PERSON);
        List<Node> users = new ArrayList<>();

        for (Entry<String, NodeRef> person : people.entrySet()) {
            String userName = person.getKey();
            Node personNode = getPersonNode(userName);
            if (personNode == null) {
                continue;
            }
            users.add(personNode);
            if (limit > -1 && users.size() == limit) {
                break;
            }
        }
        return users;
    }

    @Override
    public List<NodeRef> getAllUserRefs() {
        Map<String, NodeRef> people = getBulkLoadNodeService().loadChildElementsNodeRefs(BeanHelper.getPersonService().getPeopleContainer(), ContentModel.PROP_USERNAME,
                ContentModel.TYPE_PERSON);
        return new ArrayList<NodeRef>(people.values());
    }

    @Override
    public Set<String> getAllUserNames() {
        Map<String, NodeRef> people = getBulkLoadNodeService().loadChildElementsNodeRefs(BeanHelper.getPersonService().getPeopleContainer(), ContentModel.PROP_USERNAME,
                ContentModel.TYPE_PERSON);
        return people.keySet();
    }

    @Override
    public Node getPersonNode(String userName) {
        CachedUser person = personNodesCache.get(userName);
        if (person == null) {
            NodeRef personRef = getPersonRef(userName);
            if (personRef != null) {
                person = new CachedUser(new Node(personRef));
            }
            if (person != null) {
                personCache.put(userName, personRef);
                personNodesCache.put(userName, person);
            }
        }
        return person != null ? person.getNode() : null;
    }

    @Override
    public NodeRef getPerson(String userName) {
        NodeRef personRef = personCache.get(userName);
        if (personRef == null) {
            personRef = getPersonInternal(userName);
            addToCache(userName, personRef);
        }
        return personRef;
    }

    @Override
    public NodeRef getPersonFromRepo(String userName) {
        removeFromCache(userName);
        return getPerson(userName);
    }

    private NodeRef getPersonOrNull(String searchUserName)
    {
        NodeRef returnRef = personCache.get(searchUserName);
        if (returnRef == null)
        {
            returnRef = getPersonRef(searchUserName);

            if (returnRef != null) {
                makeHomeFolderIfRequired(returnRef, searchUserName);
                addToCache(searchUserName, returnRef);
            }
        }
        return returnRef;
    }

    private NodeRef getPersonRef(String searchUserName) {
        List<NodeRef> refs = new ArrayList<>(new HashSet<>(personDao.getPersonOrNull(searchUserName, userNameMatcher)));
        if (refs.size() > 1)
        {
            return handleDuplicates(refs, searchUserName);
        }
        else if (refs.size() == 1)
        {
            return refs.get(0);
        }
        return null;
    }

    private NodeRef handleDuplicates(List<NodeRef> refs, String searchUserName)
    {
        if (processDuplicates)
        {
            NodeRef best = findBest(refs);
            HashSet<NodeRef> toHandle = new HashSet<NodeRef>();
            toHandle.addAll(refs);
            toHandle.remove(best);
            addDuplicateNodeRefsToHandle(toHandle);
            return best;
        }
        else
        {
            String userNameSensitivity = " (user name is case-" + (userNameMatcher.getUserNamesAreCaseSensitive() ? "sensitive" : "insensitive") + ")";
            String domainNameSensitivity = "";
            if (!userNameMatcher.getDomainSeparator().equals(""))
            {
                domainNameSensitivity = " (domain name is case-" + (userNameMatcher.getDomainNamesAreCaseSensitive() ? "sensitive" : "insensitive") + ")";
            }

            throw new AlfrescoRuntimeException("Found more than one user for " + searchUserName + userNameSensitivity + domainNameSensitivity);
        }
    }

    @Override
    public Map<QName, Serializable> getPersonProperties(String userName) {
        if (StringUtils.isBlank(userName)) {
            return null;
        }
        CachedUser person = personNodesCache.get(userName);
        if (person == null) {
            // Don't use cache for getting person ref, because
            // 1) it seems that personCache is not updated when deleting person and thus
            // querying this cache may result in erroneous data or RuntimeException when calling
            // getPerson -> getPersonImpl -> getPersonOrNull -> makeHomeFolderIfRequired
            // 2) to avoid updating personCache by getPersonOrNull call, because at the same time
            // other transaction may be updating personCache.
            NodeRef personRef = getPersonRef(userName);
            if (personRef != null) {
                person = new CachedUser(new Node(personRef));
                personCache.put(userName, personRef);
                personNodesCache.put(userName, person);
            }
        }
        return person != null ? person.getProps() : null;
    }

    @Override
    public Object getPersonProperty(String userName, QName property) {
        Node personNode = getPersonNode(userName);
        return personNode == null ? null : personNode.getProperties().get(property);
    }

    @Override
    public void updateCache(Node personNode) {
        String userName = (String) personNode.getProperties().get(ContentModel.PROP_USERNAME);
        removeFromCache(userName);
        updateCache(userName);
    }

    @Override
    public void updateCache(String userName) {
        NodeRef personRef;
        personRef = personCache.get(userName);
        removeFromCache(userName);
        if (personRef == null) {
            personRef = getPersonInternal(userName);
        }
        addToCache(userName, personRef);
    }

    private static final String KEY_POST_TXN_DUPLICATES = "PersonServiceImpl.KEY_POST_TXN_DUPLICATES";

    /**
     * Get the txn-bound usernames that need cleaning up
     */
    private Set<NodeRef> getPostTxnDuplicates()
    {
        @SuppressWarnings("unchecked")
        Set<NodeRef> postTxnDuplicates = (Set<NodeRef>) AlfrescoTransactionSupport.getResource(KEY_POST_TXN_DUPLICATES);
        if (postTxnDuplicates == null)
        {
            postTxnDuplicates = new HashSet<NodeRef>();
            AlfrescoTransactionSupport.bindResource(KEY_POST_TXN_DUPLICATES, postTxnDuplicates);
        }
        return postTxnDuplicates;
    }

    /**
     * Flag a username for cleanup after the transaction.
     */
    private void addDuplicateNodeRefsToHandle(Set<NodeRef> refs)
    {
        // Firstly, bind this service to the transaction
        AlfrescoTransactionSupport.bindListener(this);
        // Now get the post txn duplicate list
        Set<NodeRef> postTxnDuplicates = getPostTxnDuplicates();
        postTxnDuplicates.addAll(refs);
    }

    /**
     * Process clean up any duplicates that were flagged during the transaction.
     */
    @Override
    public void afterCommit()
    {
        // Get the duplicates in a form that can be read by the transaction work anonymous instance
        final Set<NodeRef> postTxnDuplicates = getPostTxnDuplicates();

        RetryingTransactionCallback<Object> processDuplicateWork = new RetryingTransactionCallback<Object>()
        {
            @Override
            public Object execute() throws Throwable
            {

                if (duplicateMode.equalsIgnoreCase(SPLIT))
                {
                    split(postTxnDuplicates);
                    s_logger.info("Split duplicate person objects");
                }
                else if (duplicateMode.equalsIgnoreCase(DELETE))
                {
                    delete(postTxnDuplicates);
                    s_logger.info("Deleted duplicate person objects");
                }
                else
                {
                    if (s_logger.isDebugEnabled())
                    {
                        s_logger.debug("Duplicate person objects exist");
                    }
                }

                // Done
                return null;
            }
        };
        transactionService.getRetryingTransactionHelper().doInTransaction(processDuplicateWork, false, true);
    }

    private void delete(Set<NodeRef> toDelete)
    {
        for (NodeRef nodeRef : toDelete)
        {
            nodeService.deleteNode(nodeRef);
            for (String userName : personCache.getKeys()) {
                if (nodeRef.equals(personCache.get(userName))) {
                    removeFromCache(userName);
                    break;
                }
            }
        }
    }

    private void split(Set<NodeRef> toSplit)
    {
        for (NodeRef nodeRef : toSplit)
        {
            String userName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));
            removeFromCache(userName);
            String newName = userName + GUID.generate();
            nodeService.setProperty(nodeRef, ContentModel.PROP_USERNAME, newName);
            addToCache(newName, nodeRef);
        }
    }

    private NodeRef findBest(List<NodeRef> refs)
    {
        if (lastIsBest)
        {
            Collections.sort(refs, new CreationDateComparator(nodeService, false));
        }
        else
        {
            Collections.sort(refs, new CreationDateComparator(nodeService, true));
        }

        NodeRef fallBack = null;

        for (NodeRef nodeRef : refs)
        {
            if (fallBack == null)
            {
                fallBack = nodeRef;
            }

            if (includeAutoCreated || !wasAutoCreated(nodeRef))
            {
                return nodeRef;
            }
        }

        return fallBack;
    }

    private boolean wasAutoCreated(NodeRef nodeRef)
    {
        String userName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));

        String testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_FIRSTNAME));
        if ((testString == null) || !testString.equals(userName))
        {
            return false;
        }

        testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_LASTNAME));
        if ((testString == null) || !testString.equals(""))
        {
            return false;
        }

        testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_EMAIL));
        if ((testString == null) || !testString.equals(""))
        {
            return false;
        }

        testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_ORGID));
        if ((testString == null) || !testString.equals(""))
        {
            return false;
        }

        testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_HOME_FOLDER_PROVIDER));
        if ((testString == null) || !testString.equals(defaultHomeFolderProvider))
        {
            return false;
        }

        return true;
    }

    @Override
    public boolean createMissingPeople()
    {
        return createMissingPeople;
    }

    @Override
    public Set<QName> getMutableProperties()
    {
        return mutableProperties;
    }

    @Override
    public void setPersonProperties(String userName, Map<QName, Serializable> properties)
    {
        NodeRef personRef = getPersonOrNull(userName);
        if (personRef == null)
        {
            if (createMissingPeople())
            {
                personRef = createMissingPerson(userName);
            }
            else
            {
                throw new PersonException("No person found for user name " + userName);
            }

        }
        else
        {
            String realUserName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(personRef, ContentModel.PROP_USERNAME));
            properties.put(ContentModel.PROP_USERNAME, realUserName);
        }
        Map<QName, Serializable> update = nodeService.getProperties(personRef);
        update.putAll(properties);

        nodeService.setProperties(personRef, update);
        removeFromCache(userName);
    }

    @Override
    public void setPersonProperty(String username, QName property, Serializable value) {
        NodeRef personRef = getPerson(username);
        if (personRef == null) {
            return;
        }
        nodeService.setProperty(personRef, property, value);
        removeFromCache(username);
    }

    @Override
    public boolean isMutable()
    {
        return true;
    }

    private NodeRef createMissingPerson(String userName)
    {
        HashMap<QName, Serializable> properties = getDefaultProperties(userName);
        NodeRef person = createPerson(properties);
        makeHomeFolderIfRequired(person, userName);
        return person;
    }

    private void makeHomeFolderIfRequired(NodeRef person, String userName)
    {
        if (person != null)
        {
            if (personNodesCache.contains(userName) && personNodesCache.get(userName).getProps().get(ContentModel.PROP_HOMEFOLDER) != null) {
                return;
            }
            NodeRef homeFolder = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, nodeService.getProperty(person, ContentModel.PROP_HOMEFOLDER));
            if (homeFolder == null)
            {
                final ChildAssociationRef ref = nodeService.getPrimaryParent(person);


                boolean requiresNew = false;

                if (AlfrescoTransactionSupport.getTransactionReadState() != TxnReadState.TXN_READ_WRITE) {
                    requiresNew = true;
                }
                
                transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
                {
                    @Override
                    public Object execute() throws Throwable
                    {
                        homeFolderManager.onCreateNode(ref);
                        return null;
                    }
                }, false, requiresNew);
                
            }
        }
    }

    private HashMap<QName, Serializable> getDefaultProperties(String userName)
    {
        HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_USERNAME, userName);
        properties.put(ContentModel.PROP_FIRSTNAME, tenantService.getBaseNameUser(userName));
        properties.put(ContentModel.PROP_LASTNAME, "");
        properties.put(ContentModel.PROP_EMAIL, "");
        properties.put(ContentModel.PROP_ORGID, "");
        properties.put(ContentModel.PROP_HOME_FOLDER_PROVIDER, defaultHomeFolderProvider);

        properties.put(ContentModel.PROP_SIZE_CURRENT, 0L);
        properties.put(ContentModel.PROP_SIZE_QUOTA, -1L); // no quota

        return properties;
    }

    @Override
    public NodeRef createPerson(Map<QName, Serializable> properties)
    {
        return createPerson(properties, authorityService.getDefaultZones());
    }

    @Override
    public NodeRef createPerson(Map<QName, Serializable> properties, Set<String> zones)
    {
        String userName = DefaultTypeConverter.INSTANCE.convert(String.class, properties.get(ContentModel.PROP_USERNAME));
        AuthorityType authorityType = AuthorityType.getAuthorityType(userName);
        if (authorityType != AuthorityType.USER)
        {
            throw new AlfrescoRuntimeException("Attempt to create person for an authority which is not a user");
        }

        tenantService.checkDomainUser(userName);

        properties.put(ContentModel.PROP_USERNAME, userName);
        properties.put(ContentModel.PROP_SIZE_CURRENT, 0L);

        NodeRef personRef = nodeService.createNode(getPeopleContainer(), ContentModel.ASSOC_CHILDREN, QName.createQName("cm", userName.toLowerCase(), namespacePrefixResolver), // Lowercase:
                // ETHREEOH-1431
                ContentModel.TYPE_PERSON, properties).getChildRef();

        if (zones != null)
        {
            for (String zone : zones)
            {
                // Add the person to an authentication zone (corresponding to an external user registry)
                // Let's preserve case on this child association
                nodeService.addChild(authorityService.getOrCreateZone(zone), personRef, ContentModel.ASSOC_IN_ZONE, QName.createQName("cm", userName, namespacePrefixResolver));
            }
        }

        if (Boolean.TRUE.equals(validCreatePersonCall.get()))
        {
            s_logger.info("Creating person (valid service call) '" + userName + "'");
        }
        else
        {
            try
            {
                throw new RuntimeException();
            } catch (RuntimeException e)
            {
                s_logger.warn("Creating person (INVALID service call) '" + userName + "'", e);
            }
        }
        addToCache(userName, personRef);
        return personRef;
    }

    @Override
    public NodeRef getPeopleContainer()
    {
        if (peopleContainerNodeRef == null) {

            NodeRef rootNodeRef = nodeService.getRootNode(tenantService.getName(storeRef));
            List<ChildAssociationRef> children = nodeService.getChildAssocs(rootNodeRef, RegexQNamePattern.MATCH_ALL, QName.createQName(SYSTEM_FOLDER_SHORT_QNAME,
                    namespacePrefixResolver));

            if (children.size() == 0)
            {
                throw new AlfrescoRuntimeException("Required people system path not found: " + SYSTEM_FOLDER_SHORT_QNAME);
            }

            NodeRef systemNodeRef = children.get(0).getChildRef();

            children = nodeService.getChildAssocs(systemNodeRef, RegexQNamePattern.MATCH_ALL, QName.createQName(PEOPLE_FOLDER_SHORT_QNAME, namespacePrefixResolver));

            if (children.size() == 0)
            {
                throw new AlfrescoRuntimeException("Required people system path not found: " + PEOPLE_FOLDER_SHORT_QNAME);
            }

            peopleContainerNodeRef = children.get(0).getChildRef();
        }
        return peopleContainerNodeRef;
    }

    @Override
    public void deletePerson(String userName)
    {
        // remove user from any containing authorities
        Set<String> containerAuthorities = authorityService.getContainingAuthorities(null, userName, true);
        for (String containerAuthority : containerAuthorities)
        {
            authorityService.removeAuthority(containerAuthority, userName);
            String groupDisplayName = authorityService.getAuthorityDisplayName(containerAuthority);
            BeanHelper.getWorkflowService().removeUserOrGroupFromCompoundWorkflowDefinitions(groupDisplayName, userName);
        }

        // remove any user permissions
        BeanHelper.getPrivilegeService().removeAuthorityPermissions(userName);

        // delete the person
        NodeRef personNodeRef = getPersonOrNull(userName);
        if (personNodeRef != null)
        {
            nodeService.deleteNode(personNodeRef);
            removeFromCache(userName);
        }
    }

    @Override
    public Set<NodeRef> getAllPeople()
    {
        return new HashSet<NodeRef>(getAllUserRefs());
    }

    @Override
    public Set<NodeRef> getPeopleFilteredByProperty(QName propertyKey, Serializable propertyValue)
    {
        // check that given property key is defined for content model type 'cm:person'
        // and throw exception if it isn't
        if (dictionaryService.getProperty(ContentModel.TYPE_PERSON, propertyKey) == null)
        {
            throw new AlfrescoRuntimeException("Property '" + propertyKey + "' is not defined " + "for content model type cm:person");
        }

        LinkedHashSet<NodeRef> people = new LinkedHashSet<NodeRef>();

        //
        // Search for people using the given property
        //

        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery("@cm\\:" + propertyKey.getLocalName() + ":\"" + propertyValue + "\"");
        sp.addStore(tenantService.getName(storeRef));
        sp.excludeDataInTheCurrentTransaction(false);

        ResultSet rs = null;

        try
        {
            rs = searchService.query(sp);

            for (ResultSetRow row : rs)
            {
                NodeRef nodeRef = row.getNodeRef();
                if (nodeService.exists(nodeRef))
                {
                    people.add(nodeRef);
                }
            }
        } finally
        {
            if (rs != null)
            {
                rs.close();
            }
        }

        return people;
    }

    // Policies

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy#onCreateNode(org.alfresco.service.cmr.repository.ChildAssociationRef)
     */
    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        NodeRef personRef = childAssocRef.getChildRef();
        CachedUser person = new CachedUser(new Node(personRef));
        String username = (String) person.getProps().get(ContentModel.PROP_USERNAME);
        personCache.put(username, personRef);
        personNodesCache.put(username, person);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy#beforeDeleteNode(org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    public void beforeDeleteNode(NodeRef nodeRef)
    {
        String username = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME);
        removeFromCache(username);
    }

    private void addToCache(String userName, NodeRef personRef) {
        personCache.put(userName, personRef);
        personNodesCache.put(userName, new CachedUser(new Node(personRef)));
    }

    private void removeFromCache(String username) {
        personCache.remove(username);
        personNodesCache.remove(username);
    }

    // IOC Setters

    @Override
    public void setCreateMissingPeople(boolean createMissingPeople)
    {
        this.createMissingPeople = createMissingPeople;
    }

    public void setNamespacePrefixResolver(NamespacePrefixResolver namespacePrefixResolver)
    {
        this.namespacePrefixResolver = namespacePrefixResolver;
    }

    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setPermissionServiceSPI(PermissionServiceSPI permissionServiceSPI)
    {
        this.permissionServiceSPI = permissionServiceSPI;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    public void setStoreUrl(String storeUrl)
    {
        storeRef = new StoreRef(storeUrl);
    }

    @Override
    public String getUserIdentifier(String caseSensitiveUserName)
    {
        NodeRef nodeRef = getPersonOrNull(caseSensitiveUserName);
        if ((nodeRef != null) && nodeService.exists(nodeRef))
        {
            String realUserName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));
            return realUserName;
        }
        return null;
    }

    public static class CreationDateComparator implements Comparator<NodeRef>
    {
        private final NodeService nodeService;

        boolean ascending;

        CreationDateComparator(NodeService nodeService, boolean ascending)
        {
            this.nodeService = nodeService;
            this.ascending = ascending;
        }

        @Override
        public int compare(NodeRef first, NodeRef second)
        {
            Date firstDate = DefaultTypeConverter.INSTANCE.convert(Date.class, nodeService.getProperty(first, ContentModel.PROP_CREATED));
            Date secondDate = DefaultTypeConverter.INSTANCE.convert(Date.class, nodeService.getProperty(second, ContentModel.PROP_CREATED));

            if (firstDate != null)
            {
                if (secondDate != null)
                {
                    return firstDate.compareTo(secondDate) * (ascending ? 1 : -1);
                }
                else
                {
                    return ascending ? -1 : 1;
                }
            }
            else
            {
                if (secondDate != null)
                {
                    return ascending ? 1 : -1;
                }
                else
                {
                    return 0;
                }
            }

        }
    }

    @Override
    public boolean getUserNamesAreCaseSensitive()
    {
        return userNameMatcher.getUserNamesAreCaseSensitive();
    }

    private BulkLoadNodeService getBulkLoadNodeService() {
        if (bulkLoadNodeService == null) {
            bulkLoadNodeService = BeanHelper.getBulkLoadNodeService();
        }
        return bulkLoadNodeService;
    }

}
