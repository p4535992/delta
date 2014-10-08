<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<<<<<<< HEAD
<h:panelGroup id="dialog-modal-wf-container" binding="#{WorkflowBlockBean.modalContainer}" />
<h:panelGroup id="workflow-data-table-group" binding="#{WorkflowBlockBean.dataTableGroup}"/>
=======

<%@page import="ee.webmedia.alfresco.common.web.BeanHelper" %>
<%@page import="ee.webmedia.alfresco.workflow.web.DelegationBean" %>
   
<h:panelGroup id="workflow-data-table-group" binding="#{WorkflowBlockBean.dataTableGroup}"/>

<h:panelGroup rendered="#{DelegationBean.confirmationRendered}">
   <h:selectOneMenu id="workflow-delegation-confirmation-messages" styleClass="workflow-delegation-confirmation-messages">
      <f:selectItems value="#{DelegationBean.confirmationMessages}" />
   </h:selectOneMenu>
</h:panelGroup>
   
<a:actionLink id="due-date-confirmation-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DelegationBean.delegateConfirmed}" styleClass="workflow-after-delegation-confirmation-link" style="display: none;" />
>>>>>>> develop-5.1
