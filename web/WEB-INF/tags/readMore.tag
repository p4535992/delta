<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8"
   description="Outputs given content in a paragraph. Read more link is added if content is longer than the specified threshold."%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<%@ attribute name="content" required="true" rtexprvalue="true" type="java.lang.String" description="Content to be printed out."%>
<%@ attribute name="threshold" required="false" rtexprvalue="true" type="java.lang.Integer" description="Number of characters that triggers 'Read more...' functionality. Defaults to 150."%>
<%@ attribute name="escape" required="false" rtexprvalue="true" type="java.lang.Boolean" description="If false or not present, escape output value" %>
<%@ attribute name="useTagContent" required="false" rtexprvalue="true" type="java.lang.Boolean" description="If true, use jsp:doBody to generate tag content (lenght is still checked by content tag value)" %>

<c:if test="${empty threshold}">
   <c:set var="threshold" value="150" />
</c:if>
<c:set value="${ empty escape || escape }" var="escapeXml" />

<c:choose>
   <c:when test="${fn:length(content) gt threshold}">
      <div class="readmore active">
      	 <c:if test="${ !useTagContent }" >
	         <p>
	            <c:out value="${content}" escapeXml="${ escapeXml }" />
	         </p>
         </c:if>
      	 <c:if test="${ useTagContent }" >
      	 	<jsp:doBody />
      	 </c:if>         
      </div>
   </c:when>
   <c:otherwise>
   	<c:if test="${ !useTagContent }" >
      <p>
         <c:out value="${content}" escapeXml="${ escapeXml }" />
      </p>
     </c:if>
     <c:if test="${ useTagContent }"  >
      	 <jsp:doBody />
     </c:if>      
   </c:otherwise>
</c:choose>