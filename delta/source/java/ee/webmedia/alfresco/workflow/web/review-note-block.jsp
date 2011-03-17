<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="review-note-block" rendered="#{WorkflowBlockBean.reviewNoteBlockRendered}" label="#{msg.workflow_task_review_notes}" progressive="true"
   expanded="false" styleClass="with-pager">
   <a:richList id="reviewNoteList" viewMode="details" value="#{WorkflowBlockBean.finishedReviewTasks}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true" pageSize="#{BrowseBean.pageSizeContent}" initialSortColumn="completedDateTime" initialSortDescending="false">

      <a:column id="col1" primary="true" style="width: 20%">
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.workflow_task_reviewer_name}" value="ownerName" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-txt" value="#{r.ownerName}" />
      </a:column>

      <a:column id="col2" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.workflow_date}" value="completedDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.completedDateTime}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col3" style="width: 70%;">
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.workflow_task_review_note}" value="outcome" styleClass="header" />
         </f:facet>

         <h:panelGroup styleClass="review-note-comment">
            <h:outputText value="#{r.outcome}: " />
            <h:outputText value="#{r.comment} " styleClass="condence150"/>
         </h:panelGroup>

      </a:column>

      <a:dataPager id="reviewNotePager" styleClass="pager" />
   </a:richList>
</a:panel>