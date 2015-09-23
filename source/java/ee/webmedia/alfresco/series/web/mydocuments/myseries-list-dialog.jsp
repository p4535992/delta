<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="series-panel" styleClass="panel-100 with-pager" label="" progressive="true">

   <%-- Main List --%>
   <a:richList id="seriesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{MySeriesListDialog.seriesFunction}" var="r">

      <%-- seriesIdentifier --%>
      <a:column id="col0" primary="true">
         <f:facet name="header">
            <a:sortLink id="col0-sort" label="#{msg.function}" value="functionTitle" styleClass="header" />
         </f:facet>
         <h:outputText id="col0-text" value="#{r.functionTitle}" />
      </a:column>

      <%-- seriesIdentifier --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.series_seriesIdentifier}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.series.seriesIdentifier}" />
      </a:column>

      <%-- Title --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.series_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text" value="#{r.series.title}" action="dialog:volumeListDialog" tooltip="#{msg.volume_list_info}"
            showLink="false" actionListener="#{VolumeListDialog.showAll}" >
            <f:param name="seriesNodeRef" value="#{r.series.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- type --%>
      <a:column id="col3" primary="true">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.series_type}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.series.type}" />
      </a:column>

      <%-- status --%>
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.series_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.series.status}" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
