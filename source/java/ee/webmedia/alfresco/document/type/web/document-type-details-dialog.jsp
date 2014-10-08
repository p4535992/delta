<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="metadata-panel" label="#{msg.document_type_details_panel_general}" styleClass="panel-100" progressive="true">
   <a:booleanEvaluator value="#{DocumentTypeDetailsDialog.showSystematicComment}">
      <f:verbatim><div class="message condence200"></f:verbatim><h:outputText value="#{DocumentTypeDetailsDialog.docType.systematicComment}" /><f:verbatim></div></f:verbatim>
   </a:booleanEvaluator>
   <r:propertySheetGrid id="ser-metatada" value="#{DocumentTypeDetailsDialog.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>