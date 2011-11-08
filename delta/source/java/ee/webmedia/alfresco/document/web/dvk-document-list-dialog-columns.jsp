<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
      
      <%-- Received DateTime --%>
      <a:column id="col1" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_receivedDateTime}" value="created" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.created}" >
            <a:convertXMLDate pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- Sender registration number --%>
      <a:column id="col2" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_senderRegNumber}" value="senderRegNumber" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text-1" value="#{r.senderRegNumber}" />
      </a:column>
      
      <%-- Sender registration number --%>
      <a:column id="col3" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_senderRegDate}" value="senderRegDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text-1" value="#{r.senderRegDate}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Document type --%>
      <a:column id="col4" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.documentTypeName}" />
      </a:column>
      
      <%-- Sender registration number --%>
      <a:column id="col5" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         
         <h:outputText id="col5-text-1" value="#{r.sender}" rendered="#{r.cssStyleClass eq 'incomingLetter'}" />
         <h:outputText id="col5-text-2" value="#{r.ownerName}" rendered="#{r.cssStyleClass ne 'incomingLetter'}" />
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
         <r:permissionEvaluator value="#{r.files[0].node}" allow="viewDocumentFiles">
            <a:actionLink id="col10-act1" value="#{r.files[0].name}" href="#{r.files[0].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[0].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[0].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[0].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[0].name}"
               title="#{r.files[0].name}" rendered="#{r.files[0] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[1].node}" allow="viewDocumentFiles">
            <a:actionLink id="col10-act2" value="#{r.files[1].name}" href="#{r.files[1].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[1].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[1].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[1].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[1].name}"
               title="#{r.files[1].name}" rendered="#{r.files[1] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[2].node}" allow="viewDocumentFiles">
            <a:actionLink id="col10-act3" value="#{r.files[2].name}" href="#{r.files[2].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[2].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[2].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[2].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[2].name}"
               title="#{r.files[2].name}" rendered="#{r.files[2] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[3].node}" allow="viewDocumentFiles">
            <a:actionLink id="col10-act4" value="#{r.files[3].name}" href="#{r.files[3].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[3].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[3].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[3].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[3].name}"
               title="#{r.files[3].name}" rendered="#{r.files[3] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[4].node}" allow="viewDocumentFiles">
            <a:actionLink id="col10-act5" value="#{r.files[4].name}" href="#{r.files[4].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[4].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[4].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[4].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[4].name}"
               title="#{r.files[4].name}" rendered="#{r.files[4] != null}" />
         </r:permissionEvaluator>
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
      
      
      
