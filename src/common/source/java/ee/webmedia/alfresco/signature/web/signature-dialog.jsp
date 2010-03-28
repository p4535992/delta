<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
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
      <object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" width="400" height="80"
         codebase="https://java.sun.com/update/1.6.0/jinstall-6u18-windows-i586.cab#Version=1,4,0,0" id="signApplet">
         <param name="java_code" value="SignApplet.class" />
         <param name="java_codebase" value="<%=request.getContextPath()%>/applet" />
         <param name="java_archive" value="SignApplet_sig.jar, iaikPkcs11Wrapper_sig.jar" />
         <param name="name" value="SignApplet1" />
         <param name="mayscript" value="true" />
         <param name="java_type" value="application/x-java-applet;version=1.4" />
         <param name="scriptable" value="true" />
         <param name="LANGUAGE" value="EST" />
         <param name="FUNC_SET_CERT" value="window.processCert" />
         <param name="FUNC_SET_SIGN" value="window.signDocument" />
         <param name="FUNC_CANCEL" value="window.cancelDlg" />
         <param name="FUNC_DRIVER_ERR" value="window.driverError" />
         <param name="DEBUG_LEVEL" value="3" />
         <param name="OPERATION"
            value="<%if (session.getAttribute("operation") != null)
                           out.write((String) session.getAttribute("operation"));%>" />
         <param name="HASH"
            value="<%if (session.getAttribute("digest") != null)
                           out.write((String) session.getAttribute("digest"));%>" />
         <param name="TOKEN_ID" value="" />

         <embed id="signApplet" type="application/x-java-applet;version=1.4" width="400" height="80"
            pluginspage="http://javadl.sun.com/webapps/download/GetFile/1.6.0_18-b07/windows-i586/xpiinstall.exe" java_code="SignApplet.class"
            java_codebase="<%=request.getContextPath()%>/applet" java_archive="SignApplet_sig.jar, iaikPkcs11Wrapper_sig.jar" NAME="SignApplet" MAYSCRIPT="true"
            LANGUAGE="EST" FUNC_SET_CERT="window.processCert" FUNC_SET_SIGN="window.signDocument" FUNC_CANCEL="window.cancelDlg"
            FUNC_DRIVER_ERR="window.driverError" DEBUG_LEVEL="3"
            OPERATION="<%if (session.getAttribute("operation") != null)
                               out.write((String) session.getAttribute("operation"));%>"
            HASH="<%if (session.getAttribute("digest") != null)
                          out.write((String) session.getAttribute("digest"));%>" TOKEN_ID="">
         <noembed></noembed>
         </embed></object>
      </div>
   </f:verbatim>
</a:panel>