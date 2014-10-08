<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil"%>


<script type="text/javascript">
var message = '<%= MessageUtil.getMessageAndEscapeJS("eventplan_delete_confirm") %>';
function confirmDelete() {
  if (window.confirm(message)) {
     document.forms['dialog']['dialog:act'].value='dialog:not-empty-main-actions-subview:eventPlanDelete';
     setPageScrollY();
     document.forms['dialog'].submit();
  }
  return false;
}
</script>

<a:panel id="eventplan-panel" label="#{msg.eventplan_data_title}" styleClass="panel-100 edit-mode">
   <r:propertySheetGrid id="eventplan-props" value="#{EventPlanDialog.predefinedPlan.node}" columns="1" validationEnabled="true"
      mode="#{EventPlanDialog.inEditMode ? 'edit' : 'view'}" externalConfig="true" labelStyleClass="propertiesLabel no-wrap" />
</a:panel>

<a:panel id="eventplan-series" label="#{msg.eventplan_series_title}" styleClass="panel-100" rendered="#{!EventPlanDialog.new}" progressive="true" expanded="false">
   <a:richList id="planSeriesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{EventPlanDialog.series}" var="r" refreshOnBind="true">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.eventplan_mark}" value="identifier" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.identifier}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.eventplan_title}" value="title" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.title}" />
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.eventplan_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.status}" />
      </a:column>

      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.eventplan_function}" value="functionMarkAndTitle" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-text" value="#{r.functionMarkAndTitle}" action="dialog:seriesListDialog" tooltip="#{msg.series_list_info}"
            showLink="false" actionListener="#{SeriesListDialog.showAll}" styleClass="no-underline" >
            <f:param name="functionNodeRef" value="#{r.functionRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.eventplan_location}" value="location" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.location}" />
      </a:column>

      <%-- show details --%>
      <a:column id="col6" actions="true" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col6-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="dialog:seriesDetailsDialog" showLink="false"
            actionListener="#{SeriesDetailsDialog.showDetails}" tooltip="#{msg.series_details_info}">
            <f:param name="seriesNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>
</a:panel>

<a:panel id="eventplan-volumes" label="#{msg.eventplan_volumes_title}" styleClass="panel-100" rendered="#{!EventPlanDialog.new}" progressive="true" expanded="false">
<h:panelGrid width="100%" id="eventplan-volumes-panelGrid" >
<a:panel id="eventplan-volumes-filter-and-buttons">
   <r:propertySheetGrid id="eventplan-volumes-search-filter" value="#{EventPlanDialog.volumesFilter}" columns="1" mode="edit" externalConfig="true"
      labelStyleClass="propertiesLabel" config="#{EventPlanDialog.volumesFilterPropertySheetConfigElement}" var="searchNode"/>
   <h:commandButton id="eventplan-volumes-button-search" value="#{msg.search}" type="submit" actionListener="#{EventPlanDialog.volumesSearch}" action="#eventplan-volumes" />
   <h:commandButton id="eventplan-volumes-button-showall" value="#{msg.show_all}" type="submit" actionListener="#{EventPlanDialog.volumesShowAll}" action="#eventplan-volumes" style="margin-left: 5px;" />
