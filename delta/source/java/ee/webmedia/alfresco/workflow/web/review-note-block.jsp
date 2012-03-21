<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@page import="javax.faces.context.FacesContext"%>
<%@page import="ee.webmedia.alfresco.utils.ComponentUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<% String printButtonTooltip = MessageUtil.getMessage("workflow_task_review_notes_print"); %>

<h:panelGroup id="review-note-block-facets">
   <f:facet name="title">
      <f:verbatim>
         <a target="_blank" href="<%= BeanHelper.getWorkflowBlockBean().getReviewNotesPrintUrl() %>" class="print icon-link" title="<%= printButtonTooltip %>"></a>
      </f:verbatim>
   </f:facet>
</h:panelGroup>

<a:panel id="review-note-block" rendered="#{WorkflowBlockBean.reviewNoteBlockRendered}" label="#{msg.workflow_task_review_notes}" progressive="true"
   expanded="false" styleClass="with-pager" facetsId="dialog:dialog-body:review-note-block-facets">
   <a:richList id="reviewNoteList" viewMode="details" value="#{WorkflowBlockBean.finishedReviewTasks}" var="r" rowStyleClass="recordSetRow" binding="#{WorkflowBlockBean.reviewNotesRichList}"
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

      <a:column id="col3" style="width: 50%;">
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.workflow_task_review_note}" value="outcome" styleClass="header" />
         </f:facet>

         <h:panelGroup styleClass="review-note-comment">
            <h:outputText value="#{r.outcome}: " />
            <h:outputText value="#{r.commentAndLinks} " styleClass="condence150" escape="false"/>
         </h:panelGroup>

      </a:column>
      
      <a:column id="col4" style="width: 20%">
         <f:facet name="header">
            <a:sortLink id="col4-header" label="#{msg.workflow_task_review_file_versions}" value="fileVersions" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-txt" value="#{r.fileVersions}" />
      </a:column>      

      <a:dataPager id="reviewNotePager" styleClass="pager" />
   </a:richList>
</a:panel>
