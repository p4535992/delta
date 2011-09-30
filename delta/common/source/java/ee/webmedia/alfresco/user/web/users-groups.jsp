<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<a:panel id="users-groups" label="#{msg.usergroups}" styleClass="with-pager">
   <a:richList id="usersGroupsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{UserDetailsDialog.groups}" var="r" refreshOnBind="true">
      
         <%-- Primary column for details view mode --%>
         <a:column primary="true" style="padding:2px;text-align:left">
            <f:facet name="small-icon">
               <a:actionLink value="#{r.name}" image="/images/icons/group.gif" actionListener="#{GroupsDialog.clickGroup}" showLink="false">
                  <f:param id="ugParam1" name="id" value="#{r.id}" />
               </a:actionLink>
            </f:facet>
            <f:facet name="header">
               <a:sortLink label="#{msg.addressbook_group_name}" value="id" mode="case-insensitive" styleClass="header"/>
            </f:facet>
            
            <a:actionLink value="#{r.displayName}" action="dialog:manageGroups" actionListener="#{GroupsDialog.clickGroup}" rendered="#{UserService.documentManager}">
               <f:param id="ugParam2" name="id" value="#{r.id}" />
            </a:actionLink>
            <h:outputText value="#{r.displayName}" rendered="#{!UserService.documentManager}" />
         </a:column>
         
        <%-- Actions column --%>
         <a:column actions="true" rendered="#{UserService.groupsEditingAllowed and UserService.documentManager}">
            <f:facet name="header">
               <h:outputText value="#{msg.actions}" />
            </f:facet>
               <a:actionLink value="" tooltip="#{msg.delete_user}" image="/images/icons/remove_user.gif" actionListener="#{UserDetailsDialog.removeFromGroup}">
                  <f:param id="ugParam3" name="group" value="#{r.group}" />
               </a:actionLink>
         </a:column>
                  
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
         <a:dataPager id="pager1" styleClass="pager" /> 
  </a:richList>
  <h:panelGroup style="position: absolute; left: -9000px;">
      <wm:search id="userGroupSearch"
           value="#{UserDetailsDialog.groupToAdd}"
           dataMultiValued="false"
           dataMandatory="true"
           pickerCallback="#{UserContactGroupSearchBean.searchGroupsWithAdminsAndDocManagers}"
           setterCallback="#{UserDetailsDialog.addToGroup}"
           dialogTitleId="usergroups_search_title"
           editable="false"
           readonly="true"
           ajaxParentLevel="2"
        />
  </h:panelGroup>
</a:panel>