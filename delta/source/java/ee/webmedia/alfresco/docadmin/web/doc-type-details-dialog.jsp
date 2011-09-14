<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- very similar to field-group-details-dialog.jsp, but since java Dialogs are different, then decided not to generalize so much --%>
<a:panel id="docTypeDetails-panel" label="#{msg.doc_type_details_panel_details}" styleClass="panel-100" progressive="true">
   <a:booleanEvaluator value="#{DocTypeDetailsDialog.showSystematicComment}">
      <f:verbatim><div class="message condence200"></f:verbatim><h:outputText value="#{DocTypeDetailsDialog.docType.systematicComment}" /><f:verbatim></div></f:verbatim>
   </a:booleanEvaluator>
   <r:propertySheetGrid id="docTypeDetailsPS" value="#{DocTypeDetailsDialog.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/fields-list-bean.jsp" />
<%-- FIXME DLSeadist
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/doc-type-assocs-followup.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/doc-type-assocs-reply.jsp" />
 --%>
