<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
      
      <%-- dueDate --%>
      <a:column id="dueDate" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" >
         <f:facet name="header">
            <a:sortLink id="dueDate-sort" label="#{msg.task_property_dueDate}" value="taskDueDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="dueDate-text" value="#{r.task.dueDateTimeStr}" action="#{r.action}" tooltip="#{r.task.dueDateTimeStr}" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- resolution --%>
      <a:column id="resolution" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 15%;">
         <f:facet name="header">
            <a:sortLink id="resolution-sort" label="#{msg.task_property_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <a:actionLink id="resolution-text" value="#{r.task.node.properties['{temp}resolution']}" action="#{r.action}" tooltip="#{r.task.node.properties['{temp}resolution']}" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- creatorName --%>
      <a:column id="creatorName" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="creatorName-sort" label="#{msg.task_property_creator_name}" value="creatorName" styleClass="header" />
         </f:facet>
         <a:actionLink id="creatorName-text" value="#{r.task.creatorName}" action="#{r.action}" tooltip="#{r.task.creatorName}" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- regNumber --%>
      <a:column id="col1" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.caseFileOrDocumentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-text" value="#{r.regNrOrVolumeMark}" action="#{r.action}" tooltip="#{r.regNrOrVolumeMark}" actionListener="#{r.actionListener}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow or r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Registration date --%>
      <a:column id="col2" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.documentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text" value="#{r.regDateTimeStr}" action="#{DocumentDialog.action}" tooltip="#{r.regDateTimeStr}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.documentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.sender}" action="#{DocumentDialog.action}" tooltip="#{r.sender}" actionListener="#{DocumentDialog.open}" styleClass="no-underline condence20-" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.task.cssStyleClass}" style="width: 15%;">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text" value="#{r.title}" action="#{r.action}" tooltip="#{r.title}" actionListener="#{r.actionListener}" styleClass="condence20-" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- DueDate --%>
      <a:column id="col7" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.caseFileOrDocumentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_dueDate}" value="documentDueDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col7-text" value="#{r.dueDateStr}" action="#{r.action}" tooltip="#{r.dueDateStr}" actionListener="#{r.actionListener}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_type}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text" value="#{r.documentTypeName}" action="#{r.action}" tooltip="#{r.documentTypeName}" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
     </a:column>
