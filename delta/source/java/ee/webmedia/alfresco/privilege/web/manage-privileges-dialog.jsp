<%@page import="org.alfresco.web.ui.repo.tag.PageTag"%>
<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@page import="org.alfresco.web.app.Application"%>
<%@page import="javax.faces.context.FacesContext"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<f:verbatim>
   <script type="text/javascript">
	var confirmMsg = '<%= MessageUtil.getMessageAndEscapeJS("manage_permissions_delete_confirm") %>';
	var privDependencies = <%= BeanHelper.getManagePrivilegesDialog().getPrivilegeDependencies() %>;
   </script>
   <script type="text/javascript" src="<%=request.getContextPath()%>/scripts/manage-privileges-dialog.js?r=<%=PageTag.urlSuffix%>"> </script>
</f:verbatim>

<a:panel id="permissions-panel" label="#{msg.users_groups}">
   <a:booleanEvaluator value="#{ManagePrivilegesDialog.editable}">
      <a:genericPicker id="picker" filters="#{UserContactGroupSearchBean.usersGroupsFilters}" queryCallback="#{UserContactGroupSearchBean.searchWithoutAdmins}"
         actionListener="#{ManagePrivilegesDialog.addAuthorities}" binding="#{ManagePrivilegesDialog.picker}"/>
   </a:booleanEvaluator>
   <a:richList id="permissions-list" binding="#{ManagePrivilegesDialog.permissionsRichList}" value="#{ManagePrivilegesDialog.userPrivilegesRows}" var="r" refreshOnBind="true"
      viewMode="detailsMultiTbody" width="100%" styleClass="privileges detailsMultiTbody" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRow">

      <%-- column where expand/collapse link is added through hack for grouping --%>
      <a:column />

      <%-- Label column (user fullname / group display name) --%>
      <a:column primary="true" styleClass="left">
         <f:facet name="header">
            <h:outputText value="#{msg.manage_permissions_column_name}" />
         </f:facet>
         <h:outputText value="#{r.userDisplayName}" />
      </a:column>

      <%-- OTHER COLUMNS ARE ADDED IN JAVA WHILE BINDING TO DIALOG --%>
   </a:richList>
</a:panel>


