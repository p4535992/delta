<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="contactgroup-panel" styleClass="panel-100 with-pager" label="#{msg.groups}" progressive="true">

   <%-- Main List --%>
   <a:richList id="contactgroup-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.contactGroups}" var="r" initialSortColumn="ab:groupName" >

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
      
      <!-- taskCapable selection -->
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink label="#{msg.addressbook_group_taskCapable}" value="ab:taskCapable" />
         </f:facet>
         <h:selectBooleanCheckbox value="#{r['ab:taskCapable']}" disabled="#{UserService.documentManager == false}"/>
      </a:column>

      <!--  selection -->
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink label="#{msg.addressbook_group_manageableForAdmin}" value="ab:manageableForAdmin" />
         </f:facet>
         <h:selectBooleanCheckbox value="#{r['ab:manageableForAdmin']}" disabled="#{UserService.documentManager == false}"/>
      </a:column>

      <%-- Actions column --%>
      <a:column id="col2-act" actions="true" style="text-align:left">
         <f:facet name="header">
            <h:outputText id="col2-head" value="#{msg.actions}" />
         </f:facet>
         <a:actionLink id="contact-group-add" value="#{msg.add}" image="/images/icons/add_group.gif" showLink="false"
            action="dialog:addressbookAddContactGroup" actionListener="#{ContactGroupAddDialog.setupAddGroup}"
            tooltip="#{msg.addressbook_contactgroup_add_title}"
            rendered="#{UserService.documentManager == true or r['ab:manageableForAdmin'] == false}">
            <f:param id="contactgroup-noderef-add-param" name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="contact-group-delete" value="#{msg.delete}" image="/images/icons/delete_group.gif" showLink="false"
            action="dialog:addressbookDeleteContactGroup" actionListener="#{ContactGroupDeleteDialog.setupDeleteGroup}" tooltip="#{msg.delete_group}"
            rendered="#{UserService.documentManager == true}">
            <f:param id="contactgroup-noderef-delete-param" name="nodeRef" value="#{r.nodeRef}" />
            <f:param id="contactgroup-groupName-delete-param" name="groupName" value="#{r['ab:groupName']}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>
<a:booleanEvaluator value="#{DialogManager.bean.hideFinishButton}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp"  />
</a:booleanEvaluator>