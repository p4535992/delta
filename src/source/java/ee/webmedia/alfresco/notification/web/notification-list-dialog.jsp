<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="notification-list-panel" label="#{msg.notification_important_notifications}" styleClass="with-pager">
      <%-- Main List --%>
   <a:richList id="notifications-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{NotificationListDialog.notifications}" var="r" refreshOnBind="true" initialSortColumn="createdDateTime" initialSortDescending="true">

      <a:column id="col3" primary="true">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.notification_message}" value="message" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" escape="false" value="#{r.message}" />
      </a:column>
      
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.notification_creator}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.creatorName}" />
      </a:column>
      
      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.notification_created}" value="createdDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.createdDateTime}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" type="both" />
         </h:outputText>
      </a:column>      
      
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.notification_active}" value="active" styleClass="header" />
         </f:facet>
         <h:outputText id="col-text-true" value="#{msg.notification_yes}" rendered="#{r.active}" />
         <h:outputText id="col-text-false" value="#{msg.notification_no}" rendered="#{not r.active}" />
      </a:column>
      
      <a:column id="col5" actions="true">
         <f:facet name="header">
            <h:outputText id="col5-header" value="#{msg.notification_actions}" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-act1" value="#{msg.notification_edit}" image="/images/icons/edit_properties.gif" action="dialog:notificationDetailsDialog"
            showLink="false" actionListener="#{NotificationDetailsDialog.setupNotification}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         
         <a:actionLink id="col5-act2" value="#{msg.notification_delete}" actionListener="#{BrowseBean.setupContentAction}" action="dialog:deleteNotification" showLink="false"
            image="/images/icons/delete.gif">
            <f:param name="id" value="#{r.nodeRef.id}" />
         </a:actionLink>
         
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="notifications-pager" styleClass="pager" />
   </a:richList>
   
</a:panel>