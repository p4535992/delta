<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<%@page import="ee.webmedia.alfresco.functions.web.FunctionsDetailsDialog"%>
<%@page import="org.alfresco.web.app.servlet.FacesHelper"%>
<%@page import="javax.faces.context.FacesContext"%>
<a:panel id="metadata-panel" label="#{msg.functions_list}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="fn-metadata" value="#{DialogManager.bean.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" binding="#{FunctionsDetailsDialog.propertySheet}" />
</a:panel>
<f:verbatim>
<script type="text/javascript">

   function postProcessButtonState(){
      var status = "</f:verbatim><h:outputText value="#{FunctionsDetailsDialog.currentNode.properties['{http://alfresco.webmedia.ee/model/functions/1.0}status']}" /><f:verbatim>";
      processFnSerVolCaseCloseButton(status);
      processFnReopenButton(status);
   }

   function processFnReopenButton(status){
      var reopenBtn = $jQ("#"+escapeId4JQ("dialog:reopen-button"));
      var reopenBtn2 = $jQ("#"+escapeId4JQ("dialog:reopen-button-2"));
      if(status != "suletud"){
         reopenBtn.remove();
         reopenBtn2.remove();
      }
   }
</script>
</f:verbatim>
<%
   final boolean isNew = ((FunctionsDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "FunctionsDetailsDialog")).isNew();
   if(isNew) {
%>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-close-button.jsp" />
<%
   }
%>
