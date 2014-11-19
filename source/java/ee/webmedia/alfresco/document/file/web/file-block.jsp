<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.document.file.web.AddFileDialog"%>
<%@ page import="org.alfresco.web.app.Application" %>

<<<<<<< HEAD
<%
   String webdavOpenMode = "webdav-" + (Application.getDialogManager().getBean() instanceof AddFileDialog? "readOnly": "open") + " condence50- strictTrim tooltip";
=======

<%
   boolean readOnly = Application.getDialogManager().getBean() instanceof AddFileDialog;
   String webdavClass = readOnly ? "" : " webdav-open";
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
%>

<h:panelGroup id="files-panel-facets">
   <f:facet name="title">
      <r:actions id="acts_add_content" value="addFileMenu" context="#{DocumentDialogHelperBean.node}" showLink="false" rendered="#{DialogManager.bean != AddFileDialog}" />
   </f:facet>
</h:panelGroup>
<h:panelGroup id="inactive-files-panel-facets">
   <f:facet name="title">
      <r:actions id="acts_add_inactive_content" value="addInactiveFileMenu" context="#{DocumentDialogHelperBean.node}" showLink="false" rendered="#{DialogManager.bean != AddFileDialog}"/>
   </f:facet>
</h:panelGroup>
<<<<<<< HEAD
<h:panelGroup id="pdf-panel-facets">
   <f:facet name="title">
      <a:actionLink id="close-pdfView" actionListener="#{FileBlockBean.hidePdfBlock}" value="#{msg.file_view_pdf_close}" image="/images/icons/close_panel.gif" showLink="false"/>
   </f:facet>
</h:panelGroup>
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

<a:panel label="#{msg.file_title} (#{FileBlockBean.activeFilesCount})" id="files-panel" facetsId="dialog:dialog-body:files-panel-facets" styleClass="panel-100" progressive="true"
   expanded="<%=new Boolean(!(Application.getDialogManager().getBean() instanceof AddFileDialog)).toString() %>">

   <a:richList id="filelistList" viewMode="details" value="#{FileBlockBean.files}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

      <%-- Name with URL link column --%>
      <a:column id="col1" primary="true" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col1-header" value="#{msg.file_name}" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:panelGroup>
               <wm:docPermissionEvaluator id="col1-act1-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
<<<<<<< HEAD
                  <a:actionLink id="col1-act1" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass='<%="inlineAction " + webdavOpenMode %>' />
=======
                  <a:actionLink id="col1-act1" value="#{r.displayName}" href='<%=(readOnly ? "#{r.readOnlyUrl}" : "#{r.downloadUrl}")%>' target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction<%=webdavClass%>" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
               </wm:docPermissionEvaluator>
               <wm:docPermissionEvaluator id="col1-act1-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
                  <h:graphicImage id="col1-act1-deny" value="#{r.fileType16}" />
               </wm:docPermissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <wm:docPermissionEvaluator id="col1-act2-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
<<<<<<< HEAD
            <a:actionLink id="col1-act2" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" styleClass="<%=webdavOpenMode %>" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator id="col1-act2a-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
            <h:outputText id="col1-act2a-eval-deny-txt" value="#{r.displayName}" />
=======
            <a:actionLink id="col1-act2" value="#{r.displayName}" href='<%=(readOnly ? "#{r.readOnlyUrl}" : "#{r.downloadUrl}")%>' target="_blank" styleClass="<%=webdavClass%>" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator id="col1-act2a-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
            <h:outputText value="#{r.displayName}" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         </wm:docPermissionEvaluator>
      </a:column>

      <%-- Created By column --%>
      <a:column id="col2" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col2-header" value="#{msg.file_added_by}" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.creator}" />
      </a:column>

      <%-- Created Date column --%>
      <a:column id="col3" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col3-header" value="#{msg.file_added_time}" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.created}" rendered="#{r.digiDocItem == false}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Modified By column --%>
      <a:column id="col4" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col4-header" value="#{msg.file_modified_by}" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-txt" value="#{r.modifier}" rendered="#{r.digiDocItem == false}" />
      </a:column>

      <%-- Modified Date column --%>
      <a:column id="col5" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col5-header" value="#{msg.file_modified_time}" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-txt" value="#{r.modified}" rendered="#{r.digiDocItem == false}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Size --%>
      <a:column id="col6" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col6-header" value="#{msg.file_size}" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-txt" value="#{r.size}" rendered="#{r.digiDocItem == false}">
            <a:convertSize />
         </h:outputText>
      </a:column>

