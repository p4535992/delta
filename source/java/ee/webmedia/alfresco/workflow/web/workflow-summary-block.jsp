<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<!-- FIXME: Very bad idea to use binding attribute with session-scoped beans -->
<!-- Needs hack in WorkflowBlockBean#setWfPanelGroup to refresh values -->
<h:panelGroup binding="#{WorkflowBlockBean.wfPanelGroup}" />
