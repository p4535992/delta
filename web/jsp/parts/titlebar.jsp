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
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/translations.jsp" />
<div id="submit-protection-msg"></div>
<div class="submit-protection-layer"></div>
<%-- Begin Header --%>
<a:panel id="header">
   <a:panel id="logo">

      <a:actionLink id="client-logo" value="#{ApplicationService.headerText}" href="/" tooltip="#{ApplicationService.headerText}" image="#{ApplicationService.logoUrl}" showLink="false" />
      <a:actionLink id="project-name" value="#{ApplicationService.headerText}" href="/" tooltip="#{ApplicationService.headerText}" />
      
      <a:panel id="search" styleClass="search-box">
         <f:verbatim>
         <span>
         </f:verbatim>
            <%--<a:actionLink id="alf_toggle_shelf"--%>
            <%--value="DeveloperBean.doStuff()"--%>
            <%--image="/images/icons/shelf.png"--%>
            <%--showLink="false"--%>
            <%--actionListener="#{TestingForDeveloperBean.doStuff}" />--%>

<%--             <a:actionLink id="override_device" --%>
<%--             value="#{msg.override_device}" --%>
<%--             image="/images/icons/deploy_server.gif" --%>
<%--             showLink="false" --%>
<%--             href="/?deviceDetectionOverride" /> --%>
<%--
            <r:actions context="#{BrowseBean.document}" value="navigator_actions" showLink="false" />
--%>
            <a:actionLink id="addressbook_manage"
                image="/images/icons/address-book-open-blue.png"
                showLink="false"
                value="#{msg.addressbook}"
                action="dialog:addressbookList"
                actionListener="#{MenuBean.clearViewStack}"
                rendered="#{UserService.documentManager}">
                   <f:param name="primaryId" value="2" />
            </a:actionLink>
         <f:verbatim>
         </span>
         </f:verbatim>

          <h:selectOneMenu id="select_user"  onchange="javascript:document.getElementsByTagName('form')[1].submit();"
                           value="#{SubstitutionBean.selectedSubstitution}" valueChangeListener="#{SubstitutionBean.substitutionSelected}">
            <f:selectItem itemValue="" itemLabel="#{NavigationBean.currentUser.fullName}"/>
            <f:selectItems value="#{SubstitutionBean.activeSubstitutions}"/>
         </h:selectOneMenu>
          <a:actionLink id="logout" image="/images/icons/logout.png" value="#{msg.logout}" rendered="#{!NavigationBean.isGuest}"
            action="#{LoginBean.logout}" immediate="true" />
         <a:actionLink id="login" image="/images/icons/login.gif" value="#{msg.login}" rendered="#{NavigationBean.isGuest}" action="#{LoginBean.logout}" />

         <h:graphicImage value="/images/parts/search_controls_left.png" width="3" height="21" styleClass="simple" rendered="#{SubstitutionBean.currentStructUnitUser}" />
         <h:inputText value="#{DocumentQuickSearchResultsDialog.searchValue}" maxlength="50" id="quickSearch" styleClass="quickSearch-input" rendered="#{SubstitutionBean.currentStructUnitUser}" />
         <h:commandButton id="quickSearchBtn" value="#{msg.search}" type="submit" action="dialog:documentQuickSearchResultsDialog" actionListener="#{DocumentQuickSearchResultsDialog.setup}" rendered="#{SubstitutionBean.currentStructUnitUser}" />
      </a:panel>

   </a:panel>
   <a:panel id="menu">
      <wm:menu primary="true" />
   </a:panel>
</a:panel>
<!-- End Header -->
