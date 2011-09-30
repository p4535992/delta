<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="fieldDefDetails-panel" label="#{msg.field_details_panel}" styleClass="panel-100" progressive="true">
   <a:booleanEvaluator value="#{FieldDetailsDialog.showSystematicComment}">
      <f:verbatim><div class="message condence200"></f:verbatim><h:outputText value="#{FieldDetailsDialog.field.systematicComment}" /><f:verbatim></div></f:verbatim>
   </a:booleanEvaluator>

   <r:propertySheetGrid id="fieldDefDetailsPS" value="#{FieldDetailsDialog.field.node}" binding="#{FieldDetailsDialog.propertySheet}" columns="1" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>