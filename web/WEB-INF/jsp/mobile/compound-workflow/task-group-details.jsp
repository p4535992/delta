<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tag:html>

	<h2><fmt:message key="workflow.task.group.title" /></h2>
	<h3><c:out value="${ groupName }" /></h3>
	<jsp:include page="/WEB-INF/jsp/mobile/compound-workflow/workflow-block-content.jsp" />
	
</tag:html>