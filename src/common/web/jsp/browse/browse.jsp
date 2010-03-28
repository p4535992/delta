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

<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="javax.faces.context.FacesContext" %>

<%@ page import="org.alfresco.web.ui.common.PanelGenerator"%>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="100kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<r:page titleId="title_browse">

<f:view>
   <%
       FacesContext fc = FacesContext.getCurrentInstance();

               // set locale for JSF framework usage
               fc.getViewRoot().setLocale(Application.getLanguage(fc));
   %>

   <%-- load a bundle of properties with I18N strings --%>
   <r:loadBundle var="msg"/>

   <h:form acceptcharset="UTF-8" id="browse">

      <%-- Title bar --%>
      <%@ include file="../parts/titlebar.jsp"%>

      <%-- Main area --%>
      <a:panel id="container">

      <%-- Shelf --%> 
      <%@ include file="../parts/shelf.jsp"%> 

      <%-- Work Area --%>
      <a:panel id="content">

      <%-- Breadcrumb --%>
      <%@ include	file="../parts/breadcrumb.jsp"%>

      <%-- Status and Actions --%>
      <%-- Status and Actions inner contents table --%>
      <%-- Generally this consists of an icon, textual summary and actions for the current object --%>

      <%-- actions for browse mode --%>
      <f:subview id="browse-titlebar"  rendered="#{NavigationBean.searchContext == null}">
         <a:panel id="titlebar">

            <h2 class="title-icon">
               <h:graphicImage id="space-logo" url="/images/icons/#{NavigationBean.nodeProperties.icon}.gif" width="32" height="32" />
               <h:outputText value="#{NavigationBean.nodeProperties.name}" id="msg2" />
            </h2>
            <%--
            <div id="description">
               <h:outputText value="#{WizardManager.subTitle}" />
               <h:outputText value="#{msg.view_description}" id="msg3" />
               <h:outputText value="#{NavigationBean.nodeProperties.description}" id="msg4" />
            </div>
            --%>

            <%-- Summary --%>
            <a:panel id="additional">
               <span>
                  <a:actionLink image="/images/icons/opennetwork.gif" value="#{msg.network_folder} #{NavigationBean.nodeProperties.cifsPathLabel}" showLink="false" href="#{NavigationBean.nodeProperties.cifsPath}" rendered="#{NavigationBean.nodeProperties.cifsPath != null}" target="new" id="cifs" />&nbsp;
                  <a:actionLink id="actRSS" value="#{msg.rss_feed_link}" showLink="false" image="/images/icons/rss.gif" href="#{NavigationBean.RSSFeedURL}" rendered="#{NavigationBean.RSSFeed == true}" />
                  <%-- Additional summary info --%>
                  <h:graphicImage id="img-rule" url="/images/icons/rule.gif" width="16" height="16" title="#{msg.rules_count}" /> <h:outputText value="(#{NavigationBean.ruleCount})" id="rulemsg1" style="vertical-align:20%" />
               </span>

               <%-- View mode settings --%>
               <a:modeList iconColumnWidth="0" id="viewMode" selectedStyleClass="selected" disabledStyleClass="disabled"
               selectedImage="/images/icons/Details.gif" value="#{BrowseBean.browseViewMode}" actionListener="#{BrowseBean.viewModeChanged}" menu="true"
               menuImage="/images/icons/arrow-down.png" styleClass="dropdown-menu right no-icon">
                  <a:listItem value="details" label="#{msg.details_view}" />
                  <a:listItem value="icons" label="#{msg.view_icon}" />
                  <a:listItem value="list" label="#{msg.view_browse}" />
                  <a:listItem value="dashboard" label="#{msg.custom_view}" disabled="#{!NavigationBean.hasCustomView}" />
               </a:modeList>
            </a:panel>

            <a:panel id="actions">
               <ul class="actions-menu extra-actions">
                  <li>
                     <%-- Create actions menu --%>
                     <a:menu id="createMenu" itemSpacing="4" label="#{msg.create_options}" image="/images/icons/arrow-down.png"
                     menuStyleClass="dropdown-menu" style="white-space:nowrap" rendered="#{NavigationBean.createChildrenPermissionEnabled}">
                        <r:actions id="acts_create" value="browse_create_menu" context="#{NavigationBean.currentNode}" />
                     </a:menu>
                  </li>
                  <li>
                     <%-- Quick upload action --%>
                     <r:actions id="acts_add_content" value="add_content_menu" context="#{NavigationBean.currentNode}" showLink="true" />
                  </li>
               </ul>

               <%-- More actions menu --%>
               <a:menu id="actionsMenu" label="#{msg.more_actions}:" styleClass="actions" menuStyleClass="actions-menu">
                  <r:actions id="acts_browse" value="browse_actions_menu" context="#{NavigationBean.currentNode}" />
               </a:menu>
            </a:panel>
         </a:panel>
      </f:subview>
      
      
      <%-- actions for search results mode --%>
      <f:subview id="search-titlebar" rendered="#{NavigationBean.searchContext != null}">
         <a:panel id="titlebar">
            <%-- Summary --%>
            <f:verbatim>
            <h2 class="title-icon">
            </f:verbatim>
               <h:graphicImage url="/images/icons/search_results_large.gif" width="32" height="32" />
               <h:outputFormat value="#{msg.search_detail}" id="msg12"><f:param value="#{NavigationBean.searchContext.text}" id="param2" /></h:outputFormat>
               <%--<h:outputText value="#{msg.search_results}" id="msg11" />--%>
            <f:verbatim>
            </h2>
            </f:verbatim>
            <%--
            <div id="description">
               <h:outputFormat value="#{msg.search_detail}" id="msg12">
                  <f:param value="#{NavigationBean.searchContext.text}" id="param2" />
               </h:outputFormat>
               <h:outputText value="#{msg.search_description}" id="msg13" />
            </div>
            --%>

            <a:panel id="additional">
               <%-- Close Search action --%>
               <a:actionLink value="#{msg.close_search}" image="/images/icons/action.gif" style="white-space:nowrap" actionListener="#{BrowseBean.closeSearch}" id="link21" />

               <%-- New Search actions --%>
               <a:actionLink value="#{msg.new_search}" image="/images/icons/search_icon.gif" style="white-space:nowrap" action="advSearch" id="link20" />
            </a:panel>
      
            <a:panel id="actions">
               <%-- More Search actions --%>
               <a:menu id="searchMenu" itemSpacing="4" label="#{msg.more_actions}" styleClass="actions" menuStyleClass="actions-menu" style="white-space:nowrap">
                  <a:booleanEvaluator value="#{NavigationBean.isGuest == false}" id="eval0">
                     <a:actionLink value="#{msg.save_new_search}" image="/images/icons/save_search.gif" action="#{AdvancedSearchDialog.saveNewSearch}"
                        id="link20_1" />
                     <a:booleanEvaluator value="#{AdvancedSearchDialog.allowEdit == true}" id="eval0_1">
                        <a:actionLink value="#{msg.save_edit_search}" image="/images/icons/edit_search.gif" action="#{AdvancedSearchDialog.saveEditSearch}"
                           id="link20_2" />
                     </a:booleanEvaluator>
                  </a:booleanEvaluator>
               </a:menu>
            </a:panel>
         </a:panel>
      </f:subview>

                     
                                 
               <%-- warning message for 'Sites' space --%>
               <h:outputText id="sites-space-warning" rendered="#{BrowseBean.sitesSpace}" value="#{BrowseBean.sitesSpaceWarningHTML}" escape="false" />
               
               <%-- Custom Template View --%>
               <a:panel id="custom-wrapper-panel" rendered="#{NavigationBean.hasCustomView && NavigationBean.searchContext == null}">
                     <a:panel id="custom-panel" border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white" styleClass="mainSubTitle"
                              label="#{msg.custom_view}" progressive="true"
                              expanded='#{BrowseBean.panels["custom-panel"]}' expandedActionListener="#{BrowseBean.expandPanel}">
                        <r:webScript id="webscript" scriptUrl="#{NavigationBean.currentNodeWebscript}" context="#{NavigationBean.currentNode.nodeRef}" rendered="#{NavigationBean.hasWebscriptView}" />
                        <r:template id="template" template="#{NavigationBean.currentNodeTemplate}" model="#{NavigationBean.templateModel}" rendered="#{!NavigationBean.hasWebscriptView && NavigationBean.hasTemplateView}" />
                     </a:panel>
               </a:panel>
               
               <%-- Details - Spaces --%>
                     
                     <a:panel id="spaces-panel" styleClass="panel-100 with-pager"
                              label="#{msg.browse_spaces}" progressive="true" facetsId="spaces-panel-facets"
                              expanded='#{BrowseBean.panels["spaces-panel"]}' expandedActionListener="#{BrowseBean.expandPanel}">
                     
                     <%-- Spaces List --%>
                     <a:richList id="spacesList" binding="#{BrowseBean.spacesRichList}" viewMode="#{BrowseBean.browseViewMode}" pageSize="#{BrowseBean.pageSizeSpaces}"
                           rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" value="#{BrowseBean.nodes}" var="r">
                        
                        <%-- component to display if the list is empty --%>
                        <f:facet name="empty">
                           <%-- TODO: either build complete message in BrowseBean or have no icon... --%>
                           <h:outputFormat id="no-space-items" value="#{msg.no_space_items}" escape="false" rendered="#{NavigationBean.searchContext == null}">
                              <f:param value="#{msg.create_space}" />
                           </h:outputFormat>
                        </f:facet>
                        
                        <%-- Primary column for details view mode --%>
                        <a:column id="col1" primary="true" style="padding:2px;text-align:left" rendered="#{BrowseBean.browseViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink id="col1-sort" label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header"/>
                           </f:facet>
                           <f:facet name="small-icon">
                              <a:actionLink id="col1-act1" value="#{r.name}" image="/images/icons/#{r.smallIcon}.gif" actionListener="#{BrowseBean.clickSpace}" showLink="false">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </f:facet>
                           <a:actionLink id="col1-act2" value="#{r.name}" actionListener="#{BrowseBean.clickSpace}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <%-- Primary column for icons view mode --%>
                        <a:column id="col2" primary="true" style="padding:2px;text-align:left;vertical-align:top;" rendered="#{BrowseBean.browseViewMode == 'icons'}">
                           <f:facet name="large-icon">
                              <a:actionLink id="col2-act1" value="#{r.name}" image="/images/icons/#{r.icon}.gif" padding="10" actionListener="#{BrowseBean.clickSpace}" showLink="false">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </f:facet>
                           <a:actionLink id="col2-act2" value="#{r.name}" actionListener="#{BrowseBean.clickSpace}" styleClass="header">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <%-- Primary column for list view mode --%>
                        <a:column id="col3" primary="true" style="padding:2px;text-align:left" rendered="#{BrowseBean.browseViewMode == 'list'}">
                           <f:facet name="large-icon">
                              <a:actionLink id="col3-act1" value="#{r.name}" image="/images/icons/#{r.icon}.gif" padding="10" actionListener="#{BrowseBean.clickSpace}" showLink="false">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </f:facet>
                           <a:actionLink id="col3-act2" value="#{r.name}" actionListener="#{BrowseBean.clickSpace}" styleClass="title">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <%-- Description column for all view modes --%>
                        <a:column id="col4" style="text-align:left">
                           <f:facet name="header">
                              <a:sortLink id="col4-sort" label="#{msg.description}" value="description" styleClass="header"/>
                           </f:facet>
                           <h:outputText id="col4-txt" value="#{r.description}" />
                        </a:column>
                        
                        <%-- Path column for search mode in details view mode --%>
                        <a:column id="col5" style="text-align:left" rendered="#{NavigationBean.searchContext != null && BrowseBean.browseViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink id="col5-sort" label="#{msg.path}" value="displayPath" styleClass="header"/>
                           </f:facet>
                           <r:nodePath id="col5-path" value="#{r.path}" actionListener="#{BrowseBean.clickSpacePath}" />
                        </a:column>
                        
                        <%-- Created Date column for details view mode --%>
                        <a:column id="col6" style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink id="col6-sort" label="#{msg.created}" value="created" styleClass="header"/>
                           </f:facet>
                           <h:outputText id="col6-txt" value="#{r.created}">
                              <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
                           </h:outputText>
                        </a:column>
                        
                        <%-- Modified Date column for details/icons view modes --%>
                        <a:column id="col7" style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details' || BrowseBean.browseViewMode == 'icons'}">
                           <f:facet name="header">
                              <a:sortLink id="col7-sort" label="#{msg.modified}" value="modified" styleClass="header"/>
                           </f:facet>
                           <h:outputText id="col7-txt" value="#{r.modified}">
                              <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
                           </h:outputText>
                        </a:column>
                        
                        <%-- Node Descendants links for list view mode --%>
                        <a:column id="col8" style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'list'}">
                           <r:nodeDescendants id="col8-kids" value="#{r.nodeRef}" styleClass="header" actionListener="#{BrowseBean.clickDescendantSpace}" />
                        </a:column>
                        
                        <%-- Space Actions column --%>
                        <a:column id="col9" actions="true" styleClass="actions-column">
                           <f:facet name="header">
                              <h:outputText id="col9-txt" value="#{msg.actions}"/>
                           </f:facet>
                           
                           <%-- actions are configured in web-client-config-actions.xml --%>
                           <r:actions id="col9-acts1" value="space_browse" context="#{r}" showLink="false" styleClass="inlineAction" />
                           
                           <%-- More actions menu --%>
                           <a:menu id="spaces-more-menu" itemSpacing="4" styleClass="wrapped" image="/images/icons/arrow-down.png"  tooltip="#{msg.more_actions}" menuStyleClass="dropdown-menu">
                              <r:actions id="col9-acts2" value="space_browse_menu" context="#{r}" />
                           </a:menu>
                        </a:column>

                        <a:panel id="page-controls1" styleClass="page-controls" style="font-size:9px">
                           <h:outputText value="#{msg.items_per_page}" id="items-txt1" />
                           <h:selectOneMenu id="spaces-pages" value="#{BrowseBean.pageSizeSpacesStr}" onchange="return applySizeSpaces(event);">
                              <f:selectItem id="item11" itemLabel="10" itemValue="10" />
                              <f:selectItem id="item12" itemLabel="20" itemValue="20" />
                              <f:selectItem id="item13" itemLabel="50" itemValue="50" />
                              <f:selectItem id="item14" itemLabel="75" itemValue="75" />
                              <f:selectItem id="item15" itemLabel="100" itemValue="100" />
                           </h:selectOneMenu>

                           <a:panel id="update-spaces-size-field" style="display: none;">
                              <a:actionLink id="spaces-apply" value="" actionListener="#{BrowseBean.updateSpacesPageSize}" />
                           </a:panel>
                        </a:panel>

                        <a:dataPager id="pager1" styleClass="pager" />
                     </a:richList>
                              
                  </a:panel>
                     
               <%-- Details - Content --%>

         <a:panel id="content-panel" styleClass="panel-100 with-pager" label="#{msg.browse_content}" progressive="true"
            facetsId="content-panel-facets" expanded='#{BrowseBean.panels["content-panel"]}' expandedActionListener="#{BrowseBean.expandPanel}">

            <%-- Content list --%>
            <a:richList id="contentRichList" binding="#{BrowseBean.contentRichList}" viewMode="#{BrowseBean.browseViewMode}"
               pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
               value="#{BrowseBean.content}" var="r">

               <%-- component to display if the list is empty --%>
               <f:facet name="empty">
                  <%-- TODO: either build complete message in BrowseBean or have no icon... --%>
                  <h:outputFormat id="no-content-items" value="#{msg.no_content_items}" escape="false" rendered="#{NavigationBean.searchContext == null}">
                     <f:param value="#{msg.add_content}" />
                     <f:param value="#{msg.create_content}" />
                  </h:outputFormat>
               </f:facet>

               <%-- Primary column for details view mode --%>
               <a:column id="col10" primary="true" style="padding:2px;text-align:left" rendered="#{BrowseBean.browseViewMode == 'details'}">
                  <f:facet name="header">
                     <a:sortLink id="col10-sort" label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header" />
                  </f:facet>
                  <f:facet name="small-icon">
                     <a:actionLink id="col10-act1" value="#{r.name}" href="#{r.url}" target="new" image="#{r.fileType16}" showLink="false"
                        styleClass="inlineAction" />
                  </f:facet>
                  <a:actionLink id="col10-act2" value="#{r.name}" href="#{r.url}" target="new" />
                  <r:lockIcon id="col10-lock" value="#{r.nodeRef}" align="absmiddle" />
                  <h:outputLabel id="col10-lang" value="#{r.lang}" styleClass="langCode" rendered="#{r.lang != null}" />
               </a:column>

               <%-- Primary column for icons view mode --%>
               <a:column id="col11" primary="true" style="padding:2px;text-align:left;vertical-align:top;" rendered="#{BrowseBean.browseViewMode == 'icons'}">
                  <f:facet name="large-icon">
                     <a:actionLink id="col11-act1" padding="10" value="#{r.name}" href="#{r.url}" target="new" image="#{r.fileType32}" showLink="false"
                        styleClass="inlineAction" />
                  </f:facet>
                  <a:actionLink id="col11-act2" value="#{r.name}" href="#{r.url}" target="new" styleClass="header" />
                  <r:lockIcon id="col11-lock" value="#{r.nodeRef}" align="absmiddle" />
                  <h:outputLabel id="col11-lang" value="#{r.lang}" styleClass="langCode" rendered="#{r.lang != null}" />
               </a:column>

               <%-- Primary column for list view mode --%>
               <a:column id="col12" primary="true" style="padding:2px;text-align:left" rendered="#{BrowseBean.browseViewMode == 'list'}">
                  <f:facet name="large-icon">
                     <a:actionLink id="col12-act1" value="#{r.name}" href="#{r.url}" target="new" image="#{r.fileType32}" padding="10" showLink="false"
                        styleClass="inlineAction" />
                  </f:facet>
                  <a:actionLink id="col12-act2" value="#{r.name}" href="#{r.url}" target="new" styleClass="title" />
                  <r:lockIcon id="col12-lock" value="#{r.nodeRef}" align="absmiddle" />
                  <h:outputLabel id="col12-lang" value="#{r.lang}" styleClass="langCode" rendered="#{r.lang != null}" />
               </a:column>

               <%-- Description column for all view modes --%>
               <a:column id="col13" style="text-align:left">
                  <f:facet name="header">
                     <a:sortLink id="col13-sort" label="#{msg.description}" value="description" styleClass="header" />
                  </f:facet>
                  <h:outputText id="col13-txt" value="#{r.description}" />
               </a:column>

               <%-- Path column for search mode in details view mode --%>
               <a:column id="col14" style="text-align:left" rendered="#{NavigationBean.searchContext != null && BrowseBean.browseViewMode == 'details'}">
                  <f:facet name="header">
                     <a:sortLink id="col14-sort" label="#{msg.path}" value="displayPath" styleClass="header" />
                  </f:facet>
                  <r:nodePath id="col14-path" value="#{r.path}" actionListener="#{BrowseBean.clickSpacePath}" />
               </a:column>

               <%-- Size for details/icons view modes --%>
               <a:column id="col15" style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details' || BrowseBean.browseViewMode == 'icons'}">
                  <f:facet name="header">
                     <a:sortLink id="col15-sort" label="#{msg.size}" value="size" styleClass="header" />
                  </f:facet>
                  <h:outputText id="col15-txt" value="#{r.size}">
                     <a:convertSize />
                  </h:outputText>
               </a:column>

               <%-- Created Date column for details view mode --%>
               <a:column id="col16" style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details'}">
                  <f:facet name="header">
                     <a:sortLink id="col16-sort" label="#{msg.created}" value="created" styleClass="header" />
                  </f:facet>
                  <h:outputText id="col16-txt" value="#{r.created}">
                     <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
                  </h:outputText>
               </a:column>

               <%-- Modified Date column for details/icons view modes --%>
               <a:column id="col17" style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details' || BrowseBean.browseViewMode == 'icons'}">
                  <f:facet name="header">
                     <a:sortLink id="col17-sort" label="#{msg.modified}" value="modified" styleClass="header" />
                  </f:facet>
                  <h:outputText id="col17-txt" value="#{r.modified}">
                     <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
                  </h:outputText>
               </a:column>

               <%-- Content Actions column --%>
               <a:column id="col18" actions="true" styleClass="actions-column">
                  <f:facet name="header">
                     <h:outputText id="col18-txt" value="#{msg.actions}" />
                  </f:facet>

                  <%-- actions are configured in web-client-config-actions.xml --%>
                  <r:actions id="col18-acts1" value="document_browse" context="#{r}" showLink="false" styleClass="inlineAction" />

                  <%-- More actions menu --%>
                  <a:menu id="content-more-menu" itemSpacing="4" image="/images/icons/arrow-down.png" tooltip="#{msg.more_actions}" styleClass="wrapped"
                     menuStyleClass="dropdown-menu">
                     <r:actions id="col18-acts2" value="document_browse_menu" context="#{r}" />
                  </a:menu>
               </a:column>

               <a:panel id="page-controls2" styleClass="page-controls" style="font-size:9px">
                  <h:outputText value="#{msg.items_per_page}" id="items-txt2" />

                  <h:selectOneMenu id="content-pages" value="#{BrowseBean.pageSizeContentStr}" onchange="return applySizeContent(event);">
                     <f:selectItem id="item21" itemLabel="10" itemValue="10" />
                     <f:selectItem id="item22" itemLabel="20" itemValue="20" />
                     <f:selectItem id="item23" itemLabel="50" itemValue="50" />
                     <f:selectItem id="item24" itemLabel="75" itemValue="75" />
                     <f:selectItem id="item25" itemLabel="100" itemValue="100" />
                  </h:selectOneMenu>

                  <a:panel id="update-content-size-field" style="display: none;">
                     <a:actionLink id="content-apply" value="" actionListener="#{BrowseBean.updateContentPageSize}" />
                  </a:panel>
               </a:panel>
               <a:dataPager id="pager2" styleClass="pager" />
            </a:richList>
         </a:panel>

         <%-- Error Messages --%>
         <%-- messages tag to show messages not handled by other specific message tags --%> 
         <a:errors message="" styleClass="message" errorClass="error-message" infoClass="info-message" />
       </a:panel>
    <f:verbatim><div class="clear"></div></f:verbatim>
    </a:panel>
    <%@ include file="../parts/footer.jsp"%>
    </h:form>
    
</f:view>

</r:page>
