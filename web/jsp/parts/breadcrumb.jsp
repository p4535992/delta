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
 * http://www.alfresco.com/legal/licensing
--%>
<%-- Breadcrumb area --%>
<%-- Designed to support a variable height breadcrumb --%>
<%-- Breadcrumb component --%>
<%--
<a:panel id="breadcrumb">
   <a:breadcrumb value="#{NavigationBean.location}" />
</a:panel>
--%>

<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<a:actionLink id="menu-shortcut-add" actionListener="#{MenuBean.addShortcut}" value="#{msg.shortcut_add}" rendered="#{MenuBean.shortcutAddable gt 0}" showLink="false" image="/images/icons/add_item.gif" styleClass="right" />
<a:actionLink id="menu-shortcut-remove" actionListener="#{MenuBean.removeShortcut}" value="#{msg.shortcut_remove}" rendered="#{MenuBean.shortcutAddable lt 0}" showLink="false" image="/images/icons/delete.gif" styleClass="right" />

<a:actionLink id="case-file-shortcut-add" actionListener="#{MenuBean.addVolumeOutcomeShortcut}" value="#{msg.shortcut_add}" rendered="#{MenuBean.showAddCaseFileVolumeShortcut}" showLink="false" image="/images/icons/add_item.gif" styleClass="right" >
	<f:param name="outcome" value="#{MenuBean.caseFileOpenOutcome}" />
	<f:param name="nodeRef" value="#{MenuBean.currentVolumeRef}" />
</a:actionLink>

<a:actionLink id="volume-shortcut-add" actionListener="#{MenuBean.addVolumeOutcomeShortcut}" value="#{msg.shortcut_add}" rendered="#{MenuBean.showAddVolumeShortcut}" showLink="false" image="/images/icons/add_item.gif" styleClass="right" >
	<f:param name="outcome" value="#{MenuBean.volumeOpenOutcome}" />
	<f:param name="nodeRef" value="#{MenuBean.currentVolumeRef}" />
</a:actionLink>

<a:actionLink id="outcome-shortcut-remove" actionListener="#{MenuBean.removeOutcomeShortcut}" value="#{msg.shortcut_remove}" rendered="#{MenuBean.showRemoveVolumeOutcomeShortcut}" showLink="false" image="/images/icons/delete.gif" styleClass="right" >
	<f:param name="menuItemNodeRef" value="#{MenuBean.currentVolumeRef}" />
</a:actionLink>

<a:actionLink value="#{msg.helptext}" onclick="return help('#{DocumentTemplateService.serverUrl}/help/documentType/#{DialogManager.bean.documentType.id}')" image="/images/office/help.gif" showLink="false" styleClass="right"
  style="background-size: 100% 100%;"
  rendered="#{DialogManager.state != null && DialogManager.currentDialog.name == 'documentDynamicDialog' && applicationScope.helpText.documentType[DialogManager.bean.documentType.id] != null}" />

<a:actionLink value="#{msg.helptext}" onclick="return help('#{DocumentTemplateService.serverUrl}/help/dialog/#{DialogManager.currentDialog.name}')" image="/images/office/help.gif" showLink="false" styleClass="right"
  style="background-size: 100% 100%;"
  rendered="#{DialogManager.state != null && DialogManager.currentDialog.name != 'documentDynamicDialog' && applicationScope.helpText.dialog[DialogManager.currentDialog.name] != null}" />

<f:verbatim>
<div id="breadcrumb">
</f:verbatim>
   <h:panelGroup id="menu-breadcrumb" binding="#{MenuBean.breadcrumb}" />
<f:verbatim>
</div>
</f:verbatim>
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
 * http://www.alfresco.com/legal/licensing
--%>
<%-- Breadcrumb area --%>
<%-- Designed to support a variable height breadcrumb --%>
<%-- Breadcrumb component --%>
<%--
<a:panel id="breadcrumb">
   <a:breadcrumb value="#{NavigationBean.location}" />
</a:panel>
--%>

<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<a:actionLink id="menu-shortcut-add" actionListener="#{MenuBean.addShortcut}" value="#{msg.shortcut_add}" rendered="#{MenuBean.shortcutAddable gt 0}" showLink="false" image="/images/icons/add_item.gif" styleClass="right" />
<a:actionLink id="menu-shortcut-remove" actionListener="#{MenuBean.removeShortcut}" value="#{msg.shortcut_remove}" rendered="#{MenuBean.shortcutAddable lt 0}" showLink="false" image="/images/icons/delete.gif" styleClass="right" />

<a:actionLink id="case-file-shortcut-add" actionListener="#{MenuBean.addVolumeOutcomeShortcut}" value="#{msg.shortcut_add}" rendered="#{MenuBean.showAddCaseFileVolumeShortcut}" showLink="false" image="/images/icons/add_item.gif" styleClass="right" >
	<f:param name="outcome" value="#{MenuBean.caseFileOpenOutcome}" />
	<f:param name="nodeRef" value="#{MenuBean.currentVolumeRef}" />
</a:actionLink>

<a:actionLink id="volume-shortcut-add" actionListener="#{MenuBean.addVolumeOutcomeShortcut}" value="#{msg.shortcut_add}" rendered="#{MenuBean.showAddVolumeShortcut}" showLink="false" image="/images/icons/add_item.gif" styleClass="right" >
	<f:param name="outcome" value="#{MenuBean.volumeOpenOutcome}" />
	<f:param name="nodeRef" value="#{MenuBean.currentVolumeRef}" />
</a:actionLink>

<a:actionLink id="outcome-shortcut-remove" actionListener="#{MenuBean.removeOutcomeShortcut}" value="#{msg.shortcut_remove}" rendered="#{MenuBean.showRemoveVolumeOutcomeShortcut}" showLink="false" image="/images/icons/delete.gif" styleClass="right" >
	<f:param name="menuItemNodeRef" value="#{MenuBean.currentVolumeRef}" />
</a:actionLink>

<a:actionLink value="#{msg.helptext}" onclick="return help('#{DocumentTemplateService.serverUrl}/help/documentType/#{DialogManager.bean.documentType.id}')" image="/images/office/help.gif" showLink="false" styleClass="right"
  style="background-size: 100% 100%;"
  rendered="#{DialogManager.state != null && DialogManager.currentDialog.name == 'documentDynamicDialog' && applicationScope.helpText.documentType[DialogManager.bean.documentType.id] != null}" />

<a:actionLink value="#{msg.helptext}" onclick="return help('#{DocumentTemplateService.serverUrl}/help/dialog/#{DialogManager.currentDialog.name}')" image="/images/office/help.gif" showLink="false" styleClass="right"
  style="background-size: 100% 100%;"
  rendered="#{DialogManager.state != null && DialogManager.currentDialog.name != 'documentDynamicDialog' && applicationScope.helpText.dialog[DialogManager.currentDialog.name] != null}" />

<f:verbatim>
<div id="breadcrumb">
</f:verbatim>
   <h:panelGroup id="menu-breadcrumb" binding="#{MenuBean.breadcrumb}" />
<f:verbatim>
</div>
</f:verbatim>
>>>>>>> develop-5.1
