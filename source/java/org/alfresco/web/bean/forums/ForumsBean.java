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
package org.alfresco.web.bean.forums;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.transaction.UserTransaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.NodePropertyResolver;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ViewsConfigElement;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIModeList;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.renderer.data.IRichListRenderer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Bean providing properties and behaviour for the forums screens.
 * 
 * @author gavinc
 */
public class ForumsBean extends BaseDialogBean implements IContextListener
{
   private static final long serialVersionUID = 7066410060288061436L;
   
   private static Log logger = LogFactory.getLog(ForumsBean.class);
   private static final String PAGE_NAME_FORUMS = "forums";
   private static final String PAGE_NAME_FORUM = "forum";
   private static final String PAGE_NAME_TOPIC = "topic"; 
   
   /** The NodeService to be used by the bean */
   transient private NodeService nodeService;
   
   /** The ContentService to be used by the bean */
   transient private ContentService contentService;
   
   /** The DictionaryService bean reference */
   transient private DictionaryService dictionaryService;
   
   /** The SearchService bean reference. */
   transient private SearchService searchService;
   
   /** The NamespaceService bean reference. */
   transient private NamespaceService namespaceService;
   
   /** The browse bean */
   protected BrowseBean browseBean;
   
   /** The NavigationBean bean reference */
   protected NavigationBean navigator;
   
   /** Views configuration object */
   protected ViewsConfigElement viewsConfig = null;
   
   /** Component references */
   protected UIRichList forumsRichList;
   protected UIRichList forumRichList;
   protected UIRichList topicRichList;

   /** Node lists */
   private List<Node> forums;
   private List<Node> topics;
   private List<Node> posts;
   
   /** The current forums view mode - set to a well known IRichListRenderer identifier */
   private String forumsViewMode;
   
   /** The current forums view page size */
   private int forumsPageSize;
   
   /** The current forum view mode - set to a well known IRichListRenderer identifier */
   private String forumViewMode;
   
   /** The current forum view page size */
   private int forumPageSize;
   
   /** The current topic view mode - set to a well known IRichListRenderer identifier */
   private String topicViewMode;
   
   /** The current topic view page size */
   private int topicPageSize;

   private NodeRef documentNodeRef;

   private NodeRef forumNodeRef;
   
   
   // ------------------------------------------------------------------------------
   // Construction 

   /**
    * Default Constructor
    */
   public ForumsBean()
   {
      UIContextService.getInstance(FacesContext.getCurrentInstance()).registerBean(this);
      
      initFromClientConfig();
   }
   
   
   // ------------------------------------------------------------------------------
   // Bean property getters and setters 
   
   /**
    * @param nodeService The NodeService to set.
    */
   public void setNodeService(NodeService nodeService)
   {
      this.nodeService = nodeService;
   }
   
   protected NodeService getNodeService()
   {
      if (nodeService == null)
      {
         nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
      }
      return nodeService;
   }
   
   /**
    * Sets the content service to use
    * 
    * @param contentService The ContentService
    */
   public void setContentService(ContentService contentService)
   {
      this.contentService = contentService;
   }
   
   protected ContentService getContentService()
   {
      if (contentService == null)
      {
         contentService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getContentService();
      }
      return contentService;
   }

   /**
    * @param dictionaryService The DictionaryService to set.
    */
   public void setDictionaryService(DictionaryService dictionaryService)
   {
      this.dictionaryService = dictionaryService;
   }
   
   protected DictionaryService getDictionaryService()
   {
      if (dictionaryService == null)
      {
         dictionaryService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getDictionaryService();
      }
      return dictionaryService;
   }
   
   /**
    * @param searchService The SearchService to set.
    */
   public void setSearchService(SearchService searchService)
   {
      this.searchService = searchService;
   }
   
   protected SearchService getSearchService()
   {
      if (searchService == null)
      {
         searchService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getSearchService();
      }
      return searchService;
   }

   /**
    * @param namespaceService The NamespaceService to set.
    */
   public void setNamespaceService(NamespaceService namespaceService)
   {
      this.namespaceService = namespaceService;
   }
   
   protected NamespaceService getNamespaceService()
   {
      if (namespaceService == null)
      {
         namespaceService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNamespaceService();
      }
      return namespaceService;
   }

   /**
    * Sets the BrowseBean instance to use to retrieve the current document
    * 
    * @param browseBean BrowseBean instance
    */
   public void setBrowseBean(BrowseBean browseBean)
   {
      this.browseBean = browseBean;
   }
   
   /**
    * @param navigator The NavigationBean to set.
    */
   public void setNavigator(NavigationBean navigator)
   {
      this.navigator = navigator;
   }
   
