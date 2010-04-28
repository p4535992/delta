<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:booleanEvaluator value="#{empty CompoundWorkflowDialog.workflow}">
   <f:verbatim><div class="message"><strong></f:verbatim><h:outputText value="#{msg.workflow_compound_not_found}" /><f:verbatim></strong></div></f:verbatim>
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
</a:booleanEvaluator>

<a:booleanEvaluator value="#{not empty CompoundWorkflowDialog.workflow}">
   <%-- just a placeholder for dynamically generated panels --%>
   <h:panelGroup binding="#{CompoundWorkflowDialog.panelGroup}" />
   
   <a:booleanEvaluator value="#{CompoundWorkflowDialog.workflow.status == 'lõpetatud'}">
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
   </a:booleanEvaluator>
</a:booleanEvaluator>

<f:verbatim>
   <script type="text/javascript">
   // hide responsibleAssignee fields of assignment workflows, leaving only one, that is filled(or first). 
   // None is rendered by JSF if there exists task with responsibleAssignee
/*
   var ownerNames = $jQ(".recipient.tasks.showOne .ownerName");
   ownerNames.each(function (index, domEle) {
      alert("hide index="+index);
       if(index >0){
           var len = $jQ.trim($jQ(domEle).val()).length;
           if(len > 0){
               $jQ(ownerNames.get(0)).parent().parent().parent().parent().parent().parent().parent().addClass("ui-helper-hidden");
           } else {
               var tRow = $jQ(domEle).parent().parent().parent().parent().parent().parent().parent();
               tRow.addClass("ui-helper-hidden");
           }
       }
   });
*/
   </script>
</f:verbatim>
