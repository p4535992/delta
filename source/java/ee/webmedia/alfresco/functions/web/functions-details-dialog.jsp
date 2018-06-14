<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@page import="ee.webmedia.alfresco.functions.model.FunctionsModel"%>
<%@page import="ee.webmedia.alfresco.functions.web.FunctionsDetailsDialog"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<a:panel id="metadata-panel" label="#{msg.functions_list}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="fn-metadata" value="#{DialogManager.bean.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" binding="#{FunctionsDetailsDialog.propertySheet}" />
</a:panel>
<f:verbatim>
<script type="text/javascript">

   function postProcessButtonState(){
      var status = '<%= StringEscapeUtils.escapeJavaScript((String)BeanHelper.getFunctionsDetailsDialog().getCurrentNode().getProperties().get(FunctionsModel.Props.STATUS)) %>';
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
      
      function fixStatusDefaultValue() {
   	   var status = document.getElementById("dialog:dialog-body:fn-metadata:prop_fnx003a_status:fnx003a_status");
   	   var statusValue = status.options[status.selectedIndex].value;
   	   if(statusValue == ""){
   		   statusValue = "avatud";
   		   status.options[0].removeAttribute("selected");
   		   status.options[1].setAttribute("selected","selected");
   	   }
      }
      window.onload = fixStatusDefaultValue;
   }
   
</script>
</f:verbatim>
<%
   final boolean isNew = BeanHelper.getFunctionsDetailsDialog().isNew();
   if(isNew) {
%>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-close-button.jsp" />
<%
   }
%>