   /**
    * @param forumsRichList The forumsRichList to set.
    */
   public void setForumsRichList(UIRichList forumsRichList)
   {
      this.forumsRichList = forumsRichList;
      if (this.forumsRichList != null)
      {
         // set the initial sort column and direction
         this.forumsRichList.setInitialSortColumn(
               this.viewsConfig.getDefaultSortColumn(PAGE_NAME_FORUMS));
         this.forumsRichList.setInitialSortDescending(
               this.viewsConfig.hasDescendingSort(PAGE_NAME_FORUMS));
         
         // ETWOONE-183 & ETWOONE-339. For URL addressability of forums spaces
         this.forumsRichList.setRefreshOnBind(true);
      }
   }
   
   /**
    * @return Returns the forumsRichList.
    */
   public UIRichList getForumsRichList()
   {
      return this.forumsRichList;
   }
   
   /**
    * @return Returns the forums View mode. See UIRichList
    */
   public String getForumsViewMode()
   {
      return this.forumsViewMode;
   }
   
   /**
    * @param forumsViewMode      The forums View mode to set. See UIRichList.
    */
   public void setForumsViewMode(String forumsViewMode)
   {
      this.forumsViewMode = forumsViewMode;
   }
   
   /**
    * @return Returns the forumsPageSize.
    */
   public int getForumsPageSize()
   {
      return this.forumsPageSize;
   }
   
   /**
    * @param forumsPageSize The forumsPageSize to set.
    */
   public void setForumsPageSize(int forumsPageSize)
   {
      this.forumsPageSize = forumsPageSize;
   }
   
   /**
    * @param topicRichList The topicRichList to set.
    */
   public void setTopicRichList(UIRichList topicRichList)
   {
      this.topicRichList = topicRichList;
      
      if (this.topicRichList != null)
      {
         // set the initial sort column and direction
         this.topicRichList.setInitialSortColumn(
               this.viewsConfig.getDefaultSortColumn(PAGE_NAME_TOPIC));
         this.topicRichList.setInitialSortDescending(
               this.viewsConfig.hasDescendingSort(PAGE_NAME_TOPIC));
         
         // ETWOONE-183. For URL addressability of topics:
         this.topicRichList.setRefreshOnBind(true);
      }
   }
   
   /**
    * @return Returns the topicRichList.
    */
   public UIRichList getTopicRichList()
   {
      return this.topicRichList;
   }
   
   /**
    * @return Returns the topics View mode. See UIRichList
    */
   public String getTopicViewMode()
   {
      return this.topicViewMode;
   }
   
   /**
    * @param topicViewMode      The topic View mode to set. See UIRichList.
    */
   public void setTopicViewMode(String topicViewMode)
   {
      this.topicViewMode = topicViewMode;
   }
   
   /**
    * @return Returns the topicsPageSize.
    */
   public int getTopicPageSize()
   {
      return this.topicPageSize;
   }
   
   /**
    * @param topicPageSize The topicPageSize to set.
    */
   public void setTopicPageSize(int topicPageSize)
   {
      this.topicPageSize = topicPageSize;
   }
   
   /**
    * @param forumRichList The forumRichList to set.
    */
   public void setForumRichList(UIRichList forumRichList)
   {
      this.forumRichList = forumRichList;
      
      if (this.forumRichList != null)
      {
         // set the initial sort column and direction
         this.forumRichList.setInitialSortColumn(
               this.viewsConfig.getDefaultSortColumn(PAGE_NAME_FORUM));
         this.forumRichList.setInitialSortDescending(
               this.viewsConfig.hasDescendingSort(PAGE_NAME_FORUM));
         
         // ETWOONE-183 & ETWOONE-339. For URL addressability of forum spaces
         this.forumRichList.setRefreshOnBind(true);
      }
   }
   
   /**
    * @return Returns the forumRichList.
    */
   public UIRichList getForumRichList()
   {
      return this.forumRichList;
   }
   
   /**
    * @return Returns the forum View mode. See UIRichList
    */
   public String getForumViewMode()
   {
      return this.forumViewMode;
   }
   
   /**
    * @param forumViewMode      The forum View mode to set. See UIRichList.
    */
   public void setForumViewMode(String forumViewMode)
   {
      this.forumViewMode = forumViewMode;
   }
   
   /**
    * @return Returns the forumPageSize.
    */
   public int getForumPageSize()
   {
      return this.forumPageSize;
   }
   
   /**
    * @param forumPageSize The forumPageSize to set.
    */
   public void setForumPageSize(int forumPageSize)
   {
      this.forumPageSize = forumPageSize;
   }
   
   public List<Node> getForums()
   {
      if (this.forums == null)
      {
         getNodes();
      }
      
      return this.forums;
   }
   
   public List<Node> getTopics()
   {
      if (this.topics == null)
      {
         getNodes();
      }
      
      return this.topics;
   }
   
   public List<Node> getPosts()
   {
      if (this.posts == null)
      {
         getNodes();
      }
      
      return this.posts;
   }
   
