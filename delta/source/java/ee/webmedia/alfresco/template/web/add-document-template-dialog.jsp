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
<%@ page import="ee.webmedia.alfresco.template.web.AddDocumentTemplateDialog"%>
<%@ page import="javax.faces.application.FacesMessage"%>
<%@ page import="org.apache.commons.io.FilenameUtils"%>

<%
    boolean fileUploaded = false;
    boolean wrongFormat = false;
    FacesContext context = FacesContext.getCurrentInstance();

    AddDocumentTemplateDialog dialog = (AddDocumentTemplateDialog) FacesHelper.getManagedBean(context, "AddDocumentTemplateDialog");
    if (dialog != null && dialog.getFileName() != null) {
        fileUploaded = true;
    }

    if (fileUploaded == true) {
        String name = dialog.getFileName();
        // FIXME check conditions!
        if (!dialog.isEmailTemplate() && !dialog.isSystemTemplate() && !FilenameUtils.getExtension(name).equalsIgnoreCase("dot") && !FilenameUtils.getExtension(name).equalsIgnoreCase("dotx") ||
             (dialog.isEmailTemplate() || dialog.isSystemTemplate()) && !FilenameUtils.getExtension(name).equalsIgnoreCase("htm") && !FilenameUtils.getExtension(name).equalsIgnoreCase("html")) {
            fileUploaded = false;
            wrongFormat = true;
        }
    }

    if (fileUploaded && dialog.isFirstLoad()) {
        dialog.updateNodeFileName(dialog.getFileName());
        dialog.setFirstLoad(false);
    }
%>
<f:verbatim>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/validation.js"></script>

<%
    if (wrongFormat) {
            PanelGenerator.generatePanelStart(out, request.getContextPath(), "message", "#ffffcc");
            out.write("<img alt='' align='absmiddle' src='");
            out.write(request.getContextPath());
            out.write("/images/icons/info_icon.gif' />&nbsp;&nbsp;");
            if (dialog.isEmailTemplate() || dialog.isSystemTemplate()) {
                out.write(Application.getBundle(context).getString("template_wrong_file_format_email"));
            } else {
                out.write(Application.getBundle(context).getString("template_wrong_file_format_doc"));
            }
            PanelGenerator.generatePanelEnd(out, request.getContextPath(), "yellowInner");
            out.write("<div style='padding:2px;'></div>");
        }

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

<%
    }
    if (fileUploaded) {
%>
   <a:panel styleClass="column panel-90" id="file-upload" label="#{msg.uploaded_content}">
   
      <h:panelGroup>
         <a:actionLink image="/images/icons/delete.gif" value="#{msg.remove}" action="#{DialogManager.bean.removeUploadedFile}" showLink="false" id="link1" />
         <h:outputText id="text1" value="#{DialogManager.bean.fileName}" styleClass="dialogpanel-title filename" />
      </h:panelGroup>
   
      <h:outputText id="text2" value="#{msg.general_properties}" styleClass="dialogpanel-title" />
      <r:propertySheetGrid value="#{DialogManager.bean.docTemplateNode}" mode="edit" labelStyleClass="propertiesLabel" externalConfig="true" columns="1" />
   
   </a:panel>
<%
    }
%>
   <f:verbatim>

</f:verbatim>