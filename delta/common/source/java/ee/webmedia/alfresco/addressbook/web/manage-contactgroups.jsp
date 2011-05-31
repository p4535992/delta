<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<a:panel id="manage-contactgroup" label="#{msg.addressbook_manage_contactgroups}" styleClass="column panel-100" rendered="#{UserService.documentManager}">
   <a:richList id="usersGroupsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{AddressbookDialog.groups}" var="r" refreshOnBind="true">


      <%-- Identifikaator --%>
      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink label="#{msg.addressbook_group_name}" value="ab:groupName" mode="case-insensitive" />
         </f:facet>
         <f:facet name="small-icon">
         <a:actionLink id="col1-act1" value="#{r['ab:groupName']}" image="/images/icons/users.gif" showLink="false" action="dialog:addressbookManageContacts" actionListener="#{ContactGroupContactsDialog.clickGroup}" styleClass="inlineAction" >
            <f:param name="groupNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         </f:facet>
         <a:actionLink id="col1-act2" value="#{r['ab:groupName']}" action="dialog:addressbookManageContacts" actionListener="#{ContactGroupContactsDialog.clickGroup}" >
            <f:param name="groupNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Actions column --%>
      <a:column id="col2" actions="true">
         <f:facet name="header">
            <h:outputText value="#{msg.addressbook_contactgroup_actions}" />
         </f:facet>
         <a:actionLink value="#{msg.remove}" image="/images/icons/remove_user.gif" showLink="false" styleClass="inlineAction"
            actionListener="#{AddressbookDialog.removeContactFromGroup}" tooltip="#{msg.addressbook_contactgroup_remove_contact}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
   
   <h:panelGroup style="position: absolute; left: -9000px;">
      <wm:search id="addressbook_add_to_group" value="#{AddressbookDialog.groupToAdd}" dataMultiValued="false" dataMandatory="true"
         pickerCallback="#{AddressbookDialog.searchContactGroups}" setterCallback="#{AddressbookDialog.addToContactGroup}"
         dialogTitleId="usergroups_search_title" editable="false" readonly="true" ajaxParentLevel="2" />
   </h:panelGroup>
</a:panel>