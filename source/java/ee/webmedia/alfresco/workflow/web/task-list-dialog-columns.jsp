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
         <a:actionLink id="dueDate-text" value="#{r.task.dueDateTimeStr}" action="#{DocumentDialog.action}" tooltip="#{r.task.dueDateTimeStr}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}">
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="dueDate-text1" value="#{r.task.dueDateTimeStr}" action="dialog:compoundWorkflowDialog" tooltip="#{r.task.dueDateTimeStr}" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>             
         <a:actionLink id="dueDate-text2" value="#{r.task.dueDateTimeStr}" tooltip="#{r.task.dueDateTimeStr}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.caseFileWorkflow}">
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <%-- resolution --%>
      <a:column id="resolution" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 15%;">
         <f:facet name="header">
            <a:sortLink id="resolution-sort" label="#{msg.task_property_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <a:actionLink id="resolution-text" value="#{r.task.node.properties['{temp}resolution']}" action="#{DocumentDialog.action}" tooltip="#{r.task.node.properties['{temp}resolution']}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="resolution-text1" value="#{r.task.node.properties['{temp}resolution']}" action="dialog:compoundWorkflowDialog" tooltip="#{r.task.node.properties['{temp}resolution']}" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink> 
         <a:actionLink id="resolution-text2" value="#{r.task.node.properties['{temp}resolution']}" tooltip="#{r.task.node.properties['{temp}resolution']}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="tooltip condence20- no-underline" rendered="#{r.compoundWorkflow.caseFileWorkflow}">
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <%-- creatorName --%>
      <a:column id="creatorName" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="creatorName-sort" label="#{msg.task_property_creator_name}" value="creatorName" styleClass="header" />
         </f:facet>
         <a:actionLink id="creatorName-text" value="#{r.task.creatorName}" action="#{DocumentDialog.action}" tooltip="#{r.task.creatorName}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="creatorName-text1" value="#{r.task.creatorName}" action="dialog:compoundWorkflowDialog" tooltip="#{r.task.creatorName}" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="creatorName-text2" value="#{r.task.creatorName}" tooltip="#{r.task.creatorName}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- regNumber --%>
      <a:column id="col1" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.caseFileOrDocumentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-text" value="#{r.regNumber}" action="#{DocumentDialog.action}" tooltip="#{r.regNumber}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col1-text2" value="#{r.volumeMark}" tooltip="#{r.volumeMark}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <%-- Registration date --%>
      <a:column id="col2" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.documentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text" value="#{r.regDateTimeStr}" action="#{DocumentDialog.action}" tooltip="#{r.regDateTimeStr}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.documentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.sender}" action="#{DocumentDialog.action}" tooltip="#{r.sender}" actionListener="#{DocumentDialog.open}" styleClass="no-underline condence20-" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.task.cssStyleClass}" style="width: 15%;">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" 
            showLink="false" actionListener="#{DocumentDialog.open}" styleClass="condence20- tooltip}" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col6-text1" value="#{r.docName}" action="dialog:compoundWorkflowDialog" tooltip="#{r.docName}"
            showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="condence20- tooltip}" rendered="#{r.compoundWorkflow.independentWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col6-text2" value="#{r.title}" tooltip="#{r.title}" 
            showLink="false" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="condence20- tooltip}" rendered="#{r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>         
      </a:column>

      <%-- DueDate --%>
      <a:column id="col7" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.caseFileOrDocumentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_dueDate}" value="documentDueDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col7-text" value="#{r.documentDueDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.documentDueDateStr}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col7-text1" value="#{r.documentDueDateStr}" action="dialog:compoundWorkflowDialog" tooltip="#{r.documentDueDateStr}" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col7-text2" value="#{r.workflowDueDateStr}" tooltip="#{r.workflowDueDateStr}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>         
      </a:column>

      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_type}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text" value="#{r.documentTypeName}" action="#{DocumentDialog.action}" tooltip="#{r.documentTypeName}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col3-text1" value="#{r.documentTypeName}" action="dialog:compoundWorkflowDialog" tooltip="#{r.documentTypeName}" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col3-text2" value="#{r.documentTypeName}" tooltip="#{r.documentTypeName}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>         
     </a:column>
