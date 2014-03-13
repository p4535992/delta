<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="Generates a task item that can be used in lists" %>

<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ attribute name="task" required="true" rtexprvalue="true" type="ee.webmedia.mobile.alfresco.workflow.model.Task" description="Task to be displayed" %>

<article>
   <a class="articlelink${not task.viewedByOwner ? ' unread' : ''}" href='<c:url value="/m/compound-workflow/details/${task.compoundWorkflowRef.id}" />'>
      <h3><tag:limitMessage message="${task.title}" /></h3>
      <c:if test="${not empty task.kind}"><p class="documentkind">${task.kind}</p></c:if>
      <p class="deadline<c:out value="${task.overDue ? ' urgent' : ''}" />"><fmt:formatDate value="${task.dueDate}" pattern="dd MMM yy" /></p>
      <c:if test="${not empty task.resolution}" ><p><fmt:message key="workflow.task.resolution" />: <tag:limitMessage message="${task.resolution}" /></p></c:if>
      <p><fmt:message key="workflow.task.creator" />: <tag:limitMessage message="${task.creatorName}" /></p>
      <c:if test="${not empty task.senderName}"><p><fmt:message key="workflow.task.sender" />: <tag:limitMessage message="${task.senderName}" /></p></c:if>
   </a>
</article>