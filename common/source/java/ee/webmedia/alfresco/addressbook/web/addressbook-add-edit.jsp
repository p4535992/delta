<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/enter-clicks-dialog-finish.jsp"  />

<a:panel id="ab-add-edit-panel" styleClass="mainSubTitle" label="#{DialogManager.bean.panelTitle}">
   <r:propertySheetGrid id="node-props" value="#{DialogManager.bean.entry}" columns="1" externalConfig="true" mode="edit" labelStyleClass="propertiesLabel" />
</a:panel>
