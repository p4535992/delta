<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:booleanEvaluator id="workflowBlockEvaluator" value="#{!DocumentDialogHelperBean.inEditMode}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-block.jsp" />
</a:booleanEvaluator>
<h:panelGroup rendered="#{DocumentDynamicDialog.modalRendered}">
   <h:panelGroup id="dialog-modal-container" binding="#{DocumentDynamicDialog.modalContainer}" />
   <f:verbatim>
      <script type="text/javascript">
         $jQ(document).ready(function () {
            showModal("</f:verbatim><a:outputText value="#{DocumentDynamicDialog.renderedModal}" /><f:verbatim>");
         });
      </script>
   </f:verbatim>
</h:panelGroup>
<a:booleanEvaluator id="confirmAccessRestrictionChangedEvaluator" value="#{DocumentDynamicDialog.showConfirmationPopup}">
   <f:verbatim>
   <script type="text/javascript">
      $jQ(document).ready(function () {
         var message = '<%= MessageUtil.getMessage("document_access_restriction_changed_confirmation") %>';
      	if(confirm(message)){
      	   $jQ("#document-after-confirmation-accepted-link").eq(0).click();
      	} else {
      	   $jQ("#document-after-confirmation-rejected-link").eq(0).click();
      	}
      });
   </script>
   </f:verbatim>
</a:booleanEvaluator>
<a:actionLink id="document-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DocumentDynamicDialog.sendAccessRestrictionChangedEmails}" styleClass="hidden" />
<a:actionLink id="document-after-confirmation-rejected-link" value="confirmationRejectedLink" actionListener="#{DocumentDynamicDialog.cancel}" styleClass="hidden" />
<%-- <a:booleanEvaluator id="foundSimilarEvaluator" value="#{DocumentDialog.showFoundSimilar}"> --%>
<%--    <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-similar-block.jsp" /> --%>
<%-- </a:booleanEvaluator> --%>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/type/web/document-type-block.jsp" />

<h:panelGroup id="metadata-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator id="metadata-link-edit-evaluator" value="#{DocumentDynamicDialog.node}" allow="editDocumentMetaData">
         <a:actionLink id="metadata-link-edit" showLink="false" image="/images/icons/edit_properties.gif" value="#{msg.modify}" tooltip="#{msg.modify}"
            actionListener="#{DocumentDynamicDialog.switchToEditMode}" rendered="#{!DocumentDialogHelperBean.inEditMode}" />
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>
<f:verbatim>
<script type="text/javascript">
$jQ(document).ready(function() {
		var container = $jQ("#"+escapeId4JQ('container-content'));
         $jQ("input:text, textarea", container).filter(':visible:enabled[readonly!="readonly"]').first().focus();
});
</script>
</f:verbatim>
<a:panel id="metadata-panel" facetsId="dialog:dialog-body:metadata-panel-facets" label="#{msg.document_metadata}"
   styleClass="panel-100 #{(DocumentDialogHelperBean.inEditMode == false) ? 'view-mode' : 'edit-mode'}" progressive="true">
   <r:propertySheetGrid id="doc-metatada" binding="#{DocumentDynamicDialog.propertySheet}" value="#{DocumentDynamicDialog.node}" columns="1" mode="#{DocumentDynamicDialog.mode}"
      config="#{DocumentDynamicDialog.propertySheetConfigElement}" showUnvalued="#{DocumentDynamicDialog.propertySheetConfigElement.showUnvalued}" externalConfig="true" labelStyleClass="propertiesLabel wrap" />
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/file/web/file-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/review-note-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/opinion-note-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/order-assignment-note-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-summary-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/associations/web/assocs-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/sendout/web/send-out-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/log/web/document-log-block.jsp" />

<a:booleanEvaluator id="searchBlockEvaluator" value="#{DocumentDynamicDialog.showSearchBlock}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-search-block.jsp" />
</a:booleanEvaluator>

<a:booleanEvaluator value="#{DocumentDialogHelperBean.inEditMode}" id="docMeta-InEditMode">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docdynamic/web/metadata-block-lockRefresh.jsp" />
</a:booleanEvaluator>
