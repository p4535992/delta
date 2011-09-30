<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- Users in Group list --%>
<a:panel id="group-contacts-panel" styleClass="panel-100 with-pager" label="#{msg.addressbook_contactgroup_contacts}" progressive="true" >

   <a:richList id="group-contacts-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" refreshOnBind="true"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.groupContacts}" var="r" initialSortColumn="name">

      <a:column id="col1-contacts" primary="true">
         <f:facet name="small-icon">
            <h:graphicImage alt="#{r.name}" value="/images/icons/person.gif" />
         </f:facet>
         <f:facet name="header">
            <a:sortLink label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <a:actionLink id="con-org-list-link" value="#{r.name}" rendered="#{r.dialogType=='org'}"
         showLink="false" action="dialog:addressbookOrgDetails" actionListener="#{AddressbookOrgDetailsDialog.setupViewEntry}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="con-people-list-link" value="#{r.name}"rendered="#{r.dialogType=='privPerson'}"
            showLink="false" action="dialog:addressbookPersonDetails" actionListener="#{AddressbookPersonDetailsDialog.setupViewEntry}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="con-org-people-list-link" value="#{r.name}"rendered="#{r.dialogType=='orgPerson'}"
            showLink="false" action="dialog:addressbookOrgPersonDetails" actionListener="#{AddressbookPersonDetailsDialog.setupViewEntry}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="col2-contacts">
         <f:facet name="header">
            <a:sortLink label="#{msg.addressbook_contactgroup_contact_type}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText value="#{r.type}" />
      </a:column>

      <a:column id="col3-contacts" actions="true">
         <f:facet name="header">
            <h:outputText value="#{msg.addressbook_contactgroup_actions}" />
         </f:facet>
         <a:actionLink value="#{msg.remove}" image="/images/icons/remove_user.gif" showLink="false" styleClass="inlineAction"
            actionListener="#{DialogManager.bean.removeContact}" tooltip="#{msg.addressbook_contactgroup_remove_contact}" rendered="#{DialogManager.bean.manageable}">
            <f:param name="contactNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />