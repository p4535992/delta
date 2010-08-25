<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
      
      <%-- regNumber --%>
      <a:column id="col1" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.regNumber}" />
      </a:column>
      
      <%-- Registration date --%>
      <a:column id="col2" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.regDateTime}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.documentTypeName}" />
      </a:column>
      
      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text-1" value="#{r.sender}" title="#{r.sender}" styleClass="tooltip condence20-" />
      </a:column>
      
      <%-- All Recipients --%>
      <a:column id="col5" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.document_allRecipients}" value="allRecipients" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text-1" value="#{r.allRecipients}" title="#{r.allRecipients}" styleClass="tooltip condence20-" />
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text-1" value="#{r.docName}" action="dialog:document" tooltip="#{r.docName}"
            actionListener="#{DocumentDialog.open}" rendered="#{r.cssStyleClass ne 'case'}" styleClass="tooltip condence20-" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <!-- if row item is not document, but case, create link to documents list of case (row item is subclass of Document, to be shown as listItem in document list) -->
         <a:actionLink id="col6-link2docList-1" value="#{r.docName}" action="dialog:documentListDialog" tooltip="#{r.docName}"
            actionListener="#{DocumentListDialog.setup}" rendered="#{r.cssStyleClass == 'case'}" styleClass="condence20-" >
            <f:param name="caseNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- DueDate --%>
      <a:column id="col7" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_dueDate}" value="dueDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.dueDate}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Complience Date --%>
      <a:column id="col9" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col9-sort" label="#{msg.document_complienceDate}" value="complienceDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col9-text" value="#{r.complienceDate}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Files --%>
      <a:column id="col10" primary="true" styleClass="doc-list-actions">
         <f:facet name="header">
            <h:outputText id="col10-header" value="#{msg.document_allFiles}" styleClass="header" />
         </f:facet>
          <f:facet name="csvExport">
              <a:param value="false"/>
          </f:facet>
         <r:permissionEvaluator value="#{r.files[0].node}" allow="ReadContent">
            <a:actionLink id="col10-act1" value="#{r.files[0].name}" href="#{r.files[0].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[0].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[0].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[0].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[0].name}"
               title="#{r.files[0].name}" rendered="#{r.files[0] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[1].node}" allow="ReadContent">
            <a:actionLink id="col10-act2" value="#{r.files[1].name}" href="#{r.files[1].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[1].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[1].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[1].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[1].name}"
               title="#{r.files[1].name}" rendered="#{r.files[1] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[2].node}" allow="ReadContent">
            <a:actionLink id="col10-act3" value="#{r.files[2].name}" href="#{r.files[2].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[2].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[2].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[2].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[2].name}"
               title="#{r.files[2].name}" rendered="#{r.files[2] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[3].node}" allow="ReadContent">
            <a:actionLink id="col10-act4" value="#{r.files[3].name}" href="#{r.files[3].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[3].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[3].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[3].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[3].name}"
               title="#{r.files[3].name}" rendered="#{r.files[3] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[4].node}" allow="ReadContent">
            <a:actionLink id="col10-act5" value="#{r.files[4].name}" href="#{r.files[4].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[4].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[4].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[4].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[4].name}"
               title="#{r.files[4].name}" rendered="#{r.files[4] != null}" />
         </r:permissionEvaluator>
      </a:column>
