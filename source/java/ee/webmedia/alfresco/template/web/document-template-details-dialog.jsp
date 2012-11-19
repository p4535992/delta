<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="documentTemplate-details-panel" styleClass="panel-100" label="#{msg.templates_title}" progressive="true">

<r:propertySheetGrid value="#{DocumentTemplateDetailsDialog.currentNode}" mode="edit" labelStyleClass="propertiesLabel" externalConfig="true" columns="1" />

</a:panel>