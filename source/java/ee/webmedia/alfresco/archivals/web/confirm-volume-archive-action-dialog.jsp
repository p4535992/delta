<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<a:panel id="volume-archive-confirm-panel" label="#{ConfirmVolumeArchiveActionDialog.blockTitle}" styleClass="panel-100" progressive="true" >
   <h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" columnClasses="propertiesLabel," width="100%">
      <h:outputText value="#{ConfirmVolumeArchiveActionDialog.confirmationDateTitle}" styleClass="no-wrap" />
      <h:inputText id="newReviewDateInput" value="#{ConfirmVolumeArchiveActionDialog.newReviewDate}" size="35" styleClass="date" converter="javax.faces.DateTime" >
	  	<f:convertDateTime pattern="dd.MM.yyyy" />
	  </h:inputText>      
   </h:panelGrid>
</a:panel>