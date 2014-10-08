<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="associationModelDetails-panel" label="#{msg.associationModel_details_panel}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="fieldDefDetailsPS" value="#{AssociationModelDetailsDialog.associationModel.node}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<a:panel id="associatedFields-panel" label="#{msg.associationModel_details_panel_fieldMappings}" styleClass="panel-100 with-pager" progressive="true" >
   <a:richList id="associatedFieldsList" value="#{FieldMappingsListBean.associatedFieldsList}" var="r" viewMode="details"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

      <a:column id="nameAndId">
         <f:facet name="header">
            <a:outputText value="#{msg.associationModel_details_panel_fieldMappings_nameAndId}" />
         </f:facet>
         <h:outputText value="#{r.nameAndFieldId}" />
      </a:column>

      <a:column id="group" >
         <f:facet name="header">
            <a:outputText value="#{msg.associationModel_details_panel_fieldMappings_group}" />
         </f:facet>
         <h:outputText value="#{r.group}" />
      </a:column>

      <a:column id="associatedField" >
         <f:facet name="header">
            <a:outputText value="#{msg.associationModel_details_panel_fieldMappings_nameAndIdOfRelatedField}" />
         </f:facet>
         <h:selectOneMenu id="relatedField" value="#{r.toField}" >
            <f:selectItems value="#{r.relatedFieldSelectItems}" />
         </h:selectOneMenu>
      </a:column>

   </a:richList>
</a:panel>
