<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="notifications-panel" label="#{msg.notification_ordering_of_email_notifications}">
<h:outputText value="#{msg.notification_subscribe_to_following_notifications}" styleClass="dialogpanel-title" />

<r:propertySheetGrid value="#{NotificationDialog.userPrefsNode}" var="r" finishButtonId="dialog:finish-button">

   <h:panelGrid columns="2">

      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}newTaskNotification']}" />
      <h:outputText value="#{msg.notification_newTaskNotification_label}" />
      
      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}signatureTaskCompleted']}" />
      <h:outputText value="#{msg.notification_signatureTaskCompleted_label}" />
      
      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}opinionTaskCompleted']}" />
      <h:outputText value="#{msg.notification_opinionTaskCompleted_label}" />
      
      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}assignmentTaskCompletedByCoResponsible']}" />
      <h:outputText value="#{msg.notification_assignmentTaskCompletedByCoResponsible_label}" />
      
      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewTaskCompleted']}" />
      <h:outputText value="#{msg.notification_reviewTaskCompleted_label}" />
      
      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewTaskCompletedNotAccepted']}" />
      <h:outputText value="#{msg.notification_reviewTaskCompletedNotAccepted_label}" />
      
      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewTaskCompletedWithRemarks']}" />
      <h:outputText value="#{msg.notification_reviewTaskCompletedWithRemarks_label}" />
      
      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}informationTaskCompleted']}" />
      <h:outputText value="#{msg.notification_informationTaskCompleted_label}" />
      
      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}workflowCompleted']}" />
      <h:outputText value="#{msg.notification_workflowCompleted_label}" />
      
      <h:selectBooleanCheckbox value="#{r.properties['{http://alfresco.webmedia.ee/model/notification/1.0}newWorkflowStarted']}" />
      <h:outputText value="#{msg.notification_newWorkflowStarted_label}" />
   
   </h:panelGrid>

</r:propertySheetGrid>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />