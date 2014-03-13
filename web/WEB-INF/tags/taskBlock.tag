<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="A collapsible container that hold task blocks"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<%@ attribute name="title" required="true" rtexprvalue="true" type="java.lang.String" description="Container title that toggles collapsed state" %>
<%@ attribute name="tasks" required="true" rtexprvalue="true" type="java.util.List" description="List of tasks that will be rendered. May be empty." %>
<%@ attribute name="count" required="true" rtexprvalue="true" type="java.lang.Integer" description="Total count of tasks." %>
<%@ attribute name="expanded" required="true" rtexprvalue="true" type="java.lang.Boolean" description="Is the container expanded and therefore tasks visible?" %>
<%@ attribute name="target" required="true" rtexprvalue="true" type="java.lang.String" description="This is used to define the block ID and trigger link." %>

<tag:expanderBlock blockId="${target}" title="${title}" expanded="${expanded}" url="/m/tasks/${target}" titleDetails="${count}">

   <c:set var="wrapperOpen" value="false" />
   <c:forEach items="${tasks}" var="task" varStatus="loop">
      <c:if test="${loop.index % 2 == 0}">
         <div class="articlewrap">
         <c:set var="wrapperOpen" value="true" />
      </c:if>

      <tag:taskItem task="${task}" />

      <c:if test="${loop.index % 2 == 1}">
         </div>
         <c:set var="wrapperOpen" value="false" />
      </c:if>
   </c:forEach>
   
   <c:if test="${wrapperOpen}">
      </div>
   </c:if>

</tag:expanderBlock>