</a:panel>
<f:verbatim><br/></f:verbatim>
<a:panel id="eventplan-volumes-results" styleClass="panel-100 with-pager" label="#{msg.search}" rendered="#{!empty EventPlanDialog.volumes}">
   <a:richList id="planVolumesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{EventPlanDialog.volumes}" var="r" initialSortColumn="series" refreshOnBind="true">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.eventplan_mark}" value="volumeMark" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.volumeMark}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.eventplan_title}" value="title" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.title}" />
      </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.eventplan_validFrom}" value="validFrom" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.validFrom}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.eventplan_validTo}" value="validTo" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-text" value="#{r.validTo}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.eventplan_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.status}" />
      </a:column>

      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.eventplan_volumeType}" value="volumeType" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.volumeType}">
            <a:convertEnum enumClass="ee.webmedia.alfresco.classificator.enums.VolumeType" />
         </h:outputText>
      </a:column>

      <a:column id="col7">
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.eventplan_owner}" value="ownerName" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.ownerName}" />
      </a:column>

      <a:column id="col8">
         <f:facet name="header">
            <a:sortLink id="col8-sort" label="#{msg.eventplan_location}" value="location" styleClass="header" />
         </f:facet>
         <h:outputText id="col8-text" value="#{r.location}" />
      </a:column>

      <a:column id="col9">
         <f:facet name="header">
            <a:sortLink id="col9-sort" label="#{msg.eventplan_series}" value="series" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text" value="#{r.series}" action="dialog:volumeListDialog" tooltip="#{msg.volume_list_info}"
            showLink="false" actionListener="#{VolumeListDialog.showAll}" >
            <f:param name="seriesNodeRef" value="#{r.seriesRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="col10">
         <f:facet name="header">
            <a:sortLink id="col10-sort" label="#{msg.eventplan_function}" value="function" styleClass="header" />
         </f:facet>
         <a:actionLink id="col10-text" value="#{r.function}" action="dialog:seriesListDialog" tooltip="#{msg.series_list_info}"
            showLink="false" actionListener="#{SeriesListDialog.showAll}" styleClass="no-underline" >
            <f:param name="functionNodeRef" value="#{r.functionRef}" />
         </a:actionLink>
      </a:column>

      <%-- show details --%>
      <a:column id="col11" actions="true" styleClass="actions-column" rendered="#{UserService.documentManager}" >
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col11-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="dialog:volumeDetailsDialog" showLink="false"
            actionListener="#{VolumeDetailsDialog.showDetails}" tooltip="#{msg.volume_details_info}" rendered="#{!r.dynamic}">
            <f:param name="volumeNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col11-act2" value="#{r.title}" image="/images/icons/edit_properties.gif" showLink="false"
            actionListener="#{CaseFileDialog.openFromEventPlanVolumeList}" tooltip="#{msg.volume_details_info}" rendered="#{r.dynamic}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col11-act3" value="#{r.title}" image="/images/icons/ico_cal.gif" action="dialog:volumeEventPlanDialog" showLink="false"
            actionListener="#{VolumeEventPlanDialog.view}" tooltip="#{msg.volume_eventplan}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager2" styleClass="pager" />

   </a:richList>
</a:panel>
</h:panelGrid>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/log/web/document-log-block.jsp" />
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil"%>


<script type="text/javascript">
var message = '<%= MessageUtil.getMessageAndEscapeJS("eventplan_delete_confirm") %>';
function confirmDelete() {
  if (window.confirm(message)) {
     document.forms['dialog']['dialog:act'].value='dialog:not-empty-main-actions-subview:eventPlanDelete';
     setPageScrollY();
     document.forms['dialog'].submit();
  }
  return false;
}
</script>

<a:panel id="eventplan-panel" label="#{msg.eventplan_data_title}" styleClass="panel-100 edit-mode">
   <r:propertySheetGrid id="eventplan-props" value="#{EventPlanDialog.predefinedPlan.node}" columns="1" validationEnabled="true"
      mode="#{EventPlanDialog.inEditMode ? 'edit' : 'view'}" externalConfig="true" labelStyleClass="propertiesLabel no-wrap" />
</a:panel>

<a:panel id="eventplan-series" label="#{msg.eventplan_series_title}" styleClass="panel-100" rendered="#{!EventPlanDialog.new}" progressive="true" expanded="false">
   <a:richList id="planSeriesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{EventPlanDialog.series}" var="r" refreshOnBind="true">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.eventplan_mark}" value="identifier" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.identifier}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.eventplan_title}" value="title" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.title}" />
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.eventplan_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.status}" />
      </a:column>

      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.eventplan_function}" value="functionMarkAndTitle" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-text" value="#{r.functionMarkAndTitle}" action="dialog:seriesListDialog" tooltip="#{msg.series_list_info}"
            showLink="false" actionListener="#{SeriesListDialog.showAll}" styleClass="no-underline" >
            <f:param name="functionNodeRef" value="#{r.functionRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.eventplan_location}" value="location" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.location}" />
      </a:column>

      <%-- show details --%>
      <a:column id="col6" actions="true" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col6-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="dialog:seriesDetailsDialog" showLink="false"
            actionListener="#{SeriesDetailsDialog.showDetails}" tooltip="#{msg.series_details_info}">
            <f:param name="seriesNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>
</a:panel>

