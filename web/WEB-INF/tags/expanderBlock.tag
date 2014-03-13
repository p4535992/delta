<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="Second level heading that can be used to toggle blocks"%>

<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%@ attribute name="blockId" required="true" rtexprvalue="true" type="java.lang.String" description="This is used to define the block ID and trigger link."%>
<%@ attribute name="title" required="false" rtexprvalue="true" type="java.lang.String" description="Container title that toggles collapsed state"%>
<%@ attribute name="titleId" required="false" rtexprvalue="true" type="java.lang.String" description="Translation id for container title that toggles collapsed state"%>
<%@ attribute name="titleDetails" required="false" rtexprvalue="true" type="java.lang.String" description="Is displayed in parenthises after title"%>
<%@ attribute name="expanded" required="false" rtexprvalue="true" type="java.lang.Boolean" description="Is the container expanded?"%>
<%@ attribute name="independent" required="false" rtexprvalue="true" type="java.lang.Boolean" description="Is the container expanded/collapsed independently e.g. are others collapsed when this is expanded?" %>
<%@ attribute name="url" required="false" rtexprvalue="true" type="java.lang.String" description="Heading link URL, source for content."%>
<%@ attribute name="rendered" required="false" rtexprvalue="true" type="java.lang.String" description="Conditionally disable all output from this tag. Dafaults to 'true'."%>

<c:if test="${empty rendered}">
   <c:set var="rendered" value="true" />
</c:if>

<c:if test="${rendered}">
   <tag:blockTitle title="${title}" titleId="${titleId}" target="${blockId}" url="${url}" titleDetails="${titleDetails}" expanded="${expanded}" independent="${independent}" />

   <div id="${blockId}" class="expander" <c:if test="${expanded}"> style="display: block;"</c:if>>
      <jsp:doBody />
   </div>
</c:if>