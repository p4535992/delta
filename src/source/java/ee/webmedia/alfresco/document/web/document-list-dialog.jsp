<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="document-panel" border="white" bgcolor="white" styleClass="panel-100" label="#{DocumentListDialog.listTitle}" progressive="true"
   facetsId="document-panel-facets">

   <%-- Main List --%>
   <a:richList id="documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" styleClass="recordSet" headerStyleClass="recordSetHeader"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" value="#{DocumentListDialog.entries}" var="r">

      <%-- regNumber --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regNumber}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.node.properties['{http://alfresco.webmedia.ee/model/document/common/1.0}regNumber']}" />
      </a:column>
      
      <%-- Registration date --%>
      <a:column id="col2" primary="true">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regDateTime}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.node.properties['{http://alfresco.webmedia.ee/model/document/common/1.0}regDateTime']}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- Document type --%>
      <a:column id="col3" primary="true">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_docType}" value="docType" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.docType}" />
      </a:column>
      
      <%-- Sender/receiver --%>
      <a:column id="col4" primary="true">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="docType" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.sender}" />
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text" value="#{r.docName}" action="dialog:document" tooltip="#{msg.document_details_info}"
            showLink="false" actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- iconLink to document details --%>
      <a:column id="col5" actions="true" styleClass="actions-column" rendered="#{UserService.documentManager}" >
         <a:actionLink id="col5-act1" value="#{r.docName}" image="/images/icons/edit_properties.gif" action="dialog:document" showLink="false"
            actionListener="#{DocumentDialog.open}" tooltip="#{msg.document_details_info}">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
