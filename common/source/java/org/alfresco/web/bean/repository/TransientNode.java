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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a transient node i.e. it is not and will not be present in the repository.
 * <p>
 * This type of node is typically used to drive the property sheet when data collection
 * is required for a type but the node does not need to be stored in the repository. An
 * example use is the workflow, transient nodes are used to collect workitem metadata.
 * </p>
 * 
 * @author gavinc
 */
public class TransientNode extends Node
{
   private static final long serialVersionUID = 2140554155948154106L;
   
   private static final Log logger = LogFactory.getLog(TransientNode.class);

   /**
    * Constructor.
    * <p>
    * NOTE: The name is NOT automatically added to the map of properties,
    * if you need the name of this node to be in the map then add it to
    * the map passed in to this constructor.
    * </p>
    * 
    * @param type The type this node will represent
    * @param name The name of the node
    * @param data The properties and associations this node will have
    */
   public TransientNode(QName type, String name, Map<QName, Serializable> data)
   {
      this(null, type, name, data);
   }
   
   public TransientNode(NodeRef nodeRef, QName type, String name, Map<QName, Serializable> data)   {
      // create a dummy NodeRef to pass to the constructor
      super(nodeRef != null ? nodeRef : new NodeRef(Repository.getStoreRef(), GUID.generate()));
      
      this.type = type;
      this.name = name;
      
      // initialise the node
      initNode(data);
   }   
   
   public TransientNode(QName type, String name, Map<QName, Serializable> data, NodeRef nodeRef) 
   {
       super((nodeRef == null ? new NodeRef(Repository.getStoreRef(), GUID.generate()) : nodeRef));
       
       this.type = type;
       this.name = name;
       
       // initialise the node
       initNode(data);
   }

   /**
    * Construct a transient node for an item yet to be created in the Repository.
    * 
    * This will apply any one-time initialisation required upon creation of the node
    * e.g. assignment of default values. 
    * 
    * @param dictionaryService dictionary service
    * @param typeDef The type definition this node will represent
    * @param name The name of the node
    * @param data The properties and associations this node will have
    * @return  transient node
    */
   public static TransientNode createNew(DictionaryService dictionaryService, TypeDefinition typeDef, String name, Map<QName, Serializable> data)
   {
       // build a complete anonymous type for the start task
       Set<QName> aspectNames = new HashSet<QName>();
       getMandatoryAspects(typeDef, aspectNames);
       ClassDefinition startTaskDef = dictionaryService.getAnonymousType(typeDef.getName(), aspectNames);

       // initialise start task values
       Map<QName, Serializable> startValues = new HashMap<QName, Serializable>();
       if (data != null)
       {
           startValues.putAll(data);
       }
       
       // apply default values
       Map<QName, PropertyDefinition> propertyDefs = startTaskDef.getProperties(); 
       for (Map.Entry<QName, PropertyDefinition> entry : propertyDefs.entrySet())
       {
           String defaultValue = entry.getValue().getDefaultValue();
           if (defaultValue != null)
           {
               if (startValues.get(entry.getKey()) == null)
               {
                   startValues.put(entry.getKey(), (Serializable)DefaultTypeConverter.INSTANCE.convert(entry.getValue().getDataType(), defaultValue));
               }
           }
       }

       return new TransientNode(typeDef.getName(), name, startValues);
   }
   
   /**
    * Gets a flattened list of all mandatory aspects for a given class
    * 
    * @param classDef  the class
    * @param aspects  a list to hold the mandatory aspects
    */
   private static void getMandatoryAspects(ClassDefinition classDef, Set<QName> aspects)
   {
       for (AspectDefinition aspect : classDef.getDefaultAspects())
       {
           QName aspectName = aspect.getName();
           if (!aspects.contains(aspectName))
           {
               aspects.add(aspect.getName());
               getMandatoryAspects(aspect, aspects);
           }
       }
   }
   
