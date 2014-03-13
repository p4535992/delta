<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8"
   description="Displays a message substring with given length and concatenates '…' if needed. Defaults to 150 chars."%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%@ attribute name="message" required="false" rtexprvalue="true" type="java.lang.String" description="Message that should be displayed."%>
<%@ attribute name="messageId" required="false" rtexprvalue="true" type="java.lang.String" description="Translation key that should be displayed."%>
<%@ attribute name="maxLength" required="false" rtexprvalue="true" type="java.lang.Integer" description="Maximum length of the message to be displayed. Defaults to 150."%>

<c:if test="${maxLength == null}">
   <c:set var="maxLength" value="150" />
</c:if>

<c:if test="${empty message and not empty messageId}">
   <fmt:message key="${messageId}" var="message" />
</c:if>

<c:if test="${fn:length(message) gt maxLength}">
   <c:set var="message" value="${fn:substring(message, 0, maxLength)}…" />
</c:if>

<c:out value="${message}" />
<jsp:doBody />
