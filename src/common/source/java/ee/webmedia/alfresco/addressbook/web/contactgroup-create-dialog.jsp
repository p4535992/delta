<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="contactgroup-create-panel" label="#{msg.group_props}" styleClass="panel-100" progressive="true">

   <a:panel id="new-group-message" styleClass="message">
      <h:graphicImage url="/images/icons/info_icon.gif" width="16" height="16" alt="" />
      <h:outputText value="#{msg.create_group_warning}" />
   </a:panel>
   
   <r:propertySheetGrid id="create-metadata" value="#{DialogManager.bean.currentNode}" columns="1" mode="edit" externalConfig="true"
      labelStyleClass="propertiesLabel" />
</a:panel>
