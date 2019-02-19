<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil" %>

<% String historyLinkLabel = MessageUtil.getMessage("task_due_date_history_show_history"); %>

<a:panel id="workflowSummaryBlock" styleClass="with-pager" label="#{msg.tasks}" progressive="true" expanded="<%=new Boolean(BeanHelper.getWorkflowBlockBean().isWorkflowSummaryBlockExpanded()).toString() %>">
   <a:richList id="workflowList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
               value="#{WorkflowBlockBean.workflowBlockItemDataProvider}" var="r" refreshOnBind="true" >
      
      <%-- startedDateTime --%>
      <a:column id="col1" primary="true" styleClass="#{r.separatorClass}" >
         <f:facet name="header">
            <h:outputText id="col1-sort" value="#{msg.workflow_started}" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.startedDateTime}">
            <a:convertXMLDate pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- dueDate --%>
      <a:column id="col2" styleClass="#{r.separatorClass}">
         <f:facet name="header">
            <h:outputText id="col2-sort" value="#{msg.task_property_due_date}" styleClass="header" />
         </f:facet>
            <h:outputText id="col2-text" value="#{r.dueDate}">
               <a:convertXMLDate pattern="#{msg.date_time_pattern}" />
            </h:outputText>
            <h:outputText id="col2-br" value="<br/>" escape="false"/>
            <a:actionLink id="task-list-due-date-history-link" value="<%=historyLinkLabel%>" onclick="return showModal('#{r.dueDateHistoryModalId}');" rendered="#{r.showDueDateHistoryModal}"/>
      </a:column>

      <%-- taskCreatorName --%>
      <a:column id="col3" styleClass="#{r.separatorClass}">
         <f:facet name="header">
            <h:outputText id="col3-sort" value="#{msg.workflow_creator}" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.taskCreatorName}" rendered="#{NavigationBean.currentUser.fullName != r.taskCreatorName}"/>
         <h:outputText id="col3b-text" value="#{r.taskCreatorName}" rendered="#{NavigationBean.currentUser.fullName == r.taskCreatorName}" styleClass="bold"/>
      </a:column>
      
      <%-- workflow --%>
      <a:column id="col4" styleClass="#{r.separatorClass}">
         <f:facet name="header">
            <h:outputText id="col4-sort" value="#{msg.workflow}" styleClass="header" />
         </f:facet>
         <h:panelGroup id="col4-panel" rendered="#{WorkflowBlockBean.inWorkspace and r.raisedRights}">
            <a:actionLink id="col4-act" value="#{r.workflowType}" action="dialog:compoundWorkflowDialog" actionListener="#{CompoundWorkflowDialog.setupWorkflow}" styleClass="workflow-conf" >
               <f:param name="nodeRef" value="#{r.compoundWorkflowNodeRef}" />
            </a:actionLink>
         </h:panelGroup>
         <h:outputText rendered="#{!WorkflowBlockBean.inWorkspace or !r.raisedRights}" id="col4-text" value="#{r.workflowType}" />
      </a:column>
      
      <%-- taskOwnerName --%>
      <a:column id="col5" styleClass="#{r.separatorClass}">
         <f:facet name="header">
            <h:outputText id="col5-sort" value="#{msg.task_property_owner}" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-act" value="#{r.groupName}" styleClass="workflow-conf" href="#{r.workflowGroupTasksUrl}" target="_blank" image="/images/icons/plus.gif" rendered="#{r.groupBlockItem}" /> 
         <h:outputText id="col5-text" value="#{r.taskOwnerName}" rendered="#{!r.groupBlockItem}" />
      </a:column>
      
      <%-- taskResolution --%>
      <a:column id="col6" styleClass="#{r.separatorClass}">
         <f:facet name="header">
            <h:outputText id="col6-sort" value="#{msg.task_property_resolution}" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-text" value="#{r.taskResolution}" styleClass="condence150" />
      </a:column>
      
      <%-- outcome --%>
      <a:column id="col7" styleClass="#{r.separatorClass}">
         <f:facet name="header">
            <h:outputText id="col7-header" value="#{msg.task_property_comment_assignmentTask}" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.taskOutcomeWithSubstituteNote}" escape="false" styleClass="condence150" />
      </a:column>
      
      <%-- taskStatus --%>
      <a:column id="col8" styleClass="#{r.separatorClass}">
         <f:facet name="header">
            <h:outputText id="col8-sort" value="#{msg.workflow_status}" styleClass="header" />
         </f:facet>
         <h:outputText id="col8-text" value="#{r.taskStatus}" />
      </a:column>

       <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
       <a:dataPager id="workflowSummaryPager" styleClass="pager" />
   </a:richList>
   
   <h:panelGroup id="task-due-date-history-modals" binding="#{WorkflowBlockBean.dueDateHistoryModalPanel}"/>

</a:panel>
