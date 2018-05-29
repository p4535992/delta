<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>


<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="default-location-panel" label="#{msg.doc_type_default_location}" styleClass="panel-100" progressive="true" rendered="#{DefaultLocationBean.panelRendered}"
   expanded="#{DefaultLocationBean.panelExpanded}">
   <r:propertySheetGrid value="#{DefaultLocationBean.locationPropHolder}" binding="#{DefaultLocationBean.propertySheet}" config="#{DefaultLocationBean.locationPropSheetConfig}"
      columns="1" labelStyleClass="propertiesLabel wrap" externalConfig="true" mode="edit" validationEnabled="false" id="location-metadata-props" />
</a:panel>
