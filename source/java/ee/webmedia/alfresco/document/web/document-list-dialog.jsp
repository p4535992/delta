<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="ee.webmedia.alfresco.document.web.BaseDocumentListDialog" %>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil" %>

<f:verbatim>
<script type="text/javascript">

$jQ(document).ready(function(){
   $jQ(".selectAllHeader").change(function() {
      $jQ(".headerSelectable").prop('checked',$jQ(this).prop('checked'));
   });
});

</script>
</f:verbatim>
<%-- This JSP is used from multiple dialogs, that's why DialogManager.bean reference is used --%>
<a:booleanEvaluator value="#{DialogManager.bean.infoMessageVisible}">
   <a:panel id="info-message" styleClass="message">
      <h:outputText value="#{DialogManager.bean.infoMessage}" />
   </a:panel>
</a:booleanEvaluator>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/limited-message-panel.jsp" />

<a:booleanEvaluator id="confirmMoveAssociatedDocumentsEvaluator" value="#{DocumentListDialog.confirmMoveAssociatedDocuments}">
   <f:verbatim>
   <script type="text/javascript">
      $jQ(document).ready(function () {
         var message = '<%= MessageUtil.getMessageAndEscapeJS("documents_move_associated_documents_confirmation") %>';
         if(confirm(message)){
            $jQ("#documents-after-confirmation-accepted-link").eq(0).click();
         } else {
            $jQ("#documents-after-confirmation-rejected-link").eq(0).click();
         }
      });
   </script>
   </f:verbatim>
   <a:actionLink id="documents-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DocumentListDialog.massChangeDocLocationConfirmed}" styleClass="hidden" />
   <a:actionLink id="documents-after-confirmation-rejected-link" value="confirmationRejectedLink" actionListener="#{DocumentListDialog.resetConfirmation}" styleClass="hidden" />   
</a:booleanEvaluator>

<a:booleanEvaluator id="showDocumentsLocationPopupEvaluator" value="#{DocumentListDialog.showDocumentsLocationPopup}">
   <f:verbatim>
      <script type="text/javascript">
      $jQ(document).ready(function () {
                     showModal('documentLocation_popup');
                     return false;
      });
      </script>
   </f:verbatim>
</a:booleanEvaluator>


<a:panel id="document-panel" styleClass="panel-100 with-pager" label="#{DialogManager.bean.listTitle}" progressive="true">

   <%-- Main List --%>
   <a:richList id="documentList" styleClass="duplicate-header" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.documents}" var="r" binding="#{DialogManager.bean.richList}" refreshOnBind="true" initialSortColumn="<%=((BaseDocumentListDialog)Application.getDialogManager().getBean()).getInitialSortColumn() %>" >
      
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
