<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/sendout/web/message-input-helper-js.jsp" />

<a:panel id="sendForInformation-panel" label="#{DialogManager.bean.blockTitle}" styleClass="panel-100" >
	<h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" columnClasses="propertiesLabel," width="100%">
		<h:panelGroup>
			<f:verbatim><span class="red">*&nbsp;</span></f:verbatim>
			<h:outputText value="#{msg.document_send_for_information_users}" styleClass="no-wrap" />
		</h:panelGroup>            
        <wm:search id="searchSendForInformationUsers" pickerCallback="#{UserContactGroupSearchBean.searchAll}" 
           	dialogTitleId="users_usergroups_search_title" value="#{DialogManager.bean.authorities}" searchLinkTooltip="search"
           	showFilter="true" filters="#{UserContactGroupSearchBean.usersGroupsFilters}" dataMultiValued="true" 
           	converter="ee.webmedia.alfresco.user.web.AuthorityConverter" />
        <h:panelGroup>
			<f:verbatim><span class="red">*&nbsp;</span></f:verbatim>
			<h:outputText value="#{msg.document_send_for_information_subject}" styleClass="no-wrap" />
		</h:panelGroup>
		<h:inputTextarea value="#{DialogManager.bean.subject}" styleClass="expand19-200 long"/>
		<h:outputText value="#{msg.document_send_for_information_email_template}" styleClass="no-wrap" /> 
        
        <h:panelGroup styleClass="no-icon">
        <h:selectOneMenu id="sendForInformationEmailTempalte" value="#{DialogManager.bean.selectedEmailTemplate}" >
        	<f:selectItems value="#{DialogManager.bean.emailTemplates}" />
        </h:selectOneMenu>
        <h:commandLink id="setTemplateBtn" value="#{msg.document_set_template}" actionListener="#{DialogManager.bean.updateTemplate}" />
      	</h:panelGroup>
      
        <h:panelGroup>
        <f:verbatim><span class="red">*&nbsp;</span></f:verbatim>
        <h:outputText value="#{msg.document_send_content}" styleClass="no-wrap" />
        </h:panelGroup>
        <h:inputTextarea id="editor" value="#{DialogManager.bean.content}" rows="5" cols="40" />
        
	</h:panelGrid>
</a:panel>