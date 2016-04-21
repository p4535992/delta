<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.document.file.web.AddFileDialog"%>
<%@ page import="ee.webmedia.alfresco.document.file.web.ChangeFileDialog"%>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="org.alfresco.web.bean.dialog.IDialogBean" %>

<%
   IDialogBean bean = Application.getDialogManager().getBean();
   boolean readOnly = bean instanceof AddFileDialog || bean instanceof ChangeFileDialog;
%>

<h:panelGroup id="files-panel-facets">
   <f:facet name="title">
      <r:actions id="acts_add_content" value="addFileMenu" context="#{DocumentDialogHelperBean.node}" showLink="false" rendered="#{FileBlockBean.guest == false and DialogManager.bean != AddFileDialog and DialogManager.bean != ChangeFileDialog}" />
   </f:facet>
</h:panelGroup>
<h:panelGroup id="inactive-files-panel-facets">
   <f:facet name="title">
      <r:actions id="acts_add_inactive_content" value="addInactiveFileMenu" context="#{DocumentDialogHelperBean.node}" showLink="false" rendered="#{DialogManager.bean != AddFileDialog and DialogManager.bean != ChangeFileDialog}"/>
   </f:facet>
</h:panelGroup>
<h:panelGroup id="pdf-panel-facets">
   <f:facet name="title">
      <a:actionLink id="close-pdfView" actionListener="#{FileBlockBean.hidePdfBlock}" value="#{msg.file_view_pdf_close}" image="/images/icons/close_panel.gif" showLink="false"/>
   </f:facet>
</h:panelGroup>

