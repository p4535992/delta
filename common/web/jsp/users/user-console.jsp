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
         <a:actionLink id="change-user-details" value="#{msg.modify}" action="dialog:editUserDetails" showLink="false" image="/images/icons/Change_details.gif"
            rendered="#{!NavigationBean.isGuest && NavigationBean.allowUserConfig}" />
      </h:panelGroup>
   </f:facet>
</h:panelGroup>

<a:panel label="#{msg.my_details}" id="mydetails-panel" facetsId="dialog:dialog-body:mydetails-panel-facets" border="white" bgcolor="white" titleBorder="lbgrey"
   expandedTitleBorder="dotted" titleBgcolor="white" styleClass="panel-100">

   <h:panelGroup>
      <h:panelGroup rendered="#{UsersBeanProperties.avatarUrl == null}">
         <f:verbatim>
            <div id="avatar-frame" style="height: 120px">
         </f:verbatim>
      </h:panelGroup>
      <h:panelGroup rendered="#{UsersBeanProperties.avatarUrl != null}">
         <f:verbatim>
            <div id="avatar-frame" style="height: auto">
         </f:verbatim>
      </h:panelGroup>
      <h:graphicImage url="#{UsersBeanProperties.avatarUrl}" width="120" rendered="#{UsersBeanProperties.avatarUrl != null}" />
      <h:outputText style="display: block;" value="#{msg.user_avatar}" />
      <f:verbatim>
         </div>
      </f:verbatim>
   </h:panelGroup>

   <h:panelGrid styleClass="personal-details" columns="2" cellpadding="2" cellspacing="2" columnClasses="propertiesLabel,userPropertyValue">
      <h:outputText value="#{msg.first_name}:" />
      <h:outputText value="#{UsersBeanProperties.person.properties.firstName}" />

      <h:outputText value="#{msg.last_name}:" />
      <h:outputText value="#{UsersBeanProperties.person.properties.lastName}" />

      <h:outputText value="#{msg.email}:" />
      <h:outputText value="#{UsersBeanProperties.person.properties.email}" />
   </h:panelGrid>

   <h:panelGrid styleClass="organizational-details" columns="2" cellpadding="2" cellspacing="2" columnClasses="propertiesLabel,userPropertyValue">
      <h:outputText value="#{msg.user_organization}:" />
      <h:outputText value="#{UsersBeanProperties.person.properties.organization}" />

      <h:outputText value="#{msg.user_jobtitle}:" />
      <h:outputText value="#{UsersBeanProperties.person.properties.jobtitle}" />

      <h:outputText value="#{msg.user_location}:" />
      <h:outputText value="#{UsersBeanProperties.person.properties.location}" />

      <h:outputText value="#{msg.presence_provider}:" />
      <h:outputText value="#{UsersBeanProperties.person.properties.presenceProvider}" />

      <h:outputText value="#{msg.presence_username}:" />
      <h:outputText value="#{UsersBeanProperties.person.properties.presenceUsername}" />
   </h:panelGrid>

   <h:panelGroup rendered="#{!empty UsersBeanProperties.personDescription}">
      <f:verbatim>
         <div class="user-bio">
      </f:verbatim>
      <h:outputText value="#{msg.user_description}" />
      <h:panelGroup rendered="#{!empty UsersBeanProperties.personDescription}">
         <f:verbatim>
            <div>
         </f:verbatim>
         <h:outputText value="#{UsersBeanProperties.personDescription}" escape="false" />
         <f:verbatim>
            </div>
         </f:verbatim>
      </h:panelGroup>
      <f:verbatim>
         </div>
      </f:verbatim>
   </h:panelGroup>

   <!-- custom properties for cm:person type -->
   <f:verbatim>
      <div style="padding-bottom: 12px">
   </f:verbatim>
   <r:propertySheetGrid id="person-props" value="#{UsersBeanProperties.person}" var="personProps" columns="1" mode="view" externalConfig="true" />
   <f:verbatim>
      </div>
   </f:verbatim>

</a:panel>

<a:panel label="#{msg.general_pref}" id="pref-panel" rendered="#{NavigationBean.isGuest == false || UserPreferencesBean.allowGuestConfig == true}"
   border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white" styleClass="column panel-65">

   <h:panelGrid columns="2" cellpadding="2" cellspacing="2" columnClasses="propertiesLabel,">
      <h:outputText value="#{msg.start_location}:" />
      <%-- Start Location drop-down selector --%>
      <h:selectOneMenu id="start-location" value="#{UserPreferencesBean.startLocation}" onchange="document.forms['dialog'].submit(); return true;">
         <f:selectItems value="#{UserPreferencesBean.startLocations}" />
      </h:selectOneMenu>

      <h:outputText value="#{msg.interface_language}:" rendered="#{LoginBean.languageSelect}" />
      <h:selectOneMenu id="language" value="#{UserPreferencesBean.language}" rendered="#{LoginBean.languageSelect}"
         onchange="document.forms['dialog'].submit(); return true;">
         <f:selectItems value="#{UserPreferencesBean.languages}" />
      </h:selectOneMenu>

      <h:outputText value="#{msg.content_language_filter}:" />
      <%-- Content Language Filter drop-down selector --%>
      <h:selectOneMenu id="content-filter-language" value="#{UserPreferencesBean.contentFilterLanguage}"
         onchange="document.forms['dialog'].submit(); return true;">
         <f:selectItems value="#{UserPreferencesBean.contentFilterLanguages}" />
      </h:selectOneMenu>
   </h:panelGrid>

   <%-- Download automatically checkbox --%>
   <f:verbatim>
      <br />
   </f:verbatim>

   <h:outputText value="#{msg.offline_editing}" />

   <h:panelGrid columns="2">
      <h:selectBooleanCheckbox id="download-automatically" value="#{UserPreferencesBean.downloadAutomatically}"
         onchange="document.forms['dialog'].submit(); return true;" />

      <h:outputText value="#{msg.download_automatically}" />
   </h:panelGrid>
</a:panel>

<a:panel label="#{msg.user_management}" id="man-panel" rendered="#{NavigationBean.isGuest == false && NavigationBean.isDeltaGuest == false}" border="white" bgcolor="white" titleBorder="lbgrey"
   expandedTitleBorder="dotted" titleBgcolor="white" styleClass="column panel-35">

   <h:panelGrid id="usage-quota" columns="2" columnClasses="propertiesLabel" rendered="#{UsersBeanProperties.usagesEnabled == true}">
      <h:outputText value="#{msg.sizeCurrent}:" />
      <h:outputText value="#{UsersBeanProperties.userUsage}">
         <a:convertSize />
      </h:outputText>

      <h:outputText value="#{msg.sizeQuota}:" />
      <h:outputText value="#{UsersBeanProperties.userQuota}">
         <a:convertSize />
      </h:outputText>
   </h:panelGrid>

   <a:actionLink id="manage-deleted-items" value="#{msg.manage_deleted_items}" action="dialog:manageDeletedItems" image="/images/icons/trashcan.gif"
      style="margin-top: 20px; display: block;" />

</a:panel>