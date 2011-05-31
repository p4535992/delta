<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<a:panel id="props-panel" styleClass="mainSubTitle column panel-100" label="#{msg.addressbook_private_person_data}">
   <r:propertySheetGrid id="node-props" value="#{DialogManager.bean.currentNode}" columns="1" externalConfig="true" mode="view"  labelStyleClass="propertiesLabel"/>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/addressbook/web/manage-contactgroups.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />