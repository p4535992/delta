<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="task-search-panel-facets">
   <f:facet name="title">
      <h:panelGroup>
         <h:outputText value="#{DialogManager.bean.savedFilterSelectTitle}" />
         <f:verbatim>&nbsp;</f:verbatim>
         <h:selectOneMenu id="filters" value="#{DialogManager.bean.selectedFilter}" converter="ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter"
            valueChangeListener="#{DialogManager.bean.selectedFilterValueChanged}" binding="#{DialogManager.bean.selectedFilterMenu}">
            <f:selectItems value="#{DialogManager.bean.allFilters}" />
         </h:selectOneMenu>
      </h:panelGroup>
   </f:facet>
</h:panelGroup>

<a:panel id="task-search-panel" facetsId="dialog:dialog-body:task-search-panel-facets" styleClass="panel-100" label="#{DialogManager.bean.filterPanelTitle}">
   <r:propertySheetGrid id="task-search-filter" value="#{DialogManager.bean.filter}" columns="1" mode="edit" externalConfig="true"
      labelStyleClass="propertiesLabel" binding="#{DialogManager.bean.propertySheet}" />
</a:panel>

<a:panel id="task-search-filters-panel" styleClass="panel-100" label="#{DialogManager.bean.manageSavedBlockTitle}" rendered="#{DialogManager.bean.showManageSavedDialog}">
   <h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" columnClasses="propertiesLabel," width="100%">
      <h:outputText value="#{msg.task_search_name}" styleClass="no-wrap" />
      <h:inputText id="searchTitle" binding="#{DialogManager.bean.searchTitleInput}" size="35" />
      <%-- empty placeholder to allign checkbox to the right with the lable --%>
      <f:verbatim />
      <a:booleanEvaluator id="foundSimilarEvaluator" value="#{UserService.administrator}">
         <h:panelGroup rendered="#{!DialogManager.bean.reportSearch}">
            <h:selectBooleanCheckbox id="toAllUsers" binding="#{DialogManager.bean.publicCheckBox}" />
            <h:outputLabel value="#{msg.document_search_save_toAllUsers}" for="toAllUsers" />
         </h:panelGroup>
      </a:booleanEvaluator>
   </h:panelGrid>

   <f:verbatim><span class="task-sheet-buttons"></f:verbatim>
   <h:commandButton id="save" actionListener="#{DialogManager.bean.saveFilter}" value="#{msg.save}" />
   <f:verbatim>&nbsp;</f:verbatim>
   <h:commandButton id="delete" actionListener="#{DialogManager.bean.deleteFilter}" value="#{msg.delete}" />
   <f:verbatim></span></f:verbatim>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="task-search-panel-facets">
   <f:facet name="title">
      <h:panelGroup>
         <h:outputText value="#{DialogManager.bean.savedFilterSelectTitle}" />
         <f:verbatim>&nbsp;</f:verbatim>
         <h:selectOneMenu id="filters" value="#{DialogManager.bean.selectedFilter}" converter="ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter"
            valueChangeListener="#{DialogManager.bean.selectedFilterValueChanged}" binding="#{DialogManager.bean.selectedFilterMenu}">
            <f:selectItems value="#{DialogManager.bean.allFilters}" />
         </h:selectOneMenu>
      </h:panelGroup>
   </f:facet>
</h:panelGroup>

<a:panel id="task-search-panel" facetsId="dialog:dialog-body:task-search-panel-facets" styleClass="panel-100" label="#{DialogManager.bean.filterPanelTitle}">
   <r:propertySheetGrid id="task-search-filter" value="#{DialogManager.bean.filter}" columns="1" mode="edit" externalConfig="true"
      labelStyleClass="propertiesLabel" binding="#{DialogManager.bean.propertySheet}" />
</a:panel>

<a:panel id="task-search-filters-panel" styleClass="panel-100" label="#{DialogManager.bean.manageSavedBlockTitle}" rendered="#{DialogManager.bean.showManageSavedDialog}">
   <h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" columnClasses="propertiesLabel," width="100%">
      <h:outputText value="#{msg.task_search_name}" styleClass="no-wrap" />
      <h:inputText id="searchTitle" binding="#{DialogManager.bean.searchTitleInput}" size="35" />
      <%-- empty placeholder to allign checkbox to the right with the lable --%>
      <f:verbatim />
      <a:booleanEvaluator id="foundSimilarEvaluator" value="#{UserService.administrator}">
         <h:panelGroup rendered="#{!DialogManager.bean.reportSearch}">
            <h:selectBooleanCheckbox id="toAllUsers" binding="#{DialogManager.bean.publicCheckBox}" />
            <h:outputLabel value="#{msg.document_search_save_toAllUsers}" for="toAllUsers" />
         </h:panelGroup>
      </a:booleanEvaluator>
   </h:panelGrid>

   <f:verbatim><span class="task-sheet-buttons"></f:verbatim>
   <h:commandButton id="save" actionListener="#{DialogManager.bean.saveFilter}" value="#{msg.save}" />
   <f:verbatim>&nbsp;</f:verbatim>
   <h:commandButton id="delete" actionListener="#{DialogManager.bean.deleteFilter}" value="#{msg.delete}" />
   <f:verbatim></span></f:verbatim>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
