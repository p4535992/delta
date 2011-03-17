<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<a:panel id="delete-panel" styleClass="message" rendered="#{DialogManager.bean.numItemsInGroup != 0}">

   <h:graphicImage url="/images/icons/info_icon.gif" width="16" height="16" />
   <h:outputFormat value="#{msg.addressbook_contactgroup_delete_warning}">
      <f:param value="#{DialogManager.bean.numItemsInGroup}" />
   </h:outputFormat>

</a:panel>

<h:outputFormat value="#{msg.addressbook_contactgroup_delete_confirm}">
   <f:param value="#{DialogManager.bean.groupName}" />
</h:outputFormat>
