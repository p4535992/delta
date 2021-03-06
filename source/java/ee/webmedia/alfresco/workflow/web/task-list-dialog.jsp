<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil"%>

<a:panel id="tasks-panel" styleClass="panel-100 with-pager" label="#{MyTasksBean.listTitle}" progressive="true" rendered="#{not MyTasksBean.hidePrimaryList}">

   <%-- Main List --%>
    <a:richList id="taskList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
       width="100%" value="#{MyTasksBean.tasks}"  var="r" refreshOnBind="true" >
 
       <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />
 
       <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
       <a:dataPager id="pager1" styleClass="pager" />
    </a:richList>

</a:panel>

<a:panel id="additional-tasks-panel" styleClass="panel-100 with-pager" label="#{MyTasksBean.additionalListTitle}" progressive="true" rendered="#{MyTasksBean.containsAdditionalTasks}" >
   <%-- Additional List --%>
      <a:richList id="additionalTaskList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
         width="100%" value="#{MyTasksBean.additionalTasks}"  var="r" refreshOnBind="true" >
   
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />
   
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
         <a:dataPager id="pager1" styleClass="pager" />
      </a:richList> 

</a:panel>

<a:panel id="linkedreview-tasks-panel" styleClass="panel-100 with-pager" label="#{msg.task_list_linked_review_title}" progressive="true" rendered="#{MyTasksBean.linkedReviewTaskEnabled}" >
   <%-- LinkedView List --%>
   <a:richList id="linkedReviewTaskList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.linkedReviewTasks}"  var="r" refreshOnBind="true" >
   
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/linkedtask-list-dialog-columns.jsp" />
   
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pagerLinked1" styleClass="pager" />
   </a:richList>   
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />