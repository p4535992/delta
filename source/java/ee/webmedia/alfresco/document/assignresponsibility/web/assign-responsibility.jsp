<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="assign-responsibility-panel" styleClass="panel-100" label="#{msg.assign_responsibility}" progressive="true" rendered="#{AssignResponsibilityBean.panelVisible}">
   <a:booleanEvaluator value="#{AssignResponsibilityBean.instructionSet}">
      <f:verbatim><div class="message"></f:verbatim>
      <h:outputText value="#{AssignResponsibilityBean.instruction}" styleClass="medium" />
      <f:verbatim></div></f:verbatim>
   </a:booleanEvaluator>
   <r:propertySheetGrid labelStyleClass="propertiesLabel" columns="1" mode="edit" value="#{AssignResponsibilityBean.node}" externalConfig="true" var="assignResponsibility" rendered="#{AssignResponsibilityBean.notLeaving}" />
   <h:commandButton actionListener="#{AssignResponsibilityBean.execute}" value="#{msg.assign_responsibility_perform}" disabled="#{AssignResponsibilityBean.ownerUnset}" style="margin-top: 5px;" rendered="#{AssignResponsibilityBean.notLeaving}" />
   <h:commandButton actionListener="#{AssignResponsibilityBean.revert}" value="#{msg.assign_responsibility_revert}" style="margin-top: 5px;" rendered="#{AssignResponsibilityBean.leavingAndAdmin}" />
</a:panel>
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="assign-responsibility-panel" styleClass="panel-100" label="#{msg.assign_responsibility}" progressive="true" rendered="#{AssignResponsibilityBean.panelVisible}">
   <a:booleanEvaluator value="#{AssignResponsibilityBean.instructionSet}">
      <f:verbatim><div class="message"></f:verbatim>
      <h:outputText value="#{AssignResponsibilityBean.instruction}" styleClass="medium" />
      <f:verbatim></div></f:verbatim>
   </a:booleanEvaluator>
   <r:propertySheetGrid labelStyleClass="propertiesLabel" columns="1" mode="edit" value="#{AssignResponsibilityBean.node}" externalConfig="true" var="assignResponsibility" rendered="#{AssignResponsibilityBean.notLeaving}" />
   <h:commandButton actionListener="#{AssignResponsibilityBean.execute}" value="#{msg.assign_responsibility_perform}" disabled="#{AssignResponsibilityBean.ownerUnset}" style="margin-top: 5px;" rendered="#{AssignResponsibilityBean.notLeaving}" />
   <h:commandButton actionListener="#{AssignResponsibilityBean.revert}" value="#{msg.assign_responsibility_revert}" style="margin-top: 5px;" rendered="#{AssignResponsibilityBean.leavingAndAdmin}" />
</a:panel>
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
