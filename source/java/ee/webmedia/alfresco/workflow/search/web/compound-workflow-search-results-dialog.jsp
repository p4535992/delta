<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/limited-message-panel.jsp" />

<a:panel id="cw-panel" styleClass="panel-100 with-pager" label="#{msg.cw_search_results}" progressive="true">
   <a:panel id="cw-panel-search-results" styleClass="overflow-wrapper">   

   <a:richList id="workflowList" styleClass="duplicate-header" width="100%" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      value="#{CompoundWorkflowSearchResultsDialog.workflows}" binding="#{CompoundWorkflowSearchResultsDialog.richList}" var="r" refreshOnBind="true">

      <a:column id="col1" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.cw_search_type}" value="workflowTypeString" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-txt1" value="#{r.workflowTypeString}" action="#{r.action}" tooltip="#{r.workflowTypeString}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col2" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.cw_search_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-txt1" value="#{r.title}" action="#{r.action}" tooltip="#{r.title}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col3" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.cw_search_owner}" value="ownerName" styleClass="header" />
         </f:facet>
          <a:actionLink id="col3-txt1" value="#{r.ownerName}" action="#{r.action}" tooltip="#{r.ownerName}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col4" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col4-header" label="#{msg.cw_search_struct_unit}" value="ownerStructUnit" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-txt1" value="#{r.ownerStructUnit}" action="#{r.action}" tooltip="#{r.ownerStructUnit}" showLink="false" actionListener="#{r.actionListener}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col5" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col5-header" label="#{msg.cw_search_job_title}" value="ownerJobTitle" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-txt1" value="#{r.ownerJobTitle}" action="#{r.action}" tooltip="#{r.ownerJobTitle}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col6" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col6-header" label="#{msg.cw_search_create_date}" value="createdDateTime" styleClass="header" />
         </f:facet>
          <a:actionLink id="col6-txt1" value="#{r.createdDateStr}" action="#{r.action}" tooltip="#{r.createdDateStr}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col7" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col7-header" label="#{msg.cw_search_ignition_date}" value="startedDateTime" styleClass="header-wrap" />
         </f:facet>
         <a:actionLink id="col7-txt1" value="#{r.startedDateStr}" action="#{r.action}" tooltip="#{r.startedDateStr}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col8" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col8-header" label="#{msg.cw_search_stopped_date}" value="stoppedDateTime" styleClass="header" />
         </f:facet>
          <a:actionLink id="col8-txt1" value="#{r.stoppedDateStr}" action="#{r.action}" tooltip="#{r.stoppedDateStr}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col9" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col9-header" label="#{msg.cw_search_ending_date}" value="endedDateTime" styleClass="header" />
         </f:facet>
           <a:actionLink id="col9-txt1" value="#{r.endedDateStr}" action="#{r.action}" tooltip="#{r.endedDateStr}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col11" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col11-header" label="#{msg.cw_search_status}" value="status" styleClass="header" />
         </f:facet>
          <a:actionLink id="col11-txt1" value="#{r.status}" action="#{r.action}" tooltip="#{r.status}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <a:column id="col12" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col12-header" label="#{msg.cw_search_doc_count}" value="numberOfDocuments" styleClass="header" />
         </f:facet>
         <a:actionLink id="col12-txt1" value="#{r.numberOfDocumentsStr}" action="#{r.action}" tooltip="#{r.numberOfDocumentsStr}" showLink="false" actionListener="#{r.actionListener}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>         
      </a:column>
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

   </a:panel>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
