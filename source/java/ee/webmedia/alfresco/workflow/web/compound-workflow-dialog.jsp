<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:booleanEvaluator id="empty-workflow-messages-evaluator" value="#{CompoundWorkflowDialog.showEmptyWorkflowMessage}">
   <f:verbatim><div class="message"><strong></f:verbatim><h:outputText value="#{msg.workflow_compound_not_found}" /><f:verbatim></strong></div></f:verbatim>
</a:booleanEvaluator>
<a:booleanEvaluator id="empty-workflow-evaluator" value="#{empty CompoundWorkflowDialog.workflow}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
</a:booleanEvaluator>

<h:panelGroup rendered="#{not empty CompoundWorkflowDialog.workflow}">

   <h:panelGroup id="dialog-modal-container" binding="#{CompoundWorkflowDialog.modalContainer}" />
   <h:panelGroup rendered="#{CompoundWorkflowDialog.modalRendered}">
   	<!-- Ensure that CompoundWorkflowDialog.renderedModal is called only once because it sets modal id = null in it's finally block-->
      <f:verbatim>
         <script type="text/javascript">
            $jQ(document).ready(function () {
               var modalId = "</f:verbatim><a:outputText value="#{CompoundWorkflowDialog.renderedModal}" /><f:verbatim>";
               showModal(modalId);
               initExpanders($jQ("#" + modalId));
            });
         </script>
      </f:verbatim>
   </h:panelGroup>

   <h:panelGroup rendered="#{CompoundWorkflowDialog.workflow.independentWorkflow}">
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-block.jsp" />
   </h:panelGroup>

   <h:panelGroup binding="#{CompoundWorkflowDialog.commonDataGroup}" />

   <h:panelGroup rendered="#{CompoundWorkflowDialog.workflow.independentWorkflow}">   
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/compound-workflow-assoc-list-block.jsp" />
      <h:panelGroup rendered="#{CompoundWorkflowAssocSearchBlock.expanded}">
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-search-block.jsp" />
      </h:panelGroup>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/related-url-list-block.jsp" />      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-summary-block.jsp" />
   </h:panelGroup>
   
   <wm:ajaxCapablePanelGroupTag id="compound-workflow-panel-group" binding="#{CompoundWorkflowDialog.panelGroup}" />
   
   <h:panelGroup rendered="#{CompoundWorkflowDialog.workflow.independentWorkflow}">
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/review-note-block.jsp" />
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/opinion-note-block.jsp" />
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/order-assignment-note-block.jsp" />
   </h:panelGroup> 
   
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/log/web/document-log-block.jsp" />
   <h:panelGroup id="workflow-saveas-panel" binding="#{CompoundWorkflowDialog.saveAsGroup}" rendered="#{CompoundWorkflowDialog.showSaveAsGroup}"/>
      
   <a:booleanEvaluator id="finished-workflow-evaluator" value="#{CompoundWorkflowDialog.workflow.status == 'lõpetatud'}">
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
   </a:booleanEvaluator>
</h:panelGroup>

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
      
      function printCompoundWorkflow(){
        window.open("<%= BeanHelper.getWorkflowBlockBean().getCompoundWorkflowPrintUrl() %>", "_blank");
      	return false;
      }
   </script>
</f:verbatim> 