<a:panel label="#{msg.file_title} (#{FileBlockBean.activeFilesCount})" id="files-panel" facetsId="dialog:dialog-body:files-panel-facets" styleClass="panel-100" progressive="true"
   expanded="<%=new Boolean(!(Application.getDialogManager().getBean() instanceof AddFileDialog || Application.getDialogManager().getBean() instanceof ChangeFileDialog)).toString() %>">
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
                  <a:actionLink id="col1-act1-1" value="#{r.displayName}" href="#{r.readOnlyUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction condence50- strictTrim tooltip" rendered="<%=Boolean.toString(readOnly) %>" />
                  <%-- Don't specify an explicit ID to this link so it can have a generated clientId --%>
                  <a:actionLink value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction condence50- strictTrim tooltip webdav-open" rendered="<%=Boolean.toString(!readOnly) %>" />
               </wm:docPermissionEvaluator>
               <wm:docPermissionEvaluator id="col1-act1-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
                  <h:graphicImage id="col1-act1-deny" value="#{r.fileType16}" />
               </wm:docPermissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <wm:docPermissionEvaluator id="col1-act2-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
            <a:actionLink id="col1-act2-1" value="#{r.displayName}" href="#{r.readOnlyUrl}" target="_blank" styleClass="condence50- strictTrim tooltip" rendered="<%=Boolean.toString(readOnly) %>" />
            <a:actionLink id="col1-act2-2" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" styleClass="condence50- strictTrim tooltip webdav-open" rendered="<%=Boolean.toString(!readOnly) %>" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator id="col1-act2a-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
            <h:outputText id="col1-act2a-eval-deny-txt" value="#{r.displayName}" />
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

      <%-- Transform checkbox --%>
      <a:column id="col6-2" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col6-2-header" value="#{msg.file_transform_column}" styleClass="header" />
         </f:facet>
         <h:selectBooleanCheckbox title="#{msg.file_convert_to_pdf_if_signed_tooltip}" id="col6-2-boolean" value="#{r.convertToPdfIfSigned}"  disabled="#{!r.transformableToPdf || !DocumentDialogHelperBean.inEditMode}">
         </h:selectBooleanCheckbox>
      </a:column>

      <%-- Remove and Version column --%>
      <a:column id="col7" rendered="#{FileBlockBean.inWorkspace and r.activeAndNotDigiDoc and DialogManager.bean != AddFileDialog and DialogManager.bean != ChangeFileDialog}">
         <a:actionLink id="col7-act36" value="#{r.activeLockOwner}" actionListener="#{FileBlockBean.unlock}" showLink="false"
            image="/images/icons/lock-unlock.png" tooltip="#{msg.file_unlock}" rendered="#{AuthenticationService.currentUserName == r.activeLockOwner}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col7-act33" value="#{r.name}" actionListener="#{FileBlockBean.toggleActive}" showLink="false"
            image="/images/icons/document-convert.png" tooltip="#{msg.file_toggle_deactive} " rendered="#{FileBlockBean.toggleActive && FileBlockBean.guest == false}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col7-act35" value="#{r.name}" showLink="false" image="/images/icons/topic-16.gif" tooltip="#{msg.file_comment_tooltip}" actionListener="#{CommentFileDialog.open}" rendered="#{UserService.userFullName == r.modifier}">               
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <wm:docPermissionEvaluator id="col7-act34-eval" value="#{r.node}" allow="editDocument">
            <a:actionLink id="col7-act34" value="#{r.name}" showLink="false" image="/images/icons/edit_properties.gif" tooltip="#{msg.file_change_tooltip}" actionListener="#{ChangeFileDialog.open}" rendered="#{FileBlockBean.guest == false}">               
               <f:param name="nodeRef" value="#{r.nodeRef}" />
               <f:param name="type" value="active" />
            </a:actionLink>
         </wm:docPermissionEvaluator>         
         <wm:docPermissionEvaluator id="col7-act2-eval" value="#{r.node}" allow="viewDocumentFiles">
            <a:actionLink id="col7-act2" value="#{r.name}" actionListener="#{VersionsListDialog.select}" action="dialog:versionsListDialog" showLink="false"
               image="/images/icons/version_history.gif" rendered="#{r.versionable && !DocumentDialogHelperBean.notEditable}" tooltip="#{msg.file_version_history}">
               <f:param name="fileName" value="#{r.name}" />
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </wm:docPermissionEvaluator>
         <a:actionLink id="col7-act" value="#{r.name}" actionListener="#{BrowseBean.setupContentAction}" action="dialog:deleteFile" showLink="false"
            image="/images/icons/delete.gif" tooltip="#{msg.file_remove}" rendered="#{FileBlockBean.deleteFileAllowed && FileBlockBean.guest == false}">
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
            <a:actionLink id="col7-act5-iframe" value="#{r.name}" actionListener="#{FileBlockBean.viewPdf}" showLink="false" styleClass="pdf-file-open-block"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_open_pdf_block}" rendered="#{r.pdf}">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
            <a:actionLink id="col7-act5-download" value="#{r.name}" href="#{r.downloadUrl}" target="_blank" styleClass="srictTrim tooltip pdf-file-download" showLink="false"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_view_pdf}" rendered="#{r.pdf}"/>            
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
                  <a:actionLink id="col21-act1-1" value="#{r.displayName}" href="#{r.readOnlyUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction" rendered="<%=Boolean.toString(readOnly) %>" />
                  <%-- Don't specify an explicit ID to this link so it can have a generated clientId --%>
                  <a:actionLink value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction webdav-open" rendered="<%=Boolean.toString(!readOnly) %>" />
               </wm:docPermissionEvaluator>
               <wm:docPermissionEvaluator id="col21-act1-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
                  <h:graphicImage id="col21-act1-deny" value="#{r.fileType16}" />
               </wm:docPermissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <wm:docPermissionEvaluator id="col21-act2-eval-allow" value="#{r.node}" allow="viewDocumentFiles">
            <a:actionLink id="col21-act2-1" value="#{r.displayName}" href="#{r.readOnlyUrl}" target="_blank" styleClass="condence50- strictTrim tooltip" rendered="<%=Boolean.toString(readOnly) %>" />
            <a:actionLink id="col21-act2-2" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" styleClass="condence50- strictTrim tooltip webdav-open" rendered="<%=Boolean.toString(!readOnly) %>" />
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator id="col21-act2-eval-deny" value="#{r.node}" deny="viewDocumentFiles">
            <h:outputText id="col21-act2-eval-deny-txt" value="#{r.displayName}" />
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
      <a:column id="col27" rendered="#{FileBlockBean.inWorkspace and r.notActiveAndNotDigiDoc and DialogManager.bean != AddFileDialog and DialogManager.bean != ChangeFileDialog}">
         <a:actionLink id="col27-act36" value="#{r.activeLockOwner}" actionListener="#{FileBlockBean.unlock}" showLink="false"
            image="/images/icons/lock-unlock.png" tooltip="#{msg.file_unlock}" rendered="#{AuthenticationService.currentUserName == r.activeLockOwner}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col27-act3" value="#{r.name}" actionListener="#{FileBlockBean.toggleActive}" showLink="false"
            image="/images/icons/document-convert.png" tooltip="#{msg.file_toggle_active}" rendered="#{FileBlockBean.toggleInActive && FileBlockBean.guest == false}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col27-act35" value="#{r.name}" showLink="false" image="/images/icons/topic-16.gif" tooltip="#{msg.file_comment_tooltip}" actionListener="#{CommentFileDialog.open}" rendered="#{UserService.userFullName == r.modifier}">               
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <wm:docPermissionEvaluator id="col27-act34-eval" value="#{r.node}" allow="editDocument">
            <a:actionLink id="col27-act34" value="#{r.name}" showLink="false" image="/images/icons/edit_properties.gif" tooltip="#{msg.file_change_tooltip}" actionListener="#{ChangeFileDialog.open}" rendered="#{FileBlockBean.guest == false}">               
               <f:param name="nodeRef" value="#{r.nodeRef}" />
               <f:param name="type" value="inactive" />
            </a:actionLink>
         </wm:docPermissionEvaluator>         
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
            <a:actionLink id="col27-act5-iframe" value="#{r.name}" actionListener="#{FileBlockBean.viewPdf}" showLink="false"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_open_pdf_block}" rendered="#{r.pdf}" styleClass="pdf-file-open-block">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
            <a:actionLink id="col27-act5-download" value="#{r.name}" href='<%=(readOnly ? "#{r.readOnlyUrl}" : "#{r.downloadUrl}")%>' target="_blank" styleClass="condence50- strictTrim tooltip pdf-file-download" showLink="false"
               image="/images/icons/file-small-gray.png" tooltip="#{msg.file_view_pdf}" rendered="#{r.pdf}"/> 
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

