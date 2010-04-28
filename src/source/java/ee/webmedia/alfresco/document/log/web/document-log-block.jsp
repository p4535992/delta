<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="document-block-block-panel" label="#{msg.document_log_title}" styleClass="panel-100" progressive="true" rendered="#{DocumentLogBlockBean.rendered}"
   expanded="false">

   <a:richList id="logList" viewMode="details" value="#{DocumentLogBlockBean.documentLogs}" var="r" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
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

   </a:richList>

</a:panel>
