<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8"
   description="Generates pager for lists. Pages must be rendered as DIV elements ehere only active page is currently visible."%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<%@ attribute name="pages" required="true" rtexprvalue="true" type="java.lang.Integer" description="Number of page elements to generate."%>
<%@ attribute name="listId" required="true" rtexprvalue="true" type="java.lang.String" description="List wrapper ID."%>
<%@ attribute name="currentPage" required="false" rtexprvalue="true" type="java.lang.Integer" description="Currently active page number."%>

<c:if test="${empty currentPage}">
   <c:set var="currentPage" value="1" />
</c:if>

<c:if test="${pages gt 1}">
   <ol class="pager">
      <c:forEach begin="1" end="${pages}" var="page">
         <li><a href="#${listId}-page-${page}" data-list="#${listId}" class="pagerlink${(page eq currentPage) ? ' pageractive' : ''}">${page}</a></li>
      </c:forEach>
      <li><a href="#${listId}" data-list="#${listId}" class="pagerlink all"><fmt:message key="component.pager.showAll" /></a></li>
   </ol>
</c:if>