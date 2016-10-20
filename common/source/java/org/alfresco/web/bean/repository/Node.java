/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.web.bean.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolverProvider;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.generator.BaseComponentGenerator;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * Lighweight client side representation of a node held in the repository.
 *
 * @author gavinc
 */
public class Node implements Serializable, NamespacePrefixResolverProvider
{
    private static final org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(Node.class);
    private static final long serialVersionUID = 3544390322739034170L;
    
    public static final String GROUP_EVERYONE = "GROUP_EVERYONE";
    
    protected NodeRef nodeRef;
    protected String name;
    protected QName type;
    protected Path path;
    protected String id;
    protected Set<QName> aspects = null;
    protected Set<Privilege> permissions;
    protected String permissionsUsername;
    protected Boolean locked = null;
    protected Boolean workingCopyOwner = null;
    protected QNameNodeMap<String, Object> properties;
    protected boolean propsRetrieved = false;
    protected transient ServiceRegistry services = null;
    protected boolean childAssocsRetrieved = false;
    protected QNameNodeMap<String, List<ChildAssociationRef>> childAssociations;
    protected boolean assocsRetrieved = false;
    protected QNameNodeMap<String, List<AssociationRef>> associations;

    protected Map<String/* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> childAssociationsAdded;
    protected Map<String/* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> childAssociationsRemoved;
    private Map<String, Map<String, AssociationRef>> associationsAdded;
    private Map<String, Map<String, AssociationRef>> associationsRemoved;

    /**
     * Constructor
     *
     * @param nodeRef The NodeRef this Node wrapper represents
     */
    public Node(NodeRef nodeRef)
    {
        if (nodeRef == null)
        {
            throw new IllegalArgumentException("NodeRef must be supplied for creation of a Node.");
        }

        this.nodeRef = nodeRef;
        id = nodeRef.getId();

        properties = new QNameNodeMap<String, Object>(this, this);
    }

    public void updateNodeRef(NodeRef newRef)
    {
        if (newRef == null)
        {
            throw new IllegalArgumentException("NodeRef cannot be changed to null.");
        }

        nodeRef = newRef;
        id = newRef.getId();
    }

    /**
     * @return All the properties known about this node.
     */
    public Map<String, Object> getProperties()
    {
        return getProperties(null, null);
    }

    /**
     * @return All the properties known about this node.
     */
    public Map<String, Object> getProperties(Map<Long, QName> propertyTypes, Set<QName> propsToLoad)
    {
        if (propsRetrieved == false)
        {
            Map<QName, Serializable> props = getServiceRegistry().getNodeService().getProperties(nodeRef, propsToLoad, propertyTypes);

            for (QName qname : props.keySet())
            {
                Serializable propValue = props.get(qname);

                // Lists returned from the node service could be unmodifiable,
                // therefore create copies for modification purposes
                if (propValue instanceof List)
                {
                    propValue = new ArrayList((List) propValue);
                }

                properties.put(qname.toString(), propValue);
            }

            propsRetrieved = true;
        }

        return properties;
    }

    /**
     * @return All the associations this node has as a Map, using the association
     *         type as the key
     */
    public final Map getAssociations()
    {
        if (assocsRetrieved == false)
        {
            associations = new QNameNodeMap<String, List<AssociationRef>>(this, this);

            List<AssociationRef> assocs = getServiceRegistry().getNodeService().getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);

            for (AssociationRef assocRef : assocs)
            {
                String assocName = assocRef.getTypeQName().toString();

                List<AssociationRef> list = (List<AssociationRef>) associations.get(assocName);
                // create the list if this is first association with 'assocName'
                if (list == null)
                {
                    list = new ArrayList<AssociationRef>();
                    associations.put(assocName, list);
                }

                // add the association to the list
                list.add(assocRef);
            }

            assocsRetrieved = true;
        }

