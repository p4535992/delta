<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- very similar to doc-type-details-dialog.jsp, but since java Dialogs are different, then decided not to generalize so much --%>
<a:panel id="fieldGroupDetails-panel" label="#{msg.fieldGroup_details_panel_details}" styleClass="panel-100" progressive="true">
   <a:booleanEvaluator value="#{FieldGroupDetailsDialog.showSystematicComment}">
      <f:verbatim><div class="message condence200"></f:verbatim><h:outputText value="#{FieldGroupDetailsDialog.fieldGroup.systematicComment}" /><f:verbatim></div></f:verbatim>
   </a:booleanEvaluator>
   <r:propertySheetGrid id="fieldGroupDetailsPS" value="#{FieldGroupDetailsDialog.currentNode}" columns="1" mode="#{FieldGroupDetailsDialog.dynamicTypeDetailsDialog.showingLatestVersion ? 'edit': 'view'}" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/fields-list-bean.jsp" />
