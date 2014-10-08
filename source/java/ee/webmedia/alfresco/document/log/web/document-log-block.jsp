<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="log-panel-facets" styleClass="nonfloating-element" >
   <f:facet name="title">
      <a:actionLink image="/images/icons/version_history.gif" id="log-details-link" showLink="false" tooltip="#{msg.compoundWorkflow_view_log_details}" value="" 
         actionListener="#{ApplicationLogDialog.searchWorkflowEntries}" action="dialog:applicationLogListDialog" rendered="#{DialogManager.bean.log.showLogDetailsLink}" >
         <f:param id="log-details-link-param" name="compoundWorkflowRef" value="#{DialogManager.bean.log.parentRef}" />
      </a:actionLink>
   </f:facet>
</h:panelGroup>

<a:panel id="document-block-block-panel" label="#{DialogManager.bean.log.listTitle}" styleClass="panel-100 with-pager" progressive="true" rendered="#{DialogManager.bean.log.rendered}"
   expanded="false" facetsId="dialog:dialog-body:log-panel-facets">

   <a:richList id="logList" viewMode="details" value="#{DialogManager.bean.log.logs}" var="r" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" refreshOnBind="true" pageSize="#{BrowseBean.pageSizeContent}" initialSortColumn="createdDateTime">

      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{DialogManager.bean.log.createdDateColumnTitle}" value="createdDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-txt" value="#{r.createdDateTime}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col2" primary="true">
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.document_log_creator}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.creatorName}" />
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{DialogManager.bean.log.eventColumnTitle}" value="eventDescription" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
         <h:outputText id="col3-txt" value="#{r.eventDescription}" />
=======
         <h:outputText id="col3-txt" styleClass="condence150" value="#{r.eventDescriptionAndLinks}" escape="false"/>
>>>>>>> develop-5.1
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="seriesLogPager" styleClass="pager" />
   </a:richList>

</a:panel>
