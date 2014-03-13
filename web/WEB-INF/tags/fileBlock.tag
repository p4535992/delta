<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="Renders list of files and an optional header."%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>
<%@ taglib uri="/WEB-INF/mDelta.tld" prefix="mDelta"%>

<%@ attribute name="title" required="false" rtexprvalue="true" type="java.lang.String" description="Title for the file block."%>
<%@ attribute name="titleId" required="false" rtexprvalue="true" type="java.lang.String" description="Translation key for file block title."%>
<%@ attribute name="files" required="true" rtexprvalue="true" type="java.util.Collection" description="List of files (ee.webmedia.alfresco.document.file.model.File) to display."%>
<%@ attribute name="signable" required="false" rtexprvalue="true" type="java.lang.Boolean" description="Are displayed files added to DDOC ehen document is signed?"%>

<c:if test="${empty title and not empty titleId}">
   <fmt:message key="${titleId}" var="title" />
</c:if>

<c:if test="${not empty files}">
   <c:if test="${not empty title}">
      <h4>
         <c:if test="${signable}">
         	<span class="unsigned"><fmt:message key="document.documentToSign" /></span>
         </c:if>
         <c:out value="${title}" />
      </h4>
   </c:if>

   <ul class="filethumbnails${signable ? ' signable' : ''}">
      <c:forEach items="${files}" var="file">
         <li>
         	<c:set value="${file.displayName} (${mDelta:fileSize(file.size)})" var="fileTitle" />
         	<c:choose>
	         	<c:when test="${mDelta:documentAllowPermission(file.node.nodeRef, 'viewDocumentFiles')}">
	               <a href="<c:url value="${file.readOnlyUrl}" />" title="${fileTitle}"><c:out value="${fileTitle}" /></a>
	            </c:when>
	            <c:otherwise>
	               <span title="${fileTitle}"><c:out value="${fileTitle}" /></span>
	            </c:otherwise>
            </c:choose>
         </li>
      </c:forEach>
   </ul>
</c:if>