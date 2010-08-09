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

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator"%>
<%@ page import="org.alfresco.web.app.Application"%>
<%@ page import="javax.faces.context.FacesContext"%>

<f:verbatim>
<div class="column panel-100">
</f:verbatim>

   <h:panelGroup id="dashboard-panel-facets">
      <f:facet name="title">
         <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write" id="evalChange">
            <a:actionLink id="actModify" value="#{msg.modify}" action="dialog:applyDocTemplate" showLink="false" image="/images/icons/preview.gif" />
            <a:actionLink id="actRemove" value="#{msg.remove}" actionListener="#{DialogManager.bean.removeTemplate}" showLink="false" image="/images/icons/delete.gif" />
         </r:permissionEvaluator>
      </f:facet>
   </h:panelGroup>

   <%-- Custom View --%>
   <a:panel label="#{msg.custom_view}" id="dashboard-panel" progressive="true" facetsId="dialog:dialog-body:dashboard-panel-facets"
      expanded='#{DialogManager.bean.panels["dashboard-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
      <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write" id="evalApply">
         <a:actionLink id="actDashboard" value="#{msg.apply_template}" rendered="#{!DialogManager.bean.hasCustomView}" action="dialog:applyDocTemplate" />
      </r:permissionEvaluator>

      <a:panel id="template-panel" rendered="#{DialogManager.bean.hasCustomView}">
         <f:verbatim>
            <r:webScript id="webscript" scriptUrl="#{DialogManager.bean.webscriptUrl}" context="#{DialogManager.bean.document.nodeRef}"
               rendered="#{DialogManager.bean.hasWebscriptView}" />
         </f:verbatim>
         <r:template id="dashboard" template="#{DialogManager.bean.templateRef}" model="#{DialogManager.bean.templateModel}" rendered="#{!DialogManager.bean.hasWebscriptView && DialogManager.bean.hasTemplateView}" />
      </a:panel>
   </a:panel>

<a:panel label="#{msg.view_links}" id="preview-panel" progressive="true" expanded='#{DialogManager.bean.panels["preview-panel"]}'
   expandedActionListener="#{DialogManager.bean.expandPanel}">

   <a:actionLink value="#{msg.view_in_browser}" href="#{DialogManager.bean.browserUrl}" target="_blank" id="link1" />
   <a:actionLink value="#{msg.view_in_webdav}" href="#{DialogManager.bean.webdavUrl}" target="_blank" id="link2" />
   <a:actionLink value="#{msg.view_in_cifs}" href="#{DialogManager.bean.cifsPath}" target="_blank" id="link3" />
   <a:actionLink value="#{msg.download_content}" href="#{DialogManager.bean.downloadUrl}" target="_blank" id="link4" />
   
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

<h:panelGroup id="props-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write">
         <a:actionLink id="titleLink1" value="#{msg.modify}" showLink="false" image="/images/icons/edit_properties.gif"
            action="#{DialogManager.bean.editContentProperties}" />
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>

<a:panel label="#{msg.properties}" id="properties-panel" facetsId="dialog:dialog-body:props-panel-facets" progressive="true"
   rendered="#{DialogManager.bean.locked == false}" expanded='#{DialogManager.bean.panels["properties-panel"]}'
   expandedActionListener="#{DialogManager.bean.expandPanel}">

   <%-- properties for the doc --%>
   <r:propertySheetGrid id="document-props" value="#{DialogManager.bean.document}" var="documentProps" columns="1" mode="view" labelStyleClass="propertiesLabel"
      externalConfig="true" />
   <h:outputText id="no-inline-msg" value="<br/>#{msg.not_inline_editable}<br/><br/>" rendered="#{DialogManager.bean.inlineEditable == false}" escape="false" />
   <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write" id="eval_inline">
      <a:actionLink id="make-inline" value="#{msg.allow_inline_editing}" action="#{DialogManager.bean.applyInlineEditable}"
         rendered="#{DialogManager.bean.inlineEditable == false}" />
   </r:permissionEvaluator>
   <h:message id="msg1" for="document-props" styleClass="statusMessage" />
</a:panel>

<a:panel label="#{msg.properties}" id="properties-panel-locked" progressive="true" rendered="#{DialogManager.bean.locked}"
   expanded='#{DialogManager.bean.panels["properties-panel-locked"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">

   <a:actionLink id="doc-logo2" value="#{DialogManager.bean.name}" href="#{DialogManager.bean.url}" target="_blank"
      image="#{DialogManager.bean.document.properties.fileType32}" showLink="false" />
   <r:propertySheetGrid id="document-props-locked" value="#{DialogManager.bean.document}" var="documentProps" columns="1" mode="view"
      labelStyleClass="propertiesLabel" externalConfig="true" />
   <h:outputText id="no-inline-msg2" value="<br/>#{msg.not_inline_editable}<br/>" rendered="#{DialogManager.bean.inlineEditable == false}" escape="false" />
   <h:message id="msg2" for="document-props-locked" styleClass="statusMessage" />

