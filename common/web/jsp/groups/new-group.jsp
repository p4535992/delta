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
--%>
<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<f:verbatim>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/validation.js"> </script>
<script type="text/javascript">
   window.onload = pageLoaded;
   
   function pageLoaded()
   {
      document.getElementById("dialog:dialog-body:name").focus();
      document.getElementById("dialog").onsubmit = validate;
      document.getElementById("dialog:finish-button").onclick = function() {finishButtonPressed = true; clear_dialog();}
   }
   
   function validate()
   {
      if (finishButtonPressed)
      {
         return finishButtonPressed = validateMandatory(document.getElementById("dialog:dialog-body:name"), '<%= MessageUtil.getMessageAndEscapeJS("common_propertysheet_validator_mandatory", "msg.identifier") %>', true);
      }
      else
      {
         return true;
      }
   }
</script>
</f:verbatim>

   <a:panel id="new-group" label="#{msg.group_props}">

      <a:panel id="new-group-message" styleClass="message">
         <h:graphicImage url="/images/icons/info_icon.gif" width="16" height="16" alt="" />
         <h:outputText value="#{msg.create_group_warning}" />
      </a:panel>

      <h:panelGrid cellpadding="0" cellspacing="0" columns="2" columnClasses="propertiesLabel," >

         <h:panelGroup>
            <f:verbatim escape="false"><span class="red">* </span></f:verbatim>
            <h:outputText value="#{msg.identifier}" />
         </h:panelGroup>
         <h:inputText id="name" value="#{DialogManager.bean.name}" size="35" maxlength="1024" validator="#{DialogManager.bean.validateGroupName}" styleClass="focus" />

      </h:panelGrid>
   </a:panel>