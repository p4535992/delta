<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<tag:html>

	<c:if test="${ !empty compoundWorkflowTitle }">
		<h1><c:out value="${ compoundWorkflowTitle }" /></h1>
	</c:if>
      
   	  <jsp:include page="/WEB-INF/jsp/mobile/compound-workflow/in-progress-tasks.jsp" />
      
      <%-- START: General block --%>
      <tag:expanderBlock blockId="workflow-general" titleId="site.workflow.general" expanded="true" independent="true">
         <tag:valueRow labelId="workflow.ownerName" value="${compoundWorkflow.ownerName}" />
         <tag:valueRow labelId="workflow.status" value="${compoundWorkflow.status}" />
      </tag:expanderBlock>
      <%-- END: General block --%>

      <%-- START: Comments block --%>
      <tag:pagedList listId="workflow-comments" titleId="site.workflow.comments" items="${comments}" expanded="true" independent="true">
         <h3>
            <c:out value="${loop.current.creatorName}" />
            <c:out value="&nbsp;" escapeXml="false" />
            <fmt:formatDate value="${loop.current.created}" pattern="dd.MM.yyyy HH:mm" />
         </h3>
         <tag:readMore content="${loop.current.commentText}" threshold="150" />

         <c:if test="${not loop.last}">
            <hr />
         </c:if>
      </tag:pagedList>
      <%-- END: Comments block --%>

      <%-- START: Objects block --%>
      <tag:pagedList listId="workflow-objects" titleId="site.workflow.objects" items="${objects}" expanded="true" independent="true">
         <h3><a href="#"> <%-- NB! loop.current.showLink --%>
            <c:out value="${loop.current.akString} ${loop.current.docName}" />
         </a></h3>
         <tag:valueRow labelId="document.type" value="${loop.current.documentTypeName}" />
         
         <tag:fileBlock files="${loop.current.files}" signable="${loop.current.documentToSign}" titleId="${loop.current.documentToSign ? 'site.document.signableFiles' : ''}" />
         <tag:fileBlock files="${loop.current.inactiveFiles}" titleId="site.document.inactiveFiles" />
      </tag:pagedList>
      <%-- END: Objects block --%>

      <%-- START: Information block --%>
      <tag:pagedList listId="workflow-information" titleId="site.workflow.information" items="${relatedUrls}" expanded="true" rendered="${not empty relatedUrls}" independent="true">
         <p><a href="<c:url value="${loop.current.url}" />" target="${loop.current.target}"><tag:limitMessage message="${loop.current.url}" /></a></p>
         <tag:readMore content="${loop.current.urlComment}" />

         <c:if test="${not loop.last}">
            <hr />
         </c:if>
      </tag:pagedList>
      <%-- END: Information block --%>
      
      <tag:expanderBlock blockId="workflow-block" titleId="workflow.block.title" titleDetails="${taskCount}" expanded="true" independent="true">
      	<jsp:include page="/WEB-INF/jsp/mobile/compound-workflow/workflow-block-content.jsp" />
      </tag:expanderBlock>

      <%-- 			<tag:list list="dokumentList" /> --%>
</tag:html>