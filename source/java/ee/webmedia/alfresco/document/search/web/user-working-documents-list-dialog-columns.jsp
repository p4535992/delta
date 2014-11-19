<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<<<<<<< HEAD

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text-1" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
            actionListener="#{DocumentDialog.open}" rendered="#{r.cssStyleClass ne 'case'}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <!-- if row item is not document, but case, create link to documents list of case (row item is subclass of Document, to be shown as listItem in document list) -->
         <a:actionLink id="col6-link2docList-1" value="#{r.docName}" action="dialog:documentListDialog" tooltip="#{r.docName}"
            actionListener="#{DocumentListDialog.setup}" rendered="#{r.cssStyleClass == 'case'}" styleClass="condence20-" >
            <f:param name="caseNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
      
      <%-- Document type --%>
      <a:column id="col4" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
         <a:actionLink id="col4-text-1" value="#{r.documentTypeName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
=======
         <a:actionLink id="col4-text-1" value="#{r.documentTypeName}" action="#{DocumentDialog.action}" tooltip="#{r.documentTypeName}"
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
<<<<<<< HEAD
      <%-- Volume --%>
      <a:column id="col3_2" primary="true" styleClass="#{r.cssStyleClass}" rendered="#{SearchBlockBean.documentVolumeColumnVisible}">
         <f:facet name="header">
            <a:sortLink id="col3_2-sort" label="#{msg.volume}" value="volume" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3_2-link2cases" value="#{r.documentVolume.volumeMarkAndTitle}" action="dialog:caseDocListDialog" tooltip="#{r.documentVolume.volumeMarkAndTitle}"
            showLink="false" actionListener="#{CaseDocumentListDialog.showAll}" rendered="#{r.documentVolume != null && !r.documentVolume.dynamic}" styleClass="tooltip condence20- no-underline" >
            <f:param name="volumeNodeRef" value="#{r.documentVolume.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col3_2-caseFile" value="#{r.documentVolume.volumeMarkAndTitle}" tooltip="#{r.documentVolume.volumeMarkAndTitle}"
            showLink="false" actionListener="#{CaseFileDialog.openFromDocumentList}" rendered="#{r.documentVolume != null && r.documentVolume.dynamic}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.documentVolume.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
      <%-- Sender/Recipient --%>
      <a:column id="col5" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.document_sender_recipient}" value="senderOrRecipients" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
         <a:actionLink id="col5-text-1" value="#{r.sender}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
=======
         <a:actionLink id="col5-text-1" value="#{r.senderOrRecipients}" action="#{DocumentDialog.action}" tooltip="#{r.senderOrRecipients}"
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            actionListener="#{DocumentDialog.open}" styleClass="condence20- tooltip no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
<<<<<<< HEAD
      
      <%-- Workflow status --%>
      <a:column id="col7" styleClass="#{r.cssStyleClass}" rendered="#{WmWorkflowService.documentWorkflowEnabled}">
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_workflow_status}" value="workflowStatus" styleClass="header" />
         </f:facet>
         <a:actionLink id="col7-text-1" value="#{r.workflowStatus}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
=======

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text-1" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
            actionListener="#{DocumentDialog.open}" rendered="#{r.cssStyleClass ne 'case'}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <!-- if row item is not document, but case, create link to documents list of case (row item is subclass of Document, to be shown as listItem in document list) -->
         <a:actionLink id="col6-link2docList-1" value="#{r.docName}" action="dialog:documentListDialog" tooltip="#{r.docName}"
            actionListener="#{DocumentListDialog.setup}" rendered="#{r.cssStyleClass == 'case'}" styleClass="condence20-" >
            <f:param name="caseNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Workflow status --%>
      <a:column id="col7" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_workflow_status}" value="workflowStatus" styleClass="header" />
         </f:facet>
         <a:actionLink id="col7-text-1" value="#{r.workflowStatus}" action="#{DocumentDialog.action}" tooltip="#{r.workflowStatus}"
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Files --%>
<<<<<<< HEAD
      <a:column id="col10" styleClass="doc-list-actions">
=======
      <a:column id="col10" primary="true" styleClass="doc-list-actions">
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         <f:facet name="header">
         
            <h:outputText id="col10-header" value="#{msg.document_allFiles}" styleClass="header" />
         </f:facet>
          <f:facet name="csvExport">
              <a:param value="false"/>
          </f:facet>

         <wm:docPermissionEvaluator value="#{r.files[0].node}" allow="viewDocumentFiles">
<<<<<<< HEAD
            <a:actionLink id="col10-act1" value="#{r.files[0].name}" href="#{r.files[0].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[0].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
=======
            <a:actionLink id="col10-act1" value="#{r.files[0].name}" href="#{r.files[0].readOnlyUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[0].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.files[0].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[0].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[0].name}"
               title="#{r.files[0].name}" rendered="#{r.files[0] != null}" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.files[1].node}" allow="viewDocumentFiles">
<<<<<<< HEAD
            <a:actionLink id="col10-act2" value="#{r.files[1].name}" href="#{r.files[1].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[1].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
=======
            <a:actionLink id="col10-act2" value="#{r.files[1].name}" href="#{r.files[1].readOnlyUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[1].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.files[1].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[1].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[1].name}"
               title="#{r.files[1].name}" rendered="#{r.files[1] != null}" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.files[2].node}" allow="viewDocumentFiles">
<<<<<<< HEAD
            <a:actionLink id="col10-act3" value="#{r.files[2].name}" href="#{r.files[2].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[2].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
=======
            <a:actionLink id="col10-act3" value="#{r.files[2].name}" href="#{r.files[2].readOnlyUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[2].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.files[2].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[2].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[2].name}"
               title="#{r.files[2].name}" rendered="#{r.files[2] != null}" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.files[3].node}" allow="viewDocumentFiles">
<<<<<<< HEAD
            <a:actionLink id="col10-act4" value="#{r.files[3].name}" href="#{r.files[3].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[3].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
=======
            <a:actionLink id="col10-act4" value="#{r.files[3].name}" href="#{r.files[3].readOnlyUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[3].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.files[3].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[3].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[3].name}"
               title="#{r.files[3].name}" rendered="#{r.files[3] != null}" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.files[4].node}" allow="viewDocumentFiles">
<<<<<<< HEAD
            <a:actionLink id="col10-act5" value="#{r.files[4].name}" href="#{r.files[4].downloadUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[4].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
=======
            <a:actionLink id="col10-act5" value="#{r.files[4].name}" href="#{r.files[4].readOnlyUrl}" target="_blank" showLink="false"
               image="/images/icons/#{r.files[4].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.files[4].node}" deny="viewDocumentFiles">
            <h:graphicImage value="/images/icons/#{r.files[4].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[4].name}"
               title="#{r.files[4].name}" rendered="#{r.files[4] != null}" />
         </wm:docPermissionEvaluator>
      </a:column>