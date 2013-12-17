<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ page import="ee.webmedia.alfresco.utils.ComponentUtil"%>
<%@ page import="javax.faces.context.FacesContext"%>
<%@ page import="org.alfresco.web.app.Application"%>
<%@ page import="org.alfresco.web.app.servlet.FacesHelper"%>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator"%>
<%@ page import="ee.webmedia.alfresco.document.file.web.AddFileDialog"%>

<%
    boolean fileUploaded = false;

    AddFileDialog dialog = (AddFileDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "AddFileDialog");
    if (dialog != null && (dialog.getFileUploadBean() != null || dialog.getSelectedFileNodeRef() != null)) {
        fileUploaded = true;
    }
%>
<f:verbatim>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/validation.js"> </script>

<%
    if (fileUploaded) {
            PanelGenerator.generatePanelStart(out, request.getContextPath(), "message", "#ffffcc");
            out.write("<img alt='' align='absmiddle' src='");
            out.write(request.getContextPath());
            out.write("/images/icons/info_icon.gif' />&nbsp;&nbsp;");
            out.write(dialog.getFileUploadSuccessMsg());
            PanelGenerator.generatePanelEnd(out, request.getContextPath(), "yellowInner");
            out.write("<div style='padding:2px;'></div>");
        }
%>

</f:verbatim>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/file/web/file-block.jsp" />

