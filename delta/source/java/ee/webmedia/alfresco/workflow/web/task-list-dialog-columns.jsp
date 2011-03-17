<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
      
      <%-- dueDate --%>
      <a:column id="dueDate" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="dueDate-sort" label="#{msg.task_property_dueDate}" value="taskDueDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="dueDate-text" value="#{r.task.dueDateStr}" action="dialog:document" tooltip="#{r.task.dueDateStr}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- resolution --%>
      <a:column id="resolution" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 15%;">
         <f:facet name="header">
            <a:sortLink id="resolution-sort" label="#{msg.task_property_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <a:actionLink id="resolution-text" value="#{r.task.node.properties['{temp}resolution']}" action="dialog:document" tooltip="#{r.task.node.properties['{temp}resolution']}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- creatorName --%>
      <a:column id="creatorName" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="creatorName-sort" label="#{msg.task_property_creator_name}" value="creatorName" styleClass="header" />
         </f:facet>
         <a:actionLink id="creatorName-text" value="#{r.task.creatorName}" action="dialog:document" tooltip="#{r.task.creatorName}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- regNumber --%>
      <a:column id="col1" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-text" value="#{r.document.regNumber}" action="dialog:document" tooltip="#{r.document.regNumber}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Registration date --%>
      <a:column id="col2" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text" value="#{r.document.regDateTimeStr}" action="dialog:document" tooltip="#{r.document.regDateTimeStr}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.document.sender}" action="dialog:document" tooltip="#{r.document.sender}" actionListener="#{DocumentDialog.open}" styleClass="no-underline condence20-" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.task.cssStyleClass}" style="width: 15%;">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text" value="#{r.document.docName}" action="dialog:document" tooltip="#{r.document.docName}"
            showLink="false" actionListener="#{DocumentDialog.open}" styleClass="condence20- tooltip}">
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- DueDate --%>
      <a:column id="col7" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_dueDate}" value="documentDueDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col7-text" value="#{r.document.dueDateStr}" action="dialog:document" tooltip="#{r.document.dueDateStr}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text" value="#{r.document.documentTypeName}" action="dialog:document" tooltip="#{r.document.documentTypeName}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
     </a:column>
