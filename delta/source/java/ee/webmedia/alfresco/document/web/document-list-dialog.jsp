<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="ee.webmedia.alfresco.document.web.BaseDocumentListDialog" %>

<f:verbatim>
<script type="text/javascript">

$jQ(".selectAllHeader").change(function() {
   $jQ(".headerSelectable").attr('checked',$jQ(this).attr('checked'));
});

</script>
</f:verbatim>
<%-- This JSP is used from multiple dialogs, that's why DialogManager.bean reference is used --%>
<a:booleanEvaluator value="#{DialogManager.bean.infoMessageVisible}">
   <a:panel id="info-message" styleClass="message">
      <h:outputText value="#{DialogManager.bean.infoMessage}" />
   </a:panel>
</a:booleanEvaluator>

<a:panel id="document-panel" styleClass="panel-100 with-pager" label="#{DialogManager.bean.listTitle}" progressive="true">

   <%-- Main List --%>
   <a:richList id="documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.documents}" var="r" binding="#{DialogManager.bean.richList}" initialSortColumn="<%=((BaseDocumentListDialog)Application.getDialogManager().getBean()).getInitialSortColumn() %>" >
      
      <%-- checkbox --%>
      <a:column id="col0" primary="true" styleClass="#{r.cssStyleClass}" rendered="#{UserService.documentManager && DialogManager.bean.showCheckboxes}" >
         <f:facet name="header">
            <h:selectBooleanCheckbox id="col0-header" value="false" styleClass="selectAllHeader"/>
         </f:facet>
         <h:selectBooleanCheckbox id="col0-checkbox" styleClass="headerSelectable" value="#{DialogManager.bean.listCheckboxes[r.nodeRef]}"/>
      </a:column>
      <jsp:include page="<%=((BaseDocumentListDialog)Application.getDialogManager().getBean()).getColumnsFile() %>" >
         <jsp:param name="showOrgStructColumn" value="<%=((BaseDocumentListDialog)Application.getDialogManager().getBean()).isShowOrgStructColumn() %>" />
      </jsp:include>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
   <h:panelGroup binding="#{DialogManager.bean.panel}" />
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
