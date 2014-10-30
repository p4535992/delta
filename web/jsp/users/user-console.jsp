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

<h:panelGroup id="mydetails-panel-facets">


   <f:facet name="title">

      <h:panelGroup>
         <%-- If users get their login from Active Directory then this should be commented out. --%>
         <%--
         <a:actionLink id="change-password" value="#{msg.change_password}"
            showLink="false" action="dialog:changeMyPassword"
            image="/images/icons/change_password.gif"
            rendered="#{!NavigationBean.isGuest && NavigationBean.allowUserConfig}" />
          --%>
         <%-- Since we get all user information from CAS, this link shouldn't be displayed. --%>
         <%--
         <a:actionLink id="change-user-details" value="#{msg.modify}" action="dialog:editUserDetails" showLink="false" image="/images/icons/Change_details.gif"
            rendered="#{!NavigationBean.isGuest && NavigationBean.allowUserConfig}" />
         --%>
      </h:panelGroup>
   </f:facet>
</h:panelGroup>

<a:panel label="#{msg.my_details}" id="mydetails-panel" facetsId="dialog:dialog-body:mydetails-panel-facets" styleClass="panel-100">
   <r:propertySheetGrid labelStyleClass="propertiesLabel" columns="1" mode="view" value="#{UserDetailsDialog.user}" externalConfig="true" />
</a:panel>

<h:outputText value="#{AssignResponsibilityBean.setFromOwnerUserConsole}" />

<f:verbatim><div style="overflow:auto;"></f:verbatim>
<f:verbatim><div class="column panel panel-65"></f:verbatim>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/assignresponsibility/web/assign-responsibility.jsp" />
<f:verbatim></div></f:verbatim>

<a:panel label="#{msg.user_management}" id="man-panel" rendered="#{NavigationBean.isGuest == false}" border="white" bgcolor="white" titleBorder="lbgrey"
   expandedTitleBorder="dotted" titleBgcolor="white" styleClass="column panel-35-f">

   <h:panelGrid id="usage-quota" columns="2" columnClasses="propertiesLabel" rendered="#{UsersBeanProperties.usagesEnabled == true}">
      <h:outputText value="#{msg.sizeCurrent}" />
      <h:outputText value="#{UsersBeanProperties.userUsage}">
         <a:convertSize />
      </h:outputText>

      <h:outputText value="#{msg.sizeQuota}" />
      <h:outputText value="#{UsersBeanProperties.userQuota}">
         <a:convertSize />
      </h:outputText>
   </h:panelGrid>

   <a:actionLink id="manage-deleted-items" value="#{msg.manage_deleted_items}" action="dialog:manageDeletedItems" image="/images/icons/trashcan.gif"
      style="margin-top: 20px; display: block;" />

</a:panel>
<f:verbatim></div></f:verbatim>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/user/web/users-groups.jsp" />

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