        return associations;
    }

    /**
     * Returns all the associations added to this node in this UI session
     *
     * @return Map of Maps of AssociationRefs
     */
    public final Map<String, Map<String, AssociationRef>> getAddedAssociations()
    {
        if (associationsAdded == null)
        {
            associationsAdded = new HashMap<String, Map<String, AssociationRef>>();
        }
        return associationsAdded;
    }

    /**
     * Returns all the associations removed from this node is this UI session
     *
     * @return Map of Maps of AssociationRefs
     */
    public final Map<String, Map<String, AssociationRef>> getRemovedAssociations()
    {
        if (associationsRemoved == null)
        {
            associationsRemoved = new HashMap<String, Map<String, AssociationRef>>();
        }
        return associationsRemoved;
    }

    /**
     * @return All the child associations this node has as a Map, using the association
     *         type as the key
     */
    public final Map<String/* assocTypeQName */, List<ChildAssociationRef>> getChildAssociations()
    {
        if (childAssocsRetrieved == false)
        {
            childAssociations = new QNameNodeMap<String, List<ChildAssociationRef>>(this, this);

            List<ChildAssociationRef> assocs = getServiceRegistry().getNodeService().getChildAssocs(nodeRef);

            for (ChildAssociationRef assocRef : assocs)
            {
                String assocName = assocRef.getTypeQName().toString();

                List<ChildAssociationRef> list = (List<ChildAssociationRef>) childAssociations.get(assocName);
                // create the list if this is first association with 'assocName'
                if (list == null)
                {
                    list = new ArrayList<ChildAssociationRef>();
                    childAssociations.put(assocName, list);
                }

                // add the association to the list
                list.add(assocRef);
            }

            childAssocsRetrieved = true;
        }

        return childAssociations;
    }

    /**
     * Returns all the child associations added to this node in this UI session
     *
     * @return Map of Maps of ChildAssociationRefs
     */
    public final Map<String/* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> getAddedChildAssociations()
    {
        if (childAssociationsAdded == null)
        {
            childAssociationsAdded = new HashMap<String, Map<String, ChildAssociationRef>>();
        }
        return childAssociationsAdded;
    }

    /**
     * Returns all the child associations removed from this node is this UI session
     *
     * @return Map of Maps of ChildAssociationRefs
     */
    public final Map<String/* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> getRemovedChildAssociations()
    {
        if (childAssociationsRemoved == null)
        {
            childAssociationsRemoved = new HashMap<String, Map<String, ChildAssociationRef>>();
        }
        return childAssociationsRemoved;
    }

    /**
     * Register a property resolver for the named property.
     *
     * @param name Name of the property this resolver is for
     * @param resolver Property resolver to register
     */
    public final void addPropertyResolver(String name, NodePropertyResolver resolver)
    {
        properties.addPropertyResolver(name, resolver);
    }

    /**
     * Returns if a property resolver with a specific name has been applied to the Node
     *
     * @param name of property resolver to look for
     * @return true if a resolver with the name is found, false otherwise
     */
    public final boolean containsPropertyResolver(String name)
    {
        return properties.containsPropertyResolver(name);
    }

    /**
     * Determines whether the given property name is held by this node
     *
     * @param propertyName Property to test existence of
     * @return true if property exists, false otherwise
     */
    public final boolean hasProperty(String propertyName)
    {
        return getProperties().containsKey(propertyName);
    }

    /**
     * @return Returns the NodeRef this Node object represents
     */
    public final NodeRef getNodeRef()
    {
        return nodeRef;
    }

    /**
     * @return Returns the string form of the NodeRef this Node represents
     */
    public final String getNodeRefAsString()
    {
        if (nodeRef == null)
        {
            return null;
        }
        return nodeRef.toString();
    }

    /**
     * @return Returns the type.
     */
    public QName getType()
    {
        if (type == null)
        {
            type = getServiceRegistry().getNodeService().getType(nodeRef);
        }

        return type;
    }

    /**
     * Set the type.
     *
     * @param type
     */
    public void setType(QName type) {
        this.type = type;
    }

    /**
     * @return The display name for the node
     */
    public String getName()
    {
        if (name == null)
        {
            // try and get the name from the properties first
            name = (String) getProperties().get("cm:name");

            // if we didn't find it as a property get the name from the association name
            if (name == null)
            {
                name = getServiceRegistry().getNodeService().getPrimaryParent(nodeRef).getQName().getLocalName();
            }
        }

        return name;
    }

    /**
     * @return The list of aspects applied to this node
     */
    public Set<QName> getAspects()
    {
        if (aspects == null)
        {
            aspects = getServiceRegistry().getNodeService().getAspects(nodeRef);
        }

        return aspects;
    }

    /**
     * @param aspect The aspect to test for
     * @return true if the node has the aspect false otherwise
     */
    public final boolean hasAspect(QName aspect)
    {
        Set aspects = getAspects();
        return aspects.contains(aspect);
    }

    public boolean hasPermission(String permission) {
        return hasPermission(Privilege.getPrivilegeByName(permission));
    }

    /**
     * Return whether the current user has the specified access permission on this Node
     *
     * @param permission Permission to validate against
     * @return true if the permission is applied to the node for this user, false otherwise
     */
    public boolean hasPermission(Privilege... permission)
    {
        return getCachedPermissions().containsAll(Arrays.asList(permission));
    }
    
    /**
     * Return whether the current user has the specified access permission on this Node
     *
     * @param permission Permission to validate against
     * @return true if the permission is applied to the node for this user, false otherwise
     */
    public boolean hasPermissionEveryone(Privilege... permission)
    {
        return BeanHelper.getPrivilegeService().hasPermission(nodeRef, GROUP_EVERYONE, permission);
    }

    private Set<Privilege> getCachedPermissions() {
        String currentUser = AuthenticationUtil.getRunAsUser();
        if (permissions == null || !currentUser.equals(permissionsUsername)) {
            permissions = BeanHelper.getPrivilegeService().getAllCurrentUserPermissions(nodeRef, type, properties);
            permissionsUsername = currentUser;
        }
        return permissions;
    }

    public boolean hasPermissions(String... permissionsToCheck) {
        Set<Privilege> privileges = new HashSet<Privilege>();
        for (String permission : permissionsToCheck) {
            privileges.add(Privilege.getPrivilegeByName(permission));
        }
        return getCachedPermissions().containsAll(privileges);
    }

    public boolean hasPermissions(List<Privilege> privileges) {
        return getCachedPermissions().containsAll(privileges);
    }

    /** Clear permissions cache - for example to validate that permission is not lost meanwhile */
    public void clearPermissionsCache() {
        permissions = null;
    }

    /** Print currently loaded permissions as list */
    public String printLoadedPermissions() {
        if (permissions == null) {
            return permissionsUsername;
        }
        List<String> privilegeStrList = new ArrayList<>();
        for (Privilege privilege : permissions) {
            privilegeStrList.add(privilege.name());
        }
        return permissionsUsername + " privileges=" + TextUtil.joinNonBlankStringsWithComma(privilegeStrList);
    }

    /**
     * @return The GUID for the node
     */
    public final String getId()
    {
        return id;
    }

    /**
     * @return The simple display path for the node
     */
    public String getPath()
    {
        return getNodePath().toString();
    }

    /**
     * @return the repo Path to the node
     */
    public Path getNodePath()
    {
        if (path == null)
        {
            path = getServiceRegistry().getNodeService().getPath(nodeRef);
        }
        return path;
    }

    /**
     * @return If the node is currently locked
     */
    public final boolean isLocked()
    {
        if (locked == null)
        {
            locked = Boolean.FALSE;

            if (hasAspect(ContentModel.ASPECT_LOCKABLE))
            {
                LockStatus lockStatus = getServiceRegistry().getLockService().getLockStatus(getNodeRef());
                if (lockStatus == LockStatus.LOCKED || lockStatus == LockStatus.LOCK_OWNER)
                {
                    locked = Boolean.TRUE;
                }
            }
        }

        return locked.booleanValue();
    }

    /**
     * @return whether a the Node is a WorkingCopy owned by the current User
     */
    public final boolean isWorkingCopyOwner()
    {
        if (workingCopyOwner == null)
        {
            workingCopyOwner = Boolean.FALSE;

            if (hasAspect(ContentModel.ASPECT_WORKING_COPY))
            {
                Object obj = getProperties().get(ContentModel.PROP_WORKING_COPY_OWNER);
                if (obj instanceof String)
                {
                    User user = Application.getCurrentUser(FacesContext.getCurrentInstance());
                    if (((String) obj).equals(user.getUserName()))
                    {
                        workingCopyOwner = Boolean.TRUE;
                    }
                }
            }
        }

        return workingCopyOwner.booleanValue();
    }

    /**
     * Resets the state of the node to force re-retrieval of the data
     */
    public void reset()
    {
        name = null;
        type = null;
        path = null;
        locked = null;
        workingCopyOwner = null;
        properties.clear();
        propsRetrieved = false;
        aspects = null;
        permissions = null;

        associations = null;
        associationsAdded = null;
        associationsRemoved = null;
        assocsRetrieved = false;

        childAssociations = null;
        childAssociationsAdded = null;
        childAssociationsRemoved = null;
        childAssocsRetrieved = false;
    }

    /**
     * Override Object.toString() to provide useful debug output
     */
    @Override
    public String toString()
    {
        if (getServiceRegistry().getNodeService() != null)
        {
            if (getServiceRegistry().getNodeService().exists(nodeRef))
            {
                return "Node Type: " + getType() +
                        "\nNode Properties: " + propsToString(getProperties()) +
                        "\nNode Aspects: " + getAspects().toString();
            }
            else
            {
                return "Node no longer exists: " + nodeRef;
            }
        }
        else
        {
            return super.toString();
        }
    }

    protected ServiceRegistry getServiceRegistry()
    {
        if (services == null)
        {
            services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
        }
        return services;
    }

    protected String propsToString(final Map<String, Object> props) {
        if (logger.isDebugEnabled()) {
            final Set<Entry<String, Object>> entrySet = props.entrySet();
            final StringBuilder sb = new StringBuilder("[");
            for (Iterator<Entry<String, Object>> iterator = entrySet.iterator(); iterator.hasNext();) {
                Entry<String, Object> entry = iterator.next();
                sb.append("\n\t" + entry.getKey() + "\t= " + entry.getValue() + (iterator.hasNext() ? ", " : ""));
            }
            return sb.append("\n]").toString();
        }
        return props.toString();
    }

    @Override
    public NamespacePrefixResolver getNamespacePrefixResolver()
    {
        return getServiceRegistry().getNamespaceService();
    }

    protected boolean allChildAssociationsByAssocTypeRetrieved = false;
    protected Map<String/* assocTypeQName */, List<Node>> allChildAssociationsByAssocType;

    /**
     * This method is used by {@link BaseComponentGenerator} to create value binding for propertySheetItems on subPropertySheets.
     *
     * @return a map of childNodes by associationTypeQName
     * @see #getAllChildAssociationsByAssocType()
     */
    public Map<String/* assocTypeQName */, List<Node>> getAllChildAssociationsByAssocType() {
        if (!allChildAssociationsByAssocTypeRetrieved) {
            final Map<String/* assocTypeQName */, List<Node>> removedChildNodesByType = getNodesOfRemovedChildAssocs();
            final Map<String/* assocTypeQName */, List<Node>> nodesOfAddedChildAssocs = getNodesOfAddedChildAssocs();
            final Map<String/* assocTypeQName */, List<Node>> existingChildAssocNodes = getNodesOfExistingChildAssocs();

            allChildAssociationsByAssocType = existingChildAssocNodes;
            for (Entry<String, List<Node>> entry : nodesOfAddedChildAssocs.entrySet()) {
                final String assocTypeQName = entry.getKey();
                final List<Node> associationNodes = entry.getValue();
                addValuesToMap(allChildAssociationsByAssocType, assocTypeQName, associationNodes);
            }
            for (Entry<String, List<Node>> entry : removedChildNodesByType.entrySet()) {
                final String assocTypeQName = entry.getKey();
                final List<Node> associationNodes = entry.getValue();
                removeValuesFromMap(allChildAssociationsByAssocType, assocTypeQName, associationNodes);
            }
            allChildAssociationsByAssocTypeRetrieved = true;
        }
        return allChildAssociationsByAssocType;
    }

    /**
     * Add child association to given node <br>
     *
     * @param assocTypeQName
     * @param childNode
     * @see #getAllChildAssociationsByAssocType()
     */
    public void addChildAssociations(QName assocTypeQName, Node childNode) {
        final Map<String, List<Node>> childAssocsByAssocType = getAllChildAssociationsByAssocType();
        List<Node> associatedNodes = childAssocsByAssocType.get(assocTypeQName.toString());
        if (associatedNodes == null) {
            associatedNodes = new ArrayList<Node>(5);
            allChildAssociationsByAssocType.put(assocTypeQName.toString(), associatedNodes);
        }
        associatedNodes.add(childNode);
    }

    /**
     * XXX: <b>NB! this method can cause problems when two persons have fetched the same propertySheet and both want to remove a child association(first remove
     * will be ok, second person who tries to remove item with higher index that item just removed would remove wrong element or get NPE).</b> In simDhs project
     * this should not be possible<br>
     * Remove association with given index from associations list
     *
     * @param assocTypeQName
     * @param assocIndex
     * @see #getAllChildAssociations(QName)
     */
    public void removeChildAssociations(QName assocTypeQName, int assocIndex) {
        final Map<String, List<Node>> childAssocsByAssocType = getAllChildAssociationsByAssocType();
        List<Node> associatedNodes = childAssocsByAssocType.get(assocTypeQName.toString());
        addRemovedChildAssociation(assocTypeQName, associatedNodes.get(assocIndex).getNodeRefAsString());
        associatedNodes.remove(assocIndex);
    }

    public void removeChildAssociations(QName assocTypeQName, List<Node> removedAssocs) {
        final Map<String, List<Node>> childAssocsByAssocType = getAllChildAssociationsByAssocType();
        List<Node> associatedNodes = childAssocsByAssocType.get(assocTypeQName.toString());
        addRemovedChildAssociations(assocTypeQName, associatedNodes, removedAssocs);
        associatedNodes.removeAll(removedAssocs);
    }

    /**
     * @param assocTypeQName
     * @return list of childNodes associated with <code>assocTypeQName</code> or null if this node has no child associations of this type with other nodes
     * @see #getAllChildAssociationsByAssocType()
     */
    public List<Node> getAllChildAssociations(QName assocTypeQName) {
        final Map<String, List<Node>> childAssocsByAssocType = getAllChildAssociationsByAssocType();
        return childAssocsByAssocType.get(assocTypeQName.toString());
    }

    private Map<String, List<Node>> getNodesOfExistingChildAssocs() {
        final Map<String/* assocTypeQName? */, List<ChildAssociationRef>> childAssociations = getChildAssociations();
        Map<String /* assocTypeQName */, List<Node>> result = new HashMap<String, List<Node>>(childAssociations.size());
        for (Entry<String, List<ChildAssociationRef>> entry : childAssociations.entrySet()) {
            final String assocTypeQName = entry.getKey();
            final List<ChildAssociationRef> assocRefs = entry.getValue();
            List<Node> nodes = new ArrayList<Node>(assocRefs.size());
            for (ChildAssociationRef childAssociationRef : assocRefs) {
                nodes.add(new Node(childAssociationRef.getChildRef()));
            }
            addValuesToMap(result, assocTypeQName, nodes);
        }
        return result;
    }

    /**
     * Add all items from <code>valuesToAdd</code> to <code>targetMap</code> entry that has key equal to <code>mapKey</code>
     *
     * @param <K> - type of the keys in map
     * @param <V> - type of the values in map(subclass of collection)
     * @param <N> - type of the items in the collection of map value
     * @param targetMap
     * @param mapKey
     * @param valuesToAdd
     */
    private static <K, V extends Collection<N>, N> void addValuesToMap(Map<K, V> targetMap, final K mapKey, V valuesToAdd) {
        V thisAssocNodes = targetMap.get(mapKey);
        if (thisAssocNodes == null) {
            targetMap.put(mapKey, valuesToAdd);
        } else {
            thisAssocNodes.addAll(valuesToAdd);
        }
    }

    private static <K, V extends Collection<N>, N> void removeValuesFromMap(Map<K, V> targetMap, final K mapKey, V valuesToRemove) {
        V thisAssocNodes = targetMap.get(mapKey);
        if (thisAssocNodes == null) {
            return;
        }
        thisAssocNodes.removeAll(valuesToRemove);
    }

    private Map<String, List<Node>> getNodesOfAddedChildAssocs() {
        final Map<String/* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> addedChildAssocs = getAddedChildAssociations();
        return getNodesOfChildAssocsByAssocType(addedChildAssocs);
    }

    private Map<String, List<Node>> getNodesOfRemovedChildAssocs() {
        final Map<String/* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> removedChildAssocs = getRemovedChildAssociations();
        return getNodesOfChildAssocsByAssocType(removedChildAssocs);
    }

    private Map<String, List<Node>> getNodesOfChildAssocsByAssocType(
            final Map<String/* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> childAssocs) {
        Map<String /* assocTypeQName */, List<Node>> result = new HashMap<String, List<Node>>(childAssocs.size());
        for (Entry<String /* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> childAssocEntry : childAssocs.entrySet()) {
            final String assocTypeQName = childAssocEntry.getKey();
            final List<Node> removedChildNodes = getChildNodesOfEntry(childAssocEntry);
            addValuesToMap(result, assocTypeQName, removedChildNodes);
        }
        return result;
    }

    private List<Node> getChildNodesOfEntry(Entry<String, Map<String, ChildAssociationRef>> childAssocEntry) {
        final Map<String, ChildAssociationRef> desiredAssocRefs = childAssocEntry.getValue();
        final List<Node> childNodes = new ArrayList<Node>(desiredAssocRefs.size());
        for (Entry<String, ChildAssociationRef> entry : desiredAssocRefs.entrySet()) {
            final ChildAssociationRef childAssocRef = entry.getValue();
            childNodes.add(new Node(childAssocRef.getChildRef()));
        }
        return childNodes;
    }

    private void addRemovedChildAssociation(QName assocTypeQName, String nodeRefAsString) {
        final Map<String/* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> removedChildAssociations = getRemovedChildAssociations();
        Map<String, ChildAssociationRef> removedChidAssocsByRef = removedChildAssociations.get(assocTypeQName.toString());
        if (removedChidAssocsByRef == null) {
            removedChidAssocsByRef = new HashMap<String, ChildAssociationRef>(3);
            removedChildAssociations.put(assocTypeQName.toString(), removedChidAssocsByRef);
        }
        ChildAssociationRef childAssoc = new DeleteChildAssociationRef(assocTypeQName,
                getNodeRef(), new NodeRef(nodeRefAsString));
        removedChidAssocsByRef.put(nodeRefAsString, childAssoc);

    }

    private void addRemovedChildAssociations(QName assocTypeQName, List<Node> associatedNodes, List<Node> removedAssocs) {
        final Map<String/* assocTypeQName */, Map<String/* childRef */, ChildAssociationRef>> removedChildAssociations = getRemovedChildAssociations();
        Map<String, ChildAssociationRef> removedChidAssocsByRef = removedChildAssociations.get(assocTypeQName.toString());
        if (removedChidAssocsByRef == null) {
            removedChidAssocsByRef = new HashMap<String, ChildAssociationRef>(3);
            removedChildAssociations.put(assocTypeQName.toString(), removedChidAssocsByRef);
        }
        for (Node removedNode : removedAssocs) {
            String nodeRefAsString = removedNode.getNodeRefAsString();
            ChildAssociationRef childAssoc = new DeleteChildAssociationRef(assocTypeQName,
                    getNodeRef(), new NodeRef(nodeRefAsString));
            removedChidAssocsByRef.put(nodeRefAsString, childAssoc);
        }
    }

    static class DeleteChildAssociationRef extends ChildAssociationRef {
        private static final long serialVersionUID = 1L;

        public DeleteChildAssociationRef(QName assocTypeQName, NodeRef parentRef, NodeRef childRef) {
            super(assocTypeQName, parentRef, null, childRef);
        }

        @Override
        public QName getQName() {
            throw new RuntimeException("Returning QName of DeleteChildAssociationRef is unimplemented");
        }
    }

}
