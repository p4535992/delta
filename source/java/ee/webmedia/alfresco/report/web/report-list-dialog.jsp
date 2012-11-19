<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="report-list-panel" styleClass="panel-100 with-pager" label="#{msg.report_executed_reports}" progressive="true">

   <a:richList id="reportsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{ReportListDialog.reportResults}" refreshOnBind="true" var="r" initialSortColumn="userStartDateTime" initialSortDescending="true" styleClass="with-pager">
      
      <a:column id="col1" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.report_name}" value="reportName" />
         </f:facet>
         <a:actionLink id="col1-act1" value="#{r.reportName}" tooltip="#{msg.report_name}" href="#{r.downloadUrl}" target="_blank" styleClass="inlineAction webdav-readOnly-report" rendered="#{r.showDownloadLink}" />
         <a:actionLink id="col1-act1-submit" value="#{r.reportName}" actionListener="#{ReportListDialog.markReportDownloaded}" styleClass="hidden" rendered="#{r.showDownloadLink}" >
            <f:param name="reportResultNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <h:outputText id="col1-txt1" value="#{r.reportName}" rendered="#{!r.showDownloadLink}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.report_userStartDateTime}" value="userStartDateTime" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.userStartDateTime}" >
            <a:convertXMLDate type="both" pattern="dd.MM.yyyy HH:mm" />
         </h:outputText>
      </a:column>
      
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.report_type}" value="reportTypeText" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.reportTypeText}" />
      </a:column>
      
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.report_output_type}" value="reportOutputTypeText" />
         </f:facet>
         <h:outputText id="col4-txt" value="#{r.reportOutputTypeText}" />
      </a:column>
      
      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.report_template_name}" value="templateName" />
         </f:facet>
         <h:outputText id="col5-txt" value="#{r.templateName}" />
      </a:column> 
      
      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.report_status}" value="status" />
         </f:facet>
         <h:outputText id="col6-txt" value="#{r.status}" />
      </a:column>      
      
      <%-- Actions column --%>
      <a:column id="act-col" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="col4-txt" />
         </f:facet>
         <a:actionLink id="act-col-delete" value="#{msg.report_delete}" tooltip="#{msg.report_delete}" actionListener="#{ReportListDialog.deleteReport}" showLink="false"
            image="/images/icons/delete.gif" styleClass="report-confirm-delete" >
            <f:param name="reportResultNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <h:outputText id="act-col-delete-confirm-msg" value="#{r.confirmDeleteMessage}" styleClass="hidden"/>         
         <a:actionLink id="act-col-cancel" value="#{msg.report_cancel}" tooltip="#{msg.report_cancel}" image="/images/icons/icon_stop.png" 
            showLink="false" actionListener="#{ReportListDialog.cancelReport}" rendered="#{r.cancellingEnabled}" >
            <f:param name="reportResultNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager" styleClass="pager" />      
   </a:richList>
   
<f:verbatim>
   <script type="text/javascript">
      prependOnclick($jQ(".report-confirm-delete"), function(e) {
         var msg = $jQ(e).next().eq(0).text();
         return confirm(msg);
       });
      $jQ(".webdav-readOnly-report").click(function() {
         window.open(this.href, '_blank');
         $jQ(this).next("a").eq(0).click();
         return false;
      });      
   </script>
</f:verbatim>   
  
</a:panel>