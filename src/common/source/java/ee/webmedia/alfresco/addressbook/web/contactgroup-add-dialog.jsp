<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<a:panel id="add-user-search" styleClass="column panel-50" label="#{msg.addressbook_contactgroup_add_contact}">
   <a:genericPicker id="picker" showFilter="false" queryCallback="#{DialogManager.bean.pickerCallback}" actionListener="#{DialogManager.bean.addSelectedUsers}" />
</a:panel>

<a:panel id="add-user-list" styleClass="column panel-50" label="#{msg.addressbook_contactgroup_selected_contacts}">

   <h:dataTable value="#{DialogManager.bean.usersDataModel}" var="row" rowClasses="selectedItemsRow,selectedItemsRowAlt" headerClass="selectedItemsHeader"
      rendered="#{DialogManager.bean.usersDataModel.rowCount != 0}">
      <h:column>
         <f:facet name="header">
            <h:outputText value="#{msg.name}" />
         </f:facet>
         <h:outputText value="#{row.name}" />
      </h:column>
      <h:column>
         <a:actionLink actionListener="#{DialogManager.bean.removeUserSelection}" image="/images/icons/delete.gif" value="#{msg.remove}" showLink="false"
            style="padding-left:6px" />
      </h:column>
   </h:dataTable>

   <a:panel id="no-items" rendered="#{DialogManager.bean.usersDataModel.rowCount == 0}">
      <h:outputText id="no-items-msg" value="#{msg.no_selected_items}" />
   </a:panel>

</a:panel>