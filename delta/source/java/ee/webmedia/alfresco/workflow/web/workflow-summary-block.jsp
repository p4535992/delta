<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="workflowSummaryBlock" label="#{msg.workflows}" progressive="true" expanded="false">
   <a:richList viewMode="details" refreshOnBind="true" id="workflowList" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" value="#{WorkflowBlockBean.workflowBlockItems}" var="r" >
      
      <%-- startedDateTime --%>
      <a:column id="col1" primary="true" rendered="#{r.task}">
         <f:facet name="header">
            <h:outputText id="col1-sort" value="#{msg.workflow_started}" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.startedDateTime}">
            <a:convertXMLDate pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- dueDate --%>
      <a:column id="col2" rendered="#{r.task}">
         <f:facet name="header">
            <h:outputText id="col2-sort" value="#{msg.task_property_due_date}" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.dueDate}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- taskCreatorName --%>
      <a:column id="col3" rendered="#{r.task}">
         <f:facet name="header">
            <h:outputText id="col3-sort" value="#{msg.workflow_creator}" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.taskCreatorName}" />
      </a:column>
      
      <%-- workflow --%>
      <a:column id="col4" rendered="#{r.task}">
         <f:facet name="header">
            <h:outputText id="col4-sort" value="#{msg.workflow}" styleClass="header" />
         </f:facet>
         <h:panelGroup rendered="#{r.raisedRights}">
            <a:actionLink  value="#{r.workflowType}" action="dialog:compoundWorkflowDialog" actionListener="#{CompoundWorkflowDialog.setupWorkflow}" styleClass="workflow-conf">
               <f:param name="nodeRef" value="#{r.compoundWorkflowNodeRef}" />
            </a:actionLink>
         </h:panelGroup>
         <h:outputText rendered="#{!r.raisedRights}" id="col4-text" value="#{r.workflowType}" />
      </a:column>
      
      <%-- taskOwnerName --%>
      <a:column id="col5" rendered="#{r.task}">
         <f:facet name="header">
            <h:outputText id="col5-sort" value="#{msg.task_property_owner}" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.taskOwnerName}" />
      </a:column>
      
      <%-- taskResolution --%>
      <a:column id="col6" rendered="#{r.task}">
         <f:facet name="header">
            <h:outputText id="col6-sort" value="#{msg.task_property_resolution}" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-text" value="#{r.taskResolution}" styleClass="condence150" />
      </a:column>
      
      <%-- outcome --%>
      <a:column id="col7" rendered="#{r.task}">
         <f:facet name="header">
            <h:outputText id="col7-header" value="#{msg.task_property_comment_assignmentTask}" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.taskOutcome}" escape="false" styleClass="condence150" />
      </a:column>
      
      <%-- taskStatus --%>
      <a:column id="col8" rendered="#{r.task}">
         <f:facet name="header">
            <h:outputText id="col8-sort" value="#{msg.workflow_status}" styleClass="header" />
         </f:facet>
         <h:outputText id="col8-text" value="#{r.taskStatus}" />
      </a:column>
      
      <%-- separator --%>
      <a:column id="sep" rendered="#{r.separator}" colspan="8" styleClass="workflow-separator" >
         <f:verbatim><hr /></f:verbatim>
      </a:column>
      <a:column id="sep-zebra" rendered="#{r.zebra}" colspan="8" styleClass="workflow-separator-zebra" />
   
   </a:richList>
</a:panel>
