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

<r:page titleId="title_topic">

<f:view>
<%-- load a bundle of properties with I18N strings --%>
<r:loadBundle var="msg"/>

<h:form acceptcharset="UTF-8" id="browse-posts">


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
                  <h:graphicImage id="space-logo" url="/images/icons/#{NavigationBean.nodeProperties.icon}.gif" width="32" height="32" />
                  <h:outputText value="#{NavigationBean.nodeProperties.name}" id="msg2" />
               </h2>
   
               <a:panel id="description" rendered="#{DialogManager.currentDialog.name eq 'aboutDialog'}">
                  <h:outputText value="#{msg.topic_info}" id="msg3" />
               </a:panel>



               <a:panel id="actions">
               <%-- Create actions menu --%>
               <a:menu id="createMenu" itemSpacing="4" label="#{msg.create_options}" menuStyleClass="actions-menu" styleClass="actions">
                  <r:actions id="actions_more" value="forum_post_actions" context="#{NavigationBean.currentNode}" />
               </a:menu>
               </a:panel>
               
               </a:panel>
               
               <%-- Error Messages --%>
               <%-- messages tag to show messages not handled by other specific message tags --%>
               <a:errors message="" infoClass="statusWarningText" errorClass="statusErrorText" />
               
               
               
               
               
               <a:panel id="posts-panel" styleClass="column panel-90" label="#{msg.browse_posts}">


<%-- Details - Posts --%>
<%-- Posts List --%>
<a:richList id="postsList" binding="#{ForumsBean.topicRichList}" viewMode="bubble" pageSize="#{ForumsBean.topicPageSize}"
styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
value="#{ForumsBean.posts}" var="r">

<%-- component to display if the list is empty --%>
<f:facet name="empty">
<h:outputFormat value="#{msg.no_posts}" escape="false" />
</f:facet>

<%-- Content column for all view modes --%>
<a:column primary="true">
<f:facet name="header">
<h:outputText value="#{msg.post}" />
</f:facet>
<h:outputText value="#{r.message}" escape="false" />
</a:column>

<%-- Author column for the details view mode --%>
<a:column style="text-align:left" rendered="#{ForumsBean.topicViewMode == 'details'}">
<f:facet name="header">
<a:sortLink label="#{msg.author}" value="creator" styleClass="header"/>
</f:facet>
<h:outputText value="#{r.creatorName}" />
</a:column>

<%-- Posted time column for details view mode --%>
<a:column style="text-align:left; white-space:nowrap"
rendered="#{ForumsBean.topicViewMode == 'details'}">
<f:facet name="header">
<a:sortLink label="#{msg.posted}" value="created" styleClass="header"/>
</f:facet>
<h:outputText value="#{r.created}">
<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
</h:outputText>
</a:column>

<%-- Posted time column for bubble view mode --%>
<a:column style="text-align:left; white-space:nowrap"
rendered="#{ForumsBean.topicViewMode == 'bubble'}">
<f:facet name="header">
<h:outputText value="#{msg.forum_posted_on}:" styleClass="header"/>
</f:facet>
<h:outputText value="&nbsp;" escape="false;"/>
<h:outputText value="#{r.created}">
<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
</h:outputText>
</a:column>

<%-- topic name column for bubble view mode --%>
<a:column style="text-align:left;"
rendered="#{ForumsBean.topicViewMode == 'bubble'}">
</a:column>

<%-- reply to column for bubble view mode --%>
<a:column style="text-align:left;"
rendered="#{ForumsBean.topicViewMode == 'bubble' && r.replyTo != null}">
<f:facet name="header">
<h:outputText value="#{msg.forum_replied_to_user}: " styleClass="header"/>
</f:facet>
<h:outputText value="&nbsp;" escape="false"/>
<h:outputText value="#{r.replyTo}" />
</a:column>


<%-- Actions column --%>
<a:column id="col1" actions="true" style="text-align:left">
<f:facet name="header">
<h:outputText value="#{msg.actions}"/>
</f:facet>

<%-- actions are configured in web-client-config-forum-actions.xml --%>
<r:actions id="actions" value="topic_actions_override" context="#{r}" showLink="false" styleClass="inlineAction" />
</a:column>

<a:dataPager styleClass="pager" />
</a:richList>

      </a:panel>
<a:panel id="dialog-buttons-panel" styleClass="column panel-10 container-buttons" rendered="#{DialogManager.OKButtonVisible || (DialogManager.currentDialog.name eq 'manageGroups' && GroupsDialog.group ne null)}">
         
         <h:commandButton actionListener="#{ForumsBean.discuss}" action="" value="#{msg.back_button}" type="submit" styleClass="wizardButton" >
            <f:param value="#{ForumsBean.documentNodeRef.id}" name="id"/>
         </h:commandButton> 
      </a:panel>

<f:verbatim><div class="clear"></div></f:verbatim>

</a:panel>
</a:panel>
<%@ include file="../parts/footer.jsp"%>


</h:form>

</f:view>

</r:page>
