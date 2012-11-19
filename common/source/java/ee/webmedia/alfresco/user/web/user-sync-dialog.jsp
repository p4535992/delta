<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="user-sync-panel" styleClass="panel-100" label="#{msg.user_sync_title}" progressive="true">

   <f:verbatim><div class="message"></f:verbatim>
   <h:outputText value="#{msg.user_sync_info}" styleClass="medium" />
   <f:verbatim></div></f:verbatim>
   <h:outputText value="#{msg.user_sync_idCodes}" styleClass="bold" />
   <f:verbatim>&nbsp;</f:verbatim>
   <h:inputText id="userIdCodes" value="#{UserSyncDialog.userIdCodes}" size="70" />
   <f:verbatim><br/></f:verbatim>
   <h:commandButton actionListener="#{UserSyncDialog.syncUsers}" value="#{msg.user_sync_execute}" style="margin-top: 5px;" styleClass="specificAction" />

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
