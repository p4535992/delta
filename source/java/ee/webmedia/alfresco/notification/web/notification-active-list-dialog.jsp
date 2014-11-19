<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="notification-active-list-panel" label="#{msg.notification_important_notifications}" rendered="#{not empty NotificationBean.generalNotifications && SubstitutionBean.currentStructUnitUser}" styleClass="with-pager">
      <%-- Main List --%>
   <a:richList id="notifications-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{NotificationBean.generalNotifications}" var="r" refreshOnBind="true" initialSortColumn="createdDateTime" initialSortDescending="true" >

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
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="notifications-pager" styleClass="pager" />
   </a:richList>
   
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="notification-active-list-panel" label="#{msg.notification_important_notifications}" rendered="#{not empty NotificationBean.generalNotifications}" styleClass="with-pager">
      <%-- Main List --%>
   <a:richList id="notifications-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{NotificationBean.generalNotifications}" var="r" refreshOnBind="true" initialSortColumn="createdDateTime" initialSortDescending="true" >

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
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="notifications-pager" styleClass="pager" />
   </a:richList>
   
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
</a:panel>