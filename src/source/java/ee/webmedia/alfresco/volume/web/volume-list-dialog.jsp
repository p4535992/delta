<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="volume-panel" styleClass="panel-100 with-pager" label="#{VolumeListDialog.parent.seriesIdentifier} #{VolumeListDialog.parent.title}" progressive="true">

   <%-- Main List --%>
   <a:richList id="volumesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{VolumeListDialog.entries}" var="r">

      <%-- volumeMark --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.volume_volumeMark}" value="volumeMark" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.volumeMark}" />
      </a:column>

      <%-- Title --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.volume_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-link2documents" value="#{r.title}" action="dialog:documentListDialog" tooltip="#{msg.document_list_info}"
            showLink="false" actionListener="#{DocumentListDialog.setup}" rendered="#{!r.containsCases}" >
            <f:param name="volumeNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col2-link2cases" value="#{r.title}" action="dialog:caseListDialog" tooltip="#{msg.case_list_info}"
            showLink="false" actionListener="#{CaseListDialog.showAll}" rendered="#{r.containsCases}" >
            <f:param name="volumeNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- validFrom --%>
      <a:column id="col3" primary="true">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.volume_validFrom}" value="validFrom" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.validFrom}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- validTo --%>
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.volume_validTo}" value="validTo" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.validTo}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- status --%>
      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.volume_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.status}" />
      </a:column>

      <%-- dispositionDate --%>
      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.volume_dispositionDate}" value="dispositionDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-text" value="#{r.dispositionDate}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- show details --%>
      <a:column id="col7" actions="true" styleClass="actions-column" rendered="#{UserService.documentManager}" >
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col7-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="dialog:volumeDetailsDialog" showLink="false"
            actionListener="#{VolumeDetailsDialog.showDetails}" tooltip="#{msg.volume_details_info}">
            <f:param name="volumeNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
