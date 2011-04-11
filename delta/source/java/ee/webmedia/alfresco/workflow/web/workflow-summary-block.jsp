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
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.workflow_started}" value="startedDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.startedDateTime}">
            <a:convertXMLDate pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- dueDate --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.task_property_due_date}" value="dueDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.dueDate}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- taskCreatorName --%>
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.workflow_creator}" value="taskCreatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.taskCreatorName}" />
      </a:column>
      
      <%-- workflow --%>
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.workflow}" value="workflowType" styleClass="header" />
         </f:facet>
         <h:panelGroup rendered="#{r.raisedRights}">
            <a:actionLink  value="#{r.workflowType}" action="dialog:compoundWorkflowDialog" actionListener="#{CompoundWorkflowDialog.setupWorkflow}" styleClass="workflow-conf">
               <f:param name="nodeRef" value="#{r.compoundWorkflowNodeRef}" />
            </a:actionLink>
         </h:panelGroup>
         <h:outputText rendered="#{!r.raisedRights}" id="col4-text" value="#{r.workflowType}" />
      </a:column>
      
      <%-- taskOwnerName --%>
      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.task_property_owner}" value="taskOwnerName" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.taskOwnerName}" />
      </a:column>
      
      <%-- taskResolution --%>
      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.task_property_resolution}" value="taskResolution" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-text" value="#{r.taskResolution}" styleClass="condence150" />
      </a:column>
      
      <%-- outcome --%>
      <a:column id="col7">
         <f:facet name="header">
            <h:outputText id="col7-header" value="#{msg.task_property_comment_assignmentTask}" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.taskOutcome}" escape="false" styleClass="condence150" />
      </a:column>
      
      <%-- taskStatus --%>
      <a:column id="col8">
         <f:facet name="header">
            <a:sortLink id="col8-sort" label="#{msg.workflow_status}" value="taskStatus" styleClass="header" />
         </f:facet>
         <h:outputText id="col8-text" value="#{r.taskStatus}" />
      </a:column>
   
   </a:richList>
</a:panel>
