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

<f:verbatim>
<script>

window.onload = pageLoaded;

function pageLoaded()
{
   document.getElementById("dialog:dialog-body:first-name").focus();
   updateButtonState();
}

function updateButtonState()
{
   if (document.getElementById("dialog:dialog-body:first-name").value.length == 0 ||
      document.getElementById("dialog:dialog-body:last-name").value.length == 0 ||
      document.getElementById("dialog:dialog-body:email").value.length == 0)
   {
      document.getElementById("dialog:finish-button").disabled = true;
   }
   else
   {
      document.getElementById("dialog:finish-button").disabled = false;
   }
}
</script>
</f:verbatim>

<a:panel styleClass="column panel-100" id="edit-user-details" label="#{msg.person_properties}">

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
      <f:verbatim>
         </div>
      </f:verbatim>
   </h:panelGroup>

   <h:panelGrid styleClass="personal-details" columns="2" cellpadding="2" cellspacing="2" columnClasses="propertiesLabel,userPropertyValue">
      <h:panelGroup>
         <h:graphicImage value="/images/icons/required_field.gif" alt="#{msg.required_field}" />
         <h:outputText value="#{msg.first_name}:" />
      </h:panelGroup>
      <h:inputText id="first-name" value="#{DialogManager.bean.firstName}" size="35" maxlength="1024" onkeyup="updateButtonState();"
         onchange="updateButtonState();" />

      <h:panelGroup>
         <h:graphicImage value="/images/icons/required_field.gif" alt="#{msg.required_field}" />
         <h:outputText value="#{msg.last_name}:" />
      </h:panelGroup>
      <h:inputText id="last-name" value="#{DialogManager.bean.lastName}" size="35" maxlength="1024" onkeyup="updateButtonState();"
         onchange="updateButtonState();" />

      <h:panelGroup>
         <h:graphicImage value="/images/icons/required_field.gif" alt="#{msg.required_field}" />
         <h:outputText value="#{msg.email}:" />
      </h:panelGroup>
      <h:inputText id="email" value="#{DialogManager.bean.email}" size="35" maxlength="1024" onkeyup="updateButtonState();" onchange="updateButtonState();" />

   </h:panelGrid>

   <h:panelGrid styleClass="organizational-details" columns="2" cellpadding="2" cellspacing="2" columnClasses="propertiesLabel,userPropertyValue">
      <h:outputText value="#{msg.user_organization}:" />
      <h:inputText id="organisation" value="#{DialogManager.bean.personProperties.organization}" size="35" maxlength="1024" />

      <h:outputText value="#{msg.user_jobtitle}:" />
      <h:inputText id="jobtitle" value="#{DialogManager.bean.personProperties.jobtitle}" size="35" maxlength="1024" />

      <h:outputText value="#{msg.user_location}:" />
      <h:inputText id="location" value="#{DialogManager.bean.personProperties.location}" size="35" maxlength="1024" />

      <h:outputText value="#{msg.presence_provider}:" />
      <h:selectOneMenu value="#{DialogManager.bean.personProperties.presenceProvider}">
         <f:selectItem itemValue="" itemLabel="(#{msg.none})" />
         <f:selectItem itemValue="skype" itemLabel="Skype" />
         <f:selectItem itemValue="yahoo" itemLabel="Yahoo" />
      </h:selectOneMenu>

      <h:outputText value="#{msg.presence_username}:" />
      <h:inputText value="#{DialogManager.bean.personProperties.presenceUsername}" size="35" maxlength="256" />

   </h:panelGrid>

   <h:panelGrid styleClass="select-avatar" columns="2" cellpadding="2" cellspacing="2" columnClasses="propertiesLabel,userPropertyValue"
      style="margin-bottom: 20px;">
      <h:outputText value="#{msg.select_avatar_prompt}:" />
      <r:ajaxFileSelector id="avatar" value="#{DialogManager.bean.personPhotoRef}" label="#{msg.select_avatar_prompt}"
         initialSelection="#{DialogManager.bean.personProperties.homeFolder}" mimetypes="image/gif,image/jpeg,image/png" styleClass="selector" />
   </h:panelGrid>

   <f:verbatim>
      <div class="user-bio">
   </f:verbatim>
   <h:outputText value="#{msg.user_description}" />
   <f:verbatim>
      <div>
   </f:verbatim>
   <h:inputTextarea id="biography" value="#{DialogManager.bean.personDescription}" rows="6" cols="60" />
   <f:verbatim>
      </div>
   </f:verbatim>
   <f:verbatim>
      </div>
   </f:verbatim>

</a:panel>

<h:panelGrid columns="2" cellpadding="2" cellspacing="0" width="100%" style="padding-top:8px">
   <!-- custom properties for cm:person type -->
   <f:verbatim/>
   <r:propertySheetGrid id="person-props" value="#{DialogManager.bean.person}"
         var="personProps" columns="1" externalConfig="true" />
</h:panelGrid>