</a:panel>


<%-- Multilingual properties --%>
<h:panelGroup id="ml-props-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write">
         <a:actionLink id="titleLinkMl" value="#{msg.modify}" showLink="false" image="/images/icons/edit_properties.gif" action="dialog:editMlContainer" />
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>

<%-- Panel if the node has the multilingual aspect--%>
<a:panel label="#{msg.ml_content_info}" facetsId="dialog:dialog-body:ml-props-panel-facets" id="ml-info-panel" progressive="true"
   rendered="#{DialogManager.bean.multilingual}" expanded='#{DialogManager.bean.panels["ml-info-panel"]}'
   expandedActionListener="#{DialogManager.bean.expandPanel}">

   <%-- properties for Ml container --%>

   <h:outputText value="#{msg.properties}" styleClass="nodeWorkflowInfoTitle" style="padding:20px;" />

   <r:propertySheetGrid id="ml-container-props-sheet" value="#{DialogManager.bean.documentMlContainer}" var="mlContainerProps" columns="1"
      labelStyleClass="propertiesLabel" externalConfig="true" cellpadding="2" cellspacing="2" mode="view" />


   <a:panel label="#{msg.related_translations}" id="related-translation-panel" progressive="true"
      expanded='#{DialogManager.bean.panels["related-translation-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}"
      styleClass="nodeWorkflowInfoTitle with-pager">


      <%-- list of translations --%>
      <a:richList id="TranslationList" viewMode="details" value="#{MultilingualManageDialog.translations}" var="r" rowStyleClass="recordSetRow" 
         altRowStyleClass="recordSetRowAlt" width="100%" pageSize="10" initialSortColumn="Name" initialSortDescending="false">

         <%-- Name and icon columns --%>
         <a:column id="col21" primary="true" width="300" style="text-align:left">
            <f:facet name="small-icon">
               <h:graphicImage url="/images/filetypes/_default.gif" width="16" height="16" />
            </f:facet>
            <f:facet name="header">
               <a:sortLink label="#{msg.name}" value="Name" mode="case-insensitive" styleClass="header" />
            </f:facet>
            <a:actionLink id="view-name" value="#{r.name}" href="#{r.url}" target="_blank" />
         </a:column>

         <%-- Language columns --%>
         <a:column id="col22" width="50" style="text-align:left">
            <f:facet name="header">
               <a:sortLink label="#{msg.language}" value="language" mode="case-insensitive" styleClass="header" />
            </f:facet>
            <h:outputText id="view-language" value="#{r.language}" />
         </a:column>

         <%-- view actions --%>
         <a:column id="col25" style="text-align: left">
            <f:facet name="header">
               <h:outputText value="#{msg.actions}" />
            </f:facet>
            <a:actionLink id="view-link" value="#{msg.view}" href="#{r.url}" target="_blank" />
            <%--
                                             Start the new edition wizard from this translation
                                          --%>
            <h:outputText id="space" value=" " />
            <a:actionLink id="new-edition-from" value="#{msg.new_edition}" action="wizard:newEditionFrom" actionListener="#{NewEditionWizard.skipFirstStep}"
               rendered="#{r.userHasRight}">
               <f:param name="lang" value="#{r.language}" />
            </a:actionLink>
         </a:column>

         <a:dataPager styleClass="pager" id="pager-translations" />
      </a:richList>
   </a:panel>

   <%-- Actions - Add Translation, Add Translation with Content --%>
   <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write" id="evalApply">
      <a:actionLink id="act-add-trans-with-c" value="#{msg.add_translation}" action="dialog:addTranslation" actionListener="#{AddTranslationDialog.start}"
         showLink="true" image="/images/icons/add_tranlsation.gif" />
      <a:actionLink id="act-add-trans-without-c" value="#{msg.add_translation_wc}" action="dialog:addTranslationWithoutContent" showLink="true"
         image="/images/icons/add_tranlsation_wc.gif" />
   </r:permissionEvaluator>
</a:panel>

<%-- Panel if the node has not the multilingual aspect--%>
<a:panel label="#{msg.ml_content_info}" id="no-ml-info-panel" progressive="true" rendered="#{DialogManager.bean.multilingual == false}"
   expanded='#{DialogManager.bean.panels["ml-info-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
   <h:outputText id="no-ml-msg" value="#{msg.not_multilingual}" />
   <%-- Action - Add Translation --%>
   <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write" id="evalApply">
      <a:actionLink id="act-make-multilingual" value="#{msg.make_multilingual}" action="dialog:makeMultilingual" showLink="true" image="/images/icons/make_ml.gif" rendered="#{DialogManager.bean.locked == false && DialogManager.bean.workingCopy == false}"/>
   </r:permissionEvaluator>
</a:panel>


