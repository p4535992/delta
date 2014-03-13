<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="Generates a row  with two spans for property description and property value. "%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>   
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>
		
<%@ attribute name="label" required="false" rtexprvalue="true" type="java.lang.String" description="Label for value row." %>
<%@ attribute name="labelId" required="false" rtexprvalue="true" type="java.lang.String" description="Translation key for value row label." %>
<%@ attribute name="value" required="true" rtexprvalue="true" type="java.lang.String" description="Value row value" %>
<%@ attribute name="hideIfEmpty" required="false" rtexprvalue="true" type="java.lang.Boolean" description="If true, don't show rows with empty values" %>
<%@ attribute name="readMore" required="false" rtexprvalue="true" type="java.lang.Integer" description="If present, enable abbreviation of value" %>
<%@ attribute name="href" required="false" rtexprvalue="true" type="java.lang.String" description="If present, value will be link to this url" %>
<%@ attribute name="escape" required="false" rtexprvalue="true" type="java.lang.Boolean" description="If false or not present, escape output html" %>

<c:if test="${empty label and not empty labelId}">
   <fmt:message key="${labelId}" var="label" />
</c:if>
<c:set value="${!empty href}" var="isLink" />
<c:set value="${empty escape || escape}" var="escpeXml" />

<c:if test="${ empty hideIfEmpty || !hideIfEmpty || !(empty value) }" >
		<p class="valuerow" >
			<span class="desc"><c:out value="${label}:" /></span>
			<span class="value">
				<c:if test="${ isLink }" >
					<a href='<c:url value="${ href }" />' >
				</c:if>		
				<c:if test="${ !abbreviate}" >
					<c:out value="${value}" escapeXml="${ escape }"/>
				</c:if>
				<c:if test="${ isLink }" >
					</a>
				</c:if>
			</span>
		</p>
</c:if>