<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@ attribute name="messages" rtexprvalue="true" type="java.util.Map"%>

<c:set var="messageMap" value="${messages}"/>
<c:if test="${not empty messageMap }">
	<div class="notifications" >
		<c:if test="${not empty messageMap['ERROR']}">
			<c:forEach items="${messageMap['ERROR']}" var="msg">
				<p class="error"><c:out value="${msg}" /></p>
			</c:forEach>
		</c:if>
		<c:if test="${not empty messageMap['WARN']}">
			<c:forEach items="${messageMap['WARN']}" var="msg">
				<p class="warning"><c:out value="${msg}" /></p>
			</c:forEach>
		</c:if>	
		<c:if test="${not empty messageMap['INFO']}">
			<c:forEach items="${messageMap['INFO']}" var="msg">
				<p class="confirmation"><c:out value="${msg}" /></p>
			</c:forEach>
		</c:if>
	</div>
</c:if>