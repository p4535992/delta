<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/dyn-type-details-dialog.jsp" />

<a:booleanEvaluator value="#{DocTypeDetailsDialog.showingLatestVersion}">
   <%-- Use same jsp for both followup and reply association panels --%>
   <% request.setAttribute("followUpOrReply", "reply"); %>
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/doc-type-assocs.jsp" />
   <% request.setAttribute("followUpOrReply", "followup"); %>
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/doc-type-assocs.jsp" />
   <% request.removeAttribute("followUpOrReply"); %>
</a:booleanEvaluator>