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
         <a:actionLink id="dueDate-text" value="#{r.task.dueDateStr}" action="dialog:document" tooltip="#{r.task.dueDateStr}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- resolution --%>
      <a:column id="resolution" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="resolution-sort" label="#{msg.task_property_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <a:actionLink id="resolution-text" value="#{r.task.node.properties['{temp}resolution']}" action="dialog:document" tooltip="#{r.task.node.properties['{temp}resolution']}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- creatorName --%>
      <a:column id="creatorName" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="creatorName-sort" label="#{msg.task_property_creator_name}" value="creatorName" styleClass="header" />
         </f:facet>
         <a:actionLink id="creatorName-text" value="#{r.task.creatorName}" action="dialog:document" tooltip="#{r.task.creatorName}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.document.shortSender}" action="dialog:document" tooltip="#{r.document.sender}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text" value="#{r.document.shortDocName}" action="dialog:document" tooltip="#{r.document.docName}" actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.task.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text" value="#{r.document.documentTypeName}" action="dialog:document" tooltip="#{r.document.documentTypeName}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
