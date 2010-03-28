<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="task-search-filters-panel" styleClass="panel-100" label="#{msg.task_search_saved_manage}">
   <h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" columnClasses="propertiesLabel,">
      <h:outputText value="#{msg.task_search_name}" />
      <h:inputText id="searchTitle" binding="#{TaskSearchDialog.searchTitleInput}" size="35" />
      <%-- empty placeholder to allign checkbox to the right with the lable --%>
      <f:verbatim />
      <a:booleanEvaluator id="foundSimilarEvaluator" value="#{UserService.administrator}">
         <h:panelGroup>
            <h:selectBooleanCheckbox id="toAllUsers" binding="#{TaskSearchDialog.publicCheckBox}" />
            <h:outputLabel value="#{msg.document_search_save_toAllUsers}" for="toAllUsers" />
         </h:panelGroup>
      </a:booleanEvaluator>
   </h:panelGrid>

   <h:commandButton id="save" actionListener="#{TaskSearchDialog.saveFilter}" value="#{msg.save}" />
   <f:verbatim>&nbsp;</f:verbatim>
   <h:commandButton id="delete" actionListener="#{TaskSearchDialog.deleteFilter}" value="#{msg.delete}" />
</a:panel>

<h:panelGroup id="task-search-panel-facets">
   <f:facet name="title">
      <h:panelGroup>
         <h:outputText value="#{msg.task_search_saved}" />
         <f:verbatim>&nbsp;</f:verbatim>
         <h:selectOneMenu id="filters" value="#{TaskSearchDialog.selectedFilter}" converter="ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter"
            valueChangeListener="#{TaskSearchDialog.selectedFilterValueChanged}" binding="#{TaskSearchDialog.selectedFilterMenu}">
            <f:selectItems value="#{TaskSearchDialog.allFilters}" />
         </h:selectOneMenu>
      </h:panelGroup>
   </f:facet>
</h:panelGroup>

<a:panel id="task-search-panel" facetsId="dialog:dialog-body:task-search-panel-facets" styleClass="panel-100" label="#{msg.task_search}">
   <r:propertySheetGrid id="task-search-filter" value="#{TaskSearchDialog.filter}" columns="1" mode="edit" externalConfig="true"
      labelStyleClass="propertiesLabel" binding="#{TaskSearchDialog.propertySheet}" />
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
