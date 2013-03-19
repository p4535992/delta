<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:outputText escape="false" value="#{SubstitutionBean.substitutionMessages}"/>
<h:outputText escape="false" value="#{AssignResponsibilityBean.assingResponsibilityMessage}"/>

<a:panel id="my-tasks-wrapper">
<a:panel id="assignment-tasks-panel" styleClass="panel-100 #{(MyTasksBean.assignmentPagerVisible) ? 'with-pager' : ''}" label="#{msg.task_list_assignment_title}" progressive="true" rendered="#{not empty MyTasksBean.assignmentTasks}">

   <%-- Main List --%>
   <a:richList id="assignment-task-list" viewMode="details" pageSize="#{MyTasksBean.pageSize}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.assignmentTasks}" var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />
      <a:booleanEvaluator value="#{MyTasksBean.assignmentPagerVisible}">
         <a:dataPager id="assignment-pager" styleClass="pager" />
      </a:booleanEvaluator>
   </a:richList>

</a:panel>

<a:panel id="group-assignment-tasks-panel" styleClass="panel-100 #{(MyTasksBean.groupAssignmentPagerVisible) ? 'with-pager' : ''}" label="#{msg.task_list_group_assignment_title}" progressive="true" rendered="#{not empty MyTasksBean.groupAssignmentTasks}">

   <%-- Main List --%>
   <a:richList id="group-assignment-task-list" viewMode="details" pageSize="#{MyTasksBean.pageSize}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.groupAssignmentTasks}" var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />
      <a:booleanEvaluator value="#{MyTasksBean.groupAssignmentPagerVisible}">
         <a:dataPager id="group-assignment-pager" styleClass="pager" />
      </a:booleanEvaluator>
   </a:richList>

</a:panel>

<a:panel id="information-tasks-panel" styleClass="panel-100 #{(MyTasksBean.informationPagerVisible) ? 'with-pager' : ''}" label="#{msg.task_list_information_title}" progressive="true" rendered="#{not empty MyTasksBean.informationTasks}">

   <%-- Main List --%>
   <a:richList id="information-task-list" viewMode="details" pageSize="#{MyTasksBean.pageSize}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.informationTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />

      <a:booleanEvaluator value="#{MyTasksBean.informationPagerVisible}">
         <a:dataPager id="information-pager" styleClass="pager" />
      </a:booleanEvaluator>
   </a:richList>

</a:panel>

<a:panel id="order-assignment-tasks-panel" styleClass="panel-100 #{(MyTasksBean.orderAssignmentPagerVisible) ? 'with-pager' : ''}" label="#{msg.task_list_order_assignment_title}" progressive="true" rendered="#{not empty MyTasksBean.orderAssignmentTasks}">

   <%-- Main List --%>
   <a:richList id="order-assignment-task-list" viewMode="details" pageSize="#{MyTasksBean.pageSize}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.orderAssignmentTasks}" var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />
      <a:booleanEvaluator value="#{MyTasksBean.orderAssignmentPagerVisible}">
         <a:dataPager id="order-assignment-pager" styleClass="pager" />
      </a:booleanEvaluator>
   </a:richList>

</a:panel>

<a:panel id="opinion-tasks-panel" styleClass="panel-100 #{(MyTasksBean.opinionPagerVisible) ? 'with-pager' : ''}" label="#{msg.task_list_opinion_title}" progressive="true" rendered="#{not empty MyTasksBean.opinionTasks}">

   <%-- Main List --%>
   <a:richList id="opinion-task-list" viewMode="details" pageSize="#{MyTasksBean.pageSize}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.opinionTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />
      <a:booleanEvaluator value="#{MyTasksBean.opinionPagerVisible}">
         <a:dataPager id="opinion-pager" styleClass="pager" />
      </a:booleanEvaluator>
   </a:richList>

</a:panel>

<a:panel id="review-tasks-panel" styleClass="panel-100 #{(MyTasksBean.reviewPagerVisible) ? 'with-pager' : ''}" label="#{msg.task_list_review_title}" progressive="true" rendered="#{not empty MyTasksBean.reviewTasks}">

   <%-- Main List --%>
   <a:richList id="review-task-list" viewMode="details" pageSize="#{MyTasksBean.pageSize}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.reviewTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-min-columns.jsp" />

      <a:booleanEvaluator value="#{MyTasksBean.reviewPagerVisible}">
         <a:dataPager id="review-pager" styleClass="pager" />
      </a:booleanEvaluator>
   </a:richList>

</a:panel>

<a:panel id="confirmation-tasks-panel" styleClass="panel-100 #{(MyTasksBean.confirmationPagerVisible) ? 'with-pager' : ''}" label="#{msg.task_list_confirmation_title}" progressive="true" rendered="#{not empty MyTasksBean.confirmationTasks}">

   <%-- Main List --%>
   <a:richList id="confirmation-task-list" viewMode="details" pageSize="#{MyTasksBean.pageSize}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.confirmationTasks}" var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />
      <a:booleanEvaluator value="#{MyTasksBean.confirmationPagerVisible}">
         <a:dataPager id="confirmation-pager" styleClass="pager" />
      </a:booleanEvaluator>
   </a:richList>

</a:panel>

<a:panel id="signature-tasks-panel" styleClass="panel-100 #{(MyTasksBean.signaturePagerVisible) ? 'with-pager' : ''}" label="#{msg.task_list_signature_title}" progressive="true"  rendered="#{not empty MyTasksBean.signatureTasks}">

   <%-- Main List --%>
   <a:richList id="signature-task-list" viewMode="details" pageSize="#{MyTasksBean.pageSize}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.signatureTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-min-columns.jsp" />

      <a:booleanEvaluator value="#{MyTasksBean.signaturePagerVisible}">
         <a:dataPager id="signature-pager" styleClass="pager" />
      </a:booleanEvaluator>
   </a:richList>

</a:panel>

<a:panel id="external-review-tasks-panel" styleClass="panel-100 #{(MyTasksBean.externalReviewPagerVisible) ? 'with-pager' : ''}" label="#{msg.task_list_external_review_title}" progressive="true" rendered="#{not empty MyTasksBean.externalReviewTasks}">

   <%-- Main List --%>
   <a:richList id="external-review-task-list" viewMode="details" pageSize="#{MyTasksBean.pageSize}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.externalReviewTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-min-columns.jsp" />

      <a:booleanEvaluator value="#{MyTasksBean.externalReviewPagerVisible}">
         <a:dataPager id="external-review-pager" styleClass="pager" />
      </a:booleanEvaluator>
   </a:richList>

</a:panel>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