<a:panel label="PDF" id="pdf-panel" facetsId="dialog:dialog-body:pdf-panel-facets" styleClass="panel-100" progressive="true" rendered="#{FileBlockBean.inWorkspace and FileBlockBean.pdfUrl != null}">
   <f:verbatim>
      <iframe id="pdf-panel-iframe" width="100%" height="450" style="z-index: -1;" wmode="transparent" tabindex="-1" src="<%=request.getContextPath()%></f:verbatim><h:outputText value="#{FileBlockBean.pdfUrl}" /><f:verbatim>" class="fileViewerFrame" name="embedpdf_1" id="embedpdf_1">
      </iframe>
   </f:verbatim>
</a:panel>

<script type="text/javascript">

   //TODO: The question of Firefox (ver 19+) built in PDF viewer remains, since there is no good and reliable method for checking it at the momnent
   //
   // http://thecodeabode.blogspot.com
   // @author: Ben Kitzelman
   // @license: Do whatever you like with it
   // @updated: 03-03-2013
   //
   var getAcrobatInfo = function() {
    
     var getBrowserName = function() {
       return this.name = this.name || function() {
         var userAgent = navigator ? navigator.userAgent.toLowerCase() : "other";
    
         if(userAgent.indexOf("chrome") > -1)        return "chrome";
         else if(userAgent.indexOf("safari") > -1)   return "safari";
         else if(userAgent.indexOf("msie") > -1)     return "ie";
         else if(userAgent.indexOf("firefox") > -1)  return "firefox";
         return userAgent;
       }();
     };
    
     var getActiveXObject = function(name) {
       try { return new ActiveXObject(name); } catch(e) {}
     };
    
     var getNavigatorPlugin = function(name) {
       for(key in navigator.plugins) {
         var plugin = navigator.plugins[key];
         if(plugin.name == name) return plugin;
       }
     };
    
     var getPDFPlugin = function() {
       return this.plugin = this.plugin || function() {
         if(getBrowserName() == 'ie') {
           //
           // load the activeX control
           // AcroPDF.PDF is used by version 7 and later
           // PDF.PdfCtrl is used by version 6 and earlier
           return getActiveXObject('AcroPDF.PDF') || getActiveXObject('PDF.PdfCtrl');
         }
         else {
           return getNavigatorPlugin('Adobe Acrobat') || getNavigatorPlugin('Chrome PDF Viewer') || getNavigatorPlugin('WebKit built-in PDF');
         }
       }();
     };
    
     var isAcrobatInstalled = function() {
       return !!getPDFPlugin();
     };
    
     var getAcrobatVersion = function() {
       try {
         var plugin = getPDFPlugin();
    
         if(getBrowserName() == 'ie') {
           var versions = plugin.GetVersions().split(',');
           var latest   = versions[0].split('=');
           return parseFloat(latest[1]);
         }
    
         if(plugin.version) return parseInt(plugin.version);
         return plugin.name
         
       }
       catch(e) {
         return null;
       }
     }
    
     //
     // The returned object
     // 
     return {
       browser:        getBrowserName(),
       acrobat:        isAcrobatInstalled() ? 'installed' : false,
       acrobatVersion: getAcrobatVersion()
     };
   };
   
   var info = getAcrobatInfo()
   if (info.acrobat) {
      $jQ('.pdf-file-open-block').show();
      $jQ('.pdf-file-open-block').show();
      $jQ('.pdf-file-download').hide();
      $jQ('.pdf-file-download').hide();      
   } else {
      $jQ('.pdf-file-open-block').hide();
      $jQ('.pdf-file-open-block').hide();
      $jQ('.pdf-file-download').show();
      $jQ('.pdf-file-download').show();      
   }
   
</script>
