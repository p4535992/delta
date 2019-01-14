<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text-1" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
            actionListener="#{DocumentDialog.open}" rendered="#{r.cssStyleClass ne 'case'}" styleClass="tooltip condence100- no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <!-- if row item is not document, but case, create link to documents list of case (row item is subclass of Document, to be shown as listItem in document list) -->
         <a:actionLink id="col6-link2docList-1" value="#{r.docName}" action="dialog:documentListDialog" tooltip="#{r.docName}"
            actionListener="#{DocumentListDialog.setup}" rendered="#{r.cssStyleClass == 'case'}" styleClass="condence20-" >
            <f:param name="caseNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Document type --%>
      <a:column id="col4" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text-1" value="#{r.documentTypeName}" action="#{DocumentDialog.action}" tooltip="#{r.documentTypeName}"
            actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Volume --%>
      <a:column id="col3_2" primary="true" styleClass="#{r.cssStyleClass}" rendered="#{applicationConstantsBean.volumeColumnEnabled}">
         <f:facet name="header">
            <a:sortLink id="col3_2-sort" label="#{msg.volume}" value="volumeLabel" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3_2-link2cases" value="#{r.documentVolume.volumeLabel}" action="dialog:caseDocListDialog" tooltip="#{r.documentVolume.volumeLabel}"
            showLink="false" actionListener="#{CaseDocumentListDialog.showAll}" rendered="#{r.documentVolume != null && !r.documentVolume.dynamic}" styleClass="tooltip condence20- no-underline" >
            <f:param name="volumeNodeRef" value="#{r.documentVolume.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col3_2-caseFile" value="#{r.documentVolume.volumeLabel}" tooltip="#{r.documentVolume.volumeLabel}"
            showLink="false" actionListener="#{CaseFileDialog.openFromDocumentList}" rendered="#{r.documentVolume != null && r.documentVolume.dynamic}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.documentVolume.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Sender/Recipient --%>
      <a:column id="col5" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.document_sender_recipient}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-text-1" value="#{r.senderOrRecipient}" action="#{DocumentDialog.action}" tooltip="#{r.senderOrRecipient}"
            actionListener="#{DocumentDialog.open}" styleClass="condence20- tooltip no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Workflow status --%>
      <a:column id="col7" styleClass="#{r.cssStyleClass}" rendered="#{workflowConstantsBean.documentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_workflow_status}" value="workflowStatus" styleClass="header" />
         </f:facet>
         <a:actionLink id="col7-text-1" value="#{r.workflowStatus}" action="#{DocumentDialog.action}" tooltip="#{r.workflowStatus}"
            actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Files --%>
      <a:column id="col10" styleClass="doc-list-actions">
         <f:facet name="header">
         
            <h:outputText id="col10-header" value="#{msg.document_allFiles}" styleClass="header" />
         </f:facet>
          <f:facet name="csvExport">
              <a:param value="false"/>
          </f:facet>

		<wm:customChildrenContainer id="document-list-files" childGenerator="#{DocumentListDialog.documentRowFileGenerator}" parameterList="#{r}"/>
        
      </a:column>