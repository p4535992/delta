<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="send-out-block-panel" label="#{msg.document_send_title}" styleClass="panel-100" progressive="true" rendered="#{SendOutBlockBean.rendered}" expanded="false">

   <a:richList id="sendOutList" viewMode="details" value="#{SendOutBlockBean.sendInfos}" var="r" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" refreshOnBind="true" initialSortColumn="sendDateTime">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.document_send_recipients}" value="recipient" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-txt" value="#{r.recipient}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.document_send_date}" value="sendDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.sendDateTime}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.document_send_mode}" value="sendMode" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.sendMode}" />
      </a:column>

      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-header" label="#{msg.document_send_status}" value="sendStatus" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
         <h:outputText id="col4-txt" value="#{r.sendStatus}" />
=======
         <h:outputText id="col4-txt" value="#{r.sendStatusWithReceivedDateTime}" />
      </a:column>
      
      <a:column id="col4-1">
         <f:facet name="header">
            <a:sortLink id="col4-1-header" label="#{msg.document_send_opened}" value="openedDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-1-txt" value="#{r.openedDateTime}" />
>>>>>>> develop-5.1
      </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-header" label="#{msg.document_send_resolution}" value="resolution" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-txt" value="#{r.resolution}" escape="false" />
      </a:column>
<<<<<<< HEAD
      
=======

>>>>>>> develop-5.1
   </a:richList>

</a:panel>