<<<<<<< HEAD
      <%-- Transform checkbox --%>
      <a:column id="col6-2" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col6-2-header" value="#{msg.file_transform_column}" styleClass="header" />
         </f:facet>
         <h:selectBooleanCheckbox title="#{msg.file_convert_to_pdf_if_signed_tooltip}" id="col6-2-boolean" value="#{r.convertToPdfIfSigned}"  disabled="#{!r.transformableToPdf || !DocumentDialogHelperBean.inEditMode}">
         </h:selectBooleanCheckbox>
      </a:column>

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
      <%-- Remove and Version column --%>
      <a:column id="col7" rendered="#{FileBlockBean.inWorkspace and r.activeAndNotDigiDoc and DialogManager.bean != AddFileDialog}">
         <a:actionLink id="col7-act33" value="#{r.name}" actionListener="#{FileBlockBean.toggleActive}" showLink="false"
            image="/images/icons/document-convert.png" tooltip="#{msg.file_toggle_deactive} " rendered="#{FileBlockBean.toggleActive}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
<<<<<<< HEAD
         <a:actionLink id="col7-act35" value="#{r.name}" showLink="false" image="/images/icons/topic-16.gif" tooltip="#{msg.file_comment_tooltip}" actionListener="#{CommentFileDialog.open}" rendered="#{UserService.userFullName == r.modifier}">               
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <wm:docPermissionEvaluator id="col7-act34-eval" value="#{r.node}" allow="editDocument">
            <a:actionLink id="col7-act34" value="#{r.name}" showLink="false" image="/images/icons/edit_properties.gif" tooltip="#{msg.file_change_tooltip}" actionListener="#{ChangeFileDialog.open}">               
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </wm:docPermissionEvaluator>         
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         <wm:docPermissionEvaluator id="col7-act2-eval" value="#{r.node}" allow="viewDocumentFiles">
            <a:actionLink id="col7-act2" value="#{r.name}" actionListener="#{VersionsListDialog.select}" action="dialog:versionsListDialog" showLink="false"
               image="/images/icons/version_history.gif" rendered="#{r.versionable && !DocumentDialogHelperBean.notEditable}" tooltip="#{msg.file_version_history}">
               <f:param name="fileName" value="#{r.name}" />
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </wm:docPermissionEvaluator>
         <a:actionLink id="col7-act" value="#{r.name}" actionListener="#{BrowseBean.setupContentAction}" action="dialog:deleteFile" showLink="false"
            image="/images/icons/delete.gif" tooltip="#{msg.file_remove}" rendered="#{FileBlockBean.deleteFileAllowed}">
            <f:param name="id" value="#{r.id}" />
            <f:param name="ref" value="#{r.nodeRef}" />
         </a:actionLink>
         <wm:docPermissionEvaluator id="col7-act4-eval" value="#{r.node}" allow="editDocument">
            <a:actionLink id="col7-act4" value="#{r.name}" actionListener="#{FileBlockBean.transformToPdf}" showLink="false"
               image="/images/filetypes/pdf.gif" tooltip="#{msg.file_generate_pdf}" rendered="#{r.transformableToPdf && !DocumentDialogHelperBean.notEditable}">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator id="col7-act5-eval" value="#{r.node}" allow="viewDocumentFiles">
