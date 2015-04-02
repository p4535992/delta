<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@ attribute name="value" required="true" rtexprvalue="true" type="java.lang.String" description="Value row value"%>
<%@ attribute name="escape" required="false" rtexprvalue="true" type="java.lang.Boolean" description="If false or not present, escape output html"%>

<c:if test="${ !(empty value) }">
   <c:set value="${empty escape || escape}" var="escpeXml" />
   <p>
      <span class="value">
         <c:out value="${value}" escapeXml="${ escape }" />
      </span>
   </p>
</c:if>