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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolverProvider;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.generator.BaseComponentGenerator;

/**
 * Lighweight client side representation of a node held in the repository. 
 * 
 * @author gavinc
 */
public class Node implements Serializable, NamespacePrefixResolverProvider
{
   private static final org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(Node.class);
   private static final long serialVersionUID = 3544390322739034170L;

   protected NodeRef nodeRef;
   protected String name;
   protected QName type;
   protected Path path;
   protected String id;
   protected Set<QName> aspects = null;
   protected Map<String, Boolean> permissions;
   protected Boolean locked = null;
   protected Boolean workingCopyOwner = null;
   protected QNameNodeMap<String, Object> properties;
   protected boolean propsRetrieved = false;
   protected transient ServiceRegistry services = null;
   protected boolean childAssocsRetrieved = false;
   protected QNameNodeMap<String, List<ChildAssociationRef>> childAssociations;
   protected boolean assocsRetrieved = false;
   protected QNameNodeMap<String, List<AssociationRef>> associations;
   
   private Map<String/*assocTypeQName*/, Map<String/*childRef*/, ChildAssociationRef>> childAssociationsAdded;
   private Map<String/*assocTypeQName*/, Map<String/*childRef*/, ChildAssociationRef>> childAssociationsRemoved;
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
      this.id = nodeRef.getId();
      
