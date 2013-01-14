<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup rendered="#{DocumentDialogHelperBean.inWorkspace and CaseFileDialog.modalRendered}">
   <h:panelGroup id="dialog-modal-container" binding="#{CaseFileDialog.modalContainer}" />
   <f:verbatim>
      <script type="text/javascript">
         $jQ(document).ready(function () {
            showModal("</f:verbatim><a:outputText value="#{CaseFileDialog.renderedModal}" /><f:verbatim>");
         });
      </script>
   </f:verbatim>
</h:panelGroup>

<h:panelGroup rendered="#{CaseFileDialog.inWorkspace and !CaseFileDialog.inEditMode}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-block.jsp" />
</h:panelGroup>

<a:panel id="document-panel" styleClass="panel-100 with-pager" label="#{msg.documents}" progressive="true" rendered="#{!CaseFileDialog.inEditMode}">

   <%-- Main List --%>
   <a:richList id="documentList" styleClass="duplicate-header" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{CaseFileDialog.documents}" binding="#{CaseFileDialog.documentRichList}" var="r" refreshOnBind="true" >
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog-columns.jsp" >
         <jsp:param name="showOrgStructColumn" value="false" />
      </jsp:include>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager" styleClass="pager" />
      
   </a:richList>
</a:panel>

<h:panelGroup id="metadata-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator id="metadata-link-edit-evaluator" value="#{CaseFileDialog.node}" allow="editCaseFile">
         <a:actionLink id="metadata-link-edit" showLink="false" image="/images/icons/edit_properties.gif" value="#{msg.modify}" tooltip="#{msg.modify}"
            actionListener="#{CaseFileDialog.switchToEditMode}" rendered="#{DocumentDialogHelperBean.inWorkspace and !DocumentDialogHelperBean.inEditMode}" />
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>
<f:verbatim>
<script type="text/javascript">
$jQ(document).ready(function() {
   if (setInputFocus) {
		var container = $jQ("#"+escapeId4JQ('container-content'));
         $jQ("input:text, textarea", container).filter(':visible:enabled[readonly!="readonly"]').first().focus();
   }
});
</script>
</f:verbatim>
<a:panel id="metadata-panel" facetsId="dialog:dialog-body:metadata-panel-facets" label="#{msg.document_metadata}"
   styleClass="panel-100 #{(DocumentDialogHelperBean.inEditMode == false) ? 'view-mode' : 'edit-mode'}" progressive="true">
   <r:propertySheetGrid id="doc-metatada" binding="#{CaseFileDialog.propertySheet}" value="#{CaseFileDialog.node}" columns="1" mode="#{CaseFileDialog.mode}"
      config="#{CaseFileDialog.propertySheetConfigElement}" showUnvalued="#{CaseFileDialog.propertySheetConfigElement.showUnvalued}" externalConfig="true" labelStyleClass="propertiesLabel wrap" />
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/associations/web/assocs-block.jsp" />
<a:booleanEvaluator id="searchBlockEvaluator" value="#{DocumentDialogHelperBean.inWorkspace and CaseFileDialog.showSearchBlock}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-search-block.jsp" />
</a:booleanEvaluator>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-summary-block.jsp" />

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/casefile/web/case-file-document-workflows.jsp" />

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/review-note-block.jsp" />

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/log/web/document-log-block.jsp" />

<a:booleanEvaluator value="#{DocumentDialogHelperBean.inWorkspace and DocumentDialogHelperBean.inEditMode}" id="docMeta-InEditMode">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docdynamic/web/metadata-block-lockRefresh.jsp" />
</a:booleanEvaluator>