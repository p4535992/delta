<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="volumeArchiveFilter-panel" styleClass="panel-100" label="#{msg.volume_archive_filter}">

   <r:propertySheetGrid id="volume-archive-search-filter" value="#{DialogManager.bean.filter}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel"
      binding="#{DialogManager.bean.propertySheet}" />

   <h:commandButton id="volumeArchiveFilterSearch" action="search" value="#{msg.volume_archive_search}" actionListener="#{DialogManager.bean.searchVolumes}" styleClass="volumeArchiveFilterPanelSearch" />
   <f:verbatim>&nbsp;</f:verbatim>
   <h:commandButton id="volumeArchiveFilterSearchAll" action="search" value="#{msg.volume_archive_search_all}" actionListener="#{DialogManager.bean.searchAllVolumes}" />
   

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/archivals/web/list-filter-disable-script.jsp" />

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/archivals/web/volume-archive-list-dialog-confirmations.jsp" />

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/select-all-header-function.jsp" />

<a:panel id="volume-search-results-panel" facetsId="dialog:dialog-body:result-list-panel-facets" styleClass="panel-100 with-pager" label="#{DialogManager.bean.volumeListTitle}" progressive="true">

   <a:richList id="applogList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      value="#{DialogManager.bean.volumes}" var="r" refreshOnBind="true">

      <a:column id="col-checkbox" >
         <f:facet name="header">
            <h:selectBooleanCheckbox id="col0-header" value="false" styleClass="selectAllHeader"/>
         </f:facet>
         <h:selectBooleanCheckbox id="col0-checkbox" styleClass="headerSelectable" value="#{DialogManager.bean.listCheckboxes[r.nodeRef]}"/>
      </a:column>

      <a:column id="col1" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.volume_report_volumeMark}" value="volumeMark" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.volumeMark}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.volume_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-link2cases" value="#{r.title} (#{r.containingDocsCount})" action="dialog:caseDocListDialog" tooltip="#{msg.case_list_info}"
            showLink="false" actionListener="#{CaseDocumentListDialog.showAll}" rendered="#{!r.dynamic}">
            <f:param name="volumeNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col2-caseFile" value="#{r.title} (#{r.containingDocsCount})" tooltip="#{msg.case_list_info}"
            showLink="false" actionListener="#{CaseFileDialog.openFromDocumentList}" rendered="#{r.dynamic}">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col3" rendered="#{DialogManager.bean.showNextEventDateColumn}">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{DialogManager.bean.nextEventLabel}" value="nextEventDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.nextEventDate}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col4" rendered="#{DialogManager.bean.showExportedForUamDateTimeColumn}">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.volume_search_exported_for_uam_date_time}" value="exportedForUamDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.exportedForUamDateTime}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col5" rendered="#{DialogManager.bean.showDestructionColumns}">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.volume_search_marked_for_destruction}" value="markedForDestruction" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.markedForDestruction}" >
         	<a:convertBoolean />
         </h:outputText>
      </a:column>
      
      <a:column id="col6" rendered="#{DialogManager.bean.showDestructionColumns}">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.volume_search_disposal_act_created}" value="disposalActCreated" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-text" value="#{r.disposalActCreated}" >
         	<a:convertBoolean />
         </h:outputText>
      </a:column>          
      
      <a:column id="col7" rendered="#{DialogManager.bean.showRetaintionColumns}">
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.volume_search_retain_until_date}" value="retainUntilDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.retainUntilDate}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col8" rendered="#{DialogManager.bean.showRetaintionColumns}">
         <f:facet name="header">
            <a:sortLink id="col8-sort" label="#{msg.volume_search_retaintion_description}" value="retaintionDescription" styleClass="header" />
         </f:facet>
         <h:outputText id="col8-text" value="#{r.retaintionDescription}" />
      </a:column>      
      
      
      <a:column id="col9" rendered="#{DialogManager.bean.showMarkedForTransferColumn}">
         <f:facet name="header">
            <a:sortLink id="col9-sort" label="#{msg.volume_search_marked_for_transfer}" value="markedForTransfer" styleClass="header" />
         </f:facet>
         <h:outputText id="col9-text" value="#{r.markedForTransfer}" >
         	<a:convertBoolean />
         </h:outputText>
      </a:column>
      
      <a:column id="col10" rendered="#{DialogManager.bean.showExportedForUamColumn}">
         <f:facet name="header">
            <a:sortLink id="col10-sort" label="#{msg.volume_search_exported_for_uam}" value="exportedForUam" styleClass="header" />
         </f:facet>
         <h:outputText id="col10-text" value="#{r.exportedForUam}" >
         	<a:convertBoolean />
         </h:outputText>
      </a:column>
      
      <a:column id="col11" rendered="#{DialogManager.bean.showDestructionColumns}">
         <f:facet name="header">
            <a:sortLink id="col11-sort" label="#{msg.volume_search_is_appraised}" value="appraised" styleClass="header" />
         </f:facet>
         <h:outputText id="col11-text" value="#{r.appraised}" >
         	<a:convertBoolean />
         </h:outputText>
      </a:column>
      
      <a:column id="col12">
         <f:facet name="header">
            <a:sortLink id="col12-sort" label="#{msg.volume_report_validFrom}" value="validFrom" styleClass="header" />
         </f:facet>
         <h:outputText id="col12-text" value="#{r.validFrom}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col13">
         <f:facet name="header">
            <a:sortLink id="col13-sort" label="#{msg.volume_report_validTo}" value="validTo" styleClass="header" />
         </f:facet>
         <h:outputText id="col13-text" value="#{r.validTo}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <a:column id="col14" rendered="#{DialogManager.bean.showStatusColumn}">
         <f:facet name="header">
            <a:sortLink id="col14-sort" label="#{msg.volume_report_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col14-text" value="#{r.status}" />
      </a:column>
      
      <a:column id="col15_0">
         <f:facet name="header">
            <a:sortLink id="col15_0-sort" label="#{msg.volume_report_volumeType}" value="volumeTypeName" styleClass="header" />
         </f:facet>
         <h:outputText id="col15_0-text" value="#{r.volumeTypeName}" />
      </a:column>      
      
      <%-- owner --%>
      <a:column id="col16">
         <f:facet name="header">
            <a:sortLink id="col16-sort" label="#{msg.caseFile_owner}" value="ownerName" styleClass="header" />
         </f:facet>
         <h:outputText id="col16-text" value="#{r.ownerName}" rendered="#{r.dynamic}" />
      </a:column>      
      
      <a:column id="col17">
         <f:facet name="header">
            <a:sortLink id="col17-sort" label="#{msg.volume_report_series}" value="seriesLabel" styleClass="header" />
         </f:facet>
         <h:outputText id="col17-text" value="#{r.seriesLabel}" />
      </a:column> 
            
      <a:column id="col18">
         <f:facet name="header">
            <a:sortLink id="col18-sort" label="#{msg.volume_report_function}" value="functionLabel" styleClass="header" />
         </f:facet>
         <h:outputText id="col18-text" value="#{r.functionLabel}" />
      </a:column>  

      <a:column id="col18_1" rendered="#{DialogManager.bean.showTransferringDeletingNextEventColumn}">
         <f:facet name="header">
            <a:sortLink id="col18_1-sort" label="#{DialogManager.bean.nextEventLabel}" value="nextEvent" styleClass="header" />
         </f:facet>
         <h:outputText id="col18_1-text" value="#{r.nextEvent}" />
      </a:column>
      
      <a:column id="col18_2" rendered="#{DialogManager.bean.showTransferringNextEventDateColumn}">
         <f:facet name="header">
            <a:sortLink id="col18_2-sort" label="#{msg.archivals_volume_next_event_date}" value="nextEventDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col18_2-text" value="#{r.nextEventDate}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col19" actions="true" styleClass="actions-column" >
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col19-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="dialog:volumeDetailsDialog" showLink="false"
            actionListener="#{VolumeDetailsDialog.showDetails}" tooltip="#{msg.volume_details_info}" rendered="#{!r.dynamic}">
            <f:param name="volumeNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col19-caseFile" image="/images/icons/edit_properties.gif" value="#{r.title}" tooltip="#{msg.case_list_info}"
            showLink="false" actionListener="#{CaseFileDialog.openFromDocumentList}" rendered="#{r.dynamic}">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>         
         <a:actionLink id="col19-act2" value="#{r.title}" image="/images/icons/ico_cal.gif" action="dialog:volumeEventPlanDialog" showLink="false"
            actionListener="#{VolumeEventPlanDialog.view}" tooltip="#{msg.volume_eventplan}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
</a:panel>