<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel label="#{msg.workflow_workflows}" id="workflow-panel" styleClass="panel-100" progressive="true" expanded="false">

   <a:richList id="workflowSummaryList" viewMode="details" value="#{WorkflowBlockBean.workflowSummaryItems}" var="r" rowStyleClass="workflow-row"
      altRowStyleClass="recordSetRow" width="100%" refreshOnBind="true">
      
      <a:column id="col1" primary="true" rendered="#{not r.taskView}">
         <f:facet name="header">
            <h:outputText value="#{msg.workflow_workflow}" />
         </f:facet>
         <a:actionLink value="#{msg.workflow_hide_task_list}" showLink="false" actionListener="#{WorkflowBlockBean.toggleTaskViewVisible}" styleClass="toggle-task-view" image="/images/icons/arrow_up.gif" rendered="#{r.taskViewVisible}">
            <f:param name="workflowNodeRef" value="#{r.workflowRef}" />
         </a:actionLink>
         <a:actionLink value="#{msg.workflow_show_task_list}" showLink="false" actionListener="#{WorkflowBlockBean.toggleTaskViewVisible}" styleClass="toggle-task-view" image="/images/icons/arrow_down.gif" rendered="#{not r.taskViewVisible}">
            <f:param name="workflowNodeRef" value="#{r.workflowRef}" />
         </a:actionLink>
         <h:outputText id="col1-txt" value="#{r.name}" rendered="#{not r.raisedRights}" />
         <a:actionLink value="#{r.name}" action="dialog:compoundWorkflowDialog" actionListener="#{CompoundWorkflowDialog.setupWorkflow}" rendered="#{r.raisedRights}">
            <f:param value="#{r.compoundWorkflowRef}" name="nodeRef" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col2" rendered="#{not r.taskView}">
         <f:facet name="header">
            <h:outputText value="#{msg.workflow_started}" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.started}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col3" rendered="#{not r.taskView}">
         <f:facet name="header">
            <h:outputText value="#{msg.workflow_stopped}" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.stopped}" >
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col4" rendered="#{not r.taskView}">
         <f:facet name="header">
            <h:outputText value="#{msg.workflow_creator}" />
         </f:facet>
         <h:outputText id="col4-txt" value="#{r.creatorName}" />
      </a:column>
      
      <a:column id="col5" rendered="#{not r.taskView}">
         <f:facet name="header">
            <h:outputText value="#{msg.workflow_responsible}" />
         </f:facet>
         <h:outputText id="col5-txt" value="#{r.responsibleName}" />
      </a:column>
      
      <a:column id="col6" rendered="#{not r.taskView}">
         <f:facet name="header">
            <h:outputText value="#{msg.workflow_status}" />
         </f:facet>
         <h:outputText id="col6-txt" value="#{r.status}" />
      </a:column>
      
      <a:column id="taskView" styleClass="workflow-task-details" colspan="99" rendered="#{r.taskViewRendered}">
         <f:verbatim>
         <div class="task-details-resolution">
         </f:verbatim>
         <h:outputText value="<b>#{msg.task_property_resolution}:</b> " escape="false" /><h:outputText value="#{r.resolution}" />
         <f:verbatim>
         </div>
         </f:verbatim>
         
         <h:dataTable id="taskList" value="#{r.tasks}" var="t" width="100%" rowClasses="recordSetRow,recordSetRowAlt"
            columnClasses="width10,width15,width15,width10,width30,width10,width10">
            
            <h:column id="col1-task">
               <f:facet name="header">
                  <h:outputText id="col1-task-header" value="#{msg.workflow_started}" />
               </f:facet>
               <h:outputText value="#{t.startedDateTime}" >
                  <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
               </h:outputText>
            </h:column>
            
            <h:column id="col2-task">
               <f:facet name="header">
                  <h:outputText id="col2-task-header" value="#{msg.workflow_creator}" />
               </f:facet>
               <h:outputText value="#{t.creatorName}" />
            </h:column>
            
            <h:column id="col3-task">
               <f:facet name="header">
                  <h:outputText id="co3-task-header" value="#{r.taskOwnerRole}" />
               </f:facet>
               <h:outputText value="#{t.ownerName}" />
            </h:column>
            
            <h:column id="col4-task">
               <f:facet name="header">
                  <h:outputText id="co4-task-header" value="#{msg.task_property_due_date}" />
               </f:facet>
               <h:outputText value="#{t.dueDate}" >
                  <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
               </h:outputText>
            </h:column>
            
            <h:column id="col5-task">
               <f:facet name="header">
                  <h:outputText id="col5-task-header" value="#{msg.workflow_completed}" />
               </f:facet>
               <h:outputText value="#{t.completedDateTime}" >
                  <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
               </h:outputText>
            </h:column>
            
            <h:column id="col6-task">
               <f:facet name="header">
                  <h:outputText id="col6-task-header" value="#{msg.task_property_comment_assignmentTask}" />
               </f:facet>
               <h:outputText value="#{t.outcomeAndComments}" />
            </h:column>
            
            <h:column id="col7-task">
               <f:facet name="header">
                  <h:outputText id="col7-task-header" value="#{msg.workflow_status}" />
               </f:facet>
               <h:outputText value="#{t.status}" />
            </h:column>
            
         </h:dataTable>

      </a:column>
      
      <a:column id="assignmentTaskView" styleClass="workflow-task-details" colspan="99" rendered="#{r.assignmentTaskViewRendered}">
         <f:verbatim>
         <div class="task-details-resolution"></div>
         </f:verbatim>
         
         <h:dataTable id="assignmentResponsibleTaskList" value="#{r.assignmentResponsibleTasks}" var="t" width="100%" rowClasses="recordSetRow,recordSetRowAlt"
            columnClasses="width10,width10,width10,width30,width10,width10,width10,width10">
            
            <h:column id="col1-task">
               <f:facet name="header">
                  <h:outputText id="col1-task-header" value="#{msg.workflow_started}" />
               </f:facet>
               <h:outputText value="#{t.startedDateTime}" >
                  <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
               </h:outputText>
            </h:column>
            
            <h:column id="col2-task">
               <f:facet name="header">
                  <h:outputText id="col2-task-header" value="#{msg.workflow_creator}" />
               </f:facet>
               <h:outputText value="#{t.creatorName}" />
            </h:column>
            
            <h:column id="col3-task">
               <f:facet name="header">
                  <h:outputText id="co3-task-header" value="#{msg.assignmentWorkflow_tasks}" />
               </f:facet>
               <h:outputText value="#{t.ownerName}" />
            </h:column>
            
            <h:column id="col4-task">
               <f:facet name="header">
                  <h:outputText id="co4-task-header" value="#{msg.task_property_resolution}" />
               </f:facet>
               <h:panelGroup styleClass="review-note-trimmed-comment">
                  <h:outputText value="#{t.shortResolution}" />
                  <a:booleanEvaluator id="col4-short-text-eval" value="#{t.resolutionLength > 149}">
                     <h:outputLink value="#" title="#{msg.workflow_task_review_show_all}" onclick="javascript: return false;"><h:outputText value=" #{msg.workflow_task_review_show_all}" /></h:outputLink>
                  </a:booleanEvaluator>
               </h:panelGroup>

               <a:booleanEvaluator id="col4-text-eval" value="#{t.resolutionLength > 149}">
                  <h:panelGroup styleClass="review-note-comment" style="display: none;">
                     <h:outputText value="#{r.resolution} " />
                     <h:outputLink value="#" title="#{msg.workflow_task_review_show_trimmed}" onclick="javascript: return false;"><h:outputText value="#{msg.workflow_task_review_show_trimmed}" /></h:outputLink>
                  </h:panelGroup>
               </a:booleanEvaluator>
               
            </h:column>
            
            <h:column id="col5-task">
               <f:facet name="header">
                  <h:outputText id="col5-task-header" value="#{msg.task_property_due_date}" />
               </f:facet>
               <h:outputText value="#{t.dueDate}" >
                  <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
               </h:outputText>
            </h:column>
            
            <h:column id="col6-task">
               <f:facet name="header">
                  <h:outputText id="col6-task-header" value="#{msg.workflow_completed}" />
               </f:facet>
               <h:outputText value="#{t.completedDateTime}" >
                  <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
               </h:outputText>
            </h:column>
            
            <h:column id="col7-task">
               <f:facet name="header">
                  <h:outputText id="col7-task-header" value="#{msg.task_property_comment_assignmentTask}" />
               </f:facet>
                              <h:panelGroup styleClass="review-note-trimmed-comment">
                  <h:outputText value="#{t.outcome} " />
                  <h:outputText value="#{t.shortComment}" />
                  <a:booleanEvaluator id="col4-short-text-eval" value="#{t.commentLength > 149}">
                     <h:outputLink value="#" title="#{msg.workflow_task_review_show_all}" onclick="javascript: return false;"><h:outputText value=" #{msg.workflow_task_review_show_all}" /></h:outputLink>
                  </a:booleanEvaluator>
               </h:panelGroup>

               <a:booleanEvaluator id="col4-text-eval" value="#{t.commentLength > 149}">
                  <h:panelGroup styleClass="review-note-comment" style="display: none;">
                     <h:outputText value="#{t.outcome} " />
                     <h:outputText value="#{t.comment}" />
                     <h:outputLink value="#" title="#{msg.workflow_task_review_show_trimmed}" onclick="javascript: return false;"><h:outputText value="#{msg.workflow_task_review_show_trimmed}" /></h:outputLink>
                  </h:panelGroup>
               </a:booleanEvaluator>
            </h:column>
            
            <h:column id="col8-task">
               <f:facet name="header">
                  <h:outputText id="col8-task-header" value="#{msg.workflow_status}" />
               </f:facet>
               <h:outputText value="#{t.status}" />
            </h:column>
            
         </h:dataTable>
         
         
         <h:dataTable id="assignmentTaskList" value="#{r.assignmentTasks}" rendered="#{not empty r.assignmentTasks}" var="t" width="100%" rowClasses="recordSetRow,recordSetRowAlt"
            columnClasses="width10,width10,width10,width30,width10,width10,width10,width10" styleClass="margin-top-10">
            
            <h:column id="col1-task">
               <f:facet name="header">
                  <h:outputText id="col1-task-header" value="#{msg.workflow_started}" />
               </f:facet>
               <h:outputText value="#{t.startedDateTime}" >
                  <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
               </h:outputText>
            </h:column>
            
            <h:column id="col2-task">
               <f:facet name="header">
                  <h:outputText id="col2-task-header" value="#{msg.workflow_creator}" />
               </f:facet>
               <h:outputText value="#{t.creatorName}" />
            </h:column>
            
            <h:column id="col3-task">
               <f:facet name="header">
                  <h:outputText id="co3-task-header" value="#{msg.task_assignment_role}" />
               </f:facet>
               <h:outputText value="#{t.ownerName}" />
            </h:column>
            
            <h:column id="col4-task">
               <f:facet name="header">
                  <h:outputText id="co4-task-header" value="#{msg.task_property_resolution}" />
               </f:facet>
               <h:panelGroup styleClass="review-note-trimmed-comment">
                  <h:outputText value="#{t.shortResolution}" />
                  <a:booleanEvaluator id="col4-short-text-eval" value="#{t.resolutionLength > 149}">
                     <h:outputLink value="#" title="#{msg.workflow_task_review_show_all}" onclick="javascript: return false;"><h:outputText value=" #{msg.workflow_task_review_show_all}" /></h:outputLink>
                  </a:booleanEvaluator>
               </h:panelGroup>

               <a:booleanEvaluator id="col4-text-eval" value="#{t.resolutionLength > 149}">
                  <h:panelGroup styleClass="review-note-comment" style="display: none;">
                     <h:outputText value="#{r.resolution} " />
                     <h:outputLink value="#" title="#{msg.workflow_task_review_show_trimmed}" onclick="javascript: return false;"><h:outputText value="#{msg.workflow_task_review_show_trimmed}" /></h:outputLink>
                  </h:panelGroup>
               </a:booleanEvaluator>

            </h:column>
            
            <h:column id="col5-task">
               <f:facet name="header">
                  <h:outputText id="col5-task-header" value="#{msg.task_property_due_date}" />
               </f:facet>
               <h:outputText value="#{t.dueDate}" >
                  <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
               </h:outputText>
            </h:column>
            
            <h:column id="col6-task">
               <f:facet name="header">
                  <h:outputText id="col6-task-header" value="#{msg.workflow_completed}" />
               </f:facet>
               <h:outputText value="#{t.completedDateTime}" >
                  <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
               </h:outputText>
            </h:column>
            
            <h:column id="col7-task">
               <f:facet name="header">
                  <h:outputText id="col7-task-header" value="#{msg.task_property_comment_assignmentTask}" />
               </f:facet>
               <h:panelGroup styleClass="review-note-trimmed-comment">
                  <h:outputText value="#{t.outcome} " />
                  <h:outputText value="#{t.shortComment}" />
                  <a:booleanEvaluator id="col4-short-text-eval" value="#{t.commentLength > 149}">
                     <h:outputLink value="#" title="#{msg.workflow_task_review_show_all}" onclick="javascript: return false;"><h:outputText value=" #{msg.workflow_task_review_show_all}" /></h:outputLink>
                  </a:booleanEvaluator>
               </h:panelGroup>

               <a:booleanEvaluator id="col4-text-eval" value="#{t.commentLength > 149}">
                  <h:panelGroup styleClass="review-note-comment" style="display: none;">
                     <h:outputText value="#{t.outcome} " />
                     <h:outputText value="#{t.comment}" />
                     <h:outputLink value="#" title="#{msg.workflow_task_review_show_trimmed}" onclick="javascript: return false;"><h:outputText value="#{msg.workflow_task_review_show_trimmed}" /></h:outputLink>
                  </h:panelGroup>
               </a:booleanEvaluator>
                              
            </h:column>
            
            <h:column id="col8-task">
               <f:facet name="header">
                  <h:outputText id="col8-task-header" value="#{msg.workflow_status}" />
               </f:facet>
               <h:outputText value="#{t.status}" />
            </h:column>
            
         </h:dataTable>

      </a:column>
      
   </a:richList>
   
</a:panel>