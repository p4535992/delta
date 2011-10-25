<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="document-search-panel-facets">
   <f:facet name="title">
      <h:panelGroup>
         <h:outputText value="#{msg.document_search_saved}" />
         <f:verbatim>&nbsp;</f:verbatim>
         <h:selectOneMenu id="filters" value="#{DocumentDynamicSearchDialog.selectedFilter}" converter="ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter"
            valueChangeListener="#{DocumentDynamicSearchDialog.selectedFilterValueChanged}" binding="#{DocumentDynamicSearchDialog.selectedFilterMenu}">
            <f:selectItems value="#{DocumentDynamicSearchDialog.allFilters}" />
         </h:selectOneMenu>
      </h:panelGroup>
   </f:facet>
</h:panelGroup>

<a:panel id="document-search-panel" facetsId="dialog:dialog-body:document-search-panel-facets" styleClass="panel-100" label="#{msg.document_search}">
   <r:propertySheetGrid id="document-search-filter" value="#{DocumentDynamicSearchDialog.filter}" columns="1" mode="edit" externalConfig="true"
      labelStyleClass="propertiesLabel" binding="#{DocumentDynamicSearchDialog.propertySheet}" config="#{DocumentDynamicSearchDialog.propertySheetConfigElement}" />
</a:panel>

<a:panel id="document-search-filters-panel" styleClass="panel-100" label="#{msg.document_search_saved_manage}">

   <h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" columnClasses="propertiesLabel," width="100%">
      <h:outputText value="#{msg.document_search_name}" styleClass="no-wrap" />
      <h:inputText id="searchTitle" binding="#{DocumentDynamicSearchDialog.searchTitleInput}" size="35" />
      <%-- empty placeholder to allign checkbox to the right with the lable --%>
      <f:verbatim />
      <a:booleanEvaluator id="foundSimilarEvaluator" value="#{UserService.administrator}" >
         <h:panelGroup>
            <h:selectBooleanCheckbox id="toAllUsers" binding="#{DocumentDynamicSearchDialog.publicCheckBox}" />
            <h:outputLabel value="#{msg.document_search_save_toAllUsers}" for="toAllUsers" />
         </h:panelGroup>
      </a:booleanEvaluator>
   </h:panelGrid>

   <f:verbatim><span class="task-sheet-buttons"></f:verbatim>
   <h:commandButton id="save" actionListener="#{DocumentDynamicSearchDialog.saveFilter}" value="#{msg.save}" />
   <f:verbatim>&nbsp;</f:verbatim>
   <h:commandButton id="delete" actionListener="#{DocumentDynamicSearchDialog.deleteFilter}" value="#{msg.delete}" />
   <f:verbatim></span></f:verbatim>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
