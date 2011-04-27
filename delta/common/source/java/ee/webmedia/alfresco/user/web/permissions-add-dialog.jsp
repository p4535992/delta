<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<a:panel id="add-authority-search" styleClass="column panel-50" label="#{msg.select_users_groups}">
   <a:genericPicker id="picker" filters="#{UserGroupSearchBean.usersGroupsFilters}" queryCallback="#{UserGroupSearchBean.searchUsersGroups}" actionListener="#{DialogManager.bean.addAuthorities}" />
</a:panel>

<a:panel id="add-authority-list" styleClass="column panel-50-f" label="#{msg.selected_users_groups}">

   <h:dataTable value="#{DialogManager.bean.authorities}" var="row" rowClasses="selectedItemsRow,selectedItemsRowAlt" styleClass="selectedItems"
      headerClass="selectedItemsHeader" cellspacing="0" cellpadding="4" rendered="#{DialogManager.bean.authorities.rowCount != 0}" width="100%">
      <h:column>
         <f:facet name="header">
            <h:outputText value="#{msg.name}" />
         </f:facet>
         <h:graphicImage url="#{row.icon}" />
         <h:outputText value="#{row.name}" />
      </h:column>
      <h:column>
         <a:actionLink actionListener="#{DialogManager.bean.removeAuthority}" image="/images/icons/delete.gif" value="#{msg.remove}" showLink="false"/>
      </h:column>
   </h:dataTable>

   <a:panel id="no-items" rendered="#{DialogManager.bean.authorities.rowCount == 0}">
      <h:outputText id="no-items-msg" value="#{msg.no_selected_items}" />
   </a:panel>

</a:panel>
