<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:outputText escape="false" value="#{SubstitutionBean.substitutionMessages}"/>

<a:panel id="assignment-tasks-panel" styleClass="panel-100 with-pager" label="#{msg.task_list_assignment_title}" progressive="true" rendered="#{not empty MyTasksBean.assignmentTasks}">

   <%-- Main List --%>
   <a:richList id="assignment-task-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.assignmentTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="assignment-pager" styleClass="pager" />
   </a:richList>

</a:panel>

<a:panel id="information-tasks-panel" styleClass="panel-100 with-pager" label="#{msg.task_list_information_title}" progressive="true" rendered="#{not empty MyTasksBean.informationTasks}">

   <%-- Main List --%>
   <a:richList id="information-task-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.informationTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="information-pager" styleClass="pager" />
   </a:richList>

</a:panel>

<a:panel id="opinion-tasks-panel" styleClass="panel-100 with-pager" label="#{msg.task_list_opinion_title}" progressive="true" rendered="#{not empty MyTasksBean.opinionTasks}">

   <%-- Main List --%>
   <a:richList id="opinion-task-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.opinionTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="opinion-pager" styleClass="pager" />
   </a:richList>

</a:panel>

<a:panel id="review-tasks-panel" styleClass="panel-100 with-pager" label="#{msg.task_list_review_title}" progressive="true" rendered="#{not empty MyTasksBean.reviewTasks}">

   <%-- Main List --%>
   <a:richList id="review-task-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.reviewTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-min-columns.jsp" />

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="review-pager" styleClass="pager" />
   </a:richList>

</a:panel>

<a:panel id="signature-tasks-panel" styleClass="panel-100 with-pager" label="#{msg.task_list_signature_title}" progressive="true"  rendered="#{not empty MyTasksBean.signatureTasks}">

   <%-- Main List --%>
   <a:richList id="signature-task-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.signatureTasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-min-columns.jsp" />

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="signature-pager" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
