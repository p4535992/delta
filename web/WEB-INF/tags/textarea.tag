<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8" description="Second level heading that can be used to toggle blocks"%>

<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@ attribute name="name" required="false" rtexprvalue="true" type="java.lang.String" %>
<%@ attribute name="id" required="false" rtexprvalue="true" type="java.lang.String" %>
<%@ attribute name="value" required="true" rtexprvalue="true" type="java.lang.String" %>

<textarea name="${ name }" id="${ id }" rows="1" cols="5" style="overflow: hidden; word-wrap: break-word; resize: horizontal; height: 43px;" ><c:out value="${ value }"></c:out></textarea>