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
      var confirmMsgRemovePerson = '<%= MessageUtil.getMessageAndEscapeJS("manage_permissions_confirm_removePerson") %>';
      var confirmMsgRemoveGroupWithUsers = '<%= MessageUtil.getMessageAndEscapeJS("manage_permissions_confirm_removeGroupWithUsers") %>';
      var confirmMsgInlineGroupUsers = '<%=BeanHelper.getManageInheritablePrivilegesDialog().getTypeHandler().getConfirmInlineGroupUsersMsg()%>';
      var privDependencies = <%= BeanHelper.getManageInheritablePrivilegesDialog().getPrivilegeDependencies() %>;

      function saveIfNeeded(cb) {
         var submitWhenCheckboxUnchecked = <%= BeanHelper.getManageInheritablePrivilegesDialog().getTypeHandler().isSubmitWhenCheckboxUnchecked() %>;
         if (submitWhenCheckboxUnchecked && !$jQ(cb).is(":checked")) {
            clickFinishButton();
         }
      }
   </script>
<%-- FIXME PRIV2 Ats - test
   <script type="text/javascript" src="<%=request.getContextPath()%>/scripts/manage-inheritable-privileges-dialog.js?r=<%=PageTag.urlSuffix%>"> </script>
 --%>
   <script type="text/javascript" src="<%=request.getContextPath()%>/scripts/manage-inheritable-privileges-dialog.js?r=<%=System.currentTimeMillis()%>"> </script>
</f:verbatim>

<%-- FIXME PRIV2 Ats - test
 --%>
<h:outputText value="#{ManageInheritablePrivilegesDialog.state.manageableRef}" />

<a:panel id="permissions-search-panel" label="#{msg.users_usergroups_search_title}" progressive="true">
   <a:genericPicker id="picker" filters="#{UserContactGroupSearchBean.usersGroupsFilters}" queryCallback="#{UserContactGroupSearchBean.searchAllWithAdminsAndDocManagers}"
      actionListener="#{ManageInheritablePrivilegesDialog.addAuthorities}" binding="#{ManageInheritablePrivilegesDialog.picker}"
      rendered="#{ManageInheritablePrivilegesDialog.typeHandler.editable}"/>
</a:panel>

<a:panel id="permissions-panel" label="#{msg.manage_permissions_panel}">
<h:panelGroup id="removeMeWhenImplemented" rendered="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxValue != null}">
   <%-- FIXME PRIV2 Ats - wrapper is temp hack for removing checkbox from series permissions management view where it means different thing.
   Wrapper should be removed when this is resolved
    --%>

   <%-- checkbox: inherit permissions 
   or in case of series permissions management: 
   documents without view-permission are visible
    --%>
   <h:outputText value="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxLabel}" />
   <h:selectBooleanCheckbox value="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxValue}" onchange="saveIfNeeded(this)" valueChangeListener="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxChanged}" disabled="#{!ManageInheritablePrivilegesDialog.typeHandler.editable}" />
</h:panelGroup>

   <a:richList id="permissions-list" binding="#{ManageInheritablePrivilegesDialog.permissionsRichList}" value="#{ManageInheritablePrivilegesDialog.userPrivilegesRows}" var="r" refreshOnBind="true"
      viewMode="detailsMultiTbody" width="100%" styleClass="privileges detailsMultiTbody" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRow">

      <%-- column where expand/collapse link is added through hack for grouping --%>
      <a:column />

      <%-- Label column (user fullname / group display name) --%>
      <a:column primary="true" styleClass="left">
         <f:facet name="header">
            <h:outputText value="#{msg.manage_permissions_column_name}" />
         </f:facet>
         <h:outputText value="#{r.userDisplayName}" title="#{r.explanationByPrivilege[ManageInheritablePrivilegesDialog.typeHandler.implicitPrivilege]}" />
      </a:column>

      <%-- OTHER COLUMNS ARE ADDED IN JAVA WHILE BINDING TO DIALOG --%>
   </a:richList>
</a:panel>


