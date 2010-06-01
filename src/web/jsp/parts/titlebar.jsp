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
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %> 

<r:loadBundle var="msg"/>
<div class="submit-protection-layer"></div>
<%-- Begin Header --%>
<a:panel id="header">
   <a:panel id="logo">

      <a:actionLink id="client-logo" value="#{ApplicationService.projectTitle}" href="/" tooltip="#{ApplicationService.projectTitle}" image="/images/logo/logo.png" showLink="false" />
      <a:actionLink id="project-name" value="#{ApplicationService.projectTitle}" href="/" tooltip="#{ApplicationService.projectTitle}" />
      
      <a:panel id="search">
         <f:verbatim>
         <span>
         </f:verbatim>
<%--
            <a:actionLink id="alf_toggle_shelf"
            value="#{msg.toggle_shelf}"
            image="/images/icons/shelf.png"
            showLink="false"
            actionListener="#{NavigationBean.toggleShelf}" />
--%>
<%--
            <r:actions context="#{BrowseBean.document}" value="navigator_actions" showLink="false" />
--%>
            <a:actionLink id="addressbook_manage"
                image="/images/icons/address-book-open-blue.png"
                showLink="false"
                value="#{msg.addressbook}"
                action="dialog:addressbookManage"
                actionListener="#{MenuBean.clearViewStack}"
                rendered="#{UserService.documentManager}">
                   <f:param name="primaryId" value="2" />
            </a:actionLink>
         <f:verbatim>
         </span>
         </f:verbatim>

          <h:selectOneMenu id="select_user" onchange="this.form.submit();"
                           value="#{SubstitutionBean.selectedSubstitution}" valueChangeListener="#{SubstitutionBean.substitutionSelected}">
            <f:selectItem itemValue="" itemLabel="#{NavigationBean.currentUser.fullName}"/>
            <f:selectItems value="#{SubstitutionBean.activeSubstitutions}"/>
         </h:selectOneMenu>
          <a:actionLink id="logout" image="/images/icons/logout.png" value="#{msg.logout}" rendered="#{!NavigationBean.isGuest}"
            action="#{LoginBean.logout}" immediate="true" />
         <a:actionLink id="login" image="/images/icons/login.gif" value="#{msg.login}" rendered="#{NavigationBean.isGuest}" action="#{LoginBean.logout}" />

         <h:graphicImage value="/images/parts/search_controls_left.png" width="3" height="21" styleClass="simple" />
         <h:inputText value="#{DocumentQuickSearchResultsDialog.searchValue}" maxlength="50" id="quickSearch" styleClass="quickSearch-input" />
         <h:commandButton id="quickSearchBtn" value="#{msg.search}" type="submit" action="dialog:documentQuickSearchResultsDialog" actionListener="#{DocumentQuickSearchResultsDialog.setup}" />
      </a:panel>

   </a:panel>
   <a:panel id="menu">
      <wm:menu primary="true" />
   </a:panel>
</a:panel>
<!-- End Header -->