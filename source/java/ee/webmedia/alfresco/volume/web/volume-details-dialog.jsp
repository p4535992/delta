<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<%@page import="ee.webmedia.alfresco.volume.model.VolumeModel"%>
<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="ee.webmedia.alfresco.volume.web.VolumeDetailsDialog"%>
<a:panel id="metadata-panel" label="#{msg.document_metadata}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="volume-metatada" value="#{VolumeDetailsDialog.currentNode}" binding="#{VolumeDetailsDialog.propertySheet}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel"/>
</a:panel>

<<<<<<< HEAD
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/associations/web/assocs-block.jsp" />
<a:booleanEvaluator id="searchBlockEvaluator" value="#{VolumeDetailsDialog.showSearchBlock}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-search-block.jsp" />
</a:booleanEvaluator>

<a:panel id="deleted-documents-panel" label="#{msg.volume_deletedDocuments}" styleClass="panel-100" progressive="true" expanded="false">
=======
<a:panel id="deleted-documents-panel" label="#{msg.volume_deletedDocuments}" styleClass="panel-100" progressive="true">
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
   <%-- Main List --%>
   <a:richList id="deletedDocumentsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{VolumeDetailsDialog.deletedDocuments}" var="r">

      <%-- deleteDateTime --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.volume_deletedDocument_deletedDateTime}" value="deletedDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.deletedDateTime}" >
            <a:convertXMLDate pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- actor --%>
      <a:column id="col2" primary="true">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.volume_deletedDocument_actor}" value="actor" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.actor}" />
      </a:column>
      
      <%-- documentData --%>
      <a:column id="col3" primary="true">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.volume_deletedDocument_documentData}" value="documentData" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.documentData}" />
      </a:column>
      
      <%-- comment --%>
      <a:column id="col4" primary="true">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.volume_deletedDocument_comment}" value="comment" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
         <h:outputText id="col4-text" value="#{r.comment}" />
      </a:column>
      
      <%-- comment --%>
      <a:column id="col5" primary="true">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.volume_deletedDocument_deletionType}" value="deletionType" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.deletionType}" />
=======
         <h:outputText id="col2-text" value="#{r.comment}" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
      </a:column>
      
   </a:richList>
</a:panel>

<%
   final boolean isNew = BeanHelper.getVolumeDetailsDialog().isNew();
   if(isNew) {
%>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-close-button.jsp" />
<%
   }
%>