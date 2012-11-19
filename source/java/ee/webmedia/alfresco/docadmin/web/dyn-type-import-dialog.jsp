<%@ page import="ee.webmedia.alfresco.docadmin.service.DynamicType"%>
<%@ page import="ee.webmedia.alfresco.docadmin.web.DynamicTypesImportDialog"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ page import="javax.faces.application.ViewHandler"%>
<%@ page import="javax.faces.context.ExternalContext"%>
<%@ page import="javax.faces.context.FacesContext"%>
<%@ page import="ee.webmedia.alfresco.docadmin.web.DocumentTypesImportDialog"%>
<%@ page import="ee.webmedia.alfresco.common.web.AbstractImportDialog"%>
<%@ page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator"%>
<%@ page import="ee.webmedia.alfresco.document.file.web.AddFileDialog"%>

<%!
   private String getMethodBinding(String dynamicTypesImportDialogBeanName, String methodName){
       return "#{"+dynamicTypesImportDialogBeanName+"."+methodName+"}";
   }
%>

<%
   String dynamicTypesImportDialogBeanName = (String) request.getAttribute(DynamicTypesImportDialog.DYNAMIC_TYPES_IMPORT_DIALOG_BEAN_NAME);
   boolean fileUploaded = false;
   @SuppressWarnings("unchecked")
   DynamicTypesImportDialog<DynamicType> dialog = BeanHelper.getSpringBean(DynamicTypesImportDialog.class,dynamicTypesImportDialogBeanName);
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
   if (!fileUploaded) {
%>
      <a:panel styleClass="panel-100" id="file-upload" label="#{msg.upload_content}">
         <h:panelGrid id="upload_panel" columns="2" cellpadding="2" cellspacing="2" border="0" width="100%"
            columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">
            <h:outputText id="out_schema" value="#{msg.file_location}:" style="padding-left:8px" />
            <r:upload id="uploader" value='<%=getMethodBinding(dynamicTypesImportDialogBeanName, "fileName")%>' framework="dialog" />
         </h:panelGrid>
      </a:panel>
<%
   }
%>
