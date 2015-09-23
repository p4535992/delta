<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="Generates a row  with two spans for property description and property value. "%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%@ attribute name="label" required="false" rtexprvalue="true" type="java.lang.String" description="Label for value row."%>
<%@ attribute name="labelId" required="false" rtexprvalue="true" type="java.lang.String" description="Translation key for value row label."%>
<%@ attribute name="value" required="true" rtexprvalue="true" type="java.lang.String"%>
<%@ attribute name="name" required="false" rtexprvalue="true" type="java.lang.String"%>
<%@ attribute name="allowSingleEntry" required="false" rtexprvalue="true" type="java.lang.Boolean"%>
<%@ attribute name="initialUserId" required="false" rtexprvalue="true" type="java.lang.String"%>
<%@ attribute name="initialUserName" required="false" rtexprvalue="true" type="java.lang.String"%>

<c:if test="${empty label and not empty labelId}">
  <fmt:message key="${labelId}" var="label" />
</c:if>
<c:if test="${allowSingleEntry}">
   <fmt:message key=" singleEntry" var="limited" />
</c:if>
<c:if test="${ not empty initialUserId and not empty initialUserName}">
   <fmt:message key=" prePopulate" var="prePopulate" />
</c:if>

<ol class="form">
  <li class="formrow">
    <span class="formlabel"> 
      <c:out value="${label}:" />
    </span> 
    <span class="formcontent">
      <input name="${name}" class="autocomplete${limited}${prePopulate}" type="text" style="display: none;" value="${value}" ></input>
      <c:if test="${ not empty prePopulate }">
        <span class="hidden" style="display: none;">
          <input id="initialUserId" value="${initialUserId}" type="hidden" ></input>
          <input id="initialUserName" value="${initialUserName}" type="hidden" ></input>
        </span>
      </c:if>
    </span>
  </li>
</ol>