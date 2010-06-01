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

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>


<%@page import="org.alfresco.web.bean.dialog.DialogManager"%>
<%@page import="javax.faces.context.FacesContext"%>

<% boolean isNull;
   String title;
   try {
       title = Application.getDialogManager().getTitle();
       isNull = false;
   } catch (Exception e) {
       isNull = true;
       title = "";
   }
   
%>

<r:page title="<%=title%>">

<f:view>

   <%-- load a bundle of properties with I18N strings --%>
   <r:loadBundle var="msg"/>

   <h:form acceptcharset="UTF-8" id="dialog">

   <%-- Title bar --%>
   <%@ include file="../parts/titlebar.jsp"%>

   <%-- Main area --%>
   <a:panel id="container">

      <%-- Shelf --%>
      <%@ include file="../parts/shelf.jsp"%>

         <%-- Work Area --%>
         <a:panel id="content">
            <%-- Breadcrumb --%>
            <%@ include file="../parts/breadcrumb.jsp"%>
            <% if(!isNull) { %>
            <%-- Status and Actions --%>
            <a:panel id="titlebar">
            
               <a:panel id="titlebar-dialog-buttons-panel" styleClass="titlebar-buttons" rendered="#{DialogManager.OKButtonVisible || (DialogManager.currentDialog.name eq 'manageGroups' && GroupsDialog.group ne null)}">
                  <r:dialogButtons id="titlebar-dialog-buttons" styleClass="wizardButton" />
               </a:panel>
            
               <%-- Status and Actions inner contents table --%>
               <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
               <h2 class="title-icon">
                  <a:actionLink id="doc-logo1" value="#{DialogManager.bean.name}" href="#{DialogManager.bean.url}" target="new"
                     image="#{DialogManager.bean.document.properties.fileType32}" showLink="false"
                     rendered="#{DialogManager.currentDialog.name eq 'showDocDetails'}" />
                  <h:graphicImage id="dialog-logo" url="#{DialogManager.icon}"
                     rendered="#{DialogManager.currentDialog.name ne 'showDocDetails'}" />
                  <h:outputText value="#{DialogManager.title}" />
               </h2>
            
            <a:panel id="description" rendered="#{DialogManager.currentDialog.name eq 'aboutDialog'}">
               <h:outputText value="#{DialogManager.subTitle}" />
               <h:outputText value="#{DialogManager.description}" />
            </a:panel>
            

            <a:panel id="actions">
            <%--<f:subview id="extra-actions" rendered="#{DialogManager.filterListVisible or DialogManager.moreActionsId != null}"> --%>
            <f:subview id="extra-actions" rendered="#{DialogManager.filterListVisible and DialogManager.currentDialog.name ne 'manageGroups' or DialogManager.currentDialog.name eq 'document' and DocumentDialog.workflow.workflowMethodBindingName != null and DocumentDialog.meta.inEditMode == false}">
               <f:verbatim>
               <ul class="actions-menu extra-actions">
               </f:verbatim>
               
               <%-- View Filters --%> 
               <f:subview id="filters-panel" rendered="#{DialogManager.filterListVisible and DialogManager.currentDialog.name ne 'manageGroups'}">
                  <f:verbatim>
                  <li>
                  </f:verbatim>
                     <a:modeList itemSpacing="0" iconColumnWidth="0" selectedStyleClass="selected" menu="true"
                        styleClass="dropdown-menu no-icon right" menuImage="/images/icons/arrow-down.png" value="#{DialogManager.bean.filterMode}"
                        actionListener="#{DialogManager.bean.filterModeChanged}">
                        <a:listItems value="#{DialogManager.bean.filterItems}" />
                     </a:modeList>
                  <f:verbatim>
                  </li>
                 </f:verbatim>
               </f:subview>
               
               <f:subview id="more-actions-panel">
               <%-- TODO: uncomment this when dropdown menu is ok (see next TODO) --%>
               <%--<f:subview id="more-actions-panel" rendered="#{DialogManager.moreActionsId != null and DialogManager.currentDialog.name ne 'document'}">--%>
               <f:subview id="more-actions-panel-sub1" rendered="#{DialogManager.moreActionsId ne ''}">
                  <f:verbatim><li></f:verbatim>
                     <a:menu id="more_actions_menu" style="white-space:nowrap" styleClass="dropdown-menu-anchor"  menuStyleClass="dropdown-menu right"
                        label="#{DialogManager.moreActionsMenuLabel}" image="/images/icons/arrow-down.png" 
                        rendered="#{DialogManager.moreActionsId ne ''}">
                        <r:actions id="more_actions_menu_items" value="#{DialogManager.moreActionsId}" context="#{DialogManager.actionsContext}" />
                     </a:menu>
                  <f:verbatim></li></f:verbatim>
               </f:subview>
               
               <f:subview id="more-actions-panel-sub2" rendered="#{DialogManager.currentDialog.name eq 'document' and DocumentDialog.workflow.workflowMethodBindingName != null and DocumentDialog.meta.inEditMode == false}">
                  <f:verbatim><li></f:verbatim>
                     <a:menu id="workflow_menu" style="white-space:nowrap" menuStyleClass="dropdown-menu right"
                        label="#{DocumentDialog.workflow.workflowMenuLabel}" image="/images/icons/arrow-down.png" 
                        rendered="#{DialogManager.currentDialog.name eq 'document' and DocumentDialog.workflow.workflowMethodBindingName != null and DocumentDialog.meta.inEditMode == false}">
                        <%-- Here the method call of the value parameter is actually returning a string in the form "#{method binding}"
                             UIActions class has been modified to interpret such strings as method bindings that take 1 parameter and
                             return a list of ActionDefinition objects. See UIActions and WorkflowBlockBean classes for details.
                              --%>
                        <r:actions id="workflow_menu_items" value="#{DocumentDialog.workflow.workflowMethodBindingName}" context="#{DialogManager.actionsContext}" />
                     </a:menu>
               <f:verbatim></li></f:verbatim>
               </f:subview>
               </f:subview>
               
               <f:verbatim>
               </ul>
               </f:verbatim>
               </f:subview>
               
               <%-- Main actions --%> 
               <a:panel id="main-actions-panel" rendered="#{DialogManager.actionsId != null}">
                  <f:subview id="not-empty-main-actions-subview" rendered="#{DialogManager.currentDialog.name eq 'manageGroups' and not empty DialogManager.bean.groups or DialogManager.currentDialog.name ne 'manageGroups'}">
                     <a:menu id="main_actions_list" label="#{msg.more_actions}" styleClass="actions" menuStyleClass="actions-menu">
                        <r:actions id="main_actions_list_items" rendered="#{DialogManager.actionsAsMenu == false}" value="#{DialogManager.actionsId}"
                           context="#{DialogManager.actionsContext}" />
                     </a:menu>
   
                     <a:menu id="main_actions_menu" rendered="#{DialogManager.actionsAsMenu == true}" itemSpacing="4" image="/images/icons/arrow-down.png"
                        menuStyleClass="" style="white-space: nowrap" label="#{DialogManager.actionsMenuLabel}">
                        <r:actions id="main_actions_menu_items" value="#{DialogManager.actionsId}" context="#{DialogManager.actionsContext}" />
                     </a:menu>
                     <%-- TODO: position the menu correctly to the left alongside the buttons --%>
                     <%--<a:menu id="more_actions_menu" rendered="#{DialogManager.moreActionsId != null and DialogManager.currentDialog.name eq 'document'}" style="white-space:nowrap" menuStyleClass="dropdown-menu"
                        label="#{DialogManager.moreActionsMenuLabel}" image="/images/icons/arrow-down.png">
                        <r:actions id="more_actions_menu_items" value="#{DialogManager.moreActionsId}" context="#{DialogManager.actionsContext}"/>
                     </a:menu>--%>
                  </f:subview>
                  <f:subview id="empty-main-actions-subview" rendered="#{not (DialogManager.currentDialog.name eq 'manageGroups' and not empty DialogManager.bean.groups or DialogManager.currentDialog.name ne 'manageGroups') and (DialogManager.filterListVisible and DialogManager.currentDialog.name ne 'manageGroups' or DialogManager.currentDialog.name eq 'document' and DocumentDialog.workflow.workflowMethodBindingName != null and DocumentDialog.meta.inEditMode == false)}">
                     <h:outputText value="&nbsp;" escape="false" style="line-height:20px;" />
                  </f:subview>
               </a:panel>
               
               
               <%-- Space Actions --%>
               <a:panel id="space-details-actions-panel" rendered="#{DialogManager.currentDialog.name eq 'showSpaceDetails' && DialogManager.bean.space != null}">
                  <a:menu id="spaceActionsMenu" label="#{msg.more_actions}" styleClass="actions" menuStyleClass="actions-menu">
                     <r:actions id="actions_space" value="space_details_actions" context="#{DialogManager.bean.space}" />
                  </a:menu>
               </a:panel>

               <%-- Document Actions --%>
               <a:panel id="document-details-actions-panel" rendered="#{DialogManager.currentDialog.name eq 'showDocDetails' && DialogManager.bean.document != null}" >
                  <a:menu id="documentActionsMenu" label="#{msg.more_actions}" styleClass="actions" menuStyleClass="actions-menu">
                     <r:actions id="actions_doc" value="doc_details_actions" context="#{DialogManager.bean.document}" />
                  </a:menu>
               </a:panel>

            </a:panel>

            <a:panel id="additional" rendered="#{DialogManager.navigationVisible or DialogManager.viewListVisible}">
               <%-- Navigation --%>
               <a:panel id="nav-panel" rendered="#{DialogManager.navigationVisible}">
                  <a:actionLink id="act-prev" value="#{msg.previous_item}" showLink="false" image="/images/icons/nav-prev.png"
                     actionListener="#{DialogManager.bean.previousItem}" action="#{DialogManager.bean.getOutcome}" padding="5">
                     <f:param name="id" value="#{DialogManager.bean.currentItemId}" />
                  </a:actionLink>

                  <h:graphicImage url="/images/icons/nav_file.gif" width="24" height="24" alt="" />

                  <a:actionLink id="act-next" value="#{msg.next_item}" image="/images/icons/nav-next.png" showLink="false"
                     actionListener="#{DialogManager.bean.nextItem}" action="#{DialogManager.bean.getOutcome}" padding="5">
                     <f:param name="id" value="#{DialogManager.bean.currentItemId}" />
                  </a:actionLink>
               </a:panel>
               
               <%-- View Mode --%>
               <a:panel id="views-panel" rendered="#{DialogManager.viewListVisible and DialogManager.currentDialog.name ne 'manageGroups'}">
                  <a:modeList iconColumnWidth="0" selectedStyleClass="selected" menu="true" styleClass="dropdown-menu right no-icon"
                     menuImage="/images/icons/arrow-down.png" value="#{DialogManager.bean.viewMode}" actionListener="#{DialogManager.bean.viewModeChanged}">
                     <a:listItems value="#{DialogManager.bean.viewItems}" />
                  </a:modeList>
               </a:panel>
               
            </a:panel>
         </a:panel>

         <%-- Details --%>
         <a:errors message="#{DialogManager.errorMessage}" styleClass="message" errorClass="error-message" infoClass="info-message" />
         
            <f:subview id="dialog-body">

               <a:panel id="container-content" styleClass="column panel-100">
                  <jsp:include page="<%=Application.getDialogManager().getPage() %>" />
               </a:panel>

            </f:subview>

      <a:panel id="footer-titlebar">
            
               <a:panel id="footer-titlebar-dialog-buttons-panel" styleClass="titlebar-buttons" rendered="#{DialogManager.OKButtonVisible || (DialogManager.currentDialog.name eq 'manageGroups' && GroupsDialog.group ne null)}">
                  <r:dialogButtons id="footer-titlebar-dialog-buttons" styleClass="wizardButton" />
               </a:panel>
            
               <%-- Status and Actions inner contents table --%>
               <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
               <h2 class="title-icon">
                  <a:actionLink id="footer-doc-logo1" value="#{DialogManager.bean.name}" href="#{DialogManager.bean.url}" target="new"
                     image="#{DialogManager.bean.document.properties.fileType32}" showLink="false"
                     rendered="#{DialogManager.currentDialog.name eq 'showDocDetails'}" />
                  <h:graphicImage id="footer-dialog-logo" url="#{DialogManager.icon}"
                     rendered="#{DialogManager.currentDialog.name ne 'showDocDetails'}" />
                  <h:outputText value="#{DialogManager.title}" />
               </h2>
            
            <a:panel id="footer-description" rendered="#{DialogManager.currentDialog.name eq 'aboutDialog'}">
               <h:outputText value="#{DialogManager.subTitle}" />
               <h:outputText value="#{DialogManager.description}" />
            </a:panel>
            

            <a:panel id="footer-actions">
            <%--<f:subview id="footer-extra-actions" rendered="#{DialogManager.filterListVisible or DialogManager.moreActionsId != null}"> --%>
            <f:subview id="footer-extra-actions" rendered="#{DialogManager.filterListVisible and DialogManager.currentDialog.name ne 'manageGroups' or DialogManager.currentDialog.name eq 'document' and DocumentDialog.workflow.workflowMethodBindingName != null and DocumentDialog.meta.inEditMode == false}">
               <f:verbatim>
               <ul class="actions-menu extra-actions">
               </f:verbatim>
               
               <%-- View Filters --%>
               <f:subview id="footer-filters-panel" rendered="#{DialogManager.filterListVisible and DialogManager.currentDialog.name ne 'manageGroups'}">
                  <f:verbatim>
                  <li>
                  </f:verbatim>
                     <a:modeList itemSpacing="0" iconColumnWidth="0" selectedStyleClass="selected" menu="true"
                        styleClass="dropdown-menu no-icon right" menuImage="/images/icons/arrow-down.png" value="#{DialogManager.bean.filterMode}"
                        actionListener="#{DialogManager.bean.filterModeChanged}">
                        <a:listItems value="#{DialogManager.bean.filterItems}" />
                     </a:modeList>
                  <f:verbatim>
                  </li>
                 </f:verbatim>
               </f:subview>
               
               <f:subview id="footer-more-actions-panel">
               <%-- TODO: uncomment this when dropdown menu is ok (see next TODO) --%>
               <%--<f:subview id="footer-more-actions-panel" rendered="#{DialogManager.moreActionsId != null and DialogManager.currentDialog.name ne 'document'}">--%>
                  <f:verbatim>
                  <li>
                  </f:verbatim>
                     <a:menu id="footer-more_actions_menu" style="white-space:nowrap" menuStyleClass="dropdown-menu right"
                        label="#{DialogManager.moreActionsMenuLabel}" image="/images/icons/arrow-down.png" 
                        rendered="#{DialogManager.moreActionsId ne ''}">
                        <r:actions id="footer-more_actions_menu_items" value="#{DialogManager.moreActionsId}" context="#{DialogManager.actionsContext}" />
                     </a:menu>
                  <f:verbatim>
                  </li>
                  <li>
                  </f:verbatim>   
                     <a:menu id="footer-workflow_menu" style="white-space:nowrap" menuStyleClass="dropdown-menu right"
                        label="#{DocumentDialog.workflow.workflowMenuLabel}" image="/images/icons/arrow-down.png" 
                        rendered="#{DialogManager.currentDialog.name eq 'document' and DocumentDialog.workflow.workflowMethodBindingName != null and DocumentDialog.meta.inEditMode == false}">
                        <%-- Here the method call of the value parameter is actually returning a string in the form "#{method binding}"
                             UIActions class has been modified to interpret such strings as method bindings that take 1 parameter and
                             return a list of ActionDefinition objects. See UIActions and WorkflowBlockBean classes for details.
                              --%>
                        <r:actions id="footer-workflow_menu_items" value="#{DocumentDialog.workflow.workflowMethodBindingName}" context="#{DialogManager.actionsContext}" />
                     </a:menu>
                  <f:verbatim>
                  </li>
                  </f:verbatim>
               </f:subview>
               
               <f:verbatim>
               </ul>
               </f:verbatim>
               </f:subview>
               
               <%-- Main actions --%> 
               <a:panel id="footer-main-actions-panel" rendered="#{DialogManager.actionsId != null}">
                  <f:subview id="footer-not-empty-main-actions-subview" rendered="#{DialogManager.currentDialog.name eq 'manageGroups' and not empty DialogManager.bean.groups or DialogManager.currentDialog.name ne 'manageGroups'}">
                     <a:menu id="footer-main_actions_list" label="#{msg.more_actions}" styleClass="actions" menuStyleClass="actions-menu">
                        <r:actions id="footer-main_actions_list_items" rendered="#{DialogManager.actionsAsMenu == false}" value="#{DialogManager.actionsId}"
                           context="#{DialogManager.actionsContext}" />
                     </a:menu>
   
                     <a:menu id="footer-main_actions_menu" rendered="#{DialogManager.actionsAsMenu == true}" itemSpacing="4" image="/images/icons/arrow-down.png"
                        menuStyleClass="" style="white-space: nowrap" label="#{DialogManager.actionsMenuLabel}">
                        <r:actions id="footer-main_actions_menu_items" value="#{DialogManager.actionsId}" context="#{DialogManager.actionsContext}" />
                     </a:menu>
                     <%-- TODO: position the menu correctly to the left alongside the buttons --%>
                     <%--<a:menu id="footer-more_actions_menu" rendered="#{DialogManager.moreActionsId != null and DialogManager.currentDialog.name eq 'document'}" style="white-space:nowrap" menuStyleClass="dropdown-menu"
                        label="#{DialogManager.moreActionsMenuLabel}" image="/images/icons/arrow-down.png">
                        <r:actions id="footer-more_actions_menu_items" value="#{DialogManager.moreActionsId}" context="#{DialogManager.actionsContext}"/>
                     </a:menu>--%>
                  </f:subview>
                  <f:subview id="footer-empty-main-actions-subview" rendered="#{not (DialogManager.currentDialog.name eq 'manageGroups' and not empty DialogManager.bean.groups or DialogManager.currentDialog.name ne 'manageGroups') and (DialogManager.filterListVisible and DialogManager.currentDialog.name ne 'manageGroups' or DialogManager.currentDialog.name eq 'document' and DocumentDialog.workflow.workflowMethodBindingName != null and DocumentDialog.meta.inEditMode == false)}">
                     <h:outputText value="&nbsp;" escape="false" style="line-height:20px;" />
                  </f:subview>
               </a:panel>
               
               
               <%-- Space Actions --%>
               <a:panel id="footer-space-details-actions-panel" rendered="#{DialogManager.currentDialog.name eq 'showSpaceDetails' && DialogManager.bean.space != null}">
                  <a:menu id="footer-spaceActionsMenu" label="#{msg.more_actions}" styleClass="actions" menuStyleClass="actions-menu">
                     <r:actions id="footer-actions_space" value="space_details_actions" context="#{DialogManager.bean.space}" />
                  </a:menu>
               </a:panel>

               <%-- Document Actions --%>
               <a:panel id="footer-document-details-actions-panel" rendered="#{DialogManager.currentDialog.name eq 'showDocDetails' && DialogManager.bean.document != null}" >
                  <a:menu id="footer-documentActionsMenu" label="#{msg.more_actions}" styleClass="actions" menuStyleClass="actions-menu">
                     <r:actions id="footer-actions_doc" value="doc_details_actions" context="#{DialogManager.bean.document}" />
                  </a:menu>
               </a:panel>

            </a:panel>

            <a:panel id="footer-additional" rendered="#{DialogManager.navigationVisible or DialogManager.viewListVisible}">
               <%-- Navigation --%>
               <a:panel id="footer-nav-panel" rendered="#{DialogManager.navigationVisible}">
                  <a:actionLink id="footer-act-prev" value="#{msg.previous_item}" showLink="false" image="/images/icons/nav-prev.png"
                     actionListener="#{DialogManager.bean.previousItem}" action="#{DialogManager.bean.getOutcome}" padding="5">
                     <f:param name="id" value="#{DialogManager.bean.currentItemId}" />
                  </a:actionLink>

                  <h:graphicImage url="/images/icons/nav_file.gif" width="24" height="24" alt="" />

                  <a:actionLink id="footer-act-next" value="#{msg.next_item}" image="/images/icons/nav-next.png" showLink="false"
                     actionListener="#{DialogManager.bean.nextItem}" action="#{DialogManager.bean.getOutcome}" padding="5">
                     <f:param name="id" value="#{DialogManager.bean.currentItemId}" />
                  </a:actionLink>
               </a:panel>
               
               <%-- View Mode --%>
               <a:panel id="footer-views-panel" rendered="#{DialogManager.viewListVisible and DialogManager.currentDialog.name ne 'manageGroups'}">
                  <a:modeList iconColumnWidth="0" selectedStyleClass="selected" menu="true" styleClass="dropdown-menu right no-icon"
                     menuImage="/images/icons/arrow-down.png" value="#{DialogManager.bean.viewMode}" actionListener="#{DialogManager.bean.viewModeChanged}">
                     <a:listItems value="#{DialogManager.bean.viewItems}" />
                  </a:modeList>
               </a:panel>
               
            </a:panel>
         </a:panel>
         <f:verbatim><div class="clear"></div></f:verbatim>
      
      <% } %>
      </a:panel>
   </a:panel>
   <%@ include file="../parts/footer.jsp"%>
   </h:form>
</f:view>
</r:page>