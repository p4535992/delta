<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-wizard-finish-button.jsp" />

<r:propertySheetGrid id="node-props" value="#{WizardManager.bean.entry}" columns="1" externalConfig="true" mode="edit" labelStyleClass="propertiesLabel" />
