<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="document-dynamic-dialog-workflow-block" rendered="#{DocumentDialogHelperBean.inWorkspace and !DocumentDialogHelperBean.inEditMode}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-block.jsp" />
</h:panelGroup>
<h:panelGroup id="dialog-modal-container" binding="#{DocumentDynamicDialog.modalContainer}" />
<h:panelGroup rendered="#{DocumentDialogHelperBean.inWorkspace and DocumentDynamicDialog.modalRendered}">
   <!-- Ensure that CompoundWorkflowDialog.fetchAndResetRenderedModal is called only once because it sets modal id = null in it's finally block-->
   <f:verbatim>
      <script type="text/javascript">
         $jQ(document).ready(function () {
            var modalId = "</f:verbatim><a:outputText value="#{DocumentDynamicDialog.fetchAndResetRenderedModal}" /><f:verbatim>";
            showModal(modalId);
            initExpanders($jQ("#" + modalId));
         });
      </script>
   </f:verbatim>
</h:panelGroup>
<a:booleanEvaluator id="confirmAccessRestrictionChangedEvaluator" value="#{DocumentDialogHelperBean.inWorkspace and DocumentDynamicDialog.showConfirmationPopup}">
   <f:verbatim>
   <script type="text/javascript">
      $jQ(document).ready(function () {
         var message = '<%= MessageUtil.getMessageAndEscapeJS("document_access_restriction_changed_confirmation") %>';
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
<a:actionLink id="document-after-confirmation-rejected-link" value="confirmationRejectedLink" actionListener="#{DocumentDynamicDialog.dontSendAccessRestrictionChangedEmails}" styleClass="hidden" />

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-similar-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/type/web/document-type-block.jsp" />

<h:panelGroup id="metadata-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator id="metadata-link-edit-evaluator" value="#{DocumentDynamicDialog.node}" allow="editDocument">
         <a:actionLink id="metadata-link-edit" showLink="false" image="/images/icons/edit_properties.gif" value="#{msg.modify}" tooltip="#{msg.modify}"
            actionListener="#{DocumentDynamicDialog.switchToEditMode}" rendered="#{DocumentDialogHelperBean.inWorkspace and !DocumentDialogHelperBean.inEditMode}" />
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docdynamic/web/metadata-unlocking-helper.jsp" />
<f:verbatim>
<script type="text/javascript">
$jQ(document).ready(function() {
   if (setInputFocus) {
      var container = $jQ("#"+escapeId4JQ('container-content'));
      $jQ("input:text, textarea", container).filter(':visible:enabled[readonly!="readonly"]').first().focus();
   }
   var inEdit = <%= BeanHelper.getDocumentDialogHelperBean().isInEditMode() %>;
   if(inEdit) {
      addTitleComponentsToExcludedList();
      addAdditionalBlockElementsToExcludedList();
      disableUnlockOnSelectElement("dialog:dialog-body:doc-types-select");
      disableUnlockOnPager();
   }
});
function addTitleComponentsToExcludedList() {
   var elems = $jQ(".title-component");
   for(var i = 0; elems != null && i < elems.length; i++) {
      var anchors = elems[i].getElementsByTagName("a");
      disable(anchors);
   }
}
function disableAssocsSearchElements() {
   if(excludedElementsSpecific != null) {
      excludedElementsSpecific.push("dialog:dialog-body:quickSearchBtn2");
   }
   var tableId = "search-documentList";
   var searchResultList = document.getElementById(tableId);
   if(searchResultList == null) {
      return;
   }
   var iconLinks = $jQ(".icon-link");
   for(var i = 0; iconLinks != null && i < iconLinks.length; i++) {
      $jQ(iconLinks[i]).on('mouseup', function() {
         // all these elements have same "id" value so behaviour is specified here
         finishButtonClicked = true;
      });
   }
   addSubelementsWithClassToExcludedList(tableId,"header"); // sortable headers
}
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
<h:panelGroup id="document-workflow-summary" rendered="#{WorkflowBlockBean.isShowDocumentWorkflowSummaryBlock}">
	<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-summary-block.jsp" />
</h:panelGroup>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/associations/web/assocs-block.jsp" />
<a:booleanEvaluator id="searchBlockEvaluator" value="#{DocumentDialogHelperBean.inWorkspace and DocumentDynamicDialog.showSearchBlock}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-search-block.jsp" />
</a:booleanEvaluator>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/sendout/web/send-out-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/log/web/document-log-block.jsp" />

<a:booleanEvaluator value="#{DocumentDialogHelperBean.inWorkspace and DocumentDialogHelperBean.inEditMode}" id="docMeta-InEditMode">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docdynamic/web/metadata-block-lockRefresh.jsp" />
</a:booleanEvaluator>
