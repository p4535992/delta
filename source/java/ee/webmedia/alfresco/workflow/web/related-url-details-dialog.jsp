<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="related-url-panel" label="#{msg.compoundWorkflow_relatedUrl_details}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="related-url-details" value="#{RelatedUrlDetailsDialog.relatedUrl}" binding="#{RelatedUrlDetailsDialog.propertySheet}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel"/>
</a:panel>