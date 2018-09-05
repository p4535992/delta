<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<h:outputText escape="false" value="<h2><strong>Telli teavitus</strong></h2>" styleClass="dialogpanel-title" />
<h:outputText value="#{msg.notification_subscribe_to_following_notifications}" />
<h:outputText value="<br/>" escape="false" />
	

<r:propertySheetGrid value="#{NotificationDialog.userPrefsNode}" var="node" finishButtonId="dialog:finish-button" columns="1">


<h:panelGrid styleClass="table-padding" columns="1"> 

    	
	<a:panel id="notifications-to-me-panel" label="#{msg.notification_notifications_related_to_tasks_assigned_to_me}" progressive="true">    	
    	<h:panelGrid columns="1" style="width: 100%;">
    
      		<h:panelGroup>
      			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}newTaskNotification']}" />
      			<h:outputText value="#{msg.notification_newTaskNotification_label}" />
      		</h:panelGroup>
      	
        	<h:panelGroup>
      			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}taskCancelled']}" />
      			<h:outputText value="#{msg.notification_taskCancelled_label}" />
      		</h:panelGroup> 
       
        	<h:panelGroup>
      			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}cancelledTaskNotification']}" />
      			<h:outputText value="#{msg.notification_cancelledTaskNotification_label}" />
        	</h:panelGroup>
		</h:panelGrid>
	</a:panel>      
        
        
    <a:panel id="mydocuments-panel" label="#{msg.notification_notifications_related_to_documents_created_by_me}" progressive="true">   
    	<h:panelGrid columns="1" style="width: 100%;">
        
       		<h:panelGroup>
     			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}opinionTaskCompleted']}" />
        		<h:outputText value="#{msg.notification_opinionTaskCompleted_label}" />
        	</h:panelGroup>
      
        	<h:panelGroup>	
        		<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}signatureTaskCompleted']}" />
      			<h:outputText value="#{msg.notification_signatureTaskCompleted_label}" />
      		</h:panelGroup>
        
        	<h:panelGroup>
      			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewTaskCompleted']}" />
      			<h:outputText value="#{msg.notification_reviewTaskCompleted_label}" />
      		</h:panelGroup>
      
        	<h:panelGroup>
      			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewTaskCompletedNotAccepted']}" />
      			<h:outputText value="#{msg.notification_reviewTaskCompletedNotAccepted_label}" />
      		</h:panelGroup>
      		
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewTaskCompletedWithRemarks']}" />
      			<h:outputText value="#{msg.notification_reviewTaskCompletedWithRemarks_label}" />
      		</h:panelGroup>
      
        	<h:panelGroup>
      			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}confirmationTaskCompleted']}" rendered="#{NotificationDialog.showConfirmationTaskData}" />
      			<h:outputText value="#{msg.notification_confirmationTaskCompleted_label}" rendered="#{NotificationDialog.showConfirmationTaskData}"  />
        	</h:panelGroup>
        
        	<h:panelGroup>
      			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}confirmationTaskCompletedNotAccepted']}" rendered="#{NotificationDialog.showConfirmationTaskData}" />
      			<h:outputText value="#{msg.notification_confirmationTaskCompletedNotAccepted_label}" rendered="#{NotificationDialog.showConfirmationTaskData}" /> 
        	</h:panelGroup>
       
       		<h:panelGroup>
      			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}orderAssignmentTaskCompleted']}" rendered="#{NotificationDialog.showOrderAssignmentTaskData}" />
      			<h:outputText value="#{msg.notification_orderAssignmentTaskCompleted_label}" rendered="#{NotificationDialog.showOrderAssignmentTaskData}" />
       		</h:panelGroup>
		</h:panelGrid>
	</a:panel> 
      
      
	<a:panel id="mytasks-panel" label="#{msg.notification_notifications_related_to_tasks_started_by_me}" progressive="true">
		<h:panelGrid columns="1" style="width: 100%;">	  
	  
	 		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}assignmentTaskCompletedByCoResponsible']}" />
      			<h:outputText value="#{msg.notification_assignmentTaskCompletedByCoResponsible_label}" />	     
      		</h:panelGroup>
      
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}informationTaskCompleted']}" />
      			<h:outputText value="#{msg.notification_informationTaskCompleted_label}" />	     
      		</h:panelGroup>
      
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}delegatedTaskCompleted']}" />
      			<h:outputText value="#{msg.notification_delegatedTaskCompleted_label}" /> 
      		</h:panelGroup>
      
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}taskDueDateExtensionTaskCompleted']}" />
      			<h:outputText value="#{msg.notification_taskDueDateExtensionTaskCompleted_label}" />
      		</h:panelGroup>
      
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}taskDueDateExtensionTaskCompletedNotAccepted']}" />
      			<h:outputText value="#{msg.notification_taskDueDateExtensionTaskCompletedNotAccepted_label}" />
      		</h:panelGroup>
      
     		 <h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}workflowCompleted']}" />
      			<h:outputText value="#{msg.notification_workflowCompleted_label}" />
      		</h:panelGroup>
      
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}newWorkflowStarted']}" />
      			<h:outputText value="#{msg.notification_newWorkflowStarted_label}" />
      		</h:panelGroup>
		</h:panelGrid>
	</a:panel>
            
    <a:panel id="mycompletedtasks-panel" label="#{msg.notification_notifications_related_to_tasks_completed_by_me}" progressive="true">      
    	<h:panelGrid columns="1" style="width: 100%;">
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewedDocumentReviewed']}" />
      			<h:outputText value="#{msg.notification_reviewedDocumentReviewed_label}" />
      		</h:panelGroup>
      		
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewedDocumentReviewedNotAccepted']}" />
      			<h:outputText value="#{msg.notification_reviewedDocumentReviewedNotAccepted_label}" />
      		</h:panelGroup>
      		
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewedDocumentReviewedWithRemarks']}" />
      			<h:outputText value="#{msg.notification_reviewedDocumentReviewedWithRemarks_label}" />
      		</h:panelGroup>
      		
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewedDocumentConfirmed']}" />
      			<h:outputText value="#{msg.notification_reviewedDocumentConfirmed_label}" />
      		</h:panelGroup>
      		
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewedDocumentConfirmedNotAccepted']}" />
      			<h:outputText value="#{msg.notification_reviewedDocumentConfirmedNotAccepted_label}" />
      		</h:panelGroup>
      
     		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewDocumentSigned']}" />
      			<h:outputText value="#{msg.notification_reviewedDocumentSigned_label}" />
     		</h:panelGroup>
      
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}reviewDocumentNotSigned']}" />
      			<h:outputText value="#{msg.notification_reviewedDocumentNotSigned}" /> 
      		</h:panelGroup>
      
      		<h:panelGroup>
	  			<h:selectBooleanCheckbox value="#{node.properties['{http://alfresco.webmedia.ee/model/notification/1.0}groupAssignmentTaskCompletedByOthers']}" rendered="#{NotificationDialog.showGroupAssignmentTaskData}" />
      			<h:outputText value="#{msg.notification_groupAssignmentTaskCompletedByOthers_label}" rendered="#{NotificationDialog.showGroupAssignmentTaskData}" />  
      		</h:panelGroup>
		</h:panelGrid>
	</a:panel>	


</h:panelGrid>
</r:propertySheetGrid>



<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />