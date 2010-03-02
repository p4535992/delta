<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- This JSP is used from multiple dialogs, that's why DialogManager.bean reference is used --%>
<a:panel id="tasks-panel" styleClass="panel-100" label="#{MyTasksBean.listTitle}" progressive="true">

   <%-- Main List --%>
   <a:richList id="taskList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MyTasksBean.tasks}"  var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/task-list-dialog-columns.jsp" />

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />