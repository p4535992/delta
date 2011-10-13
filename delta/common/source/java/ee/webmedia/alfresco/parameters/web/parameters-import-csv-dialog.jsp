<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
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
<%@ page import="ee.webmedia.alfresco.parameters.web.ParametersImportDialog"%>

<%
    boolean fileUploaded = false;
    ParametersImportDialog dialog = BeanHelper.getParametersImportDialog();
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
      <r:upload id="uploader" value="#{ParametersImportDialog.fileName}" framework="dialog" />
   </h:panelGrid>
</a:panel>

<%
    } else {
%>

<a:panel styleClass="panel-100" id="prameters-compare" label="#{msg.parameter_import_csv_overview}">
   <h:dataTable id="paramImportList" value="#{ParametersImportDialog.importableParams}" var="p" width="100%" rowClasses="recordSetRow,recordSetRowAlt">
<%-- millegi pärast ei saanud tööle koos paginaatoriga


<a:panel styleClass="column panel-90 atribuudis with-pager" id="prameters-compare" label="#{msg.parameter_import_csv_overview}">
<a:panel id="parameters-panel" label="#{msg.parameters_list}" styleClass="atribuudis with-pager">
   <a:richList id="paramImportList" value="#{ParametersImportDialog.importableParams}" var="p" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" >
 --%>

      <h:column id="paramName">
         <f:facet name="header">
            <h:outputText id="paramName-header" value="#{msg.parameters_name}" />
         </f:facet>
         <h:outputText value="#{p.paramName}" styleClass="#{p.status == 'PARAM_NEW' ? 'error' : ''}" />
      </h:column>

      <h:column id="paramStatus">
         <f:facet name="header">
            <h:outputText id="paramStatus-header" value="#{msg.parameters_status}" />
         </f:facet>
         <h:outputText value="#{msg[p.status]}" styleClass="#{p.status == 'PARAM_NEW' ? 'error' : ''}" />
      </h:column>

      <h:column id="paramVal">
         <f:facet name="header">
            <h:outputText id="paramVal-header" value="#{msg.parameter_import_newValue}" />
         </f:facet>
         <h:outputText value="#{p.paramValue}" styleClass="#{p.status == 'PARAM_NEW' ? 'error' : ''}" />
      </h:column>

      <h:column id="previousParamValue">
         <f:facet name="header">
            <h:outputText id="previousParamValue-header" value="#{msg.parameters_currentValue}" />
         </f:facet>
         <h:outputText value="#{p.previousParamValue}" styleClass="#{p.status == 'PARAM_NEW' ? 'error' : ''}" />
      </h:column>

<%--
      <a:dataPager id="pager1" styleClass="pager" />
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
   </a:richList>
 --%>
   </h:dataTable>
</a:panel>

<a:panel styleClass="panel-100" id="parameters-import" label="#{msg.parameter_import_csv_import_file}">

   <h:panelGroup>
      <a:actionLink image="/images/icons/delete.gif" value="#{msg.remove}" action="#{ParametersImportDialog.reset}" showLink="false" id="link1" />
      <h:outputText id="text3" value="#{ParametersImportDialog.fileName}" styleClass="dialogpanel-title filename" />
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
</f:verbatim>
<%
    }
%>
