<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/limited-message-panel.jsp" />

<a:panel id="task-panel" styleClass="panel-100 with-pager" label="#{msg.task_search_results}" progressive="true">
   <a:panel id="task-panel-search-results" styleClass="overflow-wrapper">   

   <a:richList id="taskList" styleClass="duplicate-header" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      value="#{TaskSearchResultsDialog.tasks}" var="r" binding="#{TaskSearchResultsDialog.richList}" >

      <a:column id="col1" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.task_search_result_regNum}" value="regNum" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-txt" value="#{r.regNum}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col2" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.task_search_result_regDate}" value="regDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-txt" value="#{r.regDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col3" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.task_search_result_docType}" value="docType" styleClass="header" />
         </f:facet>
          <a:actionLink id="col3-txt" value="#{r.docType}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col4" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-header" label="#{msg.task_search_result_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-txt" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="tooltip condence20- no-underline">
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col5" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col5-header" label="#{msg.task_search_result_creatorName}" value="creatorName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-txt" value="#{r.creatorName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col6" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-header" label="#{msg.task_search_result_startedDate}" value="startedDate" styleClass="header" />
         </f:facet>
          <a:actionLink id="col6-txt" value="#{r.startedDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col7" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col7-header" label="#{msg.task_search_result_ownerName}" value="ownerName" styleClass="header-wrap" />
         </f:facet>
         <a:actionLink id="col7-txt" value="#{r.ownerName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col8" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col8-header" label="#{msg.task_search_result_ownerOrganizationName}" value="ownerOrganizationName" styleClass="header" />
         </f:facet>
          <a:actionLink id="col8-txt" value="#{r.ownerOrganizationName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col9" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col9-header" label="#{msg.task_search_result_ownerJobTitle}" value="ownerJobTitle" styleClass="header" />
         </f:facet>
           <a:actionLink id="col9-txt" value="#{r.ownerJobTitle}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col10" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col10-header" label="#{msg.task_search_result_taskType}" value="taskTypeText" styleClass="header" />
         </f:facet>
         <a:actionLink id="col10-txt" value="#{r.taskTypeText}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col11" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col11-header" label="#{msg.task_search_result_dueDate}" value="dueDate" styleClass="header" />
         </f:facet>
          <a:actionLink id="col11-txt" value="#{r.dueDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col12" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col12-header" label="#{msg.task_search_result_completedDate}" value="completedDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col12-txt" value="#{r.completedDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col13" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col13-header" label="#{msg.task_search_result_comment}" value="comment" styleClass="header" />
         </f:facet>
         <a:actionLink id="col13-txt" value="#{r.comment}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col14" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col14-header" label="#{msg.task_search_result_responsible}" value="responsible" styleClass="header" />
         </f:facet>
         <a:actionLink id="col14-txt" value="#{r.responsible}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col15" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col15-header" label="#{msg.task_search_result_stoppedDate}" value="stoppedDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col15-txt" value="#{r.stoppedDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col16" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col16-header" label="#{msg.task_search_result_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <a:actionLink id="col16-txt" value="#{r.resolution}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col17" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col17-header" label="#{msg.task_search_result_overdue}" value="overdue" styleClass="header-wrap" />
         </f:facet>
          <a:actionLink id="col17-txt" value="#{r.overdue}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col18" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col18-header" label="#{msg.task_search_result_status}" value="status" styleClass="header" />
         </f:facet>
          <a:actionLink id="col18-txt" value="#{r.status}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

   </a:panel>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
