<<<<<<< HEAD
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
      var valueChecked = <%= BeanHelper.getManageInheritablePrivilegesDialog().getTypeHandler().getCheckboxValue() %>;

      function saveIfNeeded(cb) {
         if (<%= BeanHelper.getManageInheritablePrivilegesDialog().getTypeHandler().isSubmitWhenCheckboxUnchecked() %>) {
            var confirmRemoveInheritance = '<%= MessageUtil.getMessageAndEscapeJS("manage_permissions_confirm_removeInheritance") %>';
            var confirmSetInheritance = '<%= MessageUtil.getMessageAndEscapeJS("manage_permissions_confirm_setInheritance") %>';
            var msg = cb.checked ? confirmSetInheritance : confirmRemoveInheritance;
            if(confirm(msg)){
               clickFinishButton();
            } else {
               cb.checked = !cb.checked;
            }
         }
      }
      document.getElementById("dialog:finish-button").onclick = function() {
         var msg = '<%= BeanHelper.getManageInheritablePrivilegesDialog().getTypeHandler().getConfirmMessage() %>';
         if (msg && valueChecked != $jQ('input:checkbox:first').is(':checked') && !confirm(msg)) {
            $jQ('input:checkbox:first')[0].checked = valueChecked;
            return false;
         }
      }
      
      $jQ(document).ready(function() {
         $jQ('.saveIfNeeded').change(function() {
            saveIfNeeded(this);
         });
      });

   </script>
   <script type="text/javascript" src="<%=request.getContextPath()%>/scripts/manage-inheritable-privileges-dialog.js?r=<%=PageTag.urlSuffix%>"> </script>
</f:verbatim>

<%-- FIXME PRIV2 - jätan selle Kaareli soovil alles, et õiguseid lihtsam debugida oleks
<h:outputText value="#{ManageInheritablePrivilegesDialog.state.manageableRef}" />
 --%>

<a:panel id="permissions-search-panel" label="#{msg.users_usergroups_search_title}" progressive="true">
   <a:genericPicker id="picker" filters="#{UserContactGroupSearchBean.usersGroupsFilters}" queryCallback="#{UserContactGroupSearchBean.searchAllWithAdminsAndDocManagers}"
      actionListener="#{ManageInheritablePrivilegesDialog.addAuthorities}" binding="#{ManageInheritablePrivilegesDialog.picker}"
      rendered="#{ManageInheritablePrivilegesDialog.typeHandler.editable}"/>
</a:panel>

<a:panel id="permissions-panel" label="#{msg.manage_permissions_panel}">
   <%-- checkbox: inherit permissions 
   or in case of series permissions management: 
   documents without view-permission are visible
    --%>
   <h:outputText value="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxLabel}" />
   <h:selectBooleanCheckbox value="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxValue}" styleClass="saveIfNeeded" valueChangeListener="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxChanged}" disabled="#{!ManageInheritablePrivilegesDialog.typeHandler.editable}" />

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


=======
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
         if (submitWhenCheckboxUnchecked ) {
            var jQCB = $jQ(cb);
            var checked = jQCB.is(":checked");
            var confirmRemoveInheritance = '<%= MessageUtil.getMessageAndEscapeJS("manage_permissions_confirm_removeInheritance") %>';
            var confirmSetInheritance = '<%= MessageUtil.getMessageAndEscapeJS("manage_permissions_confirm_setInheritance") %>';
            var msg = checked ? confirmSetInheritance : confirmRemoveInheritance;
            if(confirm(msg)){
               clickFinishButton();
            } else {
               jQCB.prop('checked', !checked);
            }
         }
      }
      
      $jQ(document).ready(function() {
         $jQ('.saveIfNeeded').change(function() {
            saveIfNeeded(this);
         });
      });

   </script>
   <script type="text/javascript" src="<%=request.getContextPath()%>/scripts/manage-inheritable-privileges-dialog.js?r=<%=PageTag.urlSuffix%>"> </script>
</f:verbatim>

<%-- FIXME PRIV2 - jätan selle soovil alles, et õiguseid lihtsam debugida oleks
<h:outputText value="#{ManageInheritablePrivilegesDialog.state.manageableRef}" />
 --%>

<a:panel id="permissions-search-panel" label="#{msg.users_usergroups_search_title}" progressive="true">
   <a:genericPicker id="picker" filters="#{UserContactGroupSearchBean.usersGroupsFilters}" queryCallback="#{UserContactGroupSearchBean.searchAllWithAdminsAndDocManagers}"
      actionListener="#{ManageInheritablePrivilegesDialog.addAuthorities}" binding="#{ManageInheritablePrivilegesDialog.picker}"
      rendered="#{ManageInheritablePrivilegesDialog.typeHandler.editable}"/>
</a:panel>

<a:panel id="permissions-panel" label="#{msg.manage_permissions_panel}">
<h:panelGroup id="removeMeWhenImplemented" rendered="#{ManageInheritablePrivilegesDialog.typeHandler.class.simpleName != 'SeriesTypePrivilegesHandler'}">
   <%-- FIXME PRIV2 - wrapper is temp hack for removing checkbox from series permissions management view where it means different thing.
   Wrapper should be removed when this is resolved
    --%>

   <%-- checkbox: inherit permissions 
   or in case of series permissions management: 
   documents without view-permission are visible
    --%>
   <h:outputText value="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxLabel}" />
   <h:selectBooleanCheckbox value="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxValue}" styleClass="saveIfNeeded" valueChangeListener="#{ManageInheritablePrivilegesDialog.typeHandler.checkboxChanged}" disabled="#{!ManageInheritablePrivilegesDialog.typeHandler.editable}" />
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


>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