<h:panelGroup id="workflow-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write">
         <a:actionLink id="titleLink2" value="#{msg.title_edit_simple_workflow}" showLink="false" image="/images/icons/Change_details.gif" action="dialog:editContentSimpleWorkflow" rendered="#{DialogManager.bean.approveStepName != null}" />
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>
<a:panel label="#{msg.workflows}" id="workflow-panel" facetsId="dialog:dialog-body:workflow-panel-facets"
   expanded='#{DialogManager.bean.panels["workflow-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}">
   <r:nodeWorkflowInfo id="workflow-info" value="#{DialogManager.bean.document}" />
</a:panel>


<h:panelGroup id="category-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write" id="eval_cat0">
         <a:actionLink id="titleLink3" value="#{msg.change_category}" showLink="false" image="/images/icons/Change_details.gif" action="dialog:editNodeCategories" actionListener="#{DialogManager.setupParameters}">
            <f:param name="nodeRef" value="#{DialogManager.bean.document.nodeRefAsString}" />
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
   <h:outputText id="no-category-msg" value="#{msg.not_in_category}<br/><br/>" escape="false" />
   <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write" id="eval_cat">
      <a:actionLink id="make-classifiable" value="#{msg.allow_categorization}" action="#{DialogManager.bean.applyClassifiable}" rendered="#{DialogManager.bean.locked == false}" />
   </r:permissionEvaluator>
</a:panel>


<a:panel label="#{msg.version_history}" id="version-history-panel" progressive="true" rendered="#{DialogManager.bean.versionable && !NavigationBean.isGuest}"
   expanded='#{DialogManager.bean.panels["version-history-panel"]}' expandedActionListener="#{DialogManager.bean.expandPanel}" styleClass="with-pager">

   <a:richList id="versionHistoryList" viewMode="details" value="#{DialogManager.bean.versionHistory}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" pageSize="10" initialSortColumn="versionDate" initialSortDescending="true">

      <%-- Primary column for details view mode --%>
      <a:column id="col1" primary="true" width="100" style="padding:2px;text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.version}" value="versionLabel" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <a:actionLink id="label-link" value="#{r.versionLabel}" href="#{r.url}" target="_blank" rendered="#{r.url != null}" />
         <a:actionLink id="label-no-link" value="#{r.versionLabel}" rendered="#{r.url == null}" />
      </a:column>

      <%-- Version notes columns --%>
      <a:column id="col2" width="170" style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.notes}" value="notes" styleClass="header" />
         </f:facet>
         <h:outputText id="notes" value="#{r.notes}" />
      </a:column>

      <%-- Description columns --%>
      <a:column id="col3" style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.author}" value="author" styleClass="header" />
         </f:facet>
         <h:outputText id="author" value="#{r.author}" />
      </a:column>

      <%-- Created Date column for details view mode --%>
      <a:column id="col4" style="text-align:left; white-space:nowrap">
         <f:facet name="header">
            <a:sortLink label="#{msg.date}" value="versionDate" styleClass="header" />
         </f:facet>
         <h:outputText id="date" value="#{r.versionDate}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- view the contents of the specific version --%>
      <a:column id="col5" style="text-align: left">
         <f:facet name="header">
            <h:outputText value="#{msg.actions}" />
         </f:facet>
         <a:actionLink id="view-version-props" value="#{msg.properties}" action="dialog:showVersionedDetails"
            actionListener="#{VersionedDocumentDetailsDialog.setBrowsingVersion}">
            <f:param id="pm1" name="id" value="#{DialogManager.bean.document.id}" />
            <f:param id="pm2" name="versionLabel" value="#{r.versionLabel}" />
         </a:actionLink>
         <h:outputText id="space" value=" " />
         <a:actionLink id="view-link" value="#{msg.view}" href="#{r.url}" target="_blank" rendered="#{r.url != null}" />
      </a:column>

      <a:dataPager styleClass="pager" id="pager-version-history" />
   </a:richList>
</a:panel>

<%@ include file="/WEB-INF/classes/ee/webmedia/alfresco/signature/web/signature-file-panel.jsp" %>

<a:panel label="#{msg.version_history}" id="no-version-history-panel" progressive="true"
   rendered="#{DialogManager.bean.versionable == false && !NavigationBean.isGuest}" expanded='#{DialogManager.bean.panels["version-history-panel"]}'
   expandedActionListener="#{DialogManager.bean.expandPanel}">
   <h:outputText id="no-history-msg" value="#{msg.not_versioned}<br/><br/>" escape="false" />
   <r:permissionEvaluator value="#{DialogManager.bean.document}" allow="Write" id="eval_ver">
      <a:actionLink id="make-versionable" value="#{msg.allow_versioning}" action="#{DialogManager.bean.applyVersionable}"
         rendered="#{DialogManager.bean.locked == false}" />
   </r:permissionEvaluator>
</a:panel>
<f:verbatim>
</div>
</f:verbatim>