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
package org.alfresco.web.bean.trashcan;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyWildcardQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.archive.RestoreNodeReport;
import org.alfresco.repo.node.archive.RestoreNodeReport.RestoreStatus;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.web.scripts.FileTypeImageUtils;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.CachingDateFormat;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.NodePropertyResolver;
import org.alfresco.web.bean.repository.QNameNodeMap;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.spaces.CreateSpaceWizard;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.Utils.URLMode;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIModeList;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Backing bean for the Manage Deleted Items (soft delete and archiving) pages.
 * 
 * @author Kevin Roast
 */
public class TrashcanDialog extends BaseDialogBean implements IContextListener
{
   private static final long serialVersionUID = -7783683979079046969L;

   protected TrashcanDialogProperty property;
   
   private static final String MSG_DELETED_ITEMS_FOR = "deleted_items_for";
   private static final String MSG_DELETED_ITEMS = "deleted_items";
   private static final String MSG_RECOVERED_ITEM_INTEGRITY_S = "recovered_item_integrity_short";
   private static final String MSG_RECOVERED_ITEM_PERMISSION_S = "recovered_item_permission_short";
   private static final String MSG_RECOVERED_ITEM_PARENT_S = "recovered_item_parent_short";
   private static final String MSG_RECOVERED_ITEM_FAILURE_S = "recovered_item_failure_short";
   private static final String MSG_RECOVERY_REASON = "recovery_report_reason";
   private static final String MSG_LOCATION = "original_location";
   private static final String MSG_NAME = "name";
   private final static String MSG_CLOSE = "close";
   
   private static final String PROP_RECOVERSTATUS = "recoverstatus";
   
   private static final String FILTER_TYPE_ALL    = "all";
   private static final String FILTER_DATE_ALL    = "all";
   private static final String FILTER_DATE_TODAY  = "today";
   private static final String FILTER_DATE_WEEK   = "week";
   private static final String FILTER_DATE_MONTH  = "month";
   private static final String FILTER_USER_USER   = "user";
   private static final String FILTER_DOCU_TYPE   = "type";   
   
   private static final String OUTCOME_DIALOGCLOSE = "dialog:close";
   
   private final static String USER_ATTR = Repository.escapeQName(ContentModel.PROP_ARCHIVED_BY);
   private final static String DATE_ATTR = Repository.escapeQName(ContentModel.PROP_ARCHIVED_DATE);
   
   private final static String SEARCH_USERPREFIX  = "@" + USER_ATTR + ":%s";
   
   private boolean skipRefresh = false;
   
   @Override
   public void restored(){
       refresh();
   }
   
   @Override
   public void init(Map<String, String> parameters) {
       super.init(parameters);
       property.resetFilters();
       contextUpdated();       
   }
   
   public void setProperty(TrashcanDialogProperty property)
   {
      this.property = property;
   }

   public TrashcanDialogProperty getProperty()
   {
      return property;
   }
   