<%
    if (fileUploaded == false) {
%>

   <a:panel styleClass="column panel-100" id="file-upload" label="#{msg.upload_content}">
   <f:verbatim>
   <div id="uploader-wrapper">
	   <applet class="file-upload-applet" width="750" height="400" archive='<c:url value="/applet/jumploader_z.jar" />' code="jmaster.jumploader.app.JumpLoaderApplet.class" name="jumpLoaderApplet">
	    	<param value="false" name="uc_imageEditorEnabled" />
			<param value="<c:url value='/uploadFileServlet' />" name="uc_uploadUrl" />
			<param value="-1" name="uc_partitionLength" />
			<param name="ac_fireUploaderStatusChanged" value="false" />
			<param name="ac_messagesZipUrl" value='<c:url value="/applet/messages_et.zip" />'/>
			<param name="vc_lookAndFeel" value="system" />
			<param name="vc_uploadViewStartUploadButtonText" value="</f:verbatim><h:outputText value="#{msg.file_upload_selected}" /><f:verbatim>"/>
			<param name="vc_uploadViewStartUploadButtonImageUrl" value="<c:url value="/images/icons/media-play-green.png" />"/>
			<param name="vc_uploadViewStopUploadButtonText" value="</f:verbatim><h:outputText value="#{msg.file_abort_uploading}" /><f:verbatim>"/>
			<param name="vc_uploadViewStopUploadButtonImageUrl" value="<c:url value="/images/icons/media-stop-red.png" />"/>
	   </applet>
   </div>
   
   <script type="text/javascript">
   $jQ(document).ready(function() {
      var uploaderInitialized = false;
      try {
         var uploader = document.jumpLoaderApplet.getUploader();
         var attrSet = uploader.getAttributeSet();
         var returnPage = attrSet.createStringAttribute("return-page", "ajax");
         returnPage.setSendToServer(true);
         uploaderInitialized = true;
      } catch (e) {
         if (typeof $jQ.log !== "undefined") {
            $jQ.log('jumpLoaderApplet error: ' + e);
         }
      }
      if (typeof $jQ.log !== "undefined"){
         $jQ.log('jumpLoaderApplet initialized = ' + uploaderInitialized);
      }
      if (uploaderInitialized !== true) {
         $jQ('#uploader-wrapper').hide();
         $jQ('#' + escapeId4JQ('dialog:dialog-body:upload_panel')).show();
      }
   });

   function uploaderStatusChanged( uploader ) {
	   if(uploader.getStatus() == 0) {
		   document.forms[1].submit();
	   }
   } 

   $jQ("#" + escapeId4JQ("dialog:confirmFileSelectionButton")).live("mousedown", function(event) {
      event.stopImmediatePropagation();
      var attachment = $jQ("#" + escapeId4JQ("dialog:dialog-body:select_attachment"));
      var scanned = $jQ("#" + escapeId4JQ("dialog:dialog-body:select_scanned_file"));
      var file = document.jumpLoaderApplet.getUploader().getFile(0);
      if((file != null && file.getStatus() == 2) || attachment[0].selectedIndex != -1 || scanned[0].selectedIndex != -1) {
         return true;
      }
      alert('<%= MessageUtil.getMessageAndEscapeJS("file_no_files_selected") %>');
      return false;
   });
   
   </script>
   </f:verbatim>

   <h:panelGrid id="upload_panel" columns="2" cellpadding="2" cellspacing="2" border="0" width="100%"
      columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn" style="display: none;">
      <h:outputText id="out_schema" value="#{msg.file_location}:" style="padding-left:8px" />
      <r:upload id="uploader" value="" framework="dialog" />
   </h:panelGrid>

   </a:panel>

   <a:panel styleClass="column panel-50" id="attachment-upload" label="#{msg.file_add_attachment}" rendered="#{UserService.documentManager}">
      <a:panel id="attachment-folder-panel" rendered="#{AddFileDialog.showAttachmentFolderSelect}">
         <h:panelGrid id="attachment-folder-select-panel" columns="2" columnClasses="vertical-align-middle,vertical-align-middle" >
            <h:outputText id="attachment-folder-label" value="#{msg.file_add_folder_label}" />
            <a:panel id="att-folder-panel">
               <h:selectOneMenu id="attachment-folder-select" styleClass="#{AddFileDialog.onChangeStyleClass}">
                  <f:selectItems value="#{AddFileDialog.attachmentFolders}" />
               </h:selectOneMenu>
               <a:actionLink id="submit-att-folder-link" value="" actionListener="#{AddFileDialog.attachmentFolderSelected}" styleClass="hidden" />
            </a:panel>
         </h:panelGrid>
      </a:panel>
      <a:panel id="attachment-folder-panel-empty" rendered="#{AddFileDialog.showBlankForAttachmentBlock}">
      	<f:verbatim> <p style='height:22px;'> </p></f:verbatim>
      </a:panel>
      <h:outputText id="out_attachment" value="#{msg.file_add_attachment_label}" styleClass="dialogpanel-title block" />
      <h:selectManyMenu id="select_attachment" style="width: 100%; height: 200px;" binding="#{AddFileDialog.attachmentSelect}" validator="#{AddFileDialog.validate}">
         <f:selectItems value="#{AddFileDialog.attachments}" />
      </h:selectManyMenu>
   </a:panel>

    <a:panel styleClass="column panel-50-f" id="scanned-file-upload" label="#{msg.file_add_scanned}" rendered="#{UserService.documentManager}">
      <a:panel id="scanned-folder-panel" rendered="#{AddFileDialog.showScannedFolderSelect}">
         <h:panelGrid id="scanned-folder-select-panel" columns="2" columnClasses="vertical-align-middle,vertical-align-middle" >
            <h:outputText id="scanned-folder-label" value="#{msg.file_add_folder_label}" />
            <a:panel id="scan-folder-panel">
               <h:selectOneMenu id="scanned-folder-select" styleClass="#{AddFileDialog.onChangeStyleClass}">
                  <f:selectItems value="#{AddFileDialog.scannedFolders}" />
               </h:selectOneMenu>
               <a:actionLink id="submit-scan-folder-link" value="" actionListener="#{AddFileDialog.scannedFolderSelected}" styleClass="hidden" />
            </a:panel>
         </h:panelGrid>
      </a:panel>    
      <a:panel id="scanned-folder-panel-empty" rendered="#{AddFileDialog.showBlankForScannedBlock}">
        <f:verbatim> <p style='height:22px;'> </p></f:verbatim>
      </a:panel>
       <h:outputText id="out_scanned_file" value="#{msg.file_add_scanned_label}" styleClass="dialogpanel-title block"/>
       <h:selectManyMenu id="select_scanned_file" style="width: 100%; height: 200px;" binding="#{AddFileDialog.scannedSelect}" validator="#{AddFileDialog.validate}">
           <f:selectItems value="#{AddFileDialog.scannedFiles}"/>
       </h:selectManyMenu>
    </a:panel>
   
<%
    }
    if (fileUploaded) {
%>
   <a:panel styleClass="column panel-100" id="file-upload" label="#{msg.uploaded_content}">
      <h:panelGroup id="uploadedFiles" binding="#{AddFileDialog.uploadedFilesPanelGroup}" />
   </a:panel>
<%
    }
%>
   <f:verbatim>

<script type="text/javascript">
$jQ(document).ready(function(){
        var finishButton = document.getElementById("dialog:finish-button");
        if(finishButton != null) {
	    	document.getElementById("dialog:finish-button").disabled = false;
	        document.getElementById("dialog:finish-button").onclick = function() {
		    <%if ( dialog.isNeedMultipleInvoiceConfirmation()){ %>
	           if (!confirm('<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("file_add_confirm_multiple_invoice")%>')){
	              return false;
	           }
	        <%
	           }
            %>	           
	           clear_dialog();
	        }
        }
      })
</script>
</f:verbatim>
<% if (!fileUploaded) { %>
	<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
<% } %>
<a:booleanEvaluator value="#{DocumentDialogHelperBean.inEditMode}" id="addfile-docMetaInEditMode">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docdynamic/web/metadata-block-lockRefresh.jsp" />
</a:booleanEvaluator>
