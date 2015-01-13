<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:booleanEvaluator value="#{empty CompoundWorkflowDialog.workflow}">
   <f:verbatim><div class="message"><strong></f:verbatim><h:outputText value="#{msg.workflow_compound_not_found}" /><f:verbatim></strong></div></f:verbatim>
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
</a:booleanEvaluator>

<a:booleanEvaluator id="compound-workflow-panel-group-evaluator" value="#{not empty CompoundWorkflowDialog.workflow}">
   <%-- just a placeholder for dynamically generated panels --%>
   <wm:ajaxCapablePanelGroupTag id="compound-workflow-panel-group" binding="#{CompoundWorkflowDialog.panelGroup}" />
   
   <a:booleanEvaluator value="#{CompoundWorkflowDialog.workflow.status == 'lõpetatud'}">
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
   </a:booleanEvaluator>
</a:booleanEvaluator>

<f:verbatim>
   <script type="text/javascript">
      prependOnclick($jQ(".compoundWorkflowStart"), function(){
         propSheetFinishBtnPressed = true;
         return propSheetValidateSubmit();
      });
      // override minimum screen width for compound workflow screen
      var minWidth = 1220;
      $jQ("#wrapper").css("min-width", minWidth + "px");
      setMinScreenWidth(minWidth);
   </script>
</f:verbatim>
