<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="log-panel-facets" styleClass="nonfloating-element" >
   <f:facet name="title">
      <a:actionLink image="/images/icons/version_history.gif" id="log-details-link" showLink="false" tooltip="#{msg.document_log_view_details}" value="" 
         actionListener="#{ApplicationLogDialog.searchNodeRefEntries}" action="dialog:applicationLogListDialog" rendered="#{LogBlockBean.showLogDetailsLink}" >
         <f:param name="nodeRef" value="#{LogBlockBean.parentRef}" />
      </a:actionLink>
   </f:facet>
</h:panelGroup>

<a:panel id="document-block-block-panel" label="#{msg.document_log_title}" styleClass="panel-100 with-pager" progressive="true" rendered="#{LogBlockBean.rendered}"
   expanded="false" facetsId="dialog:dialog-body:log-panel-facets">

   <a:richList id="logList" viewMode="details" value="#{LogBlockBean.logs}" var="r" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" refreshOnBind="true" pageSize="#{BrowseBean.pageSizeContent}" initialSortColumn="createdDateTime">

      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.document_log_date}" value="createdDateTime" styleClass="header" />
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
            <a:sortLink id="col3-header" label="#{msg.document_log_event}" value="eventDescription" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.eventDescription}" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="seriesLogPager" styleClass="pager" />
   </a:richList>

</a:panel>