   /**
    * @return Message to display in the title of the panel area
    */
   public String getPanelMessage()
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      String msg = Application.getMessage(fc, MSG_DELETED_ITEMS);
      if (isAdminUser() == false)
      {
         msg = msg + ' ' + MessageFormat.format(
               Application.getMessage(fc, MSG_DELETED_ITEMS_FOR), Application.getCurrentUser(fc).getUserName());
      }
      return msg;
   }
   
   /**
    * Returns the URL to the content for the current document item
    *  
    * @return Content url to the current document item
    */
   public String getItemBrowserUrl()
   {
      return Utils.generateURL(FacesContext.getCurrentInstance(), property.getItem(), URLMode.HTTP_INLINE);
   }

   /**
    * Returns the download URL to the content for the current document item
    *  
    * @return Download url to the current document item
    */
   public String getItemDownloadUrl()
   {
      return Utils.generateURL(FacesContext.getCurrentInstance(), property.getItem(), URLMode.HTTP_DOWNLOAD);
   }
   
   /**
    * Return the Alfresco NodeRef URL for the current item node
    * 
    * @return the Alfresco NodeRef URL
    */
   public String getItemNodeRefUrl()
   {
      return property.getItem().getNodeRef().toString();
   }
   
   /**
    * @return HTML table of the listed items
    */
   public String getListedItemsTable()
   {
      return buildItemsTable(property.getListedItems(), "recoveredItemsList", false, true);
   }
   
   /**
    * @return HTML table of the items successfully recovered 
    */
   public String getSuccessItemsTable()
   {
      return buildItemsTable(property.getSuccessItems(), "recoveredItemsList", false, false);
   }
   
   /**
    * @return HTML table of the items that failed to recover
    */
   public String getFailureItemsTable()
   {
      return buildItemsTable(property.getFailureItems(), "failedItemsList", true, false);
   }
   
   /**
    * @return the list of deleted items to display
    */
   public List<Node> getItems()
   {
      // to get deleted items from deleted items store
      // use a search to find the items - also filters by name/username
      List<Node> itemNodes = null;
      
      UserTransaction tx = null;
      ResultSet results = null;
      try
      {
         tx = Repository.getUserTransaction(FacesContext.getCurrentInstance(), true);
         tx.begin();
         
         // get the root node to the deleted items store
         if (getArchiveRootRef() != null && property.isShowItems())
         {
            String query = buildSearchQuery(property);
            final SearchParameters sp = new SearchParameters();
            sp.setLanguage(SearchService.LANGUAGE_LUCENE);
            sp.setQuery(query);
            sp.addStore(getArchiveRootRef().getStoreRef());     // the Archived Node store
            results = getSearchService().query(sp);
            itemNodes = new ArrayList<Node>(results.length());
         }
         
         if (results != null && results.length() != 0)
         {
            for (ResultSetRow row : results)
            {
               NodeRef nodeRef = row.getNodeRef();
               
               if (getNodeService().exists(nodeRef))
               {
                  QName type = getNodeService().getType(nodeRef);
                  
                  MapNode node = new MapNode(nodeRef, getNodeService(), false);
                  node.addPropertyResolver("locationPath", resolverLocationPath);
                  node.addPropertyResolver("displayPath", resolverDisplayPath);
                  node.addPropertyResolver("deletedDate", resolverDeletedDate);
                  node.addPropertyResolver("deletedBy", resolverDeletedBy);
                  node.addPropertyResolver("isFolder", resolverIsFolder);
                  node.addPropertyResolver("objectName", resolverObjectName);
                  node.addPropertyResolver("objectType", resolverObjectType);
                  node.addPropertyResolver("objectTypeStr", resolverObjectTypeStr);
                  node.addPropertyResolver("downloadURL", resolverDownloadURL);
                  
                  if (getDictionaryService().isSubClass(type, ContentModel.TYPE_FOLDER) == true &&
                      getDictionaryService().isSubClass(type, ContentModel.TYPE_SYSTEM_FOLDER) == false)
                  {
                     node.addPropertyResolver("typeIcon", this.resolverSmallIcon);
                  }
                  else
                  {
                     node.addPropertyResolver("typeIcon", this.resolverFileType16);
                  }
                  itemNodes.add(node);
               }
            }
         }
         
         tx.commit();
      }
      catch (Throwable err)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), new Object[] {err.getMessage()}), err );
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      finally
      {
         if (results != null)
         {
            results.close();
         }
      }
      
      property.setListedItems((itemNodes != null ? itemNodes : Collections.<Node> emptyList()));
      
      return property.getListedItems();
   }
   
   private NodePropertyResolver resolverObjectName = new NodePropertyResolver() {
       private static final long serialVersionUID = -9181535522349485509L;
       
       @Override
       public Object get(Node node) {
           Map<String, Object> properties = node.getProperties();
           Object archivedObjectName = properties.get(ContentModel.PROP_ARCHIVED_OBJECT_NAME);
           if (archivedObjectName == null) {
               QName type = node.getType();
               archivedObjectName = RepoUtil.getArchivedObjectName(type, RepoUtil.toQNameProperties(properties));
           }
           return archivedObjectName;
       }
    };
    
    private NodePropertyResolver resolverObjectType = new NodePropertyResolver() {
        private static final long serialVersionUID = -9181535522349485509L;
       
        @Override
        public Object get(Node node) {
            return node.getProperties().get(ContentModel.PROP_ARCHIVED_OBJECT_TYPE);
        }
    };
    
    private NodePropertyResolver resolverObjectTypeStr = new NodePropertyResolver() {
        private static final long serialVersionUID = -9181535522349485509L;

        @Override
        public Object get(Node node) {
            return node.getProperties().get(ContentModel.PROP_ARCHIVED_OBJECT_TYPE_STRING);
        }
    };
     
    private NodePropertyResolver resolverDownloadURL = new NodePropertyResolver() {
        private static final long serialVersionUID = -9181535522349485509L;

        @Override
        public Object get(Node node) {
            return BeanHelper.getFileService().generateURL(node.getNodeRef());
        }
    };
   
   private NodePropertyResolver resolverLocationPath = new NodePropertyResolver()
   {
      private static final long serialVersionUID = -2501720368642759082L;
      
      public Object get(Node node)
      {
         ChildAssociationRef childRef = (ChildAssociationRef)node.getProperties().get(ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC);
         if (getNodeService().exists(childRef.getParentRef()))
         {
            return getNodeService().getPath(childRef.getParentRef());
         }
         else
         {
            return null;
         }
      }
   };
   
   private NodePropertyResolver resolverDisplayPath = new NodePropertyResolver()
   {
      private static final long serialVersionUID = 9178556770343499694L;
      
      public Object get(Node node)
      {
          return node.getProperties().get(ContentModel.PROP_ARCHIVED_ORIGINAL_LOCATION_STRING);
      }
   };
   
   private NodePropertyResolver resolverFileType16 = new NodePropertyResolver()
   {
      private static final long serialVersionUID = 7462526266770371703L;
      
      public Object get(Node node)
      {
         return FileTypeImageUtils.getFileTypeImage(node.getName(), true);
      }
   };
   
   private NodePropertyResolver resolverSmallIcon = new NodePropertyResolver()
   {
      private static final long serialVersionUID = 5528945140207247127L;

      @SuppressWarnings("unchecked")
      public Object get(Node node)
      {
         QNameNodeMap props = (QNameNodeMap)node.getProperties();
         String icon = (String) props.getRaw("app:icon");
         return "/images/icons/" + (icon != null ? icon + "-16.gif" : BrowseBean.SPACE_SMALL_DEFAULT + ".gif");
      }
   };
   
   private NodePropertyResolver resolverFileType32 = new NodePropertyResolver()
   {
      private static final long serialVersionUID = -5681639025578263060L;
      
      public Object get(Node node)
      {
         return FileTypeImageUtils.getFileTypeImage(node.getName(), false);
      }
   };
   
   private NodePropertyResolver resolverLargeIcon = new NodePropertyResolver()
   {
      private static final long serialVersionUID = -8334570770580388654L;
      
      @SuppressWarnings("unchecked")
      public Object get(Node node)
      {
         QNameNodeMap props = (QNameNodeMap)node.getProperties();
         String icon = (String) props.getRaw("app:icon");
         return "/images/icons/" + (icon != null ? icon : CreateSpaceWizard.DEFAULT_SPACE_ICON_NAME) + ".gif";
      }
   };
   
   private NodePropertyResolver resolverMimetype = new NodePropertyResolver()
   {
      private static final long serialVersionUID = -5892550146037635522L;
      
      public Object get(Node node)
      {
         ContentData content = (ContentData)node.getProperties().get(ContentModel.PROP_CONTENT);
         return (content != null ? content.getMimetype() : null);
      }
   };
   
   private NodePropertyResolver resolverSize = new NodePropertyResolver()
   {
      private static final long serialVersionUID = -191591211947393578L;
      
      public Object get(Node node)
      {
         ContentData content = (ContentData)node.getProperties().get(ContentModel.PROP_CONTENT);
         return (content != null ? new Long(content.getSize()) : 0L);
      }
   };
   
   private NodePropertyResolver resolverEncoding = new NodePropertyResolver()
   {
      private static final long serialVersionUID = -1594354572323978873L;
      
      public Object get(Node node)
      {
         ContentData content = (ContentData)node.getProperties().get(ContentModel.PROP_CONTENT);
         return (content != null ? content.getEncoding() : null);
      }
   };
   
   private NodePropertyResolver resolverDeletedDate = new NodePropertyResolver()
   {
      private static final long serialVersionUID = 3240286507786251191L;
      
      public Object get(Node node)
      {
         return node.getProperties().get(ContentModel.PROP_ARCHIVED_DATE);
      }
   };
   
   private NodePropertyResolver resolverDeletedBy = new NodePropertyResolver()
   {
      private static final long serialVersionUID = -8678755146743606599L;
      
      public Object get(Node node)
      {
          return node.getProperties().get(ContentModel.PROP_ARCHIVED_BY_NAME) + 
                 " (" + node.getProperties().get(ContentModel.PROP_ARCHIVED_BY) + ")";
      }
   };
   
   private NodePropertyResolver resolverIsFolder = new NodePropertyResolver()
   {
      private static final long serialVersionUID = -9181535522349485509L;
      
      public Object get(Node node)
      {
         return getDictionaryService().isSubClass(node.getType(), ContentModel.TYPE_FOLDER);
      }
   };
   
   
   // ------------------------------------------------------------------------------
   // Action handlers
   
   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      return null;
   }
   
   /**
    * Search the deleted item store by name
    */
   public void searchName(ActionEvent event)
   {
      // simply clear the current list and refresh the screen
      // the search query text will be found and processed by the getItems() method
      contextUpdated();
      property.setShowItems(true);
      property.setFullTextSearch(false);
   }
   
   /**
    * Search the deleted item store by text
    */
   public void searchContent(ActionEvent event)
   {
      // simply clear the current list and refresh the screen
      // the search query text will be found and processed by the getItems() method
      contextUpdated();
      property.setShowItems(true);
      property.setFullTextSearch(true);
   }
   
   /**
    * Action handler to clear the current search results and show all items
    */
   public void clearSearch(ActionEvent event)
   {
      contextUpdated();
      property.setSearchText(null);
      property.setShowItems(true);
   }
   
   public void searchObjects(ActionEvent event) {
       contextUpdated();
       property.setShowItems(true);
       property.setFullTextSearch(true);
   }
   
   /**
    * Action handler called to prepare the selected item for an action
    */
   public void setupItemAction(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         try
         {
            // create the node ref, then our node representation
            NodeRef ref = new NodeRef(getArchiveRootRef().getStoreRef(), id);
            Node node = new Node(ref);
            
            node.addPropertyResolver("locationPath", resolverLocationPath);
            node.addPropertyResolver("deletedDate", resolverDeletedDate);
            node.addPropertyResolver("deletedBy", resolverDeletedBy);
            node.addPropertyResolver("isFolder", resolverIsFolder);
            node.addPropertyResolver("mimetype", resolverMimetype);
            node.addPropertyResolver("size", resolverSize);
            node.addPropertyResolver("encoding", resolverEncoding);            
            node.addPropertyResolver("objectName", resolverObjectName);
            node.addPropertyResolver("objectType", resolverObjectType);
            node.addPropertyResolver("objectTypeStr", resolverObjectTypeStr);
            node.addPropertyResolver("downloadURL", resolverDownloadURL);
            
            if (getDictionaryService().isSubClass(node.getType(), ContentModel.TYPE_FOLDER) == true && 
                getDictionaryService().isSubClass(node.getType(), ContentModel.TYPE_SYSTEM_FOLDER) == false)
            {
               node.addPropertyResolver("icon", this.resolverLargeIcon);
            }
            else
            {
               node.addPropertyResolver("icon", this.resolverFileType32);
            }
            
            // prepare a node for the action context
            property.setItem(node);
            property.setDestination(null);
         }
         catch (InvalidNodeRefException refErr)
         {
            Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {id}));
         }
      }
      else
      {
         property.setItem(null);
      }
      
      // clear the UI state in preparation for finishing the next action
      contextUpdated();
   }
   
   /**
    * Action handler to setup actions that act on lists 
    */
   public void setupListAction(ActionEvent event)
   {
      // clear the UI state in preparation for finishing the next action
      property.setDestination(null);
      contextUpdated();
   }
   
   /**
    * @return outcome to close the main list screen and reset other beans ready for display
    */
   public String close()
   {
      // call beans to update UI context for other screens
      UIContextService.getInstance(FacesContext.getCurrentInstance()).notifyBeans();
      return OUTCOME_DIALOGCLOSE;
   }
   
   /**
    * Action handler to initially setup the trashcan screen
    */
   public void setupTrashcan(ActionEvent event)
   {
      contextUpdated();
   }
   
   /**
    * Action handler called when the Date filter is changed by the user
    */
   public void dateFilterChanged(ActionEvent event)
   {
      UIModeList filterComponent = (UIModeList)event.getComponent();
      property.setDateFilter(filterComponent.getValue().toString());
      refresh();
   }
   
   /**
    * Action handler called when the User filter is changed by the user
    */
   public void userFilterChanged(ActionEvent event)
   {
      UIModeList filterComponent = (UIModeList)event.getComponent();
      property.setUserFilter(filterComponent.getValue().toString());
      refresh();
   }
   
   public void userFilterClear(ActionEvent event) {
       property.setUserFilter(TrashcanDialog.FILTER_TYPE_ALL);
       property.setUserSearchText("");
       refresh();
   }
   
   public void docTypeFilterChanged(ActionEvent event) {
       UIModeList filterComponent = (UIModeList) event.getComponent();
       property.setDocTypeFilter(filterComponent.getValue().toString());
       refresh();
   }

   public void docTypeFilterClear(ActionEvent event) {
       property.setDocTypeFilter(TrashcanDialog.FILTER_TYPE_ALL);
       property.setDocTypeSearchText("");
       refresh();
   }
   
   // ------------------------------------------------------------------------------
   // Private helpers
   
   /**
    * @return the archive store root node ref
    */
   private NodeRef getArchiveRootRef()
   {
      if (property.getArchiveRootRef() == null)
      {
         property.setArchiveRootRef(property.getNodeArchiveService().getStoreArchiveNode(Repository.getStoreRef()));
      }
      return property.getArchiveRootRef();
   }
   
   /**
    * @return the search query to use when displaying the list of deleted items
    */
   private String buildSearchQuery(TrashcanDialogProperty filter) {
       List<String> queryParts = new ArrayList<String>(50);
       
       String searchText = filter.getSearchText();
       if (searchText != null && !StringUtils.isEmpty(searchText)) {
           queryParts.add(
                   getDocumentSearchService().generateDeletedSearchQuery(searchText, null)
           );
       }
       if (FILTER_DOCU_TYPE.equals(property.getDocTypeFilter())) {
           queryParts.add(generateStringExactQuery(property.getDocTypeSearchText(), DocumentAdminModel.Props.OBJECT_TYPE_ID));
       }
       if (FILTER_DATE_ALL.equals(property.getDateFilter()) == false) {
           Date toDate = new Date();
           Date fromDate = new Date(toDate.getYear(), toDate.getMonth(), toDate.getDate(), 0, 0, 0);           
           if (FILTER_DATE_WEEK.equals(property.getDateFilter())) {
               fromDate = new Date(fromDate.getTime() - (1000L * 60L * 60L * 24L * 7L));               
           } else if (FILTER_DATE_MONTH.equals(property.getDateFilter())) {
               fromDate = new Date(fromDate.getTime() - (1000L * 60L * 60L * 24L * 30L));
           }
           SimpleDateFormat df = CachingDateFormat.getDateFormat();
           String strFromDate = LuceneQueryParser.escape(df.format(fromDate));
           String strToDate = LuceneQueryParser.escape(df.format(toDate));
           StringBuilder buf = new StringBuilder(128);
           buf.append("@").append(DATE_ATTR)
                   .append(":").append("[").append(strFromDate)
                   .append(" TO ").append(strToDate).append("]");

           queryParts.add(buf.toString());
       }

       // append user search clause
       String username = null;
       if (isAdminUser() == false) {
           // prefix the current username
           username = Application.getCurrentUser(FacesContext.getCurrentInstance()).getUserName();
       } else if (FILTER_USER_USER.equals(property.getUserFilter())) {
           // append the entered user if admin has requested a search
           username = property.getUserSearchText();
       }
       if (username != null && username.length() != 0) {
           queryParts.add(generateStringExactQuery(username, ContentModel.PROP_ARCHIVED_BY));
       }

       queryParts.add("ASPECT:\"" + ContentModel.ASPECT_ARCHIVED + "\"");
       
       return joinQueryPartsAnd(queryParts);
   }
   
   /**
    * Save the detail of the items that were successfully or unsuccessfully restored
    * 
    * @param reports     The List of RestoreNodeReport objects to walk for results
    */
   protected void saveReportDetail(List<RestoreNodeReport> reports)
   {
      // store the results ready for the next dialog page
      property.setSuccessItems(new ArrayList<Node>(reports.size()));
      property.setFailureItems(new ArrayList<Node>(reports.size()));
      for (RestoreNodeReport report : reports)
      {
         if (RestoreStatus.SUCCESS == report.getStatus())
         {
            Node node = new Node(report.getRestoredNodeRef());
            node.getProperties().put(PROP_RECOVERSTATUS, report.getStatus());
            property.getSuccessItems().add(node);
         }
         else
         {
            Node node = new Node(report.getArchivedNodeRef());
            node.getProperties().put(PROP_RECOVERSTATUS, report.getStatus());
            property.getFailureItems().add(node);
         }
      }
   }
   
   /**
    * Build an HTML table of the items that are to be or have been recovered.   
    * 
    * @param items          List of Node objects to display in the table
    * @param cssClass       CSS style to apply to the table
    * @param report         Set true to report the reason for any failure. This flag requires that the Node
    *                       object has a pseudo property "recoverstatus" containing the RestoreStatus.
    * @param archivedPath   Set true to show the path from the 'sys:archivedOriginalParentAssoc' property,
    *                       else the current Node Path will be used.
    *                   
    * 
    * @return HTML table of node info
    */
   private String buildItemsTable(List<Node> items, String cssClass, boolean report, boolean archivedPath)
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      String contextPath = fc.getExternalContext().getRequestContextPath();
      
      StringBuilder buf = new StringBuilder(1024);
      
      // outer table
      buf.append("<table width=100% cellspacing=1 cellpadding=1 border=0 class='");
      buf.append(cssClass);
      buf.append("'>");
      // title row
      buf.append("<tr style='border-bottom:1px'><th></th><th align=left><b>");
      buf.append(Application.getMessage(fc, MSG_NAME));
      buf.append("</b></th>");
      if (report == true)
      {
         buf.append("<th align=left>");
         buf.append(Application.getMessage(fc, MSG_RECOVERY_REASON));
         buf.append("</th>");
      }
      else
      {
         buf.append("<th align=left><b>");
         buf.append(Application.getMessage(fc, MSG_LOCATION));
         buf.append("</b></th>");
      }
      buf.append("</tr>");
      for (Node node : items)
      {
         // listed item rows
         buf.append("<tr><td width=16>");
         String img;
         if (getDictionaryService().isSubClass(node.getType(), ContentModel.TYPE_FOLDER))
         {
            String icon = (String)node.getProperties().get("app:icon");
            img = "/images/icons/" + (icon != null ? icon + "-16.gif" : BrowseBean.SPACE_SMALL_DEFAULT + ".gif");
         }
         else
         {
            img = FileTypeImageUtils.getFileTypeImage((String)resolverObjectName.get(node), true);
         }
         buf.append("<img width=16 height=16 alt='' src='").append(contextPath).append(img).append("'>");
         buf.append("</td><td>");
         buf.append(this.resolverObjectName.get(node));
         buf.append("</td>");
         
         if (report)
         {
            buf.append("<td>");
            String msg;
            switch ((RestoreStatus)node.getProperties().get(PROP_RECOVERSTATUS))
            {
               case FAILURE_INVALID_PARENT:
                  msg = MSG_RECOVERED_ITEM_PARENT_S;
                  break;
               
               case FAILURE_PERMISSION:
                  msg = MSG_RECOVERED_ITEM_PERMISSION_S;
                  break;
               
               case FAILURE_INTEGRITY:
                  msg = MSG_RECOVERED_ITEM_INTEGRITY_S;
                  break;
               
               default:
                  msg = MSG_RECOVERED_ITEM_FAILURE_S;
                  break;
            }
            buf.append(Application.getMessage(fc, msg));
            buf.append("</td>");
         }
         else
         {
            buf.append("<td>");
            if (archivedPath)
            {
               ChildAssociationRef childRef = (ChildAssociationRef)node.getProperties().get(ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC);
               if (getNodeService().exists(childRef.getParentRef()))
               {
                   buf.append(resolverDisplayPath.get(node));
               }
            }
            else
            {
                buf.append(resolverDisplayPath.get(node));
            }
            buf.append("</td>");
         }

         buf.append("</tr>");
      }
      // end table
      buf.append("</table>");
      
      return buf.toString();
   }
   
   private boolean isAdminUser()
   {
      return Application.getCurrentUser(FacesContext.getCurrentInstance()).isAdmin();
   }
   
   private void refresh() {
       contextUpdated();
       if (skipRefresh) {
           skipRefresh = false;
           return;
       }
       property.setShowItems(true);
   }   
   
   // ------------------------------------------------------------------------------
   // IContextListener implementation

   /**
    * @see org.alfresco.web.app.context.IContextListener#contextUpdated()
    */
   public void contextUpdated()
   {
      if (property.getItemsRichList() != null)
      {
         property.getItemsRichList().setValue(null);
      }
      property.setShowItems(false);
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
   
   @Override
   public String cancel()
   {
      close();
      doNotLoadDataWhenReturningToTrashcanDialog();
      return super.cancel();
   }
   
   @Override
   public String getCancelButtonLabel()
   {
       return Application.getMessage(FacesContext.getCurrentInstance(), MSG_CLOSE);
   }
   
   protected void doNotLoadDataWhenReturningToTrashcanDialog() { // Le hack
       TrashcanDialog dialog = (TrashcanDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "TrashcanDialog");
       if (dialog != null) {
           dialog.setSkipRefresh(true);
       }
   }

   public void setSkipRefresh(boolean skipRefresh) {
       this.skipRefresh = skipRefresh;
   }

}
