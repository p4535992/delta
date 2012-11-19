<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- 
   DialogManager.bean is used to refer to subclass of DynamicTypeDetailsDialog - 
   either DocTypeDetailsDialog or CaseFileTypeDetailsDialog
 --%>
<%-- very similar to field-group-details-dialog.jsp, but since java Dialogs are different, then decided not to generalize so much --%>
<a:panel id="docTypeDetails-panel" label="#{msg.doc_type_details_panel_details}" styleClass="panel-100" progressive="true" rendered="#{DialogManager.bean.showingLatestVersion}">
   <a:booleanEvaluator value="#{DialogManager.bean == DocTypeDetailsDialog && DocTypeDetailsDialog.showSystematicComment}">
      <f:verbatim><div class="message condence200"></f:verbatim><h:outputText value="#{DocTypeDetailsDialog.docType.systematicComment}" /><f:verbatim></div></f:verbatim>
   </a:booleanEvaluator>
   <r:propertySheetGrid id="docTypeDetailsPS" value="#{DialogManager.bean.currentNode}" columns="1" mode="#{DialogManager.bean.showingLatestVersion ? 'edit': 'view'}" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<a:booleanEvaluator value="#{DialogManager.bean == DocTypeDetailsDialog}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/default-location-bean.jsp" />
</a:booleanEvaluator>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/fields-list-bean.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/doc-type-versions-list-bean.jsp" />
