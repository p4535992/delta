<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:booleanEvaluator id="document-search-filters-panel-evaluator" value="#{UserService.administrator}">
   <a:panel id="document-search-filters-panel" styleClass="panel-100" label="#{msg.document_search_saved_manage}">
      <r:propertySheetGrid id="document-search-filter-name" value="#{DocumentSearchDialog.filter}" columns="1" mode="edit" externalConfig="false"
         labelStyleClass="propertiesLabel" var="node2">
         <r:property name="docsearch:name" />
      </r:propertySheetGrid>
      <h:commandButton actionListener="#{DocumentSearchDialog.saveFilter}" value="#{msg.save}" />
      <f:verbatim>&nbsp;</f:verbatim>
      <h:commandButton actionListener="#{DocumentSearchDialog.deleteFilter}" value="#{msg.delete}" />
   </a:panel>
</a:booleanEvaluator>

<h:panelGroup id="document-search-panel-facets">
   <f:facet name="title">
      <h:panelGroup>
         <h:outputText value="#{msg.document_search_saved}" />
         <f:verbatim>&nbsp;</f:verbatim>
         <h:selectOneMenu value="#{DocumentSearchDialog.selectedFilter}" converter="ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter"
            valueChangeListener="#{DocumentSearchDialog.selectedFilterValueChanged}" binding="#{DocumentSearchDialog.selectedFilterMenu}">
            <f:selectItems value="#{DocumentSearchDialog.allFilters}" />
         </h:selectOneMenu>
      </h:panelGroup>
   </f:facet>
</h:panelGroup>

<a:panel id="document-search-panel" facetsId="dialog:dialog-body:document-search-panel-facets" styleClass="panel-100" label="#{msg.document_search}">
   <r:propertySheetGrid id="document-search-filter" value="#{DocumentSearchDialog.filter}" columns="1" mode="edit" externalConfig="true"
      labelStyleClass="propertiesLabel" binding="#{DocumentSearchDialog.propertySheet}" />
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
