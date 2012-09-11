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
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>

<f:verbatim>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/validation.js"> </script>

<script type="text/javascript">
   var finishButtonPressed = false;
   window.onload = pageLoaded;
   
   function pageLoaded()
   {
      document.getElementById("dialog:dialog-body:name").focus();
      document.getElementById("dialog").onsubmit = validate;
      document.getElementById("dialog:finish-button").onclick = function() {finishButtonPressed = true; clear_dialog();}
      checkButtonState();
   }
   
   function checkButtonState()
   {
      if (document.getElementById("dialog:dialog-body:name").value.length == 0 )
      {
         document.getElementById("dialog:finish-button").disabled = true;
      }
      else
      {
         document.getElementById("dialog:finish-button").disabled = false;
      }
   }
   
   function validate()
   {
      if (finishButtonPressed)
      {
         finishButtonPressed = false;
         return validateName(document.getElementById("dialog:dialog-body:name"), 
                             unescape('</f:verbatim><a:outputText value="#{msg.validation_invalid_character}" encodeForJavaScript="true" /><f:verbatim>'),
                             true);
      }
      else
      {
         return true;
      }
   }
</script>
</f:verbatim>
<a:panel id="save-search" label="#{msg.search_props}">
   <h:panelGrid columns="2" cellpadding="0" cellspacing="0" columnClasses="propertiesLabel,">

      <h:panelGroup>
         <f:verbatim escape="false"><span class="red">* </span></f:verbatim>
         <h:outputText value="#{msg.name}" />
      </h:panelGroup>
      <h:inputText id="name" value="#{SearchProperties.searchName}" size="35" maxlength="1024" onkeyup="javascript:checkButtonState();"
         onchange="javascript:checkButtonState();" />

      <h:outputText value="#{msg.description}" />
      <h:inputText value="#{SearchProperties.searchDescription}" size="35" maxlength="1024" />

      <h:selectBooleanCheckbox value="#{SearchProperties.searchSaveGlobal}" />
      <h:outputText value="#{msg.save_search_global}" />

   </h:panelGrid>
</a:panel>