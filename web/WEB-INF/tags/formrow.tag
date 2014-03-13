<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="Generates a row  with two spans for property description and property value. "%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>   
		
<%@ attribute name="label" required="false" rtexprvalue="true" type="java.lang.String" description="Label for value row." %>
<%@ attribute name="labelId" required="false" rtexprvalue="true" type="java.lang.String" description="Translation key for value row label." %>

<c:if test="${empty label and not empty labelId}">
   <fmt:message key="${labelId}" var="label" />
</c:if>

<ol class="form">
	<li class="formrow">
		<span class="formlabel" >
			<c:out	value="${label}:" />
		</span>
		<span class="formcontent"> 
			<jsp:doBody />
		</span>
	</li>
</ol>