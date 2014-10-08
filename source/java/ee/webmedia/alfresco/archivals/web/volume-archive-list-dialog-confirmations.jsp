<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@page import="org.alfresco.web.app.Application"%>
<%@page import="ee.webmedia.alfresco.archivals.web.VolumeArchiveBaseDialog" %>

<h:panelGroup id="voluem-archive-confirmation-panel" rendered="#{DialogManager.bean.showConfirmationMessage}">
	<% VolumeArchiveBaseDialog volumeArchiveBaseDialog = (VolumeArchiveBaseDialog) Application.getDialogManager().getBean(); %>
 	<f:verbatim>
 		<script type="text/javascript">
 		      $jQ(document).ready(function () {
 		      	if(confirm("<%= volumeArchiveBaseDialog.getConfirmationMessage() %>")){
 		      	   $jQ("#" + "<%= volumeArchiveBaseDialog.getConfirmationLinkId() %>" ).eq(0).click();
 		      	} else {
 		      	   $jQ("#volume-archive-after-confirmation-rejected-link").eq(0).click();
		      	}
 		      });
 		   </script>
 	</f:verbatim>
 	<a:actionLink id="volume-archive-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.archive}" styleClass="hidden" />
 	<a:actionLink id="volume-generate-word-file-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.generateWordFile}" styleClass="hidden" />
 	<a:actionLink id="volume-mark-for-transfer-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.markForTransfer}" styleClass="hidden" />
 	<a:actionLink id="volume-transfer-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.transfer}" styleClass="hidden" />
 	<a:actionLink id="volume-export-to-uam-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.exportToUam}" styleClass="hidden" />
 	<a:actionLink id="volume-compose-disposal-act-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.composeDisposalAct}" styleClass="hidden" />
 	<a:actionLink id="volume-start-destruction-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.startDestruction}" styleClass="hidden" />
 	<a:actionLink id="volume-start-simple-destruction-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.startSimpleDestruction}" styleClass="hidden" />
	<a:actionLink id="volume-archive-after-confirmation-rejected-link" value="confirmationRejectedLink" actionListener="#{DialogManager.bean.cancelAction}" styleClass="hidden" />
</h:panelGroup>
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@page import="org.alfresco.web.app.Application"%>
<%@page import="ee.webmedia.alfresco.archivals.web.VolumeArchiveBaseDialog" %>

<h:panelGroup id="voluem-archive-confirmation-panel" rendered="#{DialogManager.bean.showConfirmationMessage}">
	<% VolumeArchiveBaseDialog volumeArchiveBaseDialog = (VolumeArchiveBaseDialog) Application.getDialogManager().getBean(); %>
 	<f:verbatim>
 		<script type="text/javascript">
 		      $jQ(document).ready(function () {
 		      	if(confirm("<%= volumeArchiveBaseDialog.getConfirmationMessage() %>")){
 		      	   $jQ("#" + "<%= volumeArchiveBaseDialog.getConfirmationLinkId() %>" ).eq(0).click();
 		      	} else {
 		      	   $jQ("#volume-archive-after-confirmation-rejected-link").eq(0).click();
		      	}
 		      });
 		   </script>
 	</f:verbatim>
 	<a:actionLink id="volume-archive-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.archive}" styleClass="hidden" />
 	<a:actionLink id="volume-generate-word-file-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.generateWordFile}" styleClass="hidden" />
 	<a:actionLink id="volume-mark-for-transfer-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.markForTransfer}" styleClass="hidden" />
 	<a:actionLink id="volume-transfer-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.transfer}" styleClass="hidden" />
 	<a:actionLink id="volume-export-to-uam-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.exportToUam}" styleClass="hidden" />
 	<a:actionLink id="volume-compose-disposal-act-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.composeDisposalAct}" styleClass="hidden" />
 	<a:actionLink id="volume-start-destruction-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.startDestruction}" styleClass="hidden" />
 	<a:actionLink id="volume-start-simple-destruction-after-confirmation-accepted-link" value="confirmationAcceptedLink" actionListener="#{DialogManager.bean.startSimpleDestruction}" styleClass="hidden" />
	<a:actionLink id="volume-archive-after-confirmation-rejected-link" value="confirmationRejectedLink" actionListener="#{DialogManager.bean.cancelAction}" styleClass="hidden" />
</h:panelGroup>
>>>>>>> develop-5.1
