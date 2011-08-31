<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<h:outputFormat value="#{DeleteDialog.confirmMessage}">
   <f:param value="#{DeleteDialog.objectName}" />
</h:outputFormat>
