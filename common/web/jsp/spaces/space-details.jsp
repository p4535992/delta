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
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<f:verbatim>
<div class="column panel-100">
</f:verbatim>
      <%-- wrapper comment used by the panel to add additional component facets --%> 
      <h:panelGroup id="dashboard-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="evalChange">
               <a:actionLink id="actModify" value="#{msg.modify}" action="dialog:applySpaceTemplate" showLink="false" image="/images/icons/preview.gif" />
               <a:actionLink id="actRemove" value="#{msg.remove}" actionListener="#{DialogManager.bean.removeTemplate}" showLink="false" image="/images/icons/delete.gif" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      
      <%-- Custom View --%>
      <a:panel label="#{msg.custom_view}" id="dashboard-panel" progressive="true" facetsId="dialog:dialog-body:dashboard-panel-facets"
         expanded='#{DialogManager.bean.panels["dashboard-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="evalApply">
         </r:permissionEvaluator>
         <f:verbatim></f:verbatim>
         <a:panel id="template-panel" rendered="#{DialogManager.bean.hasCustomView}">
            <f:verbatim>
               <div style="padding: 4px; border: 1px dashed #cccccc">
            </f:verbatim>
            <r:webScript id="webscript" scriptUrl="#{DialogManager.bean.webscriptUrl}" context="#{DialogManager.bean.space.nodeRef}"
               rendered="#{DialogManager.bean.hasWebscriptView}" />
            <r:template id="dashboard" template="#{DialogManager.bean.templateRef}" model="#{DialogManager.bean.templateModel}"
               rendered="#{!DialogManager.bean.hasWebscriptView && DialogManager.bean.hasTemplateView}" />
            <f:verbatim>
               </div>
            </f:verbatim>
         </a:panel>
      </a:panel>

      <a:panel label="#{msg.links}" id="links-panel" progressive="true" expanded='#{DialogManager.bean.panels["links-panel"]}'
         expandedActionListener="#{DialogManager.bean.expandPanel}">

   <a:actionLink value="#{msg.view_in_webdav}" href="#{DialogManager.bean.webdavUrl}" target="_blank" id="link2" />
   <a:actionLink value="#{msg.view_in_cifs}" href="#{DialogManager.bean.cifsPath}" target="_blank" id="link3" />
   
   <f:verbatim>
   <a href='
   </f:verbatim>
   <%=request.getContextPath()%>
   <a:outputText value="#{DialogManager.bean.bookmarkUrl}" id="out1" />
   <f:verbatim>'
   onclick="return false;">
   </f:verbatim>
   <a:outputText value="#{msg.details_page_bookmark}" id="out2" />
   <f:verbatim>
   </a>
   <a href='
   </f:verbatim>
   <a:outputText value="#{DialogManager.bean.nodeRefUrl}" id="out3" />
   <f:verbatim>
   ' onclick="return false;">
   </f:verbatim>
   <a:outputText value="#{msg.noderef_link}" id="out4" />
   <f:verbatim>
      </a>
   </f:verbatim>
      </a:panel>

