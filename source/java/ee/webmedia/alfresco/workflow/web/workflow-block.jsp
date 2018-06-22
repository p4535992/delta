<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%@page import="ee.webmedia.alfresco.common.web.BeanHelper" %>
<%@page import="ee.webmedia.alfresco.workflow.web.DelegationBean" %>

<h:panelGroup id="workflow-data-table-group" binding="#{WorkflowBlockBean.dataTableGroup}"/>

<h:panelGroup rendered="#{DelegationBean.confirmationRendered}">
   <h:selectOneMenu id="workflow-delegation-confirmation-messages" styleClass="workflow-delegation-confirmation-messages">
      <f:selectItems value="#{DelegationBean.confirmationMessages}" />
   </h:selectOneMenu>
</h:panelGroup>
   
<a:actionLink id="due-date-confirmation-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DelegationBean.delegateConfirmed}" styleClass="workflow-after-delegation-confirmation-link" style="display: none;" />

<f:verbatim>
	<script type="text/javascript">
	window.onload = getDropdownValue();
	
	function getDropdownValue() {
		var select = document.getElementsByTagName("select");
		if(typeof select[1] !== "undefined"){
			var options = select[1].options;
			select[1].selectedIndex = localStorage.getItem(select[1].id);
			select[1].setAttribute("onchange", "saveDropdownValue()");
			var textarea = document.getElementsByTagName("textarea");
		}
	}
	
	function saveDropdownValue() {
		var select = document.getElementsByTagName("select");
		localStorage.setItem(select[1].id, select[1].selectedIndex);
	}
	</script>
</f:verbatim>