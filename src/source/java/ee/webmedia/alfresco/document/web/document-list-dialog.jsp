<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="ee.webmedia.alfresco.document.web.BaseDocumentListDialog" %>

<%-- This JSP is used from multiple dialogs, that's why DialogManager.bean reference is used --%>
<a:booleanEvaluator value="#{DialogManager.bean.infoMessageVisible}">
   <a:panel id="info-message" styleClass="message">
      <h:outputText value="#{DialogManager.bean.infoMessage}" />
   </a:panel>
</a:booleanEvaluator>

<a:panel id="document-panel" styleClass="panel-100 with-pager" label="#{DialogManager.bean.listTitle}" progressive="true">

   <%-- Main List --%>
   <a:richList id="documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.documents}" var="r">

      <jsp:include page="<%=((BaseDocumentListDialog)Application.getDialogManager().getBean()).getColumnsFile() %>" />
      

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