      this.properties = new QNameNodeMap<String, Object>(this, this);
   }

   /**
    * @return All the properties known about this node.
    */
   public Map<String, Object> getProperties()
   {
      if (this.propsRetrieved == false)
      {
         Map<QName, Serializable> props = getServiceRegistry().getNodeService().getProperties(this.nodeRef);
         
         for (QName qname: props.keySet())
         {
            Serializable propValue = props.get(qname);
            
            // Lists returned from the node service could be unmodifiable,
            // therefore create copies for modification purposes
            if (propValue instanceof List)
            {
               propValue = new ArrayList((List)propValue);
            }
            
            this.properties.put(qname.toString(), propValue);
         }
         
         this.propsRetrieved = true;
      }
      
      return this.properties;
   }
   
   /**
    * @return All the associations this node has as a Map, using the association
    *         type as the key
    */
   public final Map getAssociations()
   {
      if (this.assocsRetrieved == false)
      {
         this.associations = new QNameNodeMap<String, List<AssociationRef>>(this, this);
         
         List<AssociationRef> assocs = getServiceRegistry().getNodeService().getTargetAssocs(this.nodeRef, RegexQNamePattern.MATCH_ALL);
         
         for (AssociationRef assocRef: assocs)
         {
            String assocName = assocRef.getTypeQName().toString();
            
            List<AssociationRef> list = (List<AssociationRef>) this.associations.get(assocName);
            // create the list if this is first association with 'assocName'
            if (list == null)
            {
               list = new ArrayList<AssociationRef>();
               this.associations.put(assocName, list);
            }
            
            // add the association to the list
            list.add(assocRef);
         }
         
         this.assocsRetrieved = true;
      }
      
      return this.associations;
   }
   
   /**
    * Returns all the associations added to this node in this UI session
    * 
    * @return Map of Maps of AssociationRefs
    */
   public final Map<String, Map<String, AssociationRef>> getAddedAssociations()
   {
      if (this.associationsAdded == null)
      {
         this.associationsAdded = new HashMap<String, Map<String, AssociationRef>>();
      }
      return this.associationsAdded;
   }
   
   /**
    * Returns all the associations removed from this node is this UI session
    * 
    * @return Map of Maps of AssociationRefs
    */
   public final Map<String, Map<String, AssociationRef>> getRemovedAssociations()
   {
      if (this.associationsRemoved == null)
      {
         this.associationsRemoved = new HashMap<String, Map<String, AssociationRef>>();
      }
      return this.associationsRemoved;
   }
   
   /**
    * @return All the child associations this node has as a Map, using the association
    *         type as the key
    */
   public final Map<String/*assocTypeQName*/, List<ChildAssociationRef>> getChildAssociations()
   {
      if (this.childAssocsRetrieved == false)
      {
         this.childAssociations = new QNameNodeMap<String, List<ChildAssociationRef>>(this, this);
         
         List<ChildAssociationRef> assocs = getServiceRegistry().getNodeService().getChildAssocs(this.nodeRef);
         
         for (ChildAssociationRef assocRef: assocs)
         {
            String assocName = assocRef.getTypeQName().toString();
            
            List<ChildAssociationRef> list = (List<ChildAssociationRef>)this.childAssociations.get(assocName);
            // create the list if this is first association with 'assocName'
            if (list == null)
            {
               list = new ArrayList<ChildAssociationRef>();
               this.childAssociations.put(assocName, list);
            }
            
            // add the association to the list
            list.add(assocRef);
         }
         
         this.childAssocsRetrieved = true;
      }
      
      return this.childAssociations;
   }
   
   /**
    * Returns all the child associations added to this node in this UI session
    * 
    * @return Map of Maps of ChildAssociationRefs
    */
   public final Map<String/*assocTypeQName*/, Map<String/*childRef*/, ChildAssociationRef>> getAddedChildAssociations()
   {
      if (this.childAssociationsAdded == null)
      {
         this.childAssociationsAdded = new HashMap<String, Map<String, ChildAssociationRef>>();
      }
      return this.childAssociationsAdded;
   }
   
   /**
    * Returns all the child associations removed from this node is this UI session
    * 
    * @return Map of Maps of ChildAssociationRefs
    */
   public final Map<String/*assocTypeQName*/, Map<String/*childRef*/, ChildAssociationRef>> getRemovedChildAssociations()
   {
      if (this.childAssociationsRemoved == null)
      {
         this.childAssociationsRemoved = new HashMap<String, Map<String, ChildAssociationRef>>();
      }
      return this.childAssociationsRemoved;
   }
   
   /**
    * Register a property resolver for the named property.
    * 
    * @param name       Name of the property this resolver is for
    * @param resolver   Property resolver to register
    */
   public final void addPropertyResolver(String name, NodePropertyResolver resolver)
   {
      this.properties.addPropertyResolver(name, resolver);
   }
   
   /**
    * Returns if a property resolver with a specific name has been applied to the Node
    *  
    * @param name of property resolver to look for
    * 
    * @return true if a resolver with the name is found, false otherwise
    */
   public final boolean containsPropertyResolver(String name)
   {
      return this.properties.containsPropertyResolver(name);
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
      return this.nodeRef;
   }
   
   /**
    * @return Returns the string form of the NodeRef this Node represents
    */
   public final String getNodeRefAsString()
   {
      if (this.nodeRef == null)
      {
          return null;
      }
      return this.nodeRef.toString();
   }
   
   /**
    * @return Returns the type.
    */
   public QName getType()
   {
      if (this.type == null)
      {
         this.type = getServiceRegistry().getNodeService().getType(this.nodeRef);
      }
      
      return type;
   }
   
   /**
    * Set the type.
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
      if (this.name == null)
      {
         // try and get the name from the properties first
         this.name = (String)getProperties().get("cm:name");
         
         // if we didn't find it as a property get the name from the association name
         if (this.name == null)
         {
            this.name = getServiceRegistry().getNodeService().getPrimaryParent(this.nodeRef).getQName().getLocalName(); 
         }
      }
      
      return this.name;
   }

   /**
    * @return The list of aspects applied to this node
    */
   public final Set<QName> getAspects()
   {
      if (this.aspects == null)
      {
         this.aspects = getServiceRegistry().getNodeService().getAspects(this.nodeRef);
      }
      
      return this.aspects;
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
   
   /**
    * Return whether the current user has the specified access permission on this Node
    * 
    * @param permission     Permission to validate against
    * 
    * @return true if the permission is applied to the node for this user, false otherwise
    */
   public boolean hasPermission(String permission)
   {
      Boolean valid = null;
      if (this.permissions != null)
      {
         valid = this.permissions.get(permission);
      }
      else
      {
         this.permissions = new HashMap<String, Boolean>(8, 1.0f);
      }
      
      if (valid == null)
      {
         PermissionService service = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPermissionService();
         valid = Boolean.valueOf(service.hasPermission(this.nodeRef, permission) == AccessStatus.ALLOWED);
         this.permissions.put(permission, valid);
      }
      
      return valid.booleanValue();
   }

   /**
    * @return The GUID for the node
    */
   public final String getId()
   {
      return this.id;
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
      if (this.path == null)
      {
         this.path = getServiceRegistry().getNodeService().getPath(this.nodeRef);
      }
      return this.path;
   }
   
   /**
    * @return If the node is currently locked
    */
   public final boolean isLocked()
   {
      if (this.locked == null)
      {
         this.locked = Boolean.FALSE;
         
         if (hasAspect(ContentModel.ASPECT_LOCKABLE))
         {
            LockStatus lockStatus = getServiceRegistry().getLockService().getLockStatus(getNodeRef());
            if (lockStatus == LockStatus.LOCKED || lockStatus == LockStatus.LOCK_OWNER)
            {
               locked = Boolean.TRUE;
            }
         }
      }
      
      return this.locked.booleanValue();
   }
   
   /**
    * @return whether a the Node is a WorkingCopy owned by the current User
    */
   public final boolean isWorkingCopyOwner()
   {
      if (this.workingCopyOwner == null)
      {
         this.workingCopyOwner = Boolean.FALSE;
         
         if (hasAspect(ContentModel.ASPECT_WORKING_COPY))
         {
            Object obj = getProperties().get(ContentModel.PROP_WORKING_COPY_OWNER);
            if (obj instanceof String)
            {
               User user = Application.getCurrentUser(FacesContext.getCurrentInstance());
               if ( ((String)obj).equals(user.getUserName()))
               {
                  this.workingCopyOwner = Boolean.TRUE;
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
      this.name = null;
      this.type = null;
      this.path = null;
      this.locked = null;
      this.workingCopyOwner = null;
      this.properties.clear();
      this.propsRetrieved = false;
      this.aspects = null;
      this.permissions = null;
      
      this.associations = null;
      this.associationsAdded = null;
      this.associationsRemoved = null;
      this.assocsRetrieved = false;
      
      this.childAssociations = null;
      this.childAssociationsAdded = null;
      this.childAssociationsRemoved = null;
      this.childAssocsRetrieved = false;
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
                   "\nNode Properties: " + propsToString(this.getProperties()) + 
                   "\nNode Aspects: " + this.getAspects().toString();
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
      if (this.services == null)
      {
          this.services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
      }
      return this.services;
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

   public NamespacePrefixResolver getNamespacePrefixResolver()
   {
      return getServiceRegistry().getNamespaceService();
   }
   
   

   
   
    protected boolean allChildAssociationsByAssocTypeRetrieved = false;
    private Map<String/* assocTypeQName */, List<Node>> allChildAssociationsByAssocType;

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
