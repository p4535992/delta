<%--
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
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
         
   <%-- Groups List --%>
   <a:panel id="groups-panel" label="#{msg.groups}" styleClass="with-pager" rendered="#{not empty DialogManager.bean.groups}">

      <a:richList id="groups-list" binding="#{DialogManager.bean.groupsRichList}" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
         altRowStyleClass="odd" width="100%" value="#{DialogManager.bean.groups}" var="r" initialSortColumn="id">
         
         <%-- Primary column for details view mode --%>
         <a:column primary="true" style="padding:2px;text-align:left">
            <f:facet name="small-icon">
               <a:actionLink value="#{r.name}" image="/images/icons/group.gif" actionListener="#{DialogManager.bean.clickGroup}" showLink="false">
                  <f:param name="id" value="#{r.id}" />
               </a:actionLink>
            </f:facet>
            <f:facet name="header">
               <a:sortLink label="#{msg.addressbook_group_name}" value="id" mode="case-insensitive" styleClass="header"/>
            </f:facet>
            <a:actionLink value="#{r.displayName}" action="dialog:manageGroups" actionListener="#{DialogManager.bean.clickGroup}">
               <f:param name="id" value="#{r.id}" />
            </a:actionLink>
         </a:column>
         
         <%-- Actions column --%>
         <a:column actions="true" style="text-align:left">
            <f:facet name="header">
               <h:outputText value="#{msg.actions}" rendered="#{UserService.groupsEditingAllowed}" />
            </f:facet>
            <r:actions id="inline-group-actions" value="group_inline_actions_no_subgroup" context="#{r}" showLink="false" styleClass="inlineAction"
               rendered="#{r.group ne UserService.documentManagersGroup and r.group ne UserService.administratorsGroup and UserService.groupsEditingAllowed}" />
               
            <r:actions id="add-user-group" value="base_group_inline_actions" context="#{r}" showLink="false" styleClass="inlineAction"
               rendered="#{(r.group eq UserService.documentManagersGroup or r.group eq UserService.administratorsGroup) and UserService.groupsEditingAllowed}" />
               
         </a:column>
         
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
         <a:dataPager id="pager1" styleClass="pager" />
      </a:richList>
   </a:panel>

   <%-- Users in Group list --%>
   <a:panel id="users-panel" label="#{msg.users}" styleClass="with-pager" rendered="#{not empty DialogManager.bean.users}">
   
      <a:richList id="users-list" binding="#{DialogManager.bean.usersRichList}" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
         altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.users}" var="r" initialSortColumn="name">
         
         <%-- Primary column for details view mode --%>
         <a:column primary="true" style="padding:2px;text-align:left;">
            <f:facet name="small-icon">
               <h:graphicImage alt="#{r.name}" value="/images/icons/person.gif" />
            </f:facet>
            <f:facet name="header">
               <a:sortLink label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header"/>
            </f:facet>
            <h:outputText value="#{r.name}" />
         </a:column>
         
         <%-- Username column --%>
         <a:column width="120" style="text-align:left">
            <f:facet name="header">
               <a:sortLink label="#{msg.username}" value="userName" styleClass="header"/>
            </f:facet>
            <h:outputText value="#{r.userName}" />
         </a:column>
         
         <%-- Actions column --%>
         <a:column actions="true" style="text-align:left">
            <f:facet name="header">
               <h:outputText value="#{msg.actions}" rendered="#{UserService.groupsEditingAllowed}" />
            </f:facet>
            <a:actionLink value="#{msg.remove}" image="/images/icons/remove_user.gif" showLink="false" styleClass="inlineAction" actionListener="#{DialogManager.bean.removeUser}"  rendered="#{UserService.groupsEditingAllowed}">
               <f:param name="id" value="#{r.id}" />
            </a:actionLink>
         </a:column>
         
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
         <a:dataPager id="pager2" styleClass="pager" />
      </a:richList>
   </a:panel>