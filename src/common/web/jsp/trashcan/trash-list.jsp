<%--
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 
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
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator"%>

<f:verbatim>
<script type="text/javascript">

   window.onload = pageLoaded;
   
   function pageLoaded()
   {
      document.getElementById("dialog:dialog-body:search-text").focus();
      updateButtonState();
   }
   
   function updateButtonState()
   {
      if (document.getElementById("dialog:dialog-body:search-text").value.length == 0)
      {
         document.getElementById("dialog:dialog-body:search-btn1").disabled = true;
         document.getElementById("dialog:dialog-body:search-btn2").disabled = true;
      }
      else
      {
         document.getElementById("dialog:dialog-body:search-btn1").disabled = false;
         document.getElementById("dialog:dialog-body:search-btn2").disabled = false;
      }
   }
   
   function userSearch(e)
   {
      var keycode;
      if (window.event) keycode = window.event.keyCode;
      else if (e) keycode = e.which;
      if (keycode == 13)
      {	
      	document.forms['dialog']['dialog:modelist'].value='dialog:dialog-body:user-filter:user';
         document.forms['dialog'].submit();
         return false;
      }
      return true;
   }
</script>

</f:verbatim>
<a:panel id="trashcan-panel" styleClass="panel-100 with-pager" label="#{DialogManager.bean.panelMessage}">

   <f:verbatim>
   <%
   PanelGenerator.generatePanelStart(out, request.getContextPath(), "message", "#ffffcc");
   %>
   </f:verbatim>
   <h:graphicImage url="/images/icons/info_icon.gif" width="16" height="16" />
   <h:outputText value="#{msg.deleted_items_info}" />
   <f:verbatim>
   <%
   PanelGenerator.generatePanelEnd(out, request.getContextPath(), "message");
   %>
   </f:verbatim>

   <%-- Search controls --%>
   <a:panel id="search-panel">
      <h:inputText id="search-text" value="#{TrashcanDialogProperty.searchText}" size="35" maxlength="1024" onkeyup="updateButtonState();"
         onchange="updateButtonState();" />
      <f:verbatim>&nbsp;</f:verbatim>
      <h:commandButton id="search-btn1" value="#{msg.search_deleted_items_name}" actionListener="#{DialogManager.bean.searchName}" disabled="true" />
      <f:verbatim>&nbsp;</f:verbatim>
      <h:commandButton id="search-btn2" value="#{msg.search_deleted_items_text}" actionListener="#{DialogManager.bean.searchContent}" disabled="true" />
      <f:verbatim>&nbsp;</f:verbatim>
      <h:commandButton id="clear-btn" value="#{msg.show_all}" actionListener="#{DialogManager.bean.clearSearch}" />
   </a:panel>

   <%-- Filter controls --%>
   <a:panel id="filter-panel" styleClass="filter-list">
      <f:verbatim>
         <img src="<%=request.getContextPath()%>/images/icons/filter.gif" width="16" height="16">
      </f:verbatim>
      <h:outputText value="#{msg.date_filter_when}" />
      <f:verbatim>:
               </f:verbatim>
      <a:modeList itemSpacing="2" iconColumnWidth="0" horizontal="true" selectedLinkStyle="font-weight:bold" value="#{TrashcanDialogProperty.dateFilter}"
         actionListener="#{DialogManager.bean.dateFilterChanged}">
         <a:listItem value="all" label="#{msg.date_filter_all}" />
         <a:listItem value="today" label="#{msg.date_filter_today}" />
         <a:listItem value="week" label="#{msg.date_filter_week}" />
         <a:listItem value="month" label="#{msg.date_filter_month}" />
      </a:modeList>
   </a:panel>

   <%-- Only the admin user needs the username filter --%>
   <a:panel id="userfilter-panel" styleClass="filter-list" rendered="#{NavigationBean.currentUser.admin == true}">
      <f:verbatim>
         <img src="<%=request.getContextPath()%>/images/icons/filter.gif" width=16 height=16>
      </f:verbatim>
      <h:outputText value="#{msg.user_filter_who}" />
      <f:verbatim>:
               </f:verbatim>
      <a:modeList id="user-filter" itemSpacing="2" iconColumnWidth="0" horizontal="true" selectedLinkStyle="font-weight:bold"
         value="#{TrashcanDialogProperty.userFilter}" actionListener="#{DialogManager.bean.userFilterChanged}">
         <a:listItem value="all" label="#{msg.user_filter_all}" />
         <a:listItem value="user" label="#{msg.user_filter_user}" />
      </a:modeList>
      <h:inputText id="user-search" value="#{TrashcanDialogProperty.userSearchText}" size="12" maxlength="100" onkeyup="return userSearch(event);" />
   </a:panel>

   <%-- Recover Listed Items actions --%>
   <a:panel id="recover-items-panel">
      <a:actionLink value="#{msg.recover_listed_items}" image="/images/icons/recover_all.gif" action="dialog:recoverListedItems"
         actionListener="#{TrashcanRecoverListedItemsDialog.setupListAction}" />
      <f:verbatim>&nbsp;</f:verbatim>
      <a:actionLink value="#{msg.delete_listed_items}" image="/images/icons/delete_all.gif" action="dialog:deleteListedItems"
         actionListener="#{TrashcanDeleteListedItemsDialog.setupListAction}" />
   </a:panel>

   <a:richList id="trashcan-list" binding="#{TrashcanDialogProperty.itemsRichList}" viewMode="details" pageSize="10" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.items}" var="r" initialSortColumn="deletedDate" initialSortDescending="true">

      <%-- Primary column showing item name --%>
      <a:column primary="true" width="150" style="padding:2px;text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <a:actionLink value="#{r.name}" action="dialog:itemDetails" actionListener="#{TrashcanItemDetailsDialog.setupItemAction}" image="#{r.typeIcon}"
               showLink="false" styleClass="inlineAction">
               <f:param name="id" value="#{r.id}" />
            </a:actionLink>
         </f:facet>
         <a:actionLink value="#{r.name}" action="dialog:itemDetails" actionListener="#{TrashcanItemDetailsDialog.setupItemAction}">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
      </a:column>

      <%-- Original Location Path column --%>
      <a:column style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.original_location}" value="displayPath" styleClass="header" />
         </f:facet>
         <r:nodePath value="#{r.locationPath}" actionListener="#{BrowseBean.clickSpacePath}" showLeaf="true" />
      </a:column>

      <%-- Deleted Date column --%>
      <a:column style="text-align:left;white-space:nowrap">
         <f:facet name="header">
            <a:sortLink label="#{msg.deleted_date}" value="deletedDate" styleClass="header" />
         </f:facet>
         <h:outputText value="#{r.deletedDate}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Deleted by user column --%>
      <a:column width="120" style="text-align:left" rendered="#{NavigationBean.currentUser.admin == true}">
         <f:facet name="header">
            <a:sortLink label="#{msg.deleted_user}" value="deletedBy" styleClass="header" />
         </f:facet>
         <h:outputText value="#{r.deletedBy}" />
      </a:column>

      <%-- Actions column --%>
      <a:column actions="true" style="text-align:left">
         <f:facet name="header">
            <h:outputText value="#{msg.actions}" />
         </f:facet>
         <a:actionLink value="#{msg.recover}" image="/images/icons/recover.gif" showLink="false" action="dialog:recoverItem"
            actionListener="#{TrashcanRecoverItemDialog.setupItemAction}">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
         <a:actionLink value="#{msg.delete}" image="/images/icons/delete.gif" showLink="false" action="dialog:deleteItem"
            actionListener="#{TrashcanDeleteItemDialog.setupItemAction}">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
      </a:column>

      <a:dataPager styleClass="pager" />
   </a:richList>
</a:panel>