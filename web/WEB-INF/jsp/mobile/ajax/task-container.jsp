<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:forEach items="${containers}" var="container">
    <tag:taskBlock title="${container.value.title}" tasks="${container.value.tasks}" expanded="${container.value.expanded}" target="${container.value.target}"  count="${container.value.count}" />
</c:forEach>