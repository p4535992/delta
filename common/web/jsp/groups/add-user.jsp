<<<<<<< HEAD
<%--
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
--%><%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<a:panel id="add-user-search" styleClass="column panel-50" label="#{msg.select_users}">
   <a:genericPicker id="picker" showFilter="false" queryCallback="#{UserListDialog.searchUsers}" actionListener="#{DialogManager.bean.addSelectedUsers}" />
</a:panel>

<a:panel id="add-user-list" styleClass="column panel-50-f" label="#{msg.selected_users}">

   <h:dataTable value="#{DialogManager.bean.usersDataModel}" var="row" rowClasses="selectedItemsRow,selectedItemsRowAlt" styleClass="selectedItems"
      headerClass="selectedItemsHeader" cellspacing="0" cellpadding="4" rendered="#{DialogManager.bean.usersDataModel.rowCount != 0}" width="100%">
      <h:column>
         <f:facet name="header">
            <h:outputText value="#{msg.name}" />
         </f:facet>
         <h:outputText value="#{row.name}" />
      </h:column>
      <h:column>
         <a:actionLink actionListener="#{DialogManager.bean.removeUserSelection}" image="/images/icons/delete.gif" value="#{msg.remove}" showLink="false" />
      </h:column>
   </h:dataTable>

   <a:panel id="no-items" rendered="#{DialogManager.bean.usersDataModel.rowCount == 0}">
      <h:outputText id="no-items-msg" value="#{msg.no_selected_items}" />
   </a:panel>

</a:panel>
=======
<%--
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
--%><%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<a:panel id="add-user-search" styleClass="column panel-50" label="#{msg.select_users}">
   <a:genericPicker id="picker" showFilter="false" queryCallback="#{UserListDialog.searchUsers}" actionListener="#{DialogManager.bean.addSelectedUsers}" />
</a:panel>

<a:panel id="add-user-list" styleClass="column panel-50-f" label="#{msg.selected_users}">

   <h:dataTable value="#{DialogManager.bean.usersDataModel}" var="row" rowClasses="selectedItemsRow,selectedItemsRowAlt" styleClass="selectedItems"
      headerClass="selectedItemsHeader" cellspacing="0" cellpadding="4" rendered="#{DialogManager.bean.usersDataModel.rowCount != 0}" width="100%">
      <h:column>
         <f:facet name="header">
            <h:outputText value="#{msg.name}" />
         </f:facet>
         <h:outputText value="#{row.name}" />
      </h:column>
      <h:column>
         <a:actionLink actionListener="#{DialogManager.bean.removeUserSelection}" image="/images/icons/delete.gif" value="#{msg.remove}" showLink="false" />
      </h:column>
   </h:dataTable>

   <a:panel id="no-items" rendered="#{DialogManager.bean.usersDataModel.rowCount == 0}">
      <h:outputText id="no-items-msg" value="#{msg.no_selected_items}" />
   </a:panel>

</a:panel>
>>>>>>> develop-5.1
