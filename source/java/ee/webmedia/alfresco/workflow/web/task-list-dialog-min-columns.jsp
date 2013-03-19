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
         <a:actionLink id="dueDate-text" value="#{r.task.dueDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.task.dueDateStr}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="dueDate-text1" value="#{r.task.dueDateStr}" action="dialog:compoundWorkflowDialog" tooltip="#{r.task.dueDateStr}" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow}" >
	 	 	<f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
 	     </a:actionLink>            
         <a:actionLink id="dueDate-text2" value="#{r.task.dueDateStr}" tooltip="#{r.task.dueDateStr}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.caseFileWorkflow}">
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- resolution --%>
      <a:column id="resolution" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 35%;">
         <f:facet name="header">
            <a:sortLink id="resolution-sort" label="#{msg.task_property_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <a:actionLink id="resolution-text" value="#{r.task.node.properties['{temp}resolution']}" action="#{DocumentDialog.action}" tooltip="#{r.task.node.properties['{temp}resolution']}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="resolution-text1" value="#{r.task.node.properties['{temp}resolution']}" action="dialog:compoundWorkflowDialog" tooltip="#{r.task.node.properties['{temp}resolution']}" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow}">
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

      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;" rendered="#{MyTasksBean.documentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.document.sender}" action="#{DocumentDialog.action}" tooltip="#{r.document.sender}" actionListener="#{DocumentDialog.open}" styleClass="no-underline condence20-" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col4-text2" value="#{r.sender}" tooltip="#{r.sender}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="no-underline condence20-" rendered="#{r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.task.cssStyleClass}" style="width: 25%;">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text" value="#{r.document.docName}" action="#{DocumentDialog.action}" tooltip="#{r.document.docName}" actionListener="#{DocumentDialog.open}" styleClass="condence20-" rendered="#{r.compoundWorkflow.documentWorkflow}" >
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

      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.task.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_type}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text" value="#{r.document.documentTypeName}" action="#{DocumentDialog.action}" tooltip="#{r.document.documentTypeName}" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col3-text1" value="#{r.documentTypeName}" action="dialog:compoundWorkflowDialog" tooltip="#{r.documentTypeName}" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>          
         <a:actionLink id="col3-text2" value="#{r.documentTypeName}" tooltip="#{r.documentTypeName}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.node.nodeRef}" />
         </a:actionLink>
      </a:column>