<%-- wrapper comment used by the panel to add additional component facets --%> 
      <h:panelGroup id="props-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write">
               <a:actionLink id="titleLink1" value="#{msg.modify}" showLink="false" image="/images/icons/edit_properties.gif" action="dialog:editSpace" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      <a:panel label="#{msg.properties}" id="properties-panel" facetsId="dialog:dialog-body:props-panel-facets" progressive="true"
         expanded='#{DialogManager.bean.panels["properties-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">

               <%-- properties for the space --%> 
               <r:propertySheetGrid id="space-props" value="#{DialogManager.bean.space}" var="spaceProps" columns="1" mode="view" labelStyleClass="propertiesLabel" externalConfig="true" /> <h:message
                  for="space-props" styleClass="statusMessage" />
      </a:panel>

      <h:panelGroup id="workflow-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write">
               <a:actionLink id="titleLink4" value="#{msg.title_edit_simple_workflow}" showLink="false" image="/images/icons/Change_details.gif" action="dialog:editSpaceSimpleWorkflow" rendered="#{DialogManager.bean.approveStepName != null}" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      <a:panel label="#{msg.workflows}" id="workflow-panel" facetsId="dialog:dialog-body:workflow-panel-facets" progressive="true"
         expanded='#{DialogManager.bean.panels["workflow-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         <r:nodeWorkflowInfo id="workflow-info" value="#{DialogManager.bean.space}" />
      </a:panel>

      <h:panelGroup id="category-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="eval_cat0">
               <a:actionLink id="titleLink3" value="#{msg.change_category}" showLink="false" image="/images/icons/Change_details.gif" action="dialog:editNodeCategories" actionListener="#{DialogManager.setupParameters}">
                  <f:param name="nodeRef" value="#{DialogManager.bean.space.nodeRefAsString}" />
               </a:actionLink>
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      <a:panel label="#{msg.category}" id="category-panel" facetsId="dialog:dialog-body:category-panel-facets" progressive="true"
         rendered="#{DialogManager.bean.categorised}" expanded='#{DialogManager.bean.panels["category-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         <h:outputText id="category-overview" value="#{DialogManager.bean.categoriesOverviewHTML}" escape="false" />
      </a:panel>
      <a:panel label="#{msg.category}" id="no-category-panel" progressive="true" rendered="#{DialogManager.bean.categorised == false}"
         expanded='#{DialogManager.bean.panels["category-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         <h:outputText id="no-category-msg" value="#{msg.not_in_category_space}<br/><br/>" escape="false" />
         <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="eval_cat">
            <a:actionLink id="make-classifiable" value="#{msg.allow_categorization}" action="#{DialogManager.bean.applyClassifiable}" rendered="#{DialogManager.bean.locked == false}" />
         </r:permissionEvaluator>
      </a:panel>

      <h:panelGroup id="rules-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="eval_rules">
               <a:actionLink id="titleLink2" value="#{msg.modify}" showLink="false" image="/images/icons/rule.gif" action="dialog:manageRules" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      <a:panel label="#{msg.rules}" id="rules-panel" facetsId="dialog:dialog-body:rules-panel-facets" progressive="true"
         expanded='#{DialogManager.bean.panels["rules-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}" styleClass="with-pager">
         <a:richList id="rulesList" viewMode="details" value="#{RulesDialog.rules}" var="r" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
            width="100%" pageSize="10" initialSortColumn="title" initialSortDescending="true">

            <%-- Primary column for details view mode --%>
            <a:column id="col1" primary="true" width="200" style="padding:2px;text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink1" label="#{msg.title}" value="title" mode="case-insensitive" styleClass="header" />
               </f:facet>
               <h:outputText id="title" value="#{r.title}" />
            </a:column>

            <%-- Description columns --%>
            <a:column id="col2" style="text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink2" label="#{msg.description}" value="description" styleClass="header" />
               </f:facet>
               <h:outputText id="description" value="#{r.description}" />
            </a:column>

            <%-- Column to show whether the rule is local --%>
            <a:column id="col3" style="text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink3" label="#{msg.local}" value="local" styleClass="header" />
               </f:facet>
               <h:outputText value="#{r.local}">
                  <a:convertBoolean />
               </h:outputText>
            </a:column>

            <%-- Created Date column for details view mode --%>
            <a:column id="col4" style="text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink4" label="#{msg.created_date}" value="createdDate" styleClass="header" />
               </f:facet>
               <h:outputText id="createddate" value="#{r.createdDate}">
                  <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
               </h:outputText>
            </a:column>

            <%-- Modified Date column for details/icons view modes --%>
            <a:column id="col5" style="text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink5" label="#{msg.modified_date}" value="modifiedDate" styleClass="header" />
               </f:facet>
               <h:outputText id="modifieddate" value="#{r.modifiedDate}">
                  <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
               </h:outputText>
            </a:column>

            <a:dataPager styleClass="pager" />
         </a:richList>
      </a:panel>

      <h:panelGroup id="rss-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="evalChange2">
               <a:actionLink id="actModify2" value="#{msg.modify}" action="dialog:applyRSSTemplate" showLink="false" image="/images/icons/rss.gif" />
               <a:actionLink id="actRemove2" value="#{msg.remove}" actionListener="#{DialogManager.bean.removeRSSTemplate}" showLink="false" image="/images/icons/delete.gif" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup>
       
      <a:panel label="#{msg.rss_feed}" id="rss-panel" progressive="true" facetsId="dialog:dialog-body:rss-panel-facets"
         expanded='#{DialogManager.bean.panels["rss-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         
               <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="evalApply2">
                  <a:actionLink id="actApplyRSS" value="#{msg.apply_rss_feed}" rendered="#{DialogManager.bean.RSSFeed == false}" action="dialog:applyRSSTemplate" />
               </r:permissionEvaluator> <a:actionLink id="actRSS" value="#{msg.rss_feed_link}" href="#{DialogManager.bean.RSSFeedURL}" image="/images/icons/rss.gif" rendered="#{DialogManager.bean.RSSFeed == true}" padding="2" />
         
      </a:panel>
      <f:verbatim></div></f:verbatim>
      
      <%-- TODO: implement this - but READONLY details only! Manage Space Users for edits...
           need support for panel with facets - so can hide edit link unless edit permissions
           also need to wrap this panel with an permissions check: ReadPermissions
      <a:panel label="#{msg.security}" id="security-panel" progressive="true" expanded="false"
       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white"
       action="dialog:manageInvitedUsers" linkTooltip="#{msg.manage_invited_users}" linkIcon="/images/icons/invite.gif">
         <table cellspacing="2" cellpadding="0" border="0" width="100%">
         </table>
      </a:panel>
      <div style="padding:4px"></div>
       --%> <%-- TBD
      <a:panel label="Preferences" id="preferences-panel" progressive="true" expanded="false"
       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white">
         <div></div>
      </a:panel>
      --%>      
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
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<f:verbatim>
<div class="column panel-100">
</f:verbatim>
      <%-- wrapper comment used by the panel to add additional component facets --%> 
      <h:panelGroup id="dashboard-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="evalChange">
               <a:actionLink id="actModify" value="#{msg.modify}" action="dialog:applySpaceTemplate" showLink="false" image="/images/icons/preview.gif" />
               <a:actionLink id="actRemove" value="#{msg.remove}" actionListener="#{DialogManager.bean.removeTemplate}" showLink="false" image="/images/icons/delete.gif" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      
      <%-- Custom View --%>
      <a:panel label="#{msg.custom_view}" id="dashboard-panel" progressive="true" facetsId="dialog:dialog-body:dashboard-panel-facets"
         expanded='#{DialogManager.bean.panels["dashboard-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="evalApply">
         </r:permissionEvaluator>
         <f:verbatim></f:verbatim>
         <a:panel id="template-panel" rendered="#{DialogManager.bean.hasCustomView}">
            <f:verbatim>
               <div style="padding: 4px; border: 1px dashed #cccccc">
            </f:verbatim>
            <r:webScript id="webscript" scriptUrl="#{DialogManager.bean.webscriptUrl}" context="#{DialogManager.bean.space.nodeRef}"
               rendered="#{DialogManager.bean.hasWebscriptView}" />
            <r:template id="dashboard" template="#{DialogManager.bean.templateRef}" model="#{DialogManager.bean.templateModel}"
               rendered="#{!DialogManager.bean.hasWebscriptView && DialogManager.bean.hasTemplateView}" />
            <f:verbatim>
               </div>
            </f:verbatim>
         </a:panel>
      </a:panel>

      <a:panel label="#{msg.links}" id="links-panel" progressive="true" expanded='#{DialogManager.bean.panels["links-panel"]}'
         expandedActionListener="#{DialogManager.bean.expandPanel}">

   <a:actionLink value="#{msg.view_in_webdav}" href="#{DialogManager.bean.webdavUrl}" target="_blank" id="link2" />
   <a:actionLink value="#{msg.view_in_cifs}" href="#{DialogManager.bean.cifsPath}" target="_blank" id="link3" />
   
   <f:verbatim>
   <a href='
   </f:verbatim>
   <%=request.getContextPath()%>
   <a:outputText value="#{DialogManager.bean.bookmarkUrl}" id="out1" />
   <f:verbatim>'
   onclick="return false;">
   </f:verbatim>
   <a:outputText value="#{msg.details_page_bookmark}" id="out2" />
   <f:verbatim>
   </a>
   <a href='
   </f:verbatim>
   <a:outputText value="#{DialogManager.bean.nodeRefUrl}" id="out3" />
   <f:verbatim>
   ' onclick="return false;">
   </f:verbatim>
   <a:outputText value="#{msg.noderef_link}" id="out4" />
   <f:verbatim>
      </a>
   </f:verbatim>
      </a:panel>

