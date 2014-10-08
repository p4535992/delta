<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/limited-message-panel.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/linked-review-task-redirect-js.jsp" />

<a:panel id="task-panel" styleClass="panel-100 with-pager" label="#{msg.task_search_results}" progressive="true">
   <a:panel id="task-panel-search-results" styleClass="overflow-wrapper">   

   <a:richList id="taskList" styleClass="duplicate-header" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      value="#{TaskSearchResultsDialog.tasks}" var="r" binding="#{TaskSearchResultsDialog.richList}" >

      <a:column id="col1" primary="true" styleClass="#{r.cssStyleClass}" rendered="#{WmWorkflowService.documentWorkflowEnabled}" >
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.task_search_result_regNum}" value="regNum" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-txt" value="#{r.regNum}" action="#{DocumentDialog.action}" tooltip="#{r.regNum}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col1-txt1" value="#{r.regNum}" action="dialog:compoundWorkflowDialog" tooltip="#{r.regNum}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col1-txt2" value="#{r.regNum}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.regNum}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col2" primary="true" styleClass="#{r.cssStyleClass}" rendered="#{WmWorkflowService.documentWorkflowEnabled}" >
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.task_search_result_regDate}" value="regDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-txt" value="#{r.regDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.regDateStr}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col2-txt1" value="#{r.regDateStr}" action="dialog:compoundWorkflowDialog" tooltip="#{r.regDateStr}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col2-txt2" value="#{r.regDateStr}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.regDateStr}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col3" primary="true" styleClass="#{r.cssStyleClass}" rendered="#{WmWorkflowService.documentWorkflowEnabled}" >
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.task_search_result_docType}" value="docType" styleClass="header" />
         </f:facet>
          <a:actionLink id="col3-txt" value="#{r.docType}" action="#{DocumentDialog.action}" tooltip="#{r.docType}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col3-txt1" value="#{r.docType}" action="dialog:compoundWorkflowDialog" tooltip="#{r.docType}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col3-txt2" value="#{r.docType}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.docType}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col4" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-header" label="#{msg.task_search_result_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-txt" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="tooltip condence20- no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}">
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col4-txt1" value="#{r.compoundWorkflow.title}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflow.title}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="tooltip condence20- no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col4-txt2" value="#{r.docName}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.docName}" showLink="false" target="_blank" styleClass="tooltip condence20- no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col5" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col5-header" label="#{msg.task_search_result_creatorName}" value="creatorName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-txt" value="#{r.creatorName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col5-txt1" value="#{r.creatorName}" action="dialog:compoundWorkflowDialog" tooltip="#{r.creatorName}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col5-txt2" value="#{r.creatorName}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.creatorName}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col6" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-header" label="#{msg.task_search_result_startedDate}" value="startedDate" styleClass="header" />
         </f:facet>
          <a:actionLink id="col6-txt" value="#{r.startedDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.startedDateStr}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col6-txt1" value="#{r.startedDateStr}" action="dialog:compoundWorkflowDialog" tooltip="#{r.startedDateStr}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col6-txt2" value="#{r.startedDateStr}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.startedDateStr}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col7" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col7-header" label="#{msg.task_search_result_ownerName}" value="ownerName" styleClass="header-wrap" />
         </f:facet>
         <a:actionLink id="col7-txt" value="#{r.ownerName}" action="#{DocumentDialog.action}" tooltip="#{r.ownerName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col7-txt1" value="#{r.ownerName}" action="dialog:compoundWorkflowDialog" tooltip="#{r.ownerName}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col7-txt2" value="#{r.ownerName}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.ownerName}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col8" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col8-header" label="#{msg.task_search_result_ownerOrganizationName}" value="ownerOrganizationName" styleClass="header" />
         </f:facet>
          <a:actionLink id="col8-txt" value="#{r.ownerOrganizationName}" action="#{DocumentDialog.action}" tooltip="#{r.ownerOrganizationName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col8-txt1" value="#{r.ownerOrganizationName}" action="dialog:compoundWorkflowDialog" tooltip="#{r.ownerOrganizationName}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col8-txt2" value="#{r.ownerOrganizationName}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.ownerOrganizationName}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col9" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col9-header" label="#{msg.task_search_result_ownerJobTitle}" value="ownerJobTitle" styleClass="header" />
         </f:facet>
           <a:actionLink id="col9-txt" value="#{r.ownerJobTitle}" action="#{DocumentDialog.action}" tooltip="#{r.ownerJobTitle}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
           <a:actionLink id="col9-txt1" value="#{r.ownerJobTitle}" action="dialog:compoundWorkflowDialog" tooltip="#{r.ownerJobTitle}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col9-txt2" value="#{r.ownerJobTitle}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.ownerJobTitle}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col10" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col10-header" label="#{msg.task_search_result_taskType}" value="taskTypeText" styleClass="header" />
         </f:facet>
         <a:actionLink id="col10-txt" value="#{r.taskTypeText}" action="#{DocumentDialog.action}" tooltip="#{r.taskTypeText}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col10-txt1" value="#{r.taskTypeText}" action="dialog:compoundWorkflowDialog" tooltip="#{r.taskTypeText}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col10-txt2" value="#{r.taskTypeText}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.taskTypeText}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col11" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col11-header" label="#{msg.task_search_result_dueDate}" value="dueDate" styleClass="header" />
         </f:facet>
          <a:actionLink id="col11-txt" value="#{r.dueDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.dueDateStr}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col11-txt1" value="#{r.dueDateStr}" action="dialog:compoundWorkflowDialog" tooltip="#{r.dueDateStr}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col11-txt2" value="#{r.dueDateStr}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.dueDateStr}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col12" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col12-header" label="#{msg.task_search_result_completedDate}" value="completedDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col12-txt" value="#{r.completedDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.completedDateStr}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col12-txt1" value="#{r.completedDateStr}" action="dialog:compoundWorkflowDialog" tooltip="#{r.completedDateStr}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col12-txt2" value="#{r.completedDateStr}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.completedDateStr}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col13" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col13-header" label="#{msg.task_search_result_comment}" value="comment" styleClass="header" />
         </f:facet>
         <a:actionLink id="col13-txt" value="#{r.comment}" action="#{DocumentDialog.action}" tooltip="#{r.comment}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col13-txt1" value="#{r.comment}" action="dialog:compoundWorkflowDialog" tooltip="#{r.comment}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col13-txt2" value="#{r.comment}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.comment}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col14" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col14-header" label="#{msg.task_search_result_responsible}" value="responsible" styleClass="header" />
         </f:facet>
         <a:actionLink id="col14-txt" value="#{r.responsible}" action="#{DocumentDialog.action}" tooltip="#{r.responsible}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col14-txt1" value="#{r.responsible}" action="dialog:compoundWorkflowDialog" tooltip="#{r.responsible}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col14-txt2" value="#{r.responsible}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.responsible}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col15" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col15-header" label="#{msg.task_search_result_stoppedDate}" value="stoppedDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col15-txt" value="#{r.stoppedDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.stoppedDateStr}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col15-txt1" value="#{r.stoppedDateStr}" action="dialog:compoundWorkflowDialog" tooltip="#{r.stoppedDateStr}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col15-txt2" value="#{r.stoppedDateStr}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.stoppedDateStr}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col16" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col16-header" label="#{msg.task_search_result_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <a:actionLink id="col16-txt" value="#{r.resolution}" action="#{DocumentDialog.action}" tooltip="#{r.resolution}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="tooltip condence20- no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col16-txt1" value="#{r.resolution}" action="dialog:compoundWorkflowDialog" tooltip="#{r.resolution}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="tooltip condence20- no-underline"  rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col16-txt2" value="#{r.resolution}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.resolution}" showLink="false" target="_blank" styleClass="tooltip condence20- no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col17" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col17-header" label="#{msg.task_search_result_overdue}" value="overdue" styleClass="header-wrap" />
         </f:facet>
          <a:actionLink id="col17-txt" value="#{r.overdue}" action="#{DocumentDialog.action}" tooltip="#{r.overdue}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col17-txt1" value="#{r.overdue}" action="dialog:compoundWorkflowDialog" tooltip="#{r.overdue}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col17-txt2" value="#{r.overdue}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.overdue}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col18" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col18-header" label="#{msg.task_search_result_status}" value="status" styleClass="header" />
         </f:facet>
          <a:actionLink id="col18-txt" value="#{r.status}" action="#{DocumentDialog.action}" tooltip="#{r.status}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col18-txt1" value="#{r.status}" action="dialog:compoundWorkflowDialog" tooltip="#{r.status}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col18-txt2" value="#{r.status}" onclick="return redirectLinkedReviewTask('#{r.originalTaskObjectUrl}')" tooltip="#{r.status}" showLink="false" target="_blank" styleClass="no-underline originalTaskObjectUrl" rendered="#{r.linkedReviewTask}" />
      </a:column>
      
      <a:column id="col19" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col19-header" label="#{msg.task_search_result_compound_workflow_type}" value="compoundWorkflowType" styleClass="header" />
         </f:facet>
          <a:actionLink id="col19-txt" value="#{r.compoundWorkflowType}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowType}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col19-txt1" value="#{r.compoundWorkflowType}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowType}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col19_1" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col19_1-header" label="#{msg.task_search_result_compound_workflow_title}" value="compoundWorkflowTitle" styleClass="header" />
         </f:facet>
          <a:actionLink id="col19_1-txt" value="#{r.compoundWorkflowTitle}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowTitle}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col19_1-txt1" value="#{r.compoundWorkflowTitle}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowTitle}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col20" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col20-header" label="#{msg.task_search_result_compound_workflow_owner_name}" value="compoundWorkflowOwnerName" styleClass="header" />
         </f:facet>
          <a:actionLink id="col20-txt" value="#{r.compoundWorkflowOwnerName}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowOwnerName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col20-txt1" value="#{r.compoundWorkflowOwnerName}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowOwnerName}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col21" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col21-header" label="#{msg.task_search_result_compound_workflow_owner_organization_path}" value="compoundWorkflowOwnerOrganizationPath" styleClass="header" />
         </f:facet>
          <a:actionLink id="col21-txt" value="#{r.compoundWorkflowOwnerOrganizationPath}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowOwnerOrganizationPath}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col21-txt1" value="#{r.compoundWorkflowOwnerOrganizationPath}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowOwnerOrganizationPath}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column> 
      
      <a:column id="col22" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col22-header" label="#{msg.task_search_result_compound_workflow_owner_job_title}" value="compoundWorkflowOwnerJobTitle" styleClass="header" />
         </f:facet>
          <a:actionLink id="col22-txt" value="#{r.compoundWorkflowOwnerJobTitle}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowOwnerJobTitle}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col22-txt1" value="#{r.compoundWorkflowOwnerJobTitle}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowOwnerJobTitle}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>      
      
      <a:column id="col23" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col23-header" label="#{msg.task_search_result_compound_workflow_created_date_time}" value="compoundWorkflowCreatedDateTime" styleClass="header" />
         </f:facet>
          <a:actionLink id="col23-txt" value="#{r.compoundWorkflowCreatedDateTime}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowCreatedDateTime}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col23-txt1" value="#{r.compoundWorkflowCreatedDateTime}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowCreatedDateTime}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>  
      
      <a:column id="col24" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col24-header" label="#{msg.task_search_result_compound_workflow_started_date_time}" value="compoundWorkflowStartedDateTime" styleClass="header" />
         </f:facet>
          <a:actionLink id="col24-txt" value="#{r.compoundWorkflowStartedDateTime}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowStartedDateTime}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col24-txt1" value="#{r.compoundWorkflowStartedDateTime}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowStartedDateTime}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>  
      
      <a:column id="col25" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col25-header" label="#{msg.task_search_result_compound_workflow_stopped_date_time}" value="compoundWorkflowStoppedDateTime" styleClass="header" />
         </f:facet>
          <a:actionLink id="col25-txt" value="#{r.compoundWorkflowStoppedDateTime}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowStoppedDateTime}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col25-txt1" value="#{r.compoundWorkflowStoppedDateTime}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowStoppedDateTime}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>  
      
      <a:column id="col26" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col26-header" label="#{msg.task_search_result_compound_workflow_finished_date_time}" value="compoundWorkflowFinishedDateTime" styleClass="header" />
         </f:facet>
          <a:actionLink id="col26-txt" value="#{r.compoundWorkflowFinishedDateTime}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowFinishedDateTime}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col26-txt1" value="#{r.compoundWorkflowFinishedDateTime}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowFinishedDateTime}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col27" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col27-header" label="#{msg.task_search_result_compound_workflow_status}" value="compoundWorkflowStatus" styleClass="header" />
         </f:facet>
          <a:actionLink id="col27-txt" value="#{r.compoundWorkflowStatus}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowStatus}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col27-txt1" value="#{r.compoundWorkflowStatus}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowStatus}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col28" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col28-header" label="#{msg.task_search_result_compound_workflow_document_count}" value="compoundWorkflowDocumentCount" styleClass="header" />
         </f:facet>
          <a:actionLink id="col28-txt" value="#{r.compoundWorkflowDocumentCount}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowDocumentCount}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{!r.linkedReviewTask && r.compoundWorkflow.documentWorkflow}" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
          <a:actionLink id="col28-txt1" value="#{r.compoundWorkflowDocumentCount}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflowDocumentCount}" showLink="false" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}" styleClass="no-underline" rendered="#{r.compoundWorkflow.independentWorkflow || r.compoundWorkflow.caseFileWorkflow}" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>         
      </a:column>      
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

   </a:panel>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
