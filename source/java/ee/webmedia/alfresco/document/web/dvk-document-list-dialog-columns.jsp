<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
      
      <%-- Received DateTime --%>
      <a:column id="col1" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_receivedDateTime}" value="properties;cm:created" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-text" value="#{r.createdDateTimeStr}" action="#{DocumentDialog.action}" tooltip="#{r.createdDateTimeStr}" styleClass="no-underline"
          actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
      </a:column>
      
      <%-- Sender registration number --%>
      <a:column id="col2" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_senderRegNumber}" value="senderRegNumber" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text-1" value="#{r.senderRegNumber}" action="#{DocumentDialog.action}" tooltip="#{r.senderRegNumber}"
            actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Sender registration number --%>
      <a:column id="col3" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_senderRegDate}" value="senderRegDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text-1" value="#{r.senderRegDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.senderRegDateStr}" styleClass="no-underline"
          actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
      </a:column>

      <%-- Document type --%>
      <a:column id="col4" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.documentTypeName}" action="#{DocumentDialog.action}" tooltip="#{r.documentTypeName}"
            actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Sender registration number --%>
      <a:column id="col5" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-text-1" value="#{r.sender}" action="#{DocumentDialog.action}" tooltip="#{r.sender}"
            actionListener="#{DocumentDialog.open}" rendered="#{r.cssStyleClass eq 'incomingLetter'}">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col5-text-2" value="#{r.ownerName}" action="#{DocumentDialog.action}" tooltip="#{r.ownerName}"
            actionListener="#{DocumentDialog.open}" rendered="#{r.cssStyleClass ne 'incomingLetter'}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      
      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text-1" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
            showLink="false" actionListener="#{DocumentDialog.open}" rendered="#{r.cssStyleClass ne 'case'}" styleClass="condence20- tooltip">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <!-- if row item is not document, but case, create link to documents list of case (row item is subclass of Document, to be shown as listItem in document list) -->
         <a:actionLink id="col6-link2docList-1" value="#{r.docName}" action="dialog:documentListDialog" tooltip="#{r.docName}"
            showLink="false" actionListener="#{DocumentListDialog.setup}" rendered="#{r.cssStyleClass == 'case'}" styleClass="condence20- tooltip">
            <f:param name="caseNodeRef" value="#{r.node.nodeRef}" />
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
		  <wm:customChildrenContainer id="document-imap-list-files" childGenerator="#{DocumentListDialog.documentRowFileGenerator}" parameterList="#{r}"/>
      </a:column>
      
      <%-- Actions column --%>
      <a:column id="act-col" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="act-col-txt" value="" />
         </f:facet>
         <a:actionLink id="act-col-act1" value="#{msg.delete}" actionListener="#{DeleteDialog.setupDeleteDialog}" action="dialog:deleteDialog" showLink="false"
            image="/images/icons/delete.gif">
            <f:param name="nodeRef" value="#{r.node.nodeRef}"/>
            <f:param name="confirmMessagePlaceholder0" value="#{r.docName}"/>
         </a:actionLink>
      </a:column>
      
      
      
