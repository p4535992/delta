<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- just a placeholder for dynamically generated panels with action buttons--%>
<!-- FIXME: Very bad idea to use binding attribute with session-scoped beans -->
<!-- Needs hack in WorkflowBlockBean#setDataTableGroup to refresh values -->
<h:panelGroup id="workflow-data-table-group" binding="#{WorkflowBlockBean.dataTableGroup}"/>
