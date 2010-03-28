<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
      
      <%-- dueDate --%>
      <a:column id="dueDate" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="dueDate-sort" label="#{msg.task_property_dueDate}" value="dueDate" styleClass="header" />
         </f:facet>
         <h:outputText id="dueDate-text" value="#{r.task.dueDate}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- resolution --%>
      <a:column id="resolution" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="resolution-sort" label="#{msg.task_property_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <h:outputText id="resolution-text" value="#{r.task.node.properties['{temp}resolution']}" />
      </a:column>
      
      <%-- creatorName --%>
      <a:column id="creatorName" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="creatorName-sort" label="#{msg.task_property_creator_name}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="creatorName-text" value="#{r.task.creatorName}" />
      </a:column>
      
      <%-- regNumber --%>
      <a:column id="col1" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.document.regNumber}" />
      </a:column>
      
      <%-- Registration date --%>
      <a:column id="col2" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.document.regDateTime}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.document.sender}" />
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text" value="#{r.document.docName}" action="dialog:document" tooltip="#{msg.document_details_info}"
            showLink="false" actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- DueDate --%>
      <a:column id="col7" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_dueDate}" value="dueDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.document.dueDate}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.document.documentTypeName}" />
      </a:column>