   private void getNodes()
   {
      long startTime = 0;
      if (logger.isDebugEnabled())
         startTime = System.currentTimeMillis();
      
      UserTransaction tx = null;
      try
      {
         FacesContext context = FacesContext.getCurrentInstance();
         tx = Repository.getUserTransaction(context, true);
         tx.begin();
         
         // get the current space from NavigationBean
         String parentNodeId = this.navigator.getCurrentNodeId();
         
         NodeRef parentRef;
         if (parentNodeId == null)
         {
            // no specific parent node specified - use the root node
            parentRef = this.getNodeService().getRootNode(Repository.getStoreRef());
         }
         else
         {
            // build a NodeRef for the specified Id and our store
            parentRef = new NodeRef(Repository.getStoreRef(), parentNodeId);
         }
         
         List<ChildAssociationRef> childRefs = this.getNodeService().getChildAssocs(parentRef,
               ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
         this.forums = new ArrayList<Node>(childRefs.size());
         this.topics = new ArrayList<Node>(childRefs.size());
         this.posts = new ArrayList<Node>(childRefs.size());
         
         for (ChildAssociationRef ref: childRefs)
         {
            // create our Node representation from the NodeRef
            NodeRef nodeRef = ref.getChildRef();
            
            if (this.getNodeService().exists(nodeRef))
            {
               // find it's type so we can see if it's a node we are interested in
               QName type = this.getNodeService().getType(nodeRef);
               
               // make sure the type is defined in the data dictionary
               TypeDefinition typeDef = this.getDictionaryService().getType(type);
               
               if (typeDef != null)
               {
                  // extract forums, forum, topic and post types
                  
                  if (this.getDictionaryService().isSubClass(type, ContentModel.TYPE_SYSTEM_FOLDER) == false)
                  {
                     if (this.getDictionaryService().isSubClass(type, ForumModel.TYPE_FORUMS) || 
                         this.getDictionaryService().isSubClass(type, ForumModel.TYPE_FORUM)) 
                     {
                        // create our Node representation
                        MapNode node = new MapNode(nodeRef, this.getNodeService(), true);
                        node.addPropertyResolver("icon", this.browseBean.resolverSpaceIcon);
                        node.addPropertyResolver("smallIcon", this.browseBean.resolverSmallIcon);
                        
                        this.forums.add(node);
                     }
                     if (this.getDictionaryService().isSubClass(type, ForumModel.TYPE_TOPIC)) 
                     {
                        // create our Node representation
                        MapNode node = new MapNode(nodeRef, this.getNodeService(), true);
                        node.addPropertyResolver("icon", this.browseBean.resolverSpaceIcon);
                        node.addPropertyResolver("smallIcon", this.browseBean.resolverSmallIcon);
                        node.addPropertyResolver("replies", this.resolverReplies);
                        
                        this.topics.add(node);
                     }
                     else if (this.getDictionaryService().isSubClass(type, ForumModel.TYPE_POST))
                     {
                        // create our Node representation
                        MapNode node = new MapNode(nodeRef, this.getNodeService(), true);
                        
                        this.browseBean.setupCommonBindingProperties(node);
                        node.addPropertyResolver("smallIcon", this.browseBean.resolverSmallIcon);
                        node.addPropertyResolver("message", this.resolverContent);
                        node.addPropertyResolver("replyTo", this.resolverReplyTo);
                        node.addPropertyResolver("creatorName", this.resolverCreatorName);
                        
                        this.posts.add(node);
                     }
                  }
               }
               else
               {
                  if (logger.isWarnEnabled())
                     logger.warn("Found invalid object in database: id = " + nodeRef + ", type = " + type);
               }
            }
         }
         
         // commit the transaction
         tx.commit();
      }
      catch (InvalidNodeRefException refErr)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {refErr.getNodeRef()}) );
         this.forums = Collections.<Node>emptyList();
         this.topics = Collections.<Node>emptyList();
         this.posts = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      catch (Throwable err)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), err.getMessage()), err);
         this.forums = Collections.<Node>emptyList();
         this.topics = Collections.<Node>emptyList();
         this.posts = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      
      if (logger.isDebugEnabled())
      {
         long endTime = System.currentTimeMillis();
         logger.debug("Time to query and build forums nodes: " + (endTime - startTime) + "ms");
      }
   }
   
   /**
    * Returns the HTML to represent a bubble rendition of the text of the the
    * forum article being replied to.
    * 
    * @return The HTML for the bubble
    */
   public String getReplyBubbleHTML()
   {
      try
      {
         // if the forum being replied to was a new post show the orange bubble
         // with the user on the left otherwise show the yellow bubble with the
         // user on the right.
         StringWriter writer = new StringWriter();
         
         FacesContext context = FacesContext.getCurrentInstance();
         Node replyToNode = this.browseBean.getDocument();
         boolean isReplyPost = this.getNodeService().hasAspect(replyToNode.getNodeRef(),
               ContentModel.ASPECT_REFERENCING);
         String contextPath = context.getExternalContext().getRequestContextPath();
         String colour = isReplyPost ? "yellow" : "orange";
         String bgColour = isReplyPost ? "#FFF5A3" : "#FCC75E";
         
         // get the date of the article being replied to
         String postedDate = Utils.getDateTimeFormat(context).
               format(replyToNode.getProperties().get("created"));
         
         // build the HTML to represent the user that posted the article being replied to
         replyToNode.addPropertyResolver("creatorName", resolverCreatorName);
         StringBuilder replyPosterHTML = new StringBuilder("<td class=\"bubble-header-contents\"><p class=\"bubble-creator-name\">");
         replyPosterHTML.append((String)replyToNode.getProperties().get("creatorName"));
         replyPosterHTML.append("</p>");
         replyPosterHTML.append(Application.getMessage(context, "forum_posted_on"));
         replyPosterHTML.append(":&nbsp;");
         replyPosterHTML.append(postedDate);
         replyPosterHTML.append("</td>");
         
         // start the table
         if (isReplyPost)
         {
            writer.write("<td><table border='0' cellpadding='0' cellspacing='0' width='100%' class=\"reply-to-bubble\"><tr>");
            renderReplyContentHTML(context, replyToNode, writer, contextPath, colour, bgColour);
            writer.write(replyPosterHTML.toString());
         }
         else
         {
            writer.write("<td><table border='0' cellpadding='0' cellspacing='0' width='100%' class=\"new-post-bubble\"><tr>");
            writer.write(replyPosterHTML.toString());
            renderReplyContentHTML(context, replyToNode, writer, contextPath, colour, bgColour);
         }
         
         // finish the table
         writer.write("</tr></table>");
         
         return writer.toString();
      }
      catch (IOException ioe)
      {
         throw new AlfrescoRuntimeException("Failed to render reply bubble HTML", ioe);
      }
   }
   
   // ------------------------------------------------------------------------------
   // IContextListener implementation 
   
   /**
    * @see org.alfresco.web.app.context.IContextListener#contextUpdated()
    */
   public void contextUpdated()
   {
      if (logger.isDebugEnabled())
         logger.debug("Invalidating forums components...");
      
      // clear the value for the list components - will cause re-bind to it's data and refresh
      if (this.forumsRichList != null)
      {
         this.forumsRichList.setValue(null);
         if (this.forumsRichList.getInitialSortColumn() == null)
         {
            // set the initial sort column and direction
            this.forumsRichList.setInitialSortColumn(
                  this.viewsConfig.getDefaultSortColumn(PAGE_NAME_FORUMS));
            this.forumsRichList.setInitialSortDescending(
                  this.viewsConfig.hasDescendingSort(PAGE_NAME_FORUMS));
         }
      }
      
      if (this.forumRichList != null)
      {
         this.forumRichList.setValue(null);
         if (this.forumRichList.getInitialSortColumn() == null)
         {
            // set the initial sort column and direction
            this.forumRichList.setInitialSortColumn(
                  this.viewsConfig.getDefaultSortColumn(PAGE_NAME_FORUM));
            this.forumRichList.setInitialSortDescending(
                  this.viewsConfig.hasDescendingSort(PAGE_NAME_FORUM));
         }
      }
      
      if (this.topicRichList != null)
      {
         this.topicRichList.setValue(null);
         if (this.topicRichList.getInitialSortColumn() == null)
         {
            // set the initial sort column and direction
            this.topicRichList.setInitialSortColumn(
                  this.viewsConfig.getDefaultSortColumn(PAGE_NAME_TOPIC));
            this.topicRichList.setInitialSortDescending(
                  this.viewsConfig.hasDescendingSort(PAGE_NAME_TOPIC));
         }
      }
      
      // reset the lists
      this.forums = null;
      this.topics = null;
      this.posts = null;
   }
   
   /**
    * @see org.alfresco.web.app.context.IContextListener#areaChanged()
    */
   public void areaChanged()
   {
      // nothing to do
   }

   /**
    * @see org.alfresco.web.app.context.IContextListener#spaceChanged()
    */
   public void spaceChanged()
   {
      // nothing to do
   }
   
   // ------------------------------------------------------------------------------
   // Navigation action event handlers 

   /**
    * Change the current forums view mode based on user selection
    * 
    * @param event      ActionEvent
    */
   public void forumsViewModeChanged(ActionEvent event)
   {
      UIModeList viewList = (UIModeList)event.getComponent();
      
      // get the view mode ID
      String viewMode = viewList.getValue().toString();
      
      // push the view mode into the lists
      setForumsViewMode(viewMode);
      
      // get the default for the forum page
      this.forumsPageSize = this.viewsConfig.getDefaultPageSize(PAGE_NAME_FORUMS, 
            this.forumsViewMode);
      
      if (logger.isDebugEnabled())
         logger.debug("Set default forums page size to: " + this.forumsPageSize);
   }
   
   /**
    * Change the current forum view mode based on user selection
    * 
    * @param event      ActionEvent
    */
   public void forumViewModeChanged(ActionEvent event)
   {
      UIModeList viewList = (UIModeList)event.getComponent();
      
      // get the view mode ID
      String viewMode = viewList.getValue().toString();
      
      // push the view mode into the lists
      setForumViewMode(viewMode);
      
      // get the default for the forum page
      this.forumPageSize = this.viewsConfig.getDefaultPageSize(PAGE_NAME_FORUM, 
            this.forumViewMode);
      
      if (logger.isDebugEnabled())
         logger.debug("Set default forum page size to: " + this.forumPageSize);
   }
   
   /**
    * Change the current topic view mode based on user selection
    * 
    * @param event      ActionEvent
    */
   public void topicViewModeChanged(ActionEvent event)
   {
      UIModeList viewList = (UIModeList)event.getComponent();
      
      // get the view mode ID
      String viewMode = viewList.getValue().toString();
      
      // push the view mode into the lists
      setTopicViewMode(viewMode);
      
      // change the default page size if necessary
      this.topicPageSize = this.viewsConfig.getDefaultPageSize(PAGE_NAME_TOPIC, 
            this.topicViewMode);
      
      if (logger.isDebugEnabled())
         logger.debug("Set default topic page size to: " + this.topicPageSize);
   }

   /**
    * Event handler called when a user wants to view or participate 
    * in a discussion on an object
    * 
    * @param event ActionEvent
    */
   public void discuss(ActionEvent event)
   {
       
       UIComponent c = event.getComponent();
       Map<String, String> params = null;
       String id = "";
       
       if(c instanceof UIActionLink) {
           UIActionLink link = (UIActionLink) c;
           params = link.getParameterMap();
           id = params.get("id");
           if (id == null || id.length() == 0)
           {
              throw new AlfrescoRuntimeException("discuss called without an id");
           }
       } else if (c.getChildCount() != 0) { // CommandButton or something else
           for(Object child : c.getChildren()) {
               if(child instanceof UIParameter && ((UIParameter) child).getName().equals("id")) {
                   if(((UIParameter) child).getValue() != null) {
                       id = ((UIParameter) child).getValue().toString();
                   } else {
                       id = navigator.getCurrentNodeId();
                   }
               }
           }
       }
      
      NodeRef nodeRef = new NodeRef(Repository.getStoreRef(), id);
      documentNodeRef = nodeRef;
         
      if (this.getNodeService().hasAspect(nodeRef, ForumModel.ASPECT_DISCUSSABLE) == false)
      {
         if(this.getNodeService().getType(nodeRef).equals(ForumModel.TYPE_TOPIC)) { // Navigate upwards, ugly shortcut for fix :(
             NodeRef forumRef = this.getNodeService().getPrimaryParent(nodeRef).getParentRef();
             NodeRef docRef = this.getNodeService().getPrimaryParent(forumRef).getParentRef();
             if (this.getNodeService().hasAspect(docRef, ForumModel.ASPECT_DISCUSSABLE) == false) {
                 throw new AlfrescoRuntimeException("discuss called for an object that does not have a discussion!");
             }
             nodeRef = docRef;
             documentNodeRef = docRef;
             
         } else {
             throw new AlfrescoRuntimeException("discuss called for an object that does not have a discussion!");
         }
      }
      
      // as the node has the discussable aspect there must be a discussions child assoc
      List<ChildAssociationRef> children = this.getNodeService().getChildAssocs(nodeRef, 
            ForumModel.ASSOC_DISCUSSION, RegexQNamePattern.MATCH_ALL);
      
      // there should only be one child, retrieve it if there is
      if (children.size() == 1)
      {
         // show the forum for the discussion
         forumNodeRef = children.get(0).getChildRef();
         forumId = forumNodeRef.getId();
         // set the current node Id ready for page refresh
         this.navigator.setCurrentNodeId(forumId);

         // set up the dispatch context for the navigation handler
         this.navigator.setupDispatchContext(new Node(forumNodeRef));
      }
      else
      {
         // this should never happen as the action evaluator should stop the action
         // from displaying, just in case print a warning to the console
         logger.warn("Node has the discussable aspect but does not have 1 child, it has " + 
               children.size() + " children!");
      }
   }
   
   public NodeRef getDocumentNodeRef() {
       return documentNodeRef;
   }

   public void setDocumentNodeRef(NodeRef documentNodeRef) {
       this.documentNodeRef = documentNodeRef;
   }


   public NodeRef getForumNodeRef() {
       return forumNodeRef;
   }


   public void setForumNodeRef(NodeRef forumNodeRef) {
       this.forumNodeRef = forumNodeRef;
   }
   
   // ------------------------------------------------------------------------------
   // Property Resolvers
   
   public NodePropertyResolver resolverCreatorName = new NodePropertyResolver() {
    
    private static final long serialVersionUID = 1L;

    @Override
    public Object get(Node node) {
        String userName = node.getProperties().get(ContentModel.PROP_CREATOR.toString()).toString();
        PersonService personService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPersonService();
        final Map<QName, Serializable> properties = nodeService.getProperties(personService.getPerson(userName));
        String fullName = "";
        
        if(properties.get(ContentModel.PROP_FIRSTNAME) != null)
            fullName += properties.get(ContentModel.PROP_FIRSTNAME);
        
        if(properties.get(ContentModel.PROP_FIRSTNAME) != null)
            fullName += " " + properties.get(ContentModel.PROP_LASTNAME);
        
        return (fullName.length() > 0) ? fullName : userName;
    }
};
   
   public NodePropertyResolver resolverReplies = new NodePropertyResolver() {
      private static final long serialVersionUID = -4800772273246202885L;

      public Object get(Node node) 
      {
         // query for the number of posts within the given node
         String repliesXPath = "./*[(subtypeOf('" + ForumModel.TYPE_POST + "'))]";         
         List<NodeRef> replies = getSearchService().selectNodes(
                node.getNodeRef(),
                repliesXPath,
                new QueryParameterDefinition[] {},
                getNamespaceService(),
                false);
         
         // reduce the count by 1 as one of the posts will be the initial post
         int noReplies = replies.size() - 1;
         
         if (noReplies < 0)
         {
            noReplies = 0;
         }
         
         return new Integer(noReplies);
      }
   };
   
   public NodePropertyResolver resolverContent = new NodePropertyResolver() {
      private static final long serialVersionUID = -2575377410105460440L;

      public Object get(Node node) 
      {
         String content = null;
         
         // get the content property from the node and retrieve the 
         // full content as a string (obviously should only be used
         // for small amounts of content)
         ContentReader reader = getContentService().getReader(node.getNodeRef(), 
               ContentModel.PROP_CONTENT);
         
         if (reader != null)
         {
            content = Utils.stripUnsafeHTMLTags(reader.getContentString());
         }
         
         return content;
      }
   };
   
   public NodePropertyResolver resolverReplyTo = new NodePropertyResolver() {
      private static final long serialVersionUID = 2614901755220899360L;

      public Object get(Node node) 
      {
         // determine if this node is a reply to another post, if so find
         // the creator of the original poster
         String replyTo = null;
         
         List<AssociationRef> assocs = getNodeService().getTargetAssocs(node.getNodeRef(),
               ContentModel.ASSOC_REFERENCES);
         
         // there should only be one association, if there is more than one
         // just get the first one
         if (assocs.size() > 0)
         {
            AssociationRef assoc = assocs.get(0); 
            NodeRef target = assoc.getTargetRef();
            Node targetNode = new Node(target);
            targetNode.addPropertyResolver("creatorName", resolverCreatorName);
            replyTo = (String)targetNode.getProperties().get("creatorName");
         }
         
         return replyTo;
      }
   };
   
   /**
    * Creates a file name for the message being posted
    * 
    * @return The file name for the post
    */
   public static String createPostFileName()
   {
      StringBuilder name = new StringBuilder("posted-");
      
      // add a timestamp
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
      name.append(dateFormat.format(new Date()));
      
      // add Universal Unique Identifier
      // fix bugs ETWOONE-196 and ETWOONE-203
      name.append("-" + UUID.randomUUID());
      
      // add the HTML file extension
      name.append(".html");
      
      return name.toString();
   }
   
   
   // ------------------------------------------------------------------------------
   // Helpers
   
   /**
    * Initialise default values from client configuration
    */
   private void initFromClientConfig()
   {
      // TODO - review implications of these default values for dynamic/MT client
      this.viewsConfig = (ViewsConfigElement)Application.getConfigService(
            FacesContext.getCurrentInstance()).getConfig("Views").
            getConfigElement(ViewsConfigElement.CONFIG_ELEMENT_ID);
      
      // get the defaults for the forums page
      this.forumsViewMode = this.viewsConfig.getDefaultView(PAGE_NAME_FORUMS);
      this.forumsPageSize = this.viewsConfig.getDefaultPageSize(PAGE_NAME_FORUMS,
            this.forumsViewMode);
      
      // get the default for the forum page
      this.forumViewMode = this.viewsConfig.getDefaultView(PAGE_NAME_FORUM);
      this.forumPageSize = this.viewsConfig.getDefaultPageSize(PAGE_NAME_FORUM, 
            this.forumViewMode);
      
      // get the default for the topic page
      this.topicViewMode = this.viewsConfig.getDefaultView(PAGE_NAME_TOPIC);
      this.topicPageSize = this.viewsConfig.getDefaultPageSize(PAGE_NAME_TOPIC, 
            this.topicViewMode);
      
      if (logger.isDebugEnabled())
      {
         logger.debug("Set default forums view mode to: " + this.forumsViewMode);
         logger.debug("Set default forums page size to: " + this.forumsPageSize);
         logger.debug("Set default forum view mode to: " + this.forumViewMode);
         logger.debug("Set default forum page size to: " + this.forumPageSize);
         logger.debug("Set default topic view mode to: " + this.topicViewMode);
         logger.debug("Set default topic page size to: " + this.topicPageSize);
      }
   }
   
   protected void renderReplyContentHTML(FacesContext context, 
         Node replyToNode, StringWriter writer, 
         String contextPath, String colour, String bgColour) 
         throws IOException
   {
      // get the content of the article being replied to
      String replyContent = "";
      ContentReader reader = this.getContentService().getReader(replyToNode.getNodeRef(), 
            ContentModel.PROP_CONTENT);  
      if (reader != null)
      {
         replyContent = Utils.stripUnsafeHTMLTags(reader.getContentString());
      }
      
      // generate the HTML
      writer.write("<td width='75%'>");
      writer.write(replyContent);
      writer.write("</td>");
   }
   
   /**
    * Class to implement a bubble view for the RichList component used in the topics screen
    * 
    * @author gavinc
    */
   public static class TopicBubbleViewRenderer implements IRichListRenderer
   {
      private static final long serialVersionUID = -6641033880549363822L;
      
      public static final String VIEWMODEID = "bubble";
      
      public String getViewModeID()
      {
         return VIEWMODEID;
      }
      
      public void renderListBefore(FacesContext context, UIRichList richList, UIColumn[] columns)
            throws IOException
      {
         // nothing to do
      }
      
      public void renderListRow(FacesContext context, UIRichList richList, UIColumn[] columns, Object row)
            throws IOException
      {
         ResponseWriter out = context.getResponseWriter();
         
         // find primary column (which must exist) and the actions column (which doesn't 
         // have to exist)
         UIColumn primaryColumn = null;
         UIColumn actionsColumn = null;
         for (int i = 0; i < columns.length; i++)
         {
            if (columns[i].isRendered())
            {
               if (columns[i].getPrimary())
               {
                  primaryColumn = columns[i];
               }
               else if (columns[i].getActions())
               {
                  actionsColumn = columns[i];
               }
            }
         }
         
         if (primaryColumn == null)
         {
            if (logger.isWarnEnabled())
               logger.warn("No primary column found for RichList definition: " + richList.getId());
         }
         
         out.write("<tr>");
         
         Node node = (Node)row;
         if (node.getProperties().get("replyTo") == null)
         {
            renderNewPostBubble(context, out, node, primaryColumn, actionsColumn, columns);
         }
         else
         {
            renderReplyToBubble(context, out, node, primaryColumn, actionsColumn, columns);
         }
         
         out.write("</tr>");
         
         // add a little padding
         out.write("<tr><td><div style='padding:3px'></div></td></tr>");
      }
      
      public void renderListAfter(FacesContext context, UIRichList richList, UIColumn[] columns)
            throws IOException
      {
         ResponseWriter out = context.getResponseWriter();
         
         out.write("<tr><td colspan='99' align='center'>");
         for (Iterator<?> i = richList.getChildren().iterator(); i.hasNext(); /**/)
         {
            // output all remaining child components that are not UIColumn
            UIComponent child = (UIComponent)i.next();
            if (child instanceof UIColumn == false)
            {
               Utils.encodeRecursive(context, child);
            }
         }
         out.write("</td></tr>");
      }
      
      /**
       * Renders the top part of the bubble i.e. before the header
       * 
       * @param out The writer to output to
       * @param contextPath Context path of the application
       * @param colour The colour of the bubble
       * @param titleBgColour Background colour of the header area
       */
      public static void renderBubbleTop(Writer out, String contextPath, 
            String colour, String titleBgColour) throws IOException
      {

      }
      
      /**
       * Renders the middle part of the bubble i.e. after the header and before the body
       * 
       * @param out The writer to output to
       * @param contextPath Context path of the application
       * @param colour The colour of the bubble
       */
      public static void renderBubbleMiddle(Writer out, String contextPath, String colour) 
            throws IOException
      {
      }
      
      /**
       * Renders the bottom part of the bubble i.e. after the body
       * 
       * @param out The writer to output to
       * @param contextPath Context path of the application
       * @param colour The colour of the bubble
       */
      public static void renderBubbleBottom(Writer out, String contextPath, String colour)
            throws IOException
      {
      }
      
      /**
       * Renders the new post speech bubble
       * 
       * @param context Faces context
       * @param out The response writer
       * @param node The Node for the row being rendered
       * @param primaryColumn The primary column containing the message content
       * @param actionsColumn The actions column containing all the actions
       * @param columns All configured columns
       */
      private void renderNewPostBubble(FacesContext context, ResponseWriter out, Node node, 
            UIColumn primaryColumn, UIColumn actionsColumn, UIColumn[] columns) throws IOException
      {
         String contextPath = context.getExternalContext().getRequestContextPath();
         String colour = "orange";
         
         out.write("<td><table border='0' cellpadding='0' cellspacing='0' width='100%' class=\"new-post-bubble\"><tr>");
         out.write("<td class=\"bubble-header-contents\"><p class=\"bubble-creator-name\">");
         out.write((String)node.getProperties().get("creatorName"));
         renderActions(context, out, actionsColumn);
         out.write("</p>");
         renderHeaderContents(context, out, primaryColumn, columns);
         out.write("</td><td width='75%'>");
         
         renderBubbleTop(out, contextPath, colour, "#FCC75E");
         renderBubbleMiddle(out, contextPath, colour);
         renderBodyContents(context, primaryColumn);
         renderBubbleBottom(out, contextPath, colour);
         
         out.write("</td></table></td>");
      }
      
      /**
       * Renders the reply to post speech bubble
       * 
       * @param context Faces context
       * @param out The response writer
       * @param node The Node for the row being rendered
       * @param primaryColumn The primary column containing the message content
       * @param actionsColumn The actions column containing all the actions
       * @param columns All configured columns
       */
      private void renderReplyToBubble(FacesContext context, ResponseWriter out, Node node, 
            UIColumn primaryColumn, UIColumn actionsColumn, UIColumn[] columns) throws IOException
      {
         String contextPath = context.getExternalContext().getRequestContextPath();
         String colour = "yellow";
         
         out.write("<td width='100%'><table border='0' cellpadding='0' cellspacing='0' width='100%' class=\"reply-to-bubble\"><tr>");
         out.write("<td width='75%'>");
         
         renderBubbleTop(out, contextPath, colour, "#FFF5A3");
         renderBubbleMiddle(out, contextPath, colour);
         renderBodyContents(context, primaryColumn);
         renderBubbleBottom(out, contextPath, colour);
         
         out.write("</td><td class=\"bubble-header-contents\"><p class=\"bubble-creator-name\">");
         out.write((String)node.getProperties().get("creatorName"));
         renderActions(context, out, actionsColumn);
         out.write("</p>");
         renderHeaderContents(context, out, primaryColumn, columns);
         out.write("</td></table></td>");
      }

      private void renderHeaderContents(FacesContext context, ResponseWriter out,  
            UIColumn primaryColumn, UIColumn[] columns) throws IOException
      {
         // render the header area with the configured columns
         out.write("<table cellpadding='0' cellspacing='0' border='0'>");
         
         for (int i = 0; i < columns.length; i++)
         {
            UIColumn column = columns[i];
            
            if (column.isRendered() == true &&
                column.getPrimary() == false && 
                column.getActions() == false)
            {
                out.write("<tr>");
               // render the column header as the label
               UIComponent header = column.getFacet("header");
               if (header != null)
               {
                  out.write("<td><b>");
                  Utils.encodeRecursive(context, header);
                  out.write("</b></td>");
               }
               
               // render the contents of the column
               if (column.getChildCount() != 0)
               {
                  out.write("<td>");
                  Utils.encodeRecursive(context, column);
                  out.write("</td>");
               }
               out.write("</tr>");
            }
         }
         out.write("</table>");
      }

    /**
     * @param context
     * @param out
     * @param actionsColumn
     * @throws IOException
     */
    private void renderActions(FacesContext context, ResponseWriter out, UIColumn actionsColumn) throws IOException {
         if (actionsColumn != null && actionsColumn.getChildCount() != 0)
         {
            out.write("<span class=\"bubble-actions\">");
            Utils.encodeRecursive(context, actionsColumn);
            out.write("</span>");
         }
    }
      
      /**
       * Renders the body contents for the bubble using the given primary column 
       * 
       * @param context Faces context
       * @param primaryColumn The primary column holding the message text
       */
      private void renderBodyContents(FacesContext context, UIColumn primaryColumn)
            throws IOException
      {
         // render the primary column
         if (primaryColumn != null && primaryColumn.getChildCount() != 0)
         {
            Utils.encodeRecursive(context, primaryColumn);
         }
      }
   }


    ///////////////////////////////////////////////////
    // Converting forums to dialogs
    //////////////////////////////////////////////////
   
    public String forumId;
    
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // nothing to do
        return null;
    }

    @Override
    public String cancel() {
        if(getForumId() != null) {
            navigator.setCurrentNodeId(forumId);
        }
        return "dialog:close";
    }

    public void setupTopicNavigatorCurrentNodeId(ActionEvent event) {
        setForumId(navigator.getCurrentNodeId());
        navigator.setCurrentNodeId(ActionUtil.getParam(event, "id"));
    }

    @Override
    public String getContainerTitle() {
        if(navigator.getCurrentNode().getType().equals(ForumModel.TYPE_FORUM)) {
            NodeRef parentNodeRef = getNodeService().getPrimaryParent(navigator.getCurrentNode().getNodeRef()).getParentRef();
            return MessageUtil.getMessage("discussion_for", getNodeService().getProperty(parentNodeRef, DocumentCommonModel.Props.DOC_NAME).toString());
        }
        return navigator.getNodeProperties().get("name").toString();
    }

    @Override
    public Object getActionsContext() {
        return navigator.getCurrentNode();
    }
    
    public String getForumId() {
        return forumId;
    }

    public void setForumId(String forumId) {
        this.forumId = forumId;
    }

}
