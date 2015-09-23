<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
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
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@ page import="ee.webmedia.alfresco.parameters.model.Parameters"%>

<%
    boolean fileUploaded = false;

    AddFileDialog dialog = (AddFileDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "AddFileDialog");
    if (dialog != null && (dialog.getFileUploadBean() != null || dialog.getSelectedFileNodeRef() != null)) {
        fileUploaded = true;
    }
    String contextPath = request.getContextPath();
    Long maxFileSize = BeanHelper.getParametersService().getLongParameter(Parameters.UPLOAD_FILE_MAX_SIZE);
%>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/file/web/file-block.jsp" />

   <a:panel styleClass="column panel-100" id="file-upload" label="#{msg.upload_content}">
   <f:verbatim>
   <script type="text/javascript" src="<%=contextPath%>/scripts/jquery/jquery.ui.widget.min.js"></script>
   
   <!-- The Iframe Transport is required for browsers without support for XHR file uploads -->
   <script type="text/javascript" src="<%=contextPath%>/scripts/jquery/fileupload/jquery.iframe-transport.js"></script>
   <!-- The basic File Upload plugin -->
   <script type="text/javascript" src="<%=contextPath%>/scripts/jquery/fileupload/jquery.fileupload.js"></script>
   <!-- The File Upload processing plugin -->
   <script type="text/javascript" src="<%=contextPath%>/scripts/jquery/fileupload/jquery.fileupload-process.js"></script>
   <!-- The File Upload validation plugin -->
   <script type="text/javascript" src="<%=contextPath%>/scripts/jquery/fileupload/jquery.fileupload-validate.js"></script>
   <link rel="stylesheet" href="<%=contextPath%>/scripts/jquery/fileupload/jquery.fileupload.css" type="text/css">
   
   <div id="upload-container">
      <div class="row fileupload-buttonbar">
         <span class="fileinput-button" >
            <span>Lisa faile...</span>
            <input id="fileupload" type="file" multiple="" name="files[]">
         </span>
      </div>
      <div class="overall-progress-container">
         <span id="dropzone-text">Lohista failid siia...</span>
         <div class="progress-bar overall-progress">&nbsp;</div>
         <div class="progress-info">&nbsp;</div>
      </div>
      <div id="files-table">
         <table>
            <thead>
               <tr>
                  <th class="icon"></th>
                  <th class="name">Nimi</th>
                  <th class="size">Suurus</th>
                  <th class="additional"></th>
               </tr>
            </thead>
            <tbody id="files"></tbody>
         </table>
      </div>
   </div>
   
   <script type="text/javascript">
   function getHumanReadableSizeString(fileSizeInBytes) {
      var i = 0;
      var byteUnits = [' B',' kB', ' MB', ' GB'];
      while (fileSizeInBytes > 1024 && i < (byteUnits.length - 1)) {
         fileSizeInBytes = fileSizeInBytes / 1024;
         i++;
      }
      return Math.max(fileSizeInBytes, 0.1).toFixed(1) + byteUnits[i];
   };
   
   function deleteFile(deleteButton, uri) {
      deleteButton.prop('disabled', true);
      $jQ.ajax({
         type: 'POST',
         url: uri,
         dataType: 'json',
         mode: 'queue',
         success: function (responseText) {
            var parents = deleteButton.parents("tr");
            parents.fadeOut("normal", function(){
               parents.remove();
            });
         },
         error: function() {
            deleteButton.prop('disabled', true);
         }
   	  });
   }
   
   function initUploadedFiles() {
      $jQ.ajax({
         type: 'POST',
         url: '<%=contextPath%>' + '/uploadedFileServlet',
         dataType: 'json',
         mode: 'queue',
         success: function (ajaxResponse) {
            var filesTable = $jQ('#files');
            $jQ.each(ajaxResponse.files, function (index, file) {
               var row = $jQ('<tr/>');
               createRow(row, file, true);
               row.appendTo(filesTable);
            });
         }
   	  });
   }
   
   function createRow(row, file, loadAsync) {
      var icon = $jQ('<td/>').append(createIcon(file.name));
      var title = $jQ('<td/>').append($jQ('<span/>').text(file.name));
      
      var fileSize = $jQ('<td/>');
      if(!loadAsync) {
         var sizeAndProgress = $jQ('<span/>');
         var progressBarContainer = $jQ('<div class="progress-bar-container" role="progressbar"/>').append('<div class="progress-bar" />');
         sizeAndProgress.append($jQ('<p/>').text(getHumanReadableSizeString(file.size)));
         sizeAndProgress.append(progressBarContainer);
         fileSize.append(sizeAndProgress);
      } else {
         fileSize.append($jQ('<span/>').text(file.size));
      }
      var additional = $jQ('<td/>').append($jQ('<span class="additional"/>')); // button or infotext
      if(loadAsync) {
         createDeleteButton(file, additional);
      }
      icon.appendTo(row);
      title.appendTo(row);
      fileSize.appendTo(row);
      additional.appendTo(row);
   }
   
   function createIcon(fileName) {
      var iconDiv = $jQ('<div class="file-icon"/>');
      var extension = fileName.split('.').pop().toLowerCase();
      var imageUrl = '<%=contextPath%>/images/filetypes32/'+extension+'.gif';
      
      $jQ.ajax({
         type: 'GET',
         url: imageUrl,
         dataType: 'html',
         mode: 'queue',
         success: function (responseText) {
            iconDiv.css('background-image', "url('"+imageUrl+"')");
         }
   	  });
      return iconDiv;
   }
   
   function createDeleteButton(file, additional) {
      var deleteButton = $jQ('<input class="button" type="button"/>');
      deleteButton.on('click', function(){deleteFile(deleteButton, file.deleteUrl)});
      deleteButton.attr('value', 'Kustuta');
      additional.append(deleteButton);
   }
   
   $jQ(document).ready(function() {
      var uploaderInitialized = false;
      try {
         var url = '<%=contextPath%>' + '/uploadFileServlet';
         $jQ('#fileupload').fileupload({
            url: url,
            dataType: 'json',
            autoUpload: true,
            disallowCryptedFiles: true,
            maxFileSize: (<%=maxFileSize%> *1024*1024),
            limitConcurrentUploads: 1
         }).on('fileuploadadd', function (e, data) {
            data.context = $jQ('<tr/>').appendTo('#files');
            $jQ.each(data.files, function (index, file) {
               createRow(data.context, file, false);
            });
         }).on('fileuploadprocessalways', function (e, data) {
            var index = data.index;
            var file = data.files[index];
            if (file.error) {
               var additional = data.context.find('.additional');
               additional.attr('class', 'error');
               if("maxFileSize" == file.errorType) {
                  additional.text("Fail ületab maksimaalset lubatud suurust <%=maxFileSize%> MB");
               } else {
                  additional.text(file.error);
               }
            }
            if (index + 1 === data.files.length) {
               data.context.find('button').text('Upload').prop('disabled', !!data.files.error);
            }
         }).on('fileuploadprogressall', function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            var speed = getHumanReadableSizeString(data.bitrate/8) + "/s";
            var bar = $jQ('.overall-progress');
            var info = $jQ('.progress-info');
            var helpText = $jQ("#dropzone-text");
            if(helpText.length == 0) {
               return;
            }
            bar.css('width', progress + '%');
            info.text("Valmis: " + progress + "% (Kiirus: " + speed + ")");
            if(progress >= 100) {
               bar.fadeOut();
               info.fadeOut();
               helpText.delay(500).fadeIn(2000);
            } else {
               helpText.fadeOut(0);
               bar.fadeIn(50);
               info.fadeIn(50);
            }
         }).on('fileuploadprogress', function(e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            data.context.find(".progress-bar").css('width', progress + '%');
            if(progress === 100) {
               data.context.find(".progress-bar").fadeOut();
            }
         }).on('fileuploaddone', function (e, data) {
            if(data.result == null) {
               addFailMessage(data);
            }
            $jQ.each(data.result.files, function (index, file) {
               if (file.deleteUrl) {
                  var additional = data.context.find(".additional");
                  createDeleteButton(file, additional);
               }
               else if (file.error) {
                  var error = $jQ('<span class="error"/>').text(file.error);
                  $jQ(data.context.children()[index]).append('<br>').append(error);
               }
            });
         }).on('fileuploadfail', function (e, data) {
            $jQ.each(data.files, function (index) {
               addFailMessage(data);
            });
         }).prop('disabled', !$jQ.support.fileInput)
            .parent().addClass($jQ.support.fileInput ? undefined : 'disabled');
         uploaderInitialized = true;
      } catch(e) {
         uploaderInitialized = false;
      }
      if (uploaderInitialized !== true) {
         $jQ('#upload-container').hide();
         $jQ('#' + escapeId4JQ('dialog:dialog-body:upload_panel')).show();
      }
      
      if(uploaderInitialized === true && <%=fileUploaded%>) {
         initUploadedFiles();
      }
      
      if($jQ.browser.msie && (parseInt($jQ.browser.version, 10) < 10)) {
         $jQ("#dropzone-text").hide();
      }
      
   });
   
   function uploaderStatusChanged( uploader ) {
	   if(uploader.getStatus() == 0) {
		   document.forms[1].submit();
	   }
   }
   
   function addFailMessage(data) {
      var error = $jQ('<span class="error"/>').text('Faili üleslaadimine ebaõnnestus');
      data.context.find(".additional").append(error);
   }
   
   var valdateFilesSelected = function(event) {
      event.stopImmediatePropagation();
      var attachment = $jQ("#" + escapeId4JQ("dialog:dialog-body:select_attachment"));
      var scanned = $jQ("#" + escapeId4JQ("dialog:dialog-body:select_scanned_file"));
      var files = false;
      $jQ("#files tr").each(function(){
         files |= ($jQ(this).find(".error").length == 0);
      });
      
      if(files || attachment[0].selectedIndex != -1 || scanned[0].selectedIndex != -1) {
         return true;
      }
      alert('<%= MessageUtil.getMessageAndEscapeJS("file_no_files_selected") %>');
      return false;
   };
   
   $jQ("#" + escapeId4JQ("dialog:finish-button")).mousedown(valdateFilesSelected);
   $jQ("#" + escapeId4JQ("dialog:changeFileButton")).mousedown(valdateFilesSelected);
   
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
<a:booleanEvaluator value="#{DocumentDialogHelperBean.inEditMode}" id="addfile-docMetaInEditMode">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docdynamic/web/metadata-block-lockRefresh.jsp" />
</a:booleanEvaluator>
