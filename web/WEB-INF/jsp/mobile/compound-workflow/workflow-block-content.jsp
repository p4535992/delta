<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

   <c:forEach items="${workflowBlockItems}" var="item" varStatus="loopStatus">
   	  <c:if test="${ loopStatus.index > 0 }" >
      	<hr/>	   	  
   	  </c:if>
   	  <fmt:formatDate value="${item.startedDateTime}" pattern="dd.MM.yyyy HH:mm" var="startedDateTime" />
      <tag:valueRow labelId="workflow.task.startedDateTime" value="${startedDateTime}" hideIfEmpty="true" />
      <fmt:formatDate value="${item.dueDate}" pattern="dd.MM.yyyy HH:mm" var="dueDate" />
      <tag:valueRow labelId="workflow.task.dueDate" value="${dueDate}" hideIfEmpty="true" />
      <tag:valueRow labelId="workflow.task.workflow" value="${item.workflowType }" hideIfEmpty="true" />
      <c:if test="${ item.groupBlockItem }" >
   		<tag:valueRow labelId="workflow.task.ownerName" value="${item.groupName}" hideIfEmpty="true" href="/m/compound-workflow/task-group-details/${item.compoundWorkflowId}/${item.firstTaskId}" />
      </c:if>
      <c:if test="${ !item.groupBlockItem }" >
      	<tag:valueRow labelId="workflow.task.ownerName" value="${item.taskOwnerName}" hideIfEmpty="true" />
      	<tag:readMore content="${ item.taskOutcomeWithSubstituteNote }" threshold="150" useTagContent="true" >
      		<tag:valueRow labelId="workflow.task.outcome" value="${item.taskOutcomeWithSubstituteNote}" hideIfEmpty="true" readMore="150" escape="false" />
      	</tag:readMore>
      </c:if>
      <tag:valueRow labelId="workflow.task.status" value="${item.taskStatus}" hideIfEmpty="true" />
   </c:forEach>