<<<<<<< HEAD
            <a:actionLink id="col7-act5-iframe" value="#{r.name}" actionListener="#{FileBlockBean.viewPdf}" showLink="false"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_view_pdf}" rendered="#{r.pdf}">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
            <a:actionLink id="col7-act5-download" value="#{r.name}" href="#{r.downloadUrl}" target="_blank" styleClass="<%=webdavOpenMode %>" showLink="false"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_view_pdf}" rendered="#{r.pdf}"/>            
=======
            <a:actionLink id="col7-act5" value="#{r.name}" actionListener="#{FileBlockBean.viewPdf}" showLink="false"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_view_pdf}" rendered="#{r.pdf}">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         </wm:docPermissionEvaluator>         
      </a:column>

      <a:column id="col1-ddoc" primary="true" rendered="#{FileBlockBean.inWorkspace and r.activeDigiDoc}">
         <a:panel id="ddoc-inner-panel" styleClass="digidoc-panel">
            <h:dataTable id="ddocList" value="#{r.dataItems}" var="v">
               <h:column id="col1-inner">
                  <wm:docPermissionEvaluator id="col1-inner-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
                     <a:actionLink id="inner-act1" value="#{v.displayName}" tooltip="#{v.name}" href="#{v.downloadUrl}" target="_blank" image="#{v.fileType16}" showLink="false" styleClass="inlineAction" />
                     <a:actionLink id="inner-act2" value="#{v.displayName}" tooltip="#{v.name}" href="#{v.downloadUrl}" target="_blank" />
                  </wm:docPermissionEvaluator>
                  <wm:docPermissionEvaluator id="col1-inner-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
                     <h:graphicImage id="inner-act1-deny" value="#{v.fileType16}" />
                     <h:outputText id="inner-act2-deny" value="#{v.displayName}" title="#{v.name}" />
                  </wm:docPermissionEvaluator>
               </h:column>
            </h:dataTable>
         </a:panel>
      </a:column>

      <a:column id="col2-ddoc" colspan="6" rendered="#{r.activeDigiDoc}">
         <a:panel id="ddoc-inner-panel2" styleClass="digidoc-panel">
            <h:dataTable id="siglistList" value="#{r.signatureItems}" var="v" width="100%">
               <h:column id="col2-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.file_signer}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt1" value="#{v.name}" />
               </h:column>
               <h:column id="col3-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.ddoc_signature_idcode}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt2" value="#{v.legalCode}" />
               </h:column>
               <h:column id="col4-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.ddoc_signature_date}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt3" value="#{v.signingTime}">
                     <a:convertXMLDate type="both" pattern="dd.MM.yyyy" />
                  </h:outputText>
               </h:column>
               <h:column id="col5-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.ddoc_signature_status}" styleClass=" header" />
                  </f:facet>
                  <a:booleanEvaluator id="itemValidEvaluator" value="#{v.valid}">
                     <h:outputText id="inner-txt4" value="#{msg.ddoc_signature_valid}" style="color: green;" />
                  </a:booleanEvaluator>
                  <a:booleanEvaluator id="itemNotValidEvaluator" value="#{v.notValid}">
                     <h:outputText id="inner-txt5" value="#{msg.ddoc_signature_invalid}" style="color: red;" />
                  </a:booleanEvaluator>
               </h:column>
            </h:dataTable>
         </a:panel>
      </a:column>


   </a:richList>
</a:panel>



<a:panel label="#{msg.file_inactive_title} (#{FileBlockBean.notActiveFilesCount})" id="inactive-files-panel" facetsId="dialog:dialog-body:inactive-files-panel-facets" styleClass="panel-100" progressive="true" expanded="false">

   <a:richList id="inactiveFilelistList" viewMode="details" value="#{FileBlockBean.files}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

      <%-- Name with URL link column --%>
      <a:column id="col21" primary="true" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col21-header" value="#{msg.file_name}" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:panelGroup>
               <wm:docPermissionEvaluator id="col21-act1-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
<<<<<<< HEAD
                  <a:actionLink id="col21-act1" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass='<%="inlineAction " + webdavOpenMode %>' />
=======
                  <a:actionLink id="col21-act1" value="#{r.displayName}" href='<%=(readOnly ? "#{r.readOnlyUrl}" : "#{r.downloadUrl}")%>' target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction<%=webdavClass%>" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
               </wm:docPermissionEvaluator>
               <wm:docPermissionEvaluator id="col21-act1-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
                  <h:graphicImage id="col21-act1-deny" value="#{r.fileType16}" />
               </wm:docPermissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <wm:docPermissionEvaluator id="col21-act2-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
<<<<<<< HEAD
            <a:actionLink id="col21-act2" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" styleClass="<%=webdavOpenMode %>" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator id="col21-act2-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
            <h:outputText id="col21-act2-eval-deny-txt" value="#{r.displayName}" />
=======
            <a:actionLink id="col21-act2" value="#{r.displayName}" href='<%=(readOnly ? "#{r.readOnlyUrl}" : "#{r.downloadUrl}")%>' target="_blank" styleClass="<%=webdavClass%>" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator id="col21-act2-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
            <h:outputText value="#{r.displayName}" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         </wm:docPermissionEvaluator>
      </a:column>

      <%-- Created By column --%>
      <a:column id="col22" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col22-header" value="#{msg.file_added_by}" styleClass="header" />
         </f:facet>
         <h:outputText id="col22-txt" value="#{r.creator}" />
      </a:column>

      <%-- Created Date column --%>
      <a:column id="col23" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col23-header" value="#{msg.file_added_time}" styleClass="header" />
         </f:facet>
         <h:outputText id="col23-txt" value="#{r.created}" rendered="#{not r.digiDocItem}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Modified By column --%>
      <a:column id="col24" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col24-header" value="#{msg.file_modified_by}" styleClass="header" />
         </f:facet>
         <h:outputText id="col24-txt" value="#{r.modifier}" rendered="#{r.digiDocItem == false}" />
      </a:column>

      <%-- Modified Date column --%>
      <a:column id="col25" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col25-header" value="#{msg.file_modified_time}" styleClass="header" />
         </f:facet>
         <h:outputText id="col25-txt" value="#{r.modified}" rendered="#{r.digiDocItem == false}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Size --%>
      <a:column id="col26" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col26-header" value="#{msg.file_size}" styleClass="header" />
         </f:facet>
         <h:outputText id="col26-txt" value="#{r.size}" rendered="#{r.digiDocItem == false}">
            <a:convertSize />
         </h:outputText>
      </a:column>

      <%-- Remove and Version column --%>
      <a:column id="col27" rendered="#{FileBlockBean.inWorkspace and r.notActiveAndNotDigiDoc and DialogManager.bean != AddFileDialog}">
         <a:actionLink id="col27-act3" value="#{r.name}" actionListener="#{FileBlockBean.toggleActive}" showLink="false"
            image="/images/icons/document-convert.png" tooltip="#{msg.file_toggle_active}" rendered="#{FileBlockBean.toggleInActive}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
<<<<<<< HEAD
         <a:actionLink id="col27-act35" value="#{r.name}" showLink="false" image="/images/icons/topic-16.gif" tooltip="#{msg.file_comment_tooltip}" actionListener="#{CommentFileDialog.open}" rendered="#{UserService.userFullName == r.modifier}">               
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <wm:docPermissionEvaluator id="col27-act34-eval" value="#{r.node}" allow="editDocument">
            <a:actionLink id="col27-act34" value="#{r.name}" showLink="false" image="/images/icons/edit_properties.gif" tooltip="#{msg.file_change_tooltip}" actionListener="#{ChangeFileDialog.open}">               
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </wm:docPermissionEvaluator>         
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         <wm:docPermissionEvaluator id="col27-act2-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
            <a:actionLink id="col27-act2" value="#{r.name}" actionListener="#{VersionsListDialog.select}" action="dialog:versionsListDialog" showLink="false"
               image="/images/icons/version_history.gif" rendered="#{r.versionable}" tooltip="#{msg.file_version_history}">
               <f:param name="fileName" value="#{r.name}" />
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </wm:docPermissionEvaluator>
         <a:actionLink id="col27-act" value="#{r.name}" actionListener="#{BrowseBean.setupContentAction}" action="dialog:deleteFile" showLink="false"
            image="/images/icons/delete.gif" tooltip="#{msg.file_remove}" rendered="#{FileBlockBean.deleteInactiveFileAllowed}">
            <f:param name="id" value="#{r.id}" />
            <f:param name="ref" value="#{r.nodeRef}" />
         </a:actionLink>
         <wm:docPermissionEvaluator id="col27-act5-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
