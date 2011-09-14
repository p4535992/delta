<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="associationToDocTypeDetails-panel" label="#{msg.associationToDocType_details_panel}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="fieldDefDetailsPS" value="#{AssociationToDocTypeDetailsDialog.associationToDocType.node}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>