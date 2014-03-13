<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8"
   description="List generator that support pagination by rendering all items and switching between hidden pages. Exposes 'loop' (LoopTagStatus) variable so it can be used for item specific processing."%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<%@ attribute name="listId" required="true" rtexprvalue="true" type="java.lang.String" description="List ID that is unique for the rendered page."%>
<%@ attribute name="title" required="false" rtexprvalue="true" type="java.lang.String" description="Container title that toggles collapsed state" %>
<%@ attribute name="titleId" required="false" rtexprvalue="true" type="java.lang.String" description="Translation id for container title that toggles collapsed state" %>
<%@ attribute name="expanded" required="false" rtexprvalue="true" type="java.lang.Boolean" description="Is the list expanded by default." %>
<%@ attribute name="independent" required="false" rtexprvalue="true" type="java.lang.Boolean" description="Is the container expanded/collapsed independently e.g. are others collapsed when this is expanded?" %>
<%@ attribute name="items" required="true" rtexprvalue="true" type="java.util.Collection" description="Collection of items that should be iterated."%>
<%@ attribute name="pageSize" required="false" rtexprvalue="true" type="java.lang.Integer" description="Number of items displayed on a page. Defaults to '20'."%>
<%@ attribute name="rendered" required="false" rtexprvalue="true" type="java.lang.Boolean" description="Conditionally disable all output from this tag. Dafaults to 'true'."%>

<c:if test="${empty pageSize}">
   <c:set var="pageSize" value="20" />
</c:if>
<c:if test="${empty rendered}">
   <c:set var="rendered" value="true" />
</c:if>

<tag:expanderBlock blockId="${listId}" title="${title}" titleId="${titleId}" titleDetails="${fn:length(items)}" expanded="${expanded}" rendered="${rendered}" independent="${independent}">

   <c:set var="currentPage" value="1" />
   <c:set var="pageOpen" value="false" />

   <c:forEach items="${items}" var="item" varStatus="loopStatus">
      <c:set var="newPage" value="${(loopStatus.index mod pageSize) eq 0}" />

      <c:if test="${newPage}">
         <c:if test="${pageOpen}">
            </div>
            <c:set var="currentPage" value="${currentPage + 1}" />
         </c:if>

         <div id="${listId}-page-${currentPage}" class="page${(not loopStatus.first) ? ' hidden' : ''}">
         <c:set var="pageOpen" value="true" />
      </c:if>

      <c:set var="loop" value="${loopStatus}" scope="request" />
      <jsp:doBody />
      <c:remove var="loop" scope="request" />

   </c:forEach>
   
   <%-- If last page contains less than a full page... --%>
   <c:if test="${pageOpen}">
      </div>
   </c:if>

   <tag:pager pages="${currentPage}" listId="${listId}" />
   <c:remove var="currentPage" />
   <c:remove var="pageOpen" />
</tag:expanderBlock>