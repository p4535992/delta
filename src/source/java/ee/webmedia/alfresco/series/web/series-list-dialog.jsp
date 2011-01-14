<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="series-panel" styleClass="panel-100 with-pager" label="#{SeriesListDialog.listTitle}" progressive="true">

   <%-- Main List --%>
   <a:richList id="seriesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{SeriesListDialog.series}" var="r">
      
      <a:column id="functionColumn" rendered="#{SeriesListDialog.disableActions}">
         <f:facet name="header">
            <a:sortLink id="functionColumn-sort" label="#{msg.series_function}" value="order" styleClass="header" />
         </f:facet>
         <h:outputText id="functionColumn-text" value="#{SeriesListDialog.function.mark} #{SeriesListDialog.function.title}" />
      </a:column>
      
      <%-- seriesIdentifier --%>
      <a:column id="col0">
         <f:facet name="header">
            <a:sortLink id="col0-sort" label="#{msg.series_order_heading}" value="order" styleClass="header" />
         </f:facet>
         <h:outputText id="col0-text" value="#{r.order}" />
      </a:column>

      <%-- seriesIdentifier --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.series_seriesIdentifier}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.seriesIdentifier}" />
      </a:column>

      <%-- Title --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.series_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text" value="#{r.title} (#{r.containingDocsCount})" action="dialog:volumeListDialog" tooltip="#{msg.volume_list_info}"
            showLink="false" actionListener="#{VolumeListDialog.showAll}" >
            <f:param name="seriesNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- type --%>
      <a:column id="col3" primary="true">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.series_type}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.type}" />
      </a:column>

      <%-- status --%>
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.series_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.status}" />
      </a:column>

      <%-- show details --%>
      <a:column id="col5" actions="true" styleClass="actions-column" rendered="#{UserService.documentManager and not SeriesListDialog.disableActions}" >
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col5-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="dialog:seriesDetailsDialog" showLink="false"
            actionListener="#{SeriesDetailsDialog.showDetails}" tooltip="#{msg.series_details_info}">
            <f:param name="seriesNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
