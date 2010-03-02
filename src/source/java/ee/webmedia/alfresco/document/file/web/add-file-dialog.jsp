<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="javax.faces.context.FacesContext"%>
<%@ page import="org.alfresco.web.app.Application"%>
<%@ page import="org.alfresco.web.app.servlet.FacesHelper"%>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator"%>
<%@ page import="ee.webmedia.alfresco.document.file.web.AddFileDialog"%>

<%
    boolean fileUploaded = false;

    AddFileDialog dialog = (AddFileDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "AddFileDialog");
    if (dialog != null && dialog.getFileName() != null) {
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
<%
    if (fileUploaded == false) {
%>

   <a:panel styleClass="column panel-100" id="file-upload" label="#{msg.upload_content}">
      <h:panelGrid id="upload_panel" columns="2" cellpadding="2" cellspacing="2" border="0" width="100%"
         columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">
         <h:outputText id="out_schema" value="#{msg.file_location}:" style="padding-left:8px" />
         <r:upload id="uploader" value="#{DialogManager.bean.fileName}" framework="dialog" />
      </h:panelGrid>
   </a:panel>

    <a:panel styleClass="column  panel-100" id="attachment-upload" label="#{msg.file_add_attachment}">
        <h:panelGrid id="upload_attachment_panel" columns="2" cellpadding="2" cellspacing="2" border="0" width="100%"
                     columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">
            <h:outputText id="out_attachment" value="#{msg.file_add_attachment_label}:" style="padding-left:8px"/>
            <h:selectOneMenu id="select_attachment" style="width:250px" onchange="this.form.submit();" valueChangeListener="#{DialogManager.bean.fileSelected}" value="">
                <f:selectItem itemValue="" itemLabel=""/>
                <f:selectItems value="#{DialogManager.bean.attachments}"/>
            </h:selectOneMenu>
        </h:panelGrid>
    </a:panel>

    <a:panel styleClass="column  panel-100" id="scanned-file-upload" label="#{msg.file_add_scanned}">
        <h:panelGrid id="upload_scanned_file_panel" columns="2" cellpadding="2" cellspacing="2" border="0" width="100%"
                     columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">
            <h:outputText id="out_scanned_file" value="#{msg.file_add_scanned_label}:" style="padding-left:8px"/>
            <h:selectOneMenu id="select_scanned_file" style="width:250px" onchange="this.form.submit();" valueChangeListener="#{DialogManager.bean.fileSelected}" value="">
                <f:selectItem itemValue="" itemLabel=""/>
                <f:selectItems value="#{DialogManager.bean.scannedFiles}"/>
            </h:selectOneMenu>
        </h:panelGrid>
    </a:panel>

<%
    }
    if (fileUploaded) {
%>
   <a:panel styleClass="column panel-90" id="file-upload" label="#{msg.uploaded_content}">
   
      <h:panelGroup>
         <a:actionLink image="/images/icons/delete.gif" value="#{msg.remove}" action="#{DialogManager.bean.reset}" showLink="false" id="link1" />
         <h:outputText id="text3" value="#{DialogManager.bean.fileName}" styleClass="dialogpanel-title filename" />
      </h:panelGroup>
   
      <h:outputText id="text4" value="#{msg.general_properties}" styleClass="dialogpanel-title" />
   
      <h:panelGrid columns="2" columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">
         <h:panelGroup>
            <h:graphicImage id="img0" value="/images/icons/required_field.gif" alt="#{msg.required_field}" />
            <h:outputText id="text5" value="#{msg.name}:" />
         </h:panelGroup>
         <h:inputText id="file-name" value="#{DialogManager.bean.fileNameWithoutExtension}" maxlength="1024" size="35" onkeyup="checkButtonState();" onchange="checkButtonState();" />
      </h:panelGrid>
   
   </a:panel>
<%
    }
%>
   <f:verbatim>

<script type="text/javascript">
      var finishButtonPressed = false;
      window.onload = pageLoaded;

      function pageLoaded()
      {
   <%if (fileUploaded) {%>
         document.getElementById("dialog").onsubmit = validate;
   <%}%>
         document.getElementById("dialog:finish-button").onclick = function() {finishButtonPressed = true; clear_dialog();}
        checkButtonState();
      }

      function checkButtonState()
      {
         if (!isFileSelected())
         {
            document.getElementById("dialog:finish-button").disabled = true;
         }
         else
         {
            document.getElementById("dialog:finish-button").disabled = false;
         }
      }

       function isFileSelected() {
         var filename = document.getElementById("dialog:dialog-body:file-name");
         if (filename == null)
            return false; 
         return filename.value.length != 0;
       }

      function validate()
      {
         if (finishButtonPressed)
         {
            finishButtonPressed = false;
            return validateName(document.getElementById("dialog:dialog-body:file-name"),
                                unescape('</f:verbatim><a:outputText id="text11" value="#{msg.validation_invalid_character}" encodeForJavaScript="true" /><f:verbatim>'), true);
         }
         else
         {
            return true;
         }
      }
   </script>
</f:verbatim>
<a:booleanEvaluator value="#{MetadataBlockBean.inEditMode}" id="addfile-docMetaInEditMode">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/metadata/web/metadata-block-lockRefresh.jsp" />
</a:booleanEvaluator>
