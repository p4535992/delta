<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:booleanEvaluator id="workflowBlockEvaluator" value="#{!DocumentDynamicDialog.inEditMode}">
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
<%-- <a:booleanEvaluator id="foundSimilarEvaluator" value="#{DocumentDialog.showFoundSimilar}"> --%>
<%--    <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-similar-block.jsp" /> --%>
<%-- </a:booleanEvaluator> --%>

<h:panelGroup id="metadata-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator value="#{DocumentDynamicDialog.node}" allow="editDocumentMetaData">
         <a:actionLink id="metadata-link-edit" showLink="false" image="/images/icons/edit_properties.gif" value="#{msg.modify}" tooltip="#{msg.modify}"
            actionListener="#{DocumentDynamicDialog.switchToEditMode}" rendered="#{!DocumentDynamicDialog.inEditMode}" />
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>
<a:panel id="metadata-panel" facetsId="dialog:dialog-body:metadata-panel-facets" label="#{msg.document_metadata}"
   styleClass="panel-100 #{(DocumentDynamicDialog.inEditMode == false) ? 'edit-mode' : ''}" progressive="true">
   <r:propertySheetGrid id="doc-metatada" binding="#{DocumentDynamicDialog.propertySheet}" value="#{DocumentDynamicDialog.node}" columns="1" mode="#{DocumentDynamicDialog.mode}"
      config="#{DocumentDynamicDialog.propertySheetConfigElement}" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/file/web/file-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/review-note-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/opinion-note-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-summary-block.jsp" />
<%-- <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/associations/web/assocs-block.jsp" /> --%>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/sendout/web/send-out-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/log/web/document-log-block.jsp" />
<%-- <a:booleanEvaluator id="searchBlockEvaluator" value="#{DocumentDialog.showSearchBlock}"> --%>
<%--    <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-search-block.jsp" /> --%>
<%-- </a:booleanEvaluator> --%>