<a:panel id="eventplan-volumes" label="#{msg.eventplan_volumes_title}" styleClass="panel-100" rendered="#{!EventPlanDialog.new}" progressive="true" expanded="false">
<h:panelGrid width="100%" id="eventplan-volumes-panelGrid" >
<a:panel id="eventplan-volumes-filter-and-buttons">
   <r:propertySheetGrid id="eventplan-volumes-search-filter" value="#{EventPlanDialog.volumesFilter}" columns="1" mode="edit" externalConfig="true"
      labelStyleClass="propertiesLabel" config="#{EventPlanDialog.volumesFilterPropertySheetConfigElement}" var="searchNode"/>
   <h:commandButton id="eventplan-volumes-button-search" value="#{msg.search}" type="submit" actionListener="#{EventPlanDialog.volumesSearch}" action="#eventplan-volumes" />
   <h:commandButton id="eventplan-volumes-button-showall" value="#{msg.show_all}" type="submit" actionListener="#{EventPlanDialog.volumesShowAll}" action="#eventplan-volumes" style="margin-left: 5px;" />
</a:panel>
<f:verbatim><br/></f:verbatim>
<a:panel id="eventplan-volumes-results" styleClass="panel-100 with-pager" label="#{msg.search}" rendered="#{!empty EventPlanDialog.volumes}">
   <a:richList id="planVolumesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{EventPlanDialog.volumes}" var="r" initialSortColumn="series" refreshOnBind="true">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.eventplan_mark}" value="volumeMark" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.volumeMark}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.eventplan_title}" value="title" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.title}" />
      </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.eventplan_validFrom}" value="validFrom" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.validFrom}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.eventplan_validTo}" value="validTo" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-text" value="#{r.validTo}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.eventplan_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.status}" />
      </a:column>

      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.eventplan_volumeType}" value="volumeType" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.volumeType}">
            <a:convertEnum enumClass="ee.webmedia.alfresco.classificator.enums.VolumeType" />
         </h:outputText>
      </a:column>

      <a:column id="col7">
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.eventplan_owner}" value="ownerName" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.ownerName}" />
      </a:column>

      <a:column id="col8">
         <f:facet name="header">
            <a:sortLink id="col8-sort" label="#{msg.eventplan_location}" value="location" styleClass="header" />
         </f:facet>
         <h:outputText id="col8-text" value="#{r.location}" />
      </a:column>

      <a:column id="col9">
         <f:facet name="header">
            <a:sortLink id="col9-sort" label="#{msg.eventplan_series}" value="series" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text" value="#{r.series}" action="dialog:volumeListDialog" tooltip="#{msg.volume_list_info}"
            showLink="false" actionListener="#{VolumeListDialog.showAll}" >
            <f:param name="seriesNodeRef" value="#{r.seriesRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="col10">
         <f:facet name="header">
            <a:sortLink id="col10-sort" label="#{msg.eventplan_function}" value="function" styleClass="header" />
         </f:facet>
         <a:actionLink id="col10-text" value="#{r.function}" action="dialog:seriesListDialog" tooltip="#{msg.series_list_info}"
            showLink="false" actionListener="#{SeriesListDialog.showAll}" styleClass="no-underline" >
            <f:param name="functionNodeRef" value="#{r.functionRef}" />
         </a:actionLink>
      </a:column>

      <%-- show details --%>
      <a:column id="col11" actions="true" styleClass="actions-column" rendered="#{UserService.documentManager}" >
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col11-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="dialog:volumeDetailsDialog" showLink="false"
            actionListener="#{VolumeDetailsDialog.showDetails}" tooltip="#{msg.volume_details_info}" rendered="#{!r.dynamic}">
            <f:param name="volumeNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col11-act2" value="#{r.title}" image="/images/icons/edit_properties.gif" showLink="false"
            actionListener="#{CaseFileDialog.openFromEventPlanVolumeList}" tooltip="#{msg.volume_details_info}" rendered="#{r.dynamic}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col11-act3" value="#{r.title}" image="/images/icons/ico_cal.gif" action="dialog:volumeEventPlanDialog" showLink="false"
            actionListener="#{VolumeEventPlanDialog.view}" tooltip="#{msg.volume_eventplan}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager2" styleClass="pager" />

   </a:richList>
</a:panel>
</h:panelGrid>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/log/web/document-log-block.jsp" />
>>>>>>> develop-5.1
