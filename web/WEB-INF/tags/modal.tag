<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<%@ attribute name="title" required="true" rtexprvalue="true" type="java.lang.String" description="Modal title" %>

<div id="screen" class="active" style="z-index: 100;">
	<div id="alert">
		<h2>${ title }</h2>
		<jsp:doBody />
	</div>
</div>