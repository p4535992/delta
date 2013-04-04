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

        ClassificatorsImportDialog dialog = (ClassificatorsImportDialog) FacesHelper
                .getManagedBean(FacesContext.getCurrentInstance(), "ClassificatorsImportDialog");
        if (dialog != null && dialog.getFileName() != null) {
            fileUploaded = true;
        }
%>

   <script type="text/javascript" src="<%=request.getContextPath()%>/scripts/validation.js"> </script>
<%@ page import="ee.webmedia.alfresco.classificator.web.ClassificatorsImportDialog"%><f:verbatim>

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

<a:panel styleClass="panel-100" id="file-upload" label="#{msg.upload_content}">
   <h:panelGrid id="upload_panel" columns="2" cellpadding="2" cellspacing="2" border="0" width="100%"
      columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">
      <h:outputText id="out_schema" value="#{msg.file_location}:" style="padding-left:8px" />
      <r:upload id="uploader" value="#{ClassificatorsImportDialog.fileName}" framework="dialog" />
   </h:panelGrid>
</a:panel>

<%
    } else {
%>

<a:panel styleClass="panel-100" id="prameters-compare" label="#{msg.classificators_import_overview}">
   <h:dataTable id="paramImportList" value="#{ClassificatorsImportDialog.importableClassificators}" var="p" width="100%" rowClasses="recordSetRow,recordSetRowAlt">
      <h:column id="name">
         <f:facet name="header">
            <h:outputText id="name-header" value="#{msg.classificator_name}" />
         </f:facet>
         <h:outputText value="#{p.name}" styleClass="#{p.status == 'classificators_import_status_addClassificator' ? 'error' : ''}" />
      </h:column>

      <h:column id="classifStatus">
         <f:facet name="header">
            <h:outputText id="classifStatus-header" value="#{msg.classificator_status}" />
         </f:facet>
         <h:outputText value="#{msg[p.status]}" styleClass="#{p.status == 'classificators_import_status_addClassificator' ? 'error' : ''}" />
      </h:column>

   </h:dataTable>
</a:panel>

<a:panel styleClass="panel-100" id="classificators-import" label="#{msg.classificators_import_file}">

   <h:panelGroup>
      <a:actionLink image="/images/icons/delete.gif" value="#{msg.remove}" action="#{ClassificatorsImportDialog.reset}" showLink="false" id="link1" />
      <h:outputText id="text3" value="#{ClassificatorsImportDialog.fileName}" styleClass="dialogpanel-title filename" />
   </h:panelGroup>
</a:panel>

<f:verbatim>
   <script type="text/javascript">
   $jQ(document).ready(function() {
	      document.getElementById("dialog:finish-button").disabled = false;
	      var altButton = document.getElementById("dialog:finish-button-2");
	      if(altButton != null) {
	          altButton.disabled = false;
	      }
	   });
	   </script>
   </script>
</f:verbatim>
<%
    }
%>
