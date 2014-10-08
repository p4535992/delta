<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="applog-results-panel" styleClass="panel-100 with-pager" label="#{msg.applog_results}" progressive="true">

   <a:richList id="applogList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      value="#{ApplicationLogListDialog.logEntries}" var="r" initialSortColumn="createdDateTime" refreshOnBind="true">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.applog_item_id}" value="logEntryId" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.logEntryId}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.applog_item_datetime}" value="createdDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.createdDateTime}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.applog_item_user}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.creatorName}" />
      </a:column>

      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.applog_item_computer}" value="computerIp" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.computerIp}" />
      </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.applog_item_description}" value="eventDescription" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.eventDescription}" />
      </a:column>

      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.applog_item_object}" value="objectName" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.objectName}" />
      </a:column>

      <a:column id="col7">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.applog_item_object_id}" value="objectId" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.objectId}" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="applog-results-panel" styleClass="panel-100 with-pager" label="#{msg.applog_results}" progressive="true">

   <a:richList id="applogList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      value="#{ApplicationLogListDialog.logEntries}" var="r" initialSortColumn="createdDateTime" refreshOnBind="true">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.applog_item_id}" value="logEntryId" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.logEntryId}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.applog_item_datetime}" value="createdDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.createdDateTime}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_with_second_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.applog_item_user}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.creatorName}" />
      </a:column>

      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.applog_item_computer}" value="computerIp" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.computerIp}" />
      </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.applog_item_description}" value="eventDescription" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.eventDescriptionAndLinks}" styleClass="condence150" escape="false"/>
      </a:column>

      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.applog_item_object}" value="objectName" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.objectName}" />
      </a:column>

      <a:column id="col7">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.applog_item_object_id}" value="objectId" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.objectId}" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
>>>>>>> develop-5.1
</a:panel>