   /**
    * Initialises the node.
    *
    * @param data The properties and associations to initialise the node with
    */
   protected void initNode(Map<QName, Serializable> data)
   {
      // setup the transient node so that the super class methods work
      // and do not need to go back to the repository
      
      if (logger.isDebugEnabled())
         logger.debug("Initialising transient node with data: " + data);
      
      DictionaryService ddService = this.getServiceRegistry().getDictionaryService();
      
      // marshall the given properties and associations into the internal maps
      this.associations = new QNameNodeMap(this, this);
      this.childAssociations = new QNameNodeMap(this, this);

      if (data != null)
      {
         // go through all data items and allocate to the correct internal list
         for (QName item : data.keySet())
         {
            PropertyDefinition propDef = ddService.getProperty(item);
            if (propDef != null)
            {
               this.properties.put(item, data.get(item));
            }
            else
            {
               // see if the item is either type of association
               AssociationDefinition assocDef = ddService.getAssociation(item);
               if (assocDef != null)
               {
                  if (assocDef.isChild())
                  {
                     Object obj = data.get(item);
                     if (obj instanceof NodeRef)
                     {
                        NodeRef child = (NodeRef)obj;
                        
                        // create a child association reference, add it to a list and add the list
                        // to the list of child associations for this node
                        List<ChildAssociationRef> assocs = new ArrayList<ChildAssociationRef>(1);
                        ChildAssociationRef childRef = new ChildAssociationRef(assocDef.getName(), this.nodeRef,
                              null, child);
                        assocs.add(childRef);
                        
                        this.childAssociations.put(item, assocs);
                     }
                     else if (obj instanceof List)
                     {
                        List targets = (List)obj;
                        
                        List<ChildAssociationRef> assocs = new ArrayList<ChildAssociationRef>(targets.size());
                        
                        for (Object target : targets)
                        {
                           if (target instanceof NodeRef)
                           {
                              NodeRef currentChild = (NodeRef)target;
                              ChildAssociationRef childRef = new ChildAssociationRef(assocDef.getName(),
                                    this.nodeRef, null, currentChild);
                              assocs.add(childRef);
                           }
                        }
                        
                        if (assocs.size() > 0)
                        {
                           this.childAssociations.put(item, assocs);
                        }
                     }
                  }
                  else
                  {
                     Object obj = data.get(item);
                     if (obj instanceof NodeRef)
                     {
                        NodeRef target = (NodeRef)obj;
                        
                        // create a association reference, add it to a list and add the list
                        // to the list of associations for this node
                        List<AssociationRef> assocs = new ArrayList<AssociationRef>(1);
                        AssociationRef assocRef = new AssociationRef(this.nodeRef, assocDef.getName(), target);
                        assocs.add(assocRef);
                        
                        this.associations.put(item, assocs);
                     }
                     else if (obj instanceof List)
                     {
                        List targets = (List)obj;
                        
                        List<AssociationRef> assocs = new ArrayList<AssociationRef>(targets.size());
                        
                        for (Object target : targets)
                        {
                           if (target instanceof NodeRef)
                           {
                              NodeRef currentTarget = (NodeRef)target;
                              AssociationRef assocRef = new AssociationRef(this.nodeRef, assocDef.getName(), currentTarget);
                              assocs.add(assocRef);
                           }
                        }
                        
                        if (assocs.size() > 0)
                        {
                           this.associations.put(item, assocs);
                        }
                     }
                  }
               }
            }
         }
      }
  
      // show that the maps have been initialised
      this.propsRetrieved = true;
      this.assocsRetrieved = true;
      this.childAssocsRetrieved = true;
      
      // setup the list of aspects the node would have
      TypeDefinition typeDef = ddService.getType(this.type);
      if (typeDef == null)
      {
         throw new AlfrescoRuntimeException("Failed to find type definition: " + this.type);
      }
      
      this.aspects = new HashSet<QName>();
      getMandatoryAspects(typeDef, this.aspects);
      
      // setup remaining variables
      this.path = null;
      this.locked = Boolean.FALSE;
      this.workingCopyOwner = Boolean.FALSE;
   }
   
   @Override
   public boolean hasPermission(String permission)
   {
      return true;
   }

   @Override
   public void reset()
   {
      // don't reset anything otherwise we'll lose our data
      // with no way of getting it back!!
   }
   
   @Override
   public String toString()
   {
      return "Transient node of type: " + getType() + 
             "\nProperties: " + propsToString(this.getProperties());
   }

}
