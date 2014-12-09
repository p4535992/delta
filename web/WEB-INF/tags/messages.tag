<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@ attribute name="messages" rtexprvalue="true" type="java.util.Map"%>

<c:set var="messageMap" value="${messages}"/>
<c:if test="${not empty messageMap }">
	<div id="mainNotifications" class="notifications" >
		<c:if test="${not empty messageMap['ERROR']}">
			<c:forEach items="${messageMap['ERROR']}" var="item">
				<p class="error"><c:out value="${item.message}" /></p>
			</c:forEach>
		</c:if>
		<c:if test="${not empty messageMap['WARN']}">
			<c:forEach items="${messageMap['WARN']}" var="item">
				<p class="warning"><c:out value="${item.message}" /></p>
			</c:forEach>
		</c:if>	
		<c:if test="${not empty messageMap['INFO']}">
			<c:forEach items="${messageMap['INFO']}" var="item">
				<p class="confirmation"><c:out value="${item.message}" /></p>
			</c:forEach>
		</c:if>
        <c:if test="${not empty messageMap['NEUTRAL']}">
            <c:forEach items="${messageMap['NEUTRAL']}" var="item">
                <p class="neutral"><c:out value="${item.message}" />
                   <c:if test="${ not empty item.action }">
                      <input type="hidden" value="${item.action.href}"></input>
                      <a class="${item.action.elementClass}" href="javascript:void(0)">${item.action.label}</a>
                   </c:if>
                </p>
            </c:forEach>
        </c:if>
	</div>
</c:if>