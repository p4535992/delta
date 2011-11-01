<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="ee.webmedia.alfresco.document.web.BaseDocumentListDialog" %>

<a:booleanEvaluator value="#{DocumentSearchResultsDialog.infoMessageVisible}">
   <a:panel id="info-message" styleClass="message">
      <h:outputText value="#{DocumentSearchResultsDialog.infoMessage}" />
   </a:panel>
</a:booleanEvaluator>

<a:panel id="document-panel" styleClass="panel-100 with-pager" label="#{DocumentSearchResultsDialog.listTitle}" progressive="true">

   <%-- Main List --%>
   <a:richList id="documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" binding="#{DocumentSearchResultsDialog.richList}" value="#{DocumentSearchResultsDialog.documents}" var="r" >
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
