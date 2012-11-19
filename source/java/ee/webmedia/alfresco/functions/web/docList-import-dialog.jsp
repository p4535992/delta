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

    DocumentListImportDialog dialog = (DocumentListImportDialog) FacesHelper
            .getManagedBean(FacesContext.getCurrentInstance(), "DocumentListImportDialog");
    if (dialog != null && dialog.getFileName() != null) {
        fileUploaded = true;
    }
%>

   
<%@page import="ee.webmedia.alfresco.functions.web.DocumentListImportDialog"%>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/validation.js"> </script>
<f:verbatim>

   <%
       if (fileUploaded) {
               PanelGenerator.generatePanelStart(out, request
                       .getContextPath(), "message", "#ffffcc");
               out.write("<img alt='' align='absmiddle' src='");
               out.write(request.getContextPath());
               out.write("/images/icons/info_icon.gif' />&nbsp;&nbsp;");
               out.write(dialog.getFileUploadSuccessMsg());
               PanelGenerator.generatePanelEnd(out, request
                       .getContextPath(), "yellowInner");
               out.write("<div style='padding:2px;'></div>");
           }
   %>

</f:verbatim>
<%
    if (!fileUploaded) {
%>

<a:panel styleClass="column panel-100" id="file-upload" label="#{msg.upload_content}">
   <h:panelGrid id="upload_panel" columns="2" cellpadding="2" cellspacing="2" border="0" width="100%"
      columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">
      <h:outputText id="out_schema" value="#{msg.file_location}:" style="padding-left:8px" />
      <r:upload id="uploader" value="#{DocumentListImportDialog.fileName}" framework="dialog"  />
   </h:panelGrid>
</a:panel>

<%
    } else {
%>

<a:panel styleClass="column panel-90" id="docList-import" label="#{msg.docList_import_overview}">

   <h:panelGroup>
      <a:actionLink image="/images/icons/delete.gif" value="#{msg.remove}" action="#{DocumentListImportDialog.reset}" showLink="false" id="link1" />
      <h:outputText id="text3" value="#{DocumentListImportDialog.fileName}" styleClass="dialogpanel-title filename" />
   </h:panelGroup>
</a:panel>

<f:verbatim>
   <script type="text/javascript">
      document.getElementById("dialog:finish-button").disabled = false;
   </script>
</f:verbatim>
<%
    }
%>
