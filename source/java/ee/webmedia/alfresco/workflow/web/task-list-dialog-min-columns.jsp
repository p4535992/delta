<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
      
      <%-- dueDate --%>
      <a:column id="dueDate" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" >
         <f:facet name="header">
            <a:sortLink id="dueDate-sort" label="#{msg.task_property_dueDate}" value="dueDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="dueDate-text" value="#{r.task.dueDateTimeStr}" action="#{r.action}" tooltip="#{r.task.dueDateTimeStr}" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- resolution --%>
      <a:column id="resolution" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 35%;">
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

      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.documentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.document.sender}" action="#{r.action}" tooltip="#{r.document.sender}" actionListener="#{r.actionListener}" styleClass="no-underline condence20-" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.actionNodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.task.cssStyleClass}" style="width: 25%;">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text" value="#{r.title}" action="#{r.action}" tooltip="#{r.title}" actionListener="#{r.actionListener}" styleClass="condence20-" >
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
