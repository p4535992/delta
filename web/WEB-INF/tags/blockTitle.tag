<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="Second level heading that can be used to toggle blocks"%>

<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ attribute name="title" required="false" rtexprvalue="true" type="java.lang.String" description="Container title that toggles collapsed state" %>
<%@ attribute name="titleId" required="false" rtexprvalue="true" type="java.lang.String" description="Container title that toggles collapsed state" %>
<%@ attribute name="target" required="true" rtexprvalue="true" type="java.lang.String" description="This is used to define the block ID and trigger link." %>
<%@ attribute name="titleDetails" required="false" rtexprvalue="true" type="java.lang.String" description="Is displayed in parenthises after title" %>
<%@ attribute name="expanded" required="false" rtexprvalue="true" type="java.lang.Boolean" description="Is the container expanded?" %>
<%@ attribute name="independent" required="false" rtexprvalue="true" type="java.lang.Boolean" description="Is the container expanded/collapsed independently e.g. are others collapsed when this is expanded?" %>
<%@ attribute name="url" required="false" rtexprvalue="true" type="java.lang.String" description="Heading link URL" %>

<c:if test="${url == null}">
   <c:set var="url" value="#" />
</c:if>

<c:if test="${empty title and not empty titleId}">
   <fmt:message var="title" key="${titleId}" />
</c:if>

<h2>
   <a class="trigger${expanded ? ' active' : ''}${independent ? ' independent' : ''}" href='<c:url value="${url}" />' data-target="${target}"><c:out value="${title}" /><c:if test="${not empty titleDetails}"> (<c:out value="${titleDetails}" />)</c:if></a>
</h2>