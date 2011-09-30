<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:booleanEvaluator value="#{DocTypeDetailsDialog.showingLatestVersion}">
   <a:panel id="docTypeVersions-panel" label="#{msg.doc_type_details_panel_versions}" styleClass="panel-100 with-pager" progressive="true" >
      <a:richList id="docTypeVersionsList" value="#{VersionsListBean.savedVersionsList}" var="r" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
         rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%">

         <a:column id="versionNr" >
            <f:facet name="header">
               <a:outputText value="#{msg.docType_versionsList_versionNr}" />
            </f:facet>
            <h:outputText value="#{r.versionNr}"/>
         </a:column>

         <a:column id="creatorNameAndId" >
            <f:facet name="header">
               <a:outputText value="#{msg.docType_versionsList_creatorNameAndId}" />
            </f:facet>
            <h:outputText value="#{r.creatorNameAndId}"/>
         </a:column>
         <a:column id="createdDateTime" >
            <f:facet name="header">
               <a:outputText value="#{msg.docType_versionsList_createdDateTime}" />
            </f:facet>
            <h:outputText value="#{r.createdDateTime}">
               <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
            </h:outputText>
         </a:column>
   
         <a:column id="actionsCol">
            <a:actionLink value="" actionListener="#{VersionsListBean.viewDocTypeVersion}" showLink="false" image="/images/icons/view_properties.gif" tooltip="#{msg.docType_versionsList_action_viewVersion}" >
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </a:column>
   
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
         <a:dataPager id="pagerMetadata" styleClass="pager" />
      </a:richList>
   </a:panel>
</a:booleanEvaluator>
