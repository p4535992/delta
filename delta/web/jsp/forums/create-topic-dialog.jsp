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
if (document.getElementById("dialog:dialog-body:name").value.length == 0 ||
document.getElementById("dialog:dialog-body:message").value.length == 0)
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
unescape('</f:verbatim><h:outputText value="#{msg.validation_invalid_character}" /><f:verbatim>'),
true);
}
else
{
return true;
}
}

</script>
</f:verbatim>

<a:panel id="topic-properties-panel" label="#{msg.properties}">

   <h:panelGrid columns="2" styleClass="table-padding" columnClasses="propertiesLabel,">
   
      <h:panelGroup>
         <f:verbatim>
         <span class="red">* </span>
         </f:verbatim>
         <h:outputText value="#{msg.subject}:&nbsp;&nbsp;&nbsp;" escape="false" />
      </h:panelGroup>
      <h:inputText id="name" value="#{DialogManager.bean.name}" size="35" maxlength="1024" onkeyup="javascript:checkButtonState();"
         onchange="javascript:checkButtonState();" />   
      
      <h:outputText value="#{msg.icon}:&nbsp;&nbsp;&nbsp;" escape="false" />
      <a:imagePickerRadioPanel id="space-icon" columns="6" spacing="4" value="#{DialogManager.bean.icon}" panelBorder="greyround" panelBgcolor="#F5F5F5">
         <a:listItems value="#{DialogManager.bean.icons}" />
      </a:imagePickerRadioPanel>
   
   </h:panelGrid>

</a:panel>


<a:panel id="topic-message-panel" label="#{msg.message}">
         <f:verbatim>
            <b><span class="red">* </span>
         </f:verbatim>
         <h:outputText value="#{msg.message}:" />
         <f:verbatim></b></f:verbatim>
   <h:inputTextarea id="message" value="#{DialogManager.bean.message}" rows="6" cols="70" onkeyup="checkButtonState();" onchange="checkButtonState();" styleClass="expand100-250 margin-top-10" />
</a:panel>
