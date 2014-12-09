<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<c:forEach items="${delegationHistoryBlockItems}" var="item" varStatus="loopStatus">
  <c:if test="${ loopStatus.index > 0 }">
    <hr />
  </c:if>

  <tag:valueRow labelId="delegation.history.task.creator" value="${ item.creatorName }" hideIfEmpty="true" styleClass="${ item.styleClass }" />
  <tag:valueRow labelId="delegation.history.task.mainOwner" value="${ item.mainOwner }" hideIfEmpty="true" styleClass="${ item.styleClass }" />
  <tag:valueRow labelId="delegation.history.task.coOwner" value="${ item.coOwner }" hideIfEmpty="true" styleClass="${ item.styleClass }" />
  <tag:valueRow labelId="delegation.history.task.resolution" value="${ item.resolution }" hideIfEmpty="true" styleClass="${ item.styleClass }" />
  <fmt:formatDate value="${item.dueDate}" pattern="dd.MM.yyyy HH:mm" var="dueDate" />
  <tag:valueRow labelId="delegation.history.task.dueDate" value="${dueDate}" hideIfEmpty="true" styleClass="${ item.styleClass }" />

</c:forEach>