<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="applogSetup-panel" label="#{msg.applog_setup}" styleClass="panel-100" progressive="true">
   <h:panelGrid columns="1">
      <h:panelGroup>
         <h:selectBooleanCheckbox id="logDocument" value="#{ApplicationLogDialog.logSetup.documents}" title="#{msg.applog_log_documents}" />
         <h:outputLabel for="logDocument" value="#{msg.applog_log_documents}" />
      </h:panelGroup>

      <h:panelGroup>
         <h:selectBooleanCheckbox id="logWorkflow" value="#{ApplicationLogDialog.logSetup.workflows}" title="#{msg.applog_log_workflows}" />
         <h:outputLabel for="logWorkflow" value="#{msg.applog_log_workflows}" />
      </h:panelGroup>

      <h:panelGroup>
         <h:selectBooleanCheckbox id="logLogInOuts" value="#{ApplicationLogDialog.logSetup.logInOuts}" title="#{msg.applog_log_loginouts}" />
         <h:outputLabel for="logLogInOuts" value="#{msg.applog_log_loginouts}" />
      </h:panelGroup>

      <h:panelGroup>
         <h:selectBooleanCheckbox id="logUserUsergroup" value="#{ApplicationLogDialog.logSetup.userUsergroups}" title="#{msg.applog_log_user_usergroups}" />
         <h:outputLabel for="logUserUsergroup" value="#{msg.applog_log_user_usergroups}" />
      </h:panelGroup>

      <h:panelGroup>
         <h:selectBooleanCheckbox id="logSpaces" value="#{ApplicationLogDialog.logSetup.spaces}" title="#{msg.applog_log_spaces}" />
         <h:outputLabel for="logSpaces" value="#{msg.applog_log_spaces}" />
      </h:panelGroup>

      <h:panelGroup>
         <h:selectBooleanCheckbox id="logSearches" value="#{ApplicationLogDialog.logSetup.searches}" title="#{msg.applog_log_searches}" />
         <h:outputLabel for="logSearches" value="#{msg.applog_log_searches}" />
      </h:panelGroup>

      <h:panelGroup>
         <h:selectBooleanCheckbox id="logNotices" value="#{ApplicationLogDialog.logSetup.notices}" title="#{msg.applog_log_notices}" />
         <h:outputLabel for="logNotices" value="#{msg.applog_log_notices}" />
      </h:panelGroup>

      <h:panelGroup>
         <h:selectBooleanCheckbox id="logDeletedObjs" value="#{ApplicationLogDialog.logSetup.deletedObjects}" title="#{msg.applog_log_deleted_objs}" />
         <h:outputLabel for="logDeletedObjs" value="#{msg.applog_log_deleted_objs}" />
      </h:panelGroup>

      <h:panelGroup>
         <h:commandButton action="save" value="#{msg.applog_save}" actionListener="#{ApplicationLogDialog.save}" />
      </h:panelGroup>
   </h:panelGrid>
</a:panel>

<a:panel id="applogFilter-panel" facetsId="dialog:dialog-body:applog-search-panel-facets" styleClass="panel-100" label="#{msg.applog_filter}">

   <r:propertySheetGrid id="log-search-filter" value="#{ApplicationLogDialog.filter}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel"
      binding="#{ApplicationLogDialog.propertySheet}" />

   <h:commandButton action="search" value="#{msg.applog_search}" actionListener="#{ApplicationLogDialog.search}" />
</a:panel>