<%-- wrapper comment used by the panel to add additional component facets --%> 
      <h:panelGroup id="props-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write">
               <a:actionLink id="titleLink1" value="#{msg.modify}" showLink="false" image="/images/icons/edit_properties.gif" action="dialog:editSpace" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      <a:panel label="#{msg.properties}" id="properties-panel" facetsId="dialog:dialog-body:props-panel-facets" progressive="true"
         expanded='#{DialogManager.bean.panels["properties-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">

               <%-- properties for the space --%> 
               <r:propertySheetGrid id="space-props" value="#{DialogManager.bean.space}" var="spaceProps" columns="1" mode="view" labelStyleClass="propertiesLabel" externalConfig="true" /> <h:message
                  for="space-props" styleClass="statusMessage" />
      </a:panel>

      <h:panelGroup id="workflow-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write">
               <a:actionLink id="titleLink4" value="#{msg.title_edit_simple_workflow}" showLink="false" image="/images/icons/Change_details.gif" action="dialog:editSpaceSimpleWorkflow" rendered="#{DialogManager.bean.approveStepName != null}" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      <a:panel label="#{msg.workflows}" id="workflow-panel" facetsId="dialog:dialog-body:workflow-panel-facets" progressive="true"
         expanded='#{DialogManager.bean.panels["workflow-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         <r:nodeWorkflowInfo id="workflow-info" value="#{DialogManager.bean.space}" />
      </a:panel>

      <h:panelGroup id="category-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="eval_cat0">
               <a:actionLink id="titleLink3" value="#{msg.change_category}" showLink="false" image="/images/icons/Change_details.gif" action="dialog:editNodeCategories" actionListener="#{DialogManager.setupParameters}">
                  <f:param name="nodeRef" value="#{DialogManager.bean.space.nodeRefAsString}" />
               </a:actionLink>
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      <a:panel label="#{msg.category}" id="category-panel" facetsId="dialog:dialog-body:category-panel-facets" progressive="true"
         rendered="#{DialogManager.bean.categorised}" expanded='#{DialogManager.bean.panels["category-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         <h:outputText id="category-overview" value="#{DialogManager.bean.categoriesOverviewHTML}" escape="false" />
      </a:panel>
      <a:panel label="#{msg.category}" id="no-category-panel" progressive="true" rendered="#{DialogManager.bean.categorised == false}"
         expanded='#{DialogManager.bean.panels["category-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         <h:outputText id="no-category-msg" value="#{msg.not_in_category_space}<br/><br/>" escape="false" />
         <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="eval_cat">
            <a:actionLink id="make-classifiable" value="#{msg.allow_categorization}" action="#{DialogManager.bean.applyClassifiable}" rendered="#{DialogManager.bean.locked == false}" />
         </r:permissionEvaluator>
      </a:panel>

      <h:panelGroup id="rules-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="eval_rules">
               <a:actionLink id="titleLink2" value="#{msg.modify}" showLink="false" image="/images/icons/rule.gif" action="dialog:manageRules" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup> 
      <a:panel label="#{msg.rules}" id="rules-panel" facetsId="dialog:dialog-body:rules-panel-facets" progressive="true"
         expanded='#{DialogManager.bean.panels["rules-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}" styleClass="with-pager">
         <a:richList id="rulesList" viewMode="details" value="#{RulesDialog.rules}" var="r" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
            width="100%" pageSize="10" initialSortColumn="title" initialSortDescending="true">

            <%-- Primary column for details view mode --%>
            <a:column id="col1" primary="true" width="200" style="padding:2px;text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink1" label="#{msg.title}" value="title" mode="case-insensitive" styleClass="header" />
               </f:facet>
               <h:outputText id="title" value="#{r.title}" />
            </a:column>

            <%-- Description columns --%>
            <a:column id="col2" style="text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink2" label="#{msg.description}" value="description" styleClass="header" />
               </f:facet>
               <h:outputText id="description" value="#{r.description}" />
            </a:column>

            <%-- Column to show whether the rule is local --%>
            <a:column id="col3" style="text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink3" label="#{msg.local}" value="local" styleClass="header" />
               </f:facet>
               <h:outputText value="#{r.local}">
                  <a:convertBoolean />
               </h:outputText>
            </a:column>

            <%-- Created Date column for details view mode --%>
            <a:column id="col4" style="text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink4" label="#{msg.created_date}" value="createdDate" styleClass="header" />
               </f:facet>
               <h:outputText id="createddate" value="#{r.createdDate}">
                  <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
               </h:outputText>
            </a:column>

            <%-- Modified Date column for details/icons view modes --%>
            <a:column id="col5" style="text-align:left">
               <f:facet name="header">
                  <a:sortLink id="sortlink5" label="#{msg.modified_date}" value="modifiedDate" styleClass="header" />
               </f:facet>
               <h:outputText id="modifieddate" value="#{r.modifiedDate}">
                  <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
               </h:outputText>
            </a:column>

            <a:dataPager styleClass="pager" />
         </a:richList>
      </a:panel>

      <h:panelGroup id="rss-panel-facets">
         <f:facet name="title">
            <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="evalChange2">
               <a:actionLink id="actModify2" value="#{msg.modify}" action="dialog:applyRSSTemplate" showLink="false" image="/images/icons/rss.gif" />
               <a:actionLink id="actRemove2" value="#{msg.remove}" actionListener="#{DialogManager.bean.removeRSSTemplate}" showLink="false" image="/images/icons/delete.gif" />
            </r:permissionEvaluator>
         </f:facet>
      </h:panelGroup>
       
      <a:panel label="#{msg.rss_feed}" id="rss-panel" progressive="true" facetsId="dialog:dialog-body:rss-panel-facets"
         expanded='#{DialogManager.bean.panels["rss-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
         
               <r:permissionEvaluator value="#{DialogManager.bean.space}" allow="Write" id="evalApply2">
                  <a:actionLink id="actApplyRSS" value="#{msg.apply_rss_feed}" rendered="#{DialogManager.bean.RSSFeed == false}" action="dialog:applyRSSTemplate" />
               </r:permissionEvaluator> <a:actionLink id="actRSS" value="#{msg.rss_feed_link}" href="#{DialogManager.bean.RSSFeedURL}" image="/images/icons/rss.gif" rendered="#{DialogManager.bean.RSSFeed == true}" padding="2" />
         
      </a:panel>
      <f:verbatim></div></f:verbatim>
      
      <%-- TODO: implement this - but READONLY details only! Manage Space Users for edits...
           need support for panel with facets - so can hide edit link unless edit permissions
           also need to wrap this panel with an permissions check: ReadPermissions
      <a:panel label="#{msg.security}" id="security-panel" progressive="true" expanded="false"
       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white"
       action="dialog:manageInvitedUsers" linkTooltip="#{msg.manage_invited_users}" linkIcon="/images/icons/invite.gif">
         <table cellspacing="2" cellpadding="0" border="0" width="100%">
         </table>
      </a:panel>
      <div style="padding:4px"></div>
       --%> <%-- TBD
      <a:panel label="Preferences" id="preferences-panel" progressive="true" expanded="false"
       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white">
         <div></div>
      </a:panel>
      --%>      
>>>>>>> develop-5.1
