<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<a:panel id="order-assignment-note-block" rendered="#{WorkflowBlockBean.orderAssignmentNoteBlockRendered}" label="#{msg.workflow_task_order_assignment_assignments}" progressive="true"
   expanded="false" styleClass="with-pager">
   <a:richList id="orderAssigmnentNoteList" viewMode="details" value="#{WorkflowBlockBean.finishedOrderAssignmentTasks}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true" pageSize="#{BrowseBean.pageSizeContent}" initialSortColumn="completedDateTime" initialSortDescending="false">

      <a:column id="col1" primary="true" style="width: 20%">
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.workflow_task_order_assignment_executor_role}" value="ownerName" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
         <h:outputText id="col1-txt" value="#{r.ownerName}" />
=======
         <h:outputText id="col1-txt" value="#{r.ownerNameWithSubstitute}" />
>>>>>>> develop-5.1
      </a:column>

      <a:column id="col2" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.workflow_date}" value="completedDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.completedDateTime}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col3" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.workflow_file}" value="file" styleClass="header" />
         </f:facet>
         <wm:customChildrenContainer id="order-assignment-files" childGenerator="#{WorkflowBlockBean.noteBlockRowFileGenerator}" parameterList="#{r.files}"/>
      </a:column>

      <a:column id="col4" style="width: 60%;">
         <f:facet name="header">
            <a:sortLink id="col4-header" label="#{msg.workflow_task_order_assignment_note}" value="outcome" styleClass="header" />
         </f:facet>
         <h:panelGroup styleClass="review-note-comment" >
            <h:outputText value="#{r.comment} " styleClass="condence200"/>
         </h:panelGroup>

      </a:column>

      <a:dataPager id="opinionNotePager" styleClass="pager" />
   </a:richList>
</a:panel>