<<<<<<< HEAD
            <a:actionLink id="col27-act5-iframe" value="#{r.name}" actionListener="#{FileBlockBean.viewPdf}" showLink="false"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_view_pdf}" rendered="#{r.pdf}">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
            <a:actionLink id="col27-act5-download" value="#{r.name}" href="#{r.downloadUrl}" target="_blank" styleClass="<%=webdavOpenMode %>" showLink="false"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_view_pdf}" rendered="#{r.pdf}"/> 
=======
            <a:actionLink id="col27-act5" value="#{r.name}" actionListener="#{FileBlockBean.viewPdf}" showLink="false"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_view_pdf}" rendered="#{r.pdf}">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
         </wm:docPermissionEvaluator>         
      </a:column>

      <a:column id="col21-ddoc" primary="true" rendered="#{FileBlockBean.inWorkspace and r.notActiveAndDigiDoc}">
         <a:panel id="ddoc-inner-panel" styleClass="digidoc-panel">
            <h:dataTable id="ddocList" value="#{r.dataItems}" var="v">
               <h:column id="col21-inner">
                  <wm:docPermissionEvaluator id="col21-inner-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
                     <a:actionLink id="inner-act1" value="#{v.displayName}" tooltip="#{v.name}" href="#{v.downloadUrl}" target="_blank" image="#{v.fileType16}" showLink="false" styleClass="inlineAction" />
                     <a:actionLink id="inner-act2" value="#{v.displayName}" tooltip="#{v.name}" href="#{v.downloadUrl}" target="_blank" />
                  </wm:docPermissionEvaluator>
                  <wm:docPermissionEvaluator id="col21-inner-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
                     <h:graphicImage id="inner-act1-deny" value="#{v.fileType16}" />
                     <h:outputText id="inner-act2-deny" value="#{v.displayName}" title="#{v.name}" />
                  </wm:docPermissionEvaluator>
               </h:column>
            </h:dataTable>
         </a:panel>
      </a:column>

      <a:column id="col22-ddoc" colspan="6" rendered="#{r.notActiveAndDigiDoc}">
         <a:panel id="ddoc-inner-panel2" styleClass="digidoc-panel">
            <h:dataTable id="siglistList" value="#{r.signatureItems}" var="v" width="100%">
               <h:column id="col2-inner">
                  <f:facet name="header">
                     <h:outputText id="col22-inner-header" value="#{msg.file_signer}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt1" value="#{v.name}" />
               </h:column>
               <h:column id="col23-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.ddoc_signature_idcode}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt2" value="#{v.legalCode}" />
               </h:column>
               <h:column id="col24-inner">
                  <f:facet name="header">
                     <h:outputText id="col22-inner-header" value="#{msg.ddoc_signature_date}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt23" value="#{v.signingTime}">
                     <a:convertXMLDate type="both" pattern="dd.MM.yyyy" />
                  </h:outputText>
               </h:column>
               <h:column id="col25-inner">
                  <f:facet name="header">
                     <h:outputText id="col22-inner-header" value="#{msg.ddoc_signature_status}" styleClass=" header" />
                  </f:facet>
                  <a:booleanEvaluator id="itemValidEvaluator" value="#{v.valid}">
                     <h:outputText id="inner-txt4" value="#{msg.ddoc_signature_valid}" style="color: green;" />
                  </a:booleanEvaluator>
                  <a:booleanEvaluator id="itemNotValidEvaluator" value="#{v.notValid}">
                     <h:outputText id="inner-txt5" value="#{msg.ddoc_signature_invalid}" style="color: red;" />
                  </a:booleanEvaluator>
               </h:column>
            </h:dataTable>
         </a:panel>
      </a:column>

   </a:richList>
