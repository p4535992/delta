<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="task-panel" styleClass="panel-100" label="#{msg.task_search_results}" progressive="true" styleClass="with-pager">
   <a:panel id="task-panel-search-results" styleClass="overflow-wrapper">   

   <a:richList id="taskList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      value="#{TaskSearchResultsDialog.tasks}" var="r" >

      <a:column id="col1" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.task_search_result_regNum}" value="regNum" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-txt" value="#{r.regNum}" />
      </a:column>
      
      <a:column id="col2" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.task_search_result_regDate}" value="regDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.regDate}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col3" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.task_search_result_docType}" value="docType" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.docType}" />
      </a:column>
      
      <a:column id="col4" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-header" label="#{msg.task_search_result_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-txt" value="#{r.docName}" action="dialog:document" tooltip="#{r.docName}" showLink="false" actionListener="#{DocumentDialog.open}" styleClass="tooltip condence20-">
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col5" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col5-header" label="#{msg.task_search_result_creatorName}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-txt" value="#{r.creatorName}" />
      </a:column>
      
      <a:column id="col6" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-header" label="#{msg.task_search_result_startedDate}" value="startedDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-txt" value="#{r.startedDate}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col7" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col7-header" label="#{msg.task_search_result_ownerName}" value="ownerName" styleClass="header-wrap" />
         </f:facet>
         <h:outputText id="col7-txt" value="#{r.ownerName}" />
      </a:column>
      
      <a:column id="col8" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col8-header" label="#{msg.task_search_result_ownerOrganizationName}" value="ownerOrganizationName" styleClass="header" />
         </f:facet>
         <h:outputText id="col8-txt" value="#{r.ownerOrganizationName}" />
      </a:column>
      
      <a:column id="col9" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col9-header" label="#{msg.task_search_result_ownerJobTitle}" value="ownerJobTitle" styleClass="header" />
         </f:facet>
         <h:outputText id="col9-txt" value="#{r.ownerJobTitle}" />
      </a:column>
      
      <a:column id="col10" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col10-header" label="#{msg.task_search_result_taskType}" value="taskType" styleClass="header" />
         </f:facet>
         <h:outputText id="col10-txt" value="#{r.taskTypeText}" />
      </a:column>
      
      <a:column id="col11" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col11-header" label="#{msg.task_search_result_dueDate}" value="dueDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col11-txt" value="#{r.dueDate}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col12" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col12-header" label="#{msg.task_search_result_completedDate}" value="completedDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col12-txt" value="#{r.completedDate}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col13" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col13-header" label="#{msg.task_search_result_comment}" value="comment" styleClass="header" />
         </f:facet>
         <h:outputText id="col13-txt" value="#{r.comment}" />
      </a:column>
      
      <a:column id="col14" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col14-header" label="#{msg.task_search_result_responsible}" value="responsible" styleClass="header" />
         </f:facet>
         <h:outputText id="col14-txt" value="#{r.responsible}" />
      </a:column>
      
      <a:column id="col15" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col15-header" label="#{msg.task_search_result_stoppedDate}" value="stoppedDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col15-txt" value="#{r.stoppedDate}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col16" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col16-header" label="#{msg.task_search_result_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <h:outputText id="col16-txt" value="#{r.resolution}" />
      </a:column>
      
      <a:column id="col17" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col17-header" label="#{msg.task_search_result_overdue}" value="overdue" styleClass="header-wrap" />
         </f:facet>
         <h:outputText id="col17-txt" value="#{r.overdue}" />
      </a:column>
      
      <a:column id="col18" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col18-header" label="#{msg.task_search_result_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col18-txt" value="#{r.status}" />
      </a:column>
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

   </a:panel>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
