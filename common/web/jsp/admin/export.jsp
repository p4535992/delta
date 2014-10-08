<<<<<<< HEAD
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
		document.getElementById("dialog:dialog-body:package-name").focus();
		document.getElementById("dialog").onsubmit = validate;
		document.getElementById("dialog:finish-button").onclick = function() {finishButtonPressed = true; clear_dialog();}
		checkButtonState();
	}
	
	function checkButtonState()
	{
		if (document.getElementById("dialog:dialog-body:package-name").value.length == 0 ||
			 document.getElementById("destination-value").value.length == 0)
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
	      return validateName(document.getElementById("dialog:dialog-body:package-name"), 
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
<a:panel id="export" label="#{msg.locate_doc_upload}">

   <h:panelGrid columns="2" cellpadding="0" cellspacing="0" columnClasses="propertiesLabel,">
      <h:outputText value="#{msg.package_name}: " />
      <h:inputText id="package-name" value="#{DialogManager.bean.packageName}" size="35" maxlength="1024" onkeyup="javascript:checkButtonState();" />

      <h:outputText value="#{msg.destination}: " />
      <r:ajaxFolderSelector id="destination" label="#{msg.select_destination_prompt}" value="#{DialogManager.bean.destination}"
         initialSelection="#{NavigationBean.currentNode.nodeRefAsString}" styleClass="selector" />

      <h:outputText value="#{msg.export_from}: " rendered="#{NavigationBean.currentUser.admin == true}" />
      <h:selectOneRadio value="#{DialogManager.bean.mode}" layout="pageDirection" rendered="#{NavigationBean.currentUser.admin == true}">
         <%--<f:selectItem itemValue="all" itemLabel="#{msg.all_spaces_root}" itemDisabled="true" />--%>
         <f:selectItem itemValue="current" itemLabel="#{msg.current_space}" />
      </h:selectOneRadio>

      <h:outputText rendered="#{NavigationBean.currentUser.admin == true}" value="#{msg.include_children}" />
      <h:selectBooleanCheckbox rendered="#{NavigationBean.currentUser.admin == true}" value="#{DialogManager.bean.includeChildren}" />

      <h:outputText rendered="#{NavigationBean.currentUser.admin == true}" value="#{msg.include_self}" />
      <h:selectBooleanCheckbox rendered="#{NavigationBean.currentUser.admin == true}" value="#{DialogManager.bean.includeSelf}" />
   </h:panelGrid>

   <h:panelGrid columns="2" cellpadding="0" cellspacing="0" columnClasses="propertiesLabel,">
      <h:outputText rendered="#{NavigationBean.currentUser.admin == true}" value="#{msg.run_export_in_background}" />
      <h:selectBooleanCheckbox rendered="#{NavigationBean.currentUser.admin == true}" value="#{DialogManager.bean.runInBackground}" />
   </h:panelGrid>
   
   <a:panel id="export-message" styleClass="message">
      <h:graphicImage alt="" value="/images/icons/info_icon.gif" style="vertical-align: middle;" />
      <h:outputText value="#{msg.export_error_info}" />
   </a:panel>

=======
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
		document.getElementById("dialog:dialog-body:package-name").focus();
		document.getElementById("dialog").onsubmit = validate;
		document.getElementById("dialog:finish-button").onclick = function() {finishButtonPressed = true; clear_dialog();}
		checkButtonState();
	}
	
	function checkButtonState()
	{
		if (document.getElementById("dialog:dialog-body:package-name").value.length == 0 ||
			 document.getElementById("destination-value").value.length == 0)
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
	      return validateName(document.getElementById("dialog:dialog-body:package-name"), 
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
<a:panel id="export" label="#{msg.locate_doc_upload}">

   <h:panelGrid columns="2" cellpadding="0" cellspacing="0" columnClasses="propertiesLabel,">
      <h:outputText value="#{msg.package_name}: " />
      <h:inputText id="package-name" value="#{DialogManager.bean.packageName}" size="35" maxlength="1024" onkeyup="javascript:checkButtonState();" />

      <h:outputText value="#{msg.destination}: " />
      <r:ajaxFolderSelector id="destination" label="#{msg.select_destination_prompt}" value="#{DialogManager.bean.destination}"
         initialSelection="#{NavigationBean.currentNode.nodeRefAsString}" styleClass="selector" />

      <h:outputText value="#{msg.export_from}: " rendered="#{NavigationBean.currentUser.admin == true}" />
      <h:selectOneRadio value="#{DialogManager.bean.mode}" layout="pageDirection" rendered="#{NavigationBean.currentUser.admin == true}">
         <%--<f:selectItem itemValue="all" itemLabel="#{msg.all_spaces_root}" itemDisabled="true" />--%>
         <f:selectItem itemValue="current" itemLabel="#{msg.current_space}" />
      </h:selectOneRadio>

      <h:outputText rendered="#{NavigationBean.currentUser.admin == true}" value="#{msg.include_children}" />
      <h:selectBooleanCheckbox rendered="#{NavigationBean.currentUser.admin == true}" value="#{DialogManager.bean.includeChildren}" />

      <h:outputText rendered="#{NavigationBean.currentUser.admin == true}" value="#{msg.include_self}" />
      <h:selectBooleanCheckbox rendered="#{NavigationBean.currentUser.admin == true}" value="#{DialogManager.bean.includeSelf}" />
   </h:panelGrid>

   <h:panelGrid columns="2" cellpadding="0" cellspacing="0" columnClasses="propertiesLabel,">
      <h:outputText rendered="#{NavigationBean.currentUser.admin == true}" value="#{msg.run_export_in_background}" />
      <h:selectBooleanCheckbox rendered="#{NavigationBean.currentUser.admin == true}" value="#{DialogManager.bean.runInBackground}" />
   </h:panelGrid>
   
   <a:panel id="export-message" styleClass="message">
      <h:graphicImage alt="" value="/images/icons/info_icon.gif" style="vertical-align: middle;" />
      <h:outputText value="#{msg.export_error_info}" />
   </a:panel>

>>>>>>> develop-5.1
</a:panel>