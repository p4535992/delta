<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<h:outputText value="#{DeleteDialog.confirmMessage}" rendered="#{DeleteDialog.showConfirm}"/>
<r:propertySheetGrid id="node-props" value="#{DeleteDialog.objectNode}" columns="1" externalConfig="true" mode="view" rendered="#{DeleteDialog.showObjectData}" labelStyleClass="propertiesLabel"/>

=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<h:outputText value="#{DeleteDialog.confirmMessage}" rendered="#{DeleteDialog.showConfirm}"/>
<r:propertySheetGrid id="node-props" value="#{DeleteDialog.objectNode}" columns="1" externalConfig="true" mode="view" rendered="#{DeleteDialog.showObjectData}" labelStyleClass="propertiesLabel"/>

>>>>>>> develop-5.1
