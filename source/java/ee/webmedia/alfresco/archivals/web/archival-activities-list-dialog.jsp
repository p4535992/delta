<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="volumeArchiveFilter-panel" styleClass="panel-100" label="#{msg.archival_activities_filter}">

   <r:propertySheetGrid id="archival-activities-search-filter" value="#{ArchivalActivitiesListDialog.filter}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />

   <h:commandButton id="volumeArchiveFilterSearch" action="search" value="#{msg.volume_archive_search}" actionListener="#{ArchivalActivitiesListDialog.searchArchivalActivities}" styleClass="volumeArchiveFilterPanelSearch" />
   <f:verbatim>&nbsp;</f:verbatim>
   <h:commandButton id="volumeArchiveFilterSearchAll" action="search" value="#{msg.volume_archive_search_all}" actionListener="#{ArchivalActivitiesListDialog.searchAllArchivalActivities}" />
   
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/archivals/web/list-filter-disable-script.jsp" />
	
</a:panel>

<a:panel id="volume-archival-activities-result-panel" styleClass="panel-100 with-pager" label="#{msg.archival_activities_list_title}" progressive="true" >

   <a:richList id="archivalActivitiesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      value="#{ArchivalActivitiesListDialog.archivalActivities}" var="r" refreshOnBind="true" binding="#{ArchivalActivitiesListDialog.richList}">

      <a:column id="col1" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.archival_activities_activity_type}" value="activityType" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.activityType}" />
      </a:column>
      
      <a:column id="col2" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.archival_activities_created}" value="created" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.created}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>  
      
      <a:column id="col3" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.archival_activities_creator_name}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.creatorName}" />
      </a:column>
      
      <a:column id="col4" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.archival_activities_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.status}" />
      </a:column>      
      
      <%-- Files --%>
      <a:column id="col5" styleClass="doc-list-actions">
         <f:facet name="header">
            <h:outputText id="col5-header" value="#{msg.archival_activities_result_file}" styleClass="header" />
         </f:facet>
          <wm:customChildrenContainer childGenerator="#{ArchivalActivitiesListDialog.archivalActivityRowFileGenerator}" parameterList="#{r.files}"/>
      </a:column>    

      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.archival_activities_document}" value="documentTitle" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-link1" value="#{msg.archival_activities_document_create_new}" tooltip="#{msg.archival_activities_document_create_new}"
            actionListener="#{DocumentDynamicDialog.createDraftForArchivalActivity}" rendered="#{!r.hasDocument}">
            <f:param name="archivalNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col6-link2" value="#{r.documentTitle}" tooltip="#{r.documentTitle}"
             actionListener="#{DocumentDynamicDialog.openFromDocumentList}" rendered="#{r.hasDocument}">
            <f:param name="nodeRef" value="#{r.documentNodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="volumeArchiveFilter-panel" styleClass="panel-100" label="#{msg.archival_activities_filter}">

   <r:propertySheetGrid id="archival-activities-search-filter" value="#{ArchivalActivitiesListDialog.filter}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />

   <h:commandButton id="volumeArchiveFilterSearch" action="search" value="#{msg.volume_archive_search}" actionListener="#{ArchivalActivitiesListDialog.searchArchivalActivities}" styleClass="volumeArchiveFilterPanelSearch" />
   <f:verbatim>&nbsp;</f:verbatim>
   <h:commandButton id="volumeArchiveFilterSearchAll" action="search" value="#{msg.volume_archive_search_all}" actionListener="#{ArchivalActivitiesListDialog.searchAllArchivalActivities}" />
   
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/archivals/web/list-filter-disable-script.jsp" />
	
</a:panel>

<a:panel id="volume-archival-activities-result-panel" styleClass="panel-100 with-pager" label="#{msg.archival_activities_list_title}" progressive="true" >

   <a:richList id="archivalActivitiesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      value="#{ArchivalActivitiesListDialog.archivalActivities}" var="r" refreshOnBind="true" binding="#{ArchivalActivitiesListDialog.richList}">

      <a:column id="col1" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.archival_activities_activity_type}" value="activityType" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.activityType}" />
      </a:column>
      
      <a:column id="col2" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.archival_activities_created}" value="created" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.created}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>  
      
      <a:column id="col3" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.archival_activities_creator_name}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.creatorName}" />
      </a:column>
      
      <a:column id="col4" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.archival_activities_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.status}" />
      </a:column>      
      
      <%-- Files --%>
      <a:column id="col5" styleClass="doc-list-actions">
         <f:facet name="header">
            <h:outputText id="col5-header" value="#{msg.archival_activities_result_file}" styleClass="header" />
         </f:facet>
          <wm:customChildrenContainer childGenerator="#{ArchivalActivitiesListDialog.archivalActivityRowFileGenerator}" parameterList="#{r.files}"/>
      </a:column>    

      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.archival_activities_document}" value="documentTitle" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-link1" value="#{msg.archival_activities_document_create_new}" tooltip="#{msg.archival_activities_document_create_new}"
            actionListener="#{DocumentDynamicDialog.createDraftForArchivalActivity}" rendered="#{!r.hasDocument}">
            <f:param name="archivalNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col6-link2" value="#{r.documentTitle}" tooltip="#{r.documentTitle}"
             actionListener="#{DocumentDynamicDialog.openFromDocumentList}" rendered="#{r.hasDocument}">
            <f:param name="nodeRef" value="#{r.documentNodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
>>>>>>> develop-5.1
</a:panel>