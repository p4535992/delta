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
 * http://www.alfresco.com/legal/licensing
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<r:loadBundle var="msg"/>

<%-- Begin Header --%>
<a:panel id="header">
   <a:panel id="logo">

      <a:actionLink id="client-logo" value="#{ApplicationService.projectTitle}" href="/" tooltip="Avalehele" image="/images/logo/logo.png" showLink="false" />
      <a:actionLink id="project-name" value="Dokumendihaldus" href="/" tooltip="Dokumendihaldus" />
      
      <a:panel id="search">
         <f:verbatim>
         <span>
         </f:verbatim>
            <a:booleanEvaluator value="#{NavigationBean.currentUser.admin}" id="evalA">
               <a:actionLink value="#{msg.admin_console}" image="/images/icons/admin_console.gif" showLink="false" action="dialog:adminConsole"
                  id="alf_admin_console" />
            </a:booleanEvaluator>
            <a:actionLink id="alf_toggle_shelf"
            value="#{msg.toggle_shelf}"
            image="/images/icons/shelf.png"
            showLink="false"
            actionListener="#{NavigationBean.toggleShelf}" />
            <r:actions context="#{BrowseBean.document}" value="navigator_actions" showLink="false" />
            <a:actionLink id="addressbook_manage"
               image="/images/icons/add_attachment.gif"
               showLink="false"
               value="#{msg.addressbook}"
               action="dialog:addressbookManage"
               rendered="#{AddressbookDialog.userHasPermission}" />
         <f:verbatim>
         </span>
         </f:verbatim>
         <a:actionLink value="#{NavigationBean.currentUser.fullName}" showLink="true" image="/images/icons/user-console.png" action="dialog:userConsole"
            actionListener="#{UsersDialog.setupUserAction}">
            <f:param name="id" value="#{NavigationBean.currentUser.person.id}" />
         </a:actionLink>
         <a:actionLink id="logout" image="/images/icons/logout.png" value="#{msg.logout}" rendered="#{!NavigationBean.isGuest}"
            action="#{LoginBean.logout}" immediate="true" />
         <a:actionLink id="login" image="/images/icons/login.gif" value="#{msg.login}" rendered="#{NavigationBean.isGuest}" action="#{LoginBean.logout}" />
         <r:simpleSearch id="simple-search" actionListener="#{BrowseBean.search}" />
      </a:panel>

   </a:panel>
   <a:panel id="menu">
      <a:modeList iconColumnWidth="0" horizontal="true" selectedLinkStyleClass="active" value="#{NavigationBean.toolbarLocation}"
         actionListener="#{NavigationBean.toolbarLocationChanged}">
         <a:listItem value="companyhome" label="#{msg.company_home}" rendered="#{NavigationBean.companyHomeVisible}" />
         <a:listItem value="userhome" label="#{msg.my_home}" />
         <a:listItem value="guesthome" label="#{msg.guest_home}" rendered="#{!NavigationBean.isGuest && NavigationBean.guestHomeVisible}" />
         <a:listItem value="myalfresco" label="#{msg.my_alfresco}" />
      </a:modeList>
   </a:panel>
</a:panel>
<!-- End Header -->