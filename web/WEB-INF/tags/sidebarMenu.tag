<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="Renders sidebar menu."%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<%@ attribute name="entries" required="true" rtexprvalue="true" type="java.util.List" description="MenuEntry items to be rendered."%>

<c:if test="${not empty entries}">
    <nav class="sidebar">
        <ul class="sidebarcontent">
            <c:forEach items="${entries}" var="entry">
                <li>
                    <c:choose>
                        <c:when test="${entry.topLevel}">
                            <h2><c:out value="${entry.title}" /></h2>
                        </c:when>
                        <c:otherwise>
                            <a href='<c:url value="/m/${entry.target}" />'><c:out value="${entry.title}" /> <c:if test="${not empty entry.details}"> (<c:out value="${entry.details}" />)</c:if></a>
                        </c:otherwise>
                    </c:choose>
                    <c:forEach items="${entry.subItems}" var="sub">
                        <li><a href='<c:url value="/m/${sub.target}" />'><c:out value="${sub.title}" /> <c:if test="${not empty sub.details}"> (<c:out value="${sub.details}" />)</c:if></a></li>
                    </c:forEach>
                </li>
            </c:forEach>
        </ul>
    </nav>
</c:if>