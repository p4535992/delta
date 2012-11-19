<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="sendForInformation-panel" label="#{DocumentSendForInformationDialog.blockTitle}" styleClass="panel-100" >
	<h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" columnClasses="propertiesLabel," width="100%">
		<h:panelGroup>
			<f:verbatim><span class="red">*&nbsp;</span></f:verbatim>
			<h:outputText value="#{msg.document_send_for_information_users}" styleClass="no-wrap" />
		</h:panelGroup>            
        <wm:search id="searchSendForInformationUsers" pickerCallback="#{UserContactGroupSearchBean.searchAll}" 
           	dialogTitleId="users_usergroups_search_title" value="#{DocumentSendForInformationDialog.authorities}" searchLinkTooltip="search"
           	showFilter="true" filters="#{UserContactGroupSearchBean.usersGroupsFilters}" dataMultiValued="true" 
           	converter="ee.webmedia.alfresco.user.web.AuthorityConverter" />
        <h:panelGroup>
			<f:verbatim><span class="red">*&nbsp;</span></f:verbatim>
			<h:outputText value="#{msg.document_send_for_information_email_template}" styleClass="no-wrap" />
		</h:panelGroup>		 
        <h:selectOneMenu id="sendForInformationEmailTempalte" value="#{DocumentSendForInformationDialog.selectedEmailTemplate}" >
        	<f:selectItems value="#{DocumentSendForInformationDialog.emailTemplates}" />
        </h:selectOneMenu>
	</h:panelGrid>
</a:panel>