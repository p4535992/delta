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
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<f:verbatim>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/validation.js"> </script>

<script type="text/javascript">
   var finishButtonPressed = false;
   window.onload = pageLoaded;
   
   function pageLoaded()
   {
      document.getElementById("wizard:wizard-body:file-name").focus();
      document.getElementById("wizard").onsubmit = validate;
      document.getElementById("wizard:next-button").onclick = function() {finishButtonPressed = true; clear_wizard();}
      document.getElementById("wizard:finish-button").onclick = function() {finishButtonPressed = true; clear_wizard();}
   }
   
   function checkButtonState()
   {
      if (document.getElementById("wizard:wizard-body:file-name").value.length == 0 )
      {
         document.getElementById("wizard:next-button").disabled = true;
      }
      else
      {
         document.getElementById("wizard:next-button").disabled = false;
      }
   }
   
   function validate()
   {
      if (finishButtonPressed)
      {
         finishButtonPressed = false;
         return validateName(document.getElementById("wizard:wizard-body:file-name"), 
                             unescape('</f:verbatim><a:outputText value="#{msg.validation_invalid_character}" encodeForJavaScript="true" /><f:verbatim>'),
                             true);
      }
      else
      {
         return true;
      }
   }
   
   function formChanged()
   {
      if (document.getElementById("wizard:wizard-body:form-name").value != "")
      {
         document.getElementById("wizard:wizard-body:mime-type").disabled = true;
         document.getElementById("wizard:wizard-body:mime-type").value = "text/xml";
      }
      else
      {
         document.getElementById("wizard:wizard-body:mime-type").disabled = false;
      }
   }

</script>
</f:verbatim>

<h:outputText value="#{msg.general_properties}" styleClass="dialogpanel-title" />


<h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" columnClasses="propertiesLabel,">
   <h:panelGroup>
      <f:verbatim escape="false"><span class="red">* </span></f:verbatim>
      <h:outputText value="#{msg.name}:"/>
   </h:panelGroup>
   <h:inputText id="file-name" value="#{WizardManager.bean.fileName}" 
                maxlength="1024" size="35" 
                onkeyup="checkButtonState();" 
                onchange="checkButtonState();" />
                
   <h:outputText value="#{msg.type}:"/>
   <h:selectOneMenu value="#{WizardManager.bean.objectType}">
      <f:selectItems value="#{WizardManager.bean.objectTypes}" />
   </h:selectOneMenu>
   
   <h:outputText value="#{msg.content_type}:"/>
   <h:selectOneMenu id="mime-type" value="#{WizardManager.bean.mimeType}" 
                    valueChangeListener="#{WizardManager.bean.createContentChanged}">
      <f:selectItems value="#{WizardManager.bean.createMimeTypes}" />
   </h:selectOneMenu>
</h:panelGrid>

<f:subview id="create-space-wizard-other-properties" rendered="#{WizardManager.bean.otherPropertiesChoiceVisible}">
   <h:outputText value="#{msg.other_properties}" styleClass="dialogpanel-title" />
   <a:panel id="create-space-wizard-other-properties-help" styleClass="message">
      <h:graphicImage alt="" value="/images/icons/info_icon.gif" style="vertical-align: middle;" />
      <h:outputText value="#{msg.modify_props_help_text}" />
    </a:panel>
   <h:selectBooleanCheckbox value="#{WizardManager.bean.showOtherProperties}" />
   <h:outputText value="#{msg.modify_props_when_wizard_closes}" />
</f:subview>