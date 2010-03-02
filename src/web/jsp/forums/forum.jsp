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

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<r:page titleId="title_forum">

<f:view>
<%-- load a bundle of properties with I18N strings --%>
<r:loadBundle var="msg"/>

<h:form acceptcharset="UTF-8" id="browse-topics">


   <%-- Title bar --%>
   <%@ include file="../parts/titlebar.jsp"%>

   <%-- Main area --%>
   <a:panel id="container">

      <%-- Shelf --%>
      <%@ include file="../parts/shelf.jsp"%>

         <%-- Work Area --%>
         <a:panel id="content">
            <%-- Breadcrumb --%>
            <%@ include file="../parts/breadcrumb.jsp"%>
            <%-- Status and Actions --%>
            <a:panel id="titlebar">
               <%-- Status and Actions inner contents table --%>
               <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
               <h2 class="title-icon">
                  <h:graphicImage id="space-logo" url="/images/icons/forum.gif" width="32" height="32" />
                  <h:outputText value="#{NavigationBean.nodeProperties.name}" id="msg2" />
               </h2>
   
               <a:panel id="description" rendered="#{DialogManager.currentDialog.name eq 'aboutDialog'}">
                  <h:outputText value="#{msg.forum_info}" id="msg3" />
               </a:panel>



               <%-- actions for topics --%>
               <a:panel id="actions">
               <%-- Create actions menu --%>
               <a:panel id="main-actions-panel">
               <%-- More actions menu --%>
               <a:menu id="actionsMenu" itemSpacing="4" label="#{msg.more_actions}" image="/images/icons/menu.gif" menuStyleClass="actions-menu" styleClass="actions" style="white-space:nowrap">
                  <r:actions id="forum_actions" value="forum_topic_actions" context="#{NavigationBean.currentNode}" />
               </a:menu>
               </a:panel>
               </a:panel>

               </a:panel>
               
               <%-- Error Messages --%>
               <%-- messages tag to show messages not handled by other specific message tags --%>
               <a:errors message="" infoClass="statusWarningText" errorClass="statusErrorText" />


<a:panel id="topics-panel" styleClass="column panel-90" label="#{msg.browse_topics}">

<%-- Topics List --%>
<a:richList id="topicsList" binding="#{ForumsBean.forumRichList}" viewMode="#{ForumsBean.forumViewMode}" pageSize="#{ForumsBean.forumPageSize}"
styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
value="#{ForumsBean.topics}" var="r">

<%-- component to display if the list is empty --%>
<f:facet name="empty">
<h:outputFormat value="#{msg.no_topics}" escape="false" />
</f:facet>

<%-- Primary column for details view mode --%>
<a:column primary="true" style="padding:2px;text-align:left" rendered="#{ForumsBean.forumViewMode == 'details'}">
<f:facet name="header">
<a:sortLink label="#{msg.topic}" value="name" mode="case-insensitive" styleClass="header"/>
</f:facet>
<f:facet name="small-icon">
<a:actionLink value="#{r.name}" image="/images/icons/#{r.smallIcon}.gif" actionListener="#{BrowseBean.clickSpace}" showLink="false">
<f:param name="id" value="#{r.id}" />
</a:actionLink>
</f:facet>
<a:actionLink value="#{r.name}" actionListener="#{BrowseBean.clickSpace}">
<f:param name="id" value="#{r.id}" />
</a:actionLink>
</a:column>

<%-- Replies column for all view modes --%>
<a:column style="text-align:left">
<f:facet name="header">
<a:sortLink label="#{msg.replies}" value="replies" styleClass="header"/>
</f:facet>
<h:outputText value="#{r.replies}" />
</a:column>

<a:dataPager styleClass="pager" />
</a:richList>

</a:panel>

<a:panel id="dialog-buttons-panel" styleClass="column panel-10 container-buttons" rendered="#{DialogManager.OKButtonVisible || (DialogManager.currentDialog.name eq 'manageGroups' && GroupsDialog.group ne null)}">
         
         <h:commandButton actionListener="#{DocumentDialog.open}" action="dialog:document" value="#{msg.back_button}" type="submit" styleClass="wizardButton" >
            <f:param value="#{ForumsBean.documentNodeRef}" name="nodeRef"/>
         </h:commandButton> 
      </a:panel>

<f:verbatim><div class="clear"></div></f:verbatim>

</a:panel>
</a:panel>
<%@ include file="../parts/footer.jsp"%>

</h:form>
</f:view>
</r:page>