</a:panel>

<<<<<<< HEAD
<a:panel label="PDF" id="pdf-panel" facetsId="dialog:dialog-body:pdf-panel-facets" styleClass="panel-100" progressive="true" rendered="#{FileBlockBean.inWorkspace and FileBlockBean.pdfUrl != null}">
   <f:verbatim>
      <iframe id="pdf-panel-iframe" width="100%" height="450" style="z-index: -1;" wmode="transparent" tabindex="-1" src="<%=request.getContextPath()%></f:verbatim><h:outputText value="#{FileBlockBean.pdfUrl}" /><f:verbatim>" class="fileViewerFrame" name="embedpdf_1" id="embedpdf_1">
      </iframe>
   </f:verbatim>
</a:panel>

<script type="text/javascript">
<!-- source http://stackoverflow.com/questions/5645581/how-to-check-adobe-reader-installed-or-not-without-active-x-checking-in-javascri -->
   
   var acrobat = new Object();
   acrobat.installed = false;
   acrobat.version = '0.0';

   if (navigator.plugins && navigator.plugins.length) {
      for ( var x = 0, l = navigator.plugins.length; x < l; ++x) {
         if (navigator.plugins[x].description.indexOf('Adobe Acrobat') != -1 || navigator.plugins[x].description.indexOf('PDF') != -1) {
            acrobat.version = (navigator.plugins[x].description.indexOf('PDF') != -1) ? '7+' : parseFloat(navigator.plugins[x].description.split('Version ')[1]);
            if (acrobat.version.toString().length == 1)
               acrobat.version += '.0';
            acrobat.installed = true;
            break;
         }
      }
   } else if (window.ActiveXObject) {
      for (x = 2; x < 10; x++) {
         try {
            oAcro = eval("new ActiveXObject('PDF.PdfCtrl." + x + "');");
            if (oAcro) {
               acrobat.installed = true;
               acrobat.version = x + '.0';
            }
         } catch (e) {
         }
      }
      try {
         oAcro4 = new ActiveXObject('PDF.PdfCtrl.1');
         if (oAcro4) {
            acrobat.installed = true;
            acrobat.version = '4.0';
         }
      } catch (e) {
      }
      try {
         oAcro7 = new ActiveXObject('AcroPDF.PDF.1');
         if (oAcro7) {
            acrobat.installed = true;
            acrobat.version = '7+';
         }
      } catch (e) {
      }
      try {
         oAcro7 = new ActiveXObject('AcroPDF.PDF');
         if (oAcro7) {
            acrobat.installed = true;
            acrobat.version = '7+';
         }
      } catch (e) {
      }
   }
   
   if (acrobat.installed) {
      $jQ('#col7-act5-iframe').show();
      $jQ('#col27-act5-iframe').show();
      $jQ('#col7-act5-download').hide();
      $jQ('#col27-act5-download').hide();      
   } else {
      $jQ('#col7-act5-iframe').hide();
      $jQ('#col27-act5-iframe').hide();
      $jQ('#col7-act5-download').show();
      $jQ('#col27-act5-download').show();      
   }
   
</script>
=======
<a:panel label="PDF" id="pdf-panel" styleClass="panel-100" progressive="true" rendered="#{FileBlockBean.inWorkspace and FileBlockBean.pdfUrl != null}">
   <f:verbatim>
      <iframe width="100%" height="450" style="z-index: -1;" wmode="transparent" tabindex="-1" src="<%=request.getContextPath()%></f:verbatim><h:outputText value="#{FileBlockBean.pdfUrl}" /><f:verbatim>" class="fileViewerFrame" name="embedpdf_1" id="embedpdf_1">
      </iframe>
   </f:verbatim>
</a:panel>
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
