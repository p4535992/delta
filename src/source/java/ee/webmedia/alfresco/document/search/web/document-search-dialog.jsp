<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="document-search-panel" styleClass="panel-100" label="#{msg.document_search}">
   <r:propertySheetGrid id="document-search-filter" value="#{DocumentSearchDialog.filter}" columns="1" mode="edit" externalConfig="true"
      labelStyleClass="propertiesLabel" binding="#{DocumentSearchDialog.propertySheet}" />
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
