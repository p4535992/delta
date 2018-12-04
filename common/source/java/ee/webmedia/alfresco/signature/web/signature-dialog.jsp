<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel label="#{msg.ddoc_file_name}" id="filename-editable-panel" facetsId="workflow-panel-facets" progressive="true"
   rendered="#{DialogManager.bean.selectFilename}">
   <table cellpadding="0" cellspacing="3" border="0" width="100%">
      <tr>
         <td width="100%" valign="top"><h:panelGrid id="filename_panel" columns="1" cellpadding="2" cellspacing="2" border="0" width="100%"
            columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">

            <h:outputText value="#{msg.ddoc_file_new}" style="padding-left:8px" />
            <h:inputText id="filename" value="#{DialogManager.bean.filename}" />
         </h:panelGrid></td>
      </tr>
   </table>
</a:panel>

<a:panel label="#{msg.ddoc_file_name}" id="filename-panel" facetsId="workflow-panel-facets" progressive="true" rendered="#{!DialogManager.bean.selectFilename}">
   <table cellpadding="0" cellspacing="3" border="0" width="100%">
      <tr>
         <td width="100%" valign="top"><h:panelGrid id="filename_panel" columns="1" cellpadding="2" cellspacing="2" border="0" width="100%"
            columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">

            <h:outputText value="#{DialogManager.bean.filename}" style="padding-left:8px" />
         </h:panelGrid></td>
      </tr>
   </table>
</a:panel>

<a:panel label="#{msg.ddoc_file_list}" id="file-select-panel" facetsId="workflow-panel-facets" progressive="true" rendered="#{DialogManager.bean.editMode}">
   <h:selectManyCheckbox id="select-files" layout="pageDirection" value="#{DialogManager.bean.selectedItems}">
      <f:selectItems value="#{DialogManager.bean.allItems}" />
   </h:selectManyCheckbox>
</a:panel>

<%@ include file="signature-file-panel.jsp"%>

<a:panel label="#{msg.ddoc_signature_applet}" id="applet-panel" facetsId="workflow-panel-facets" rendered="#{!DialogManager.bean.error}">

   <h:commandLink action="#{DialogManager.bean.processCert}" id="processCert" style="display: none">
      <f:param id="processCert-param" name="cert" value="" />
   </h:commandLink>
   <h:commandLink action="#{DialogManager.bean.signDocument}" id="signDocument" style="display: none">
      <f:param id="signDocument-param" name="signature" value="" />
   </h:commandLink>

   <f:verbatim>
      <script>  				
           function processCert(cert, token) {
	           return oamSubmitForm('dialog','dialog:dialog-body:processCert',null,[['cert', cert]]);
           }

           function signDocument(signature) {	
               return oamSubmitForm('dialog','dialog:dialog-body:signDocument',null,[['signature', signature]]);
           }
           
           function cancelDlg() {
               document.getElementById('dialog:cancel-button').click();
           }
           
           function driverError() {
           }
      </script>

      <div id="signature-div" style="text-align: center">
      </div>
   </f:verbatim>
</a:panel>