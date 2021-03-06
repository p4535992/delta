<%--
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 
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
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<f:verbatim>
   <script type="text/javascript">
  
      window.onload = pageLoaded;
     
      function pageLoaded()
      {
         document.getElementById("dialog:dialog-body:name").focus();
         checkButtonState();
      }
     
      function checkButtonState()
      {
         if (document.getElementById("dialog:dialog-body:name").value.length == 0 )
         {
            document.getElementById("dialog:finish-button").disabled = true;
         }
         else
         {
            document.getElementById("dialog:finish-button").disabled = false;
         }
      }
   </script>
</f:verbatim>
<a:panel id="edit-category" label="#{msg.category_props}">

   <h:panelGrid columns="2" columnClasses="propertiesLabel" cellpadding="0" cellspacing="0">

      <h:panelGroup>
         <f:verbatim escape="false"><span class="red">* </span></f:verbatim>
         <h:outputText value="#{msg.name}" />
      </h:panelGroup>
      <h:inputText id="name" value="#{DialogManager.bean.name}" size="35" maxlength="1024" onkeyup="javascript:checkButtonState();"
         onchange="javascript:checkButtonState();" />

      <h:outputText value="#{msg.description}" />
      <h:inputText value="#{DialogManager.bean.description}" size="35" maxlength="1024" />

   </h:panelGrid>

</a:panel>