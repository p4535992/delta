<<<<<<< HEAD
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
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>

<a:panel id="import" label="#{msg.locate_doc_upload}">

   <h:outputText styleClass="dialogpanel-title" value="#{msg.locate_acp_upload}" />

   <h:panelGrid id="upload_panel" columns="2" cellpadding="0" cellspacing="0" columnClasses="propertiesLabel,">

      <h:outputText id="out_schema" value="#{msg.file_location}:" style="padding-left:10px" />
      <h:column id="upload_empty" rendered="#{empty DialogManager.bean.fileName}">
         <r:upload id="uploader" value="#{DialogManager.bean.fileName}" framework="dialog" />
      </h:column>
      <h:column id="upload_not_empty" rendered="#{!empty DialogManager.bean.fileName}">
         <h:outputText id="upload_name" value="#{DialogManager.bean.fileName}" style="padding-right:8px" />
         <a:actionLink id="upload_remove" image="/images/icons/delete.gif" value="#{msg.remove}" action="#{DialogManager.bean.reset}" showLink="false" />
      </h:column>
   </h:panelGrid>

   <h:selectBooleanCheckbox value="#{DialogManager.bean.runInBackground}" />
   <h:outputText value="#{msg.run_import_in_background}" />
   
   <a:panel id="import-message" styleClass="message">
      <h:graphicImage alt="" value="/images/icons/info_icon.gif" />
      <h:outputText value="#{msg.import_error_info}" />
   </a:panel>

=======
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
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>

<a:panel id="import" label="#{msg.locate_doc_upload}">

   <h:outputText styleClass="dialogpanel-title" value="#{msg.locate_acp_upload}" />

   <h:panelGrid id="upload_panel" columns="2" cellpadding="0" cellspacing="0" columnClasses="propertiesLabel,">

      <h:outputText id="out_schema" value="#{msg.file_location}:" style="padding-left:10px" />
      <h:column id="upload_empty" rendered="#{empty DialogManager.bean.fileName}">
         <r:upload id="uploader" value="#{DialogManager.bean.fileName}" framework="dialog" />
      </h:column>
      <h:column id="upload_not_empty" rendered="#{!empty DialogManager.bean.fileName}">
         <h:outputText id="upload_name" value="#{DialogManager.bean.fileName}" style="padding-right:8px" />
         <a:actionLink id="upload_remove" image="/images/icons/delete.gif" value="#{msg.remove}" action="#{DialogManager.bean.reset}" showLink="false" />
      </h:column>
   </h:panelGrid>

   <h:selectBooleanCheckbox value="#{DialogManager.bean.runInBackground}" />
   <h:outputText value="#{msg.run_import_in_background}" />
   
   <a:panel id="import-message" styleClass="message">
      <h:graphicImage alt="" value="/images/icons/info_icon.gif" />
      <h:outputText value="#{msg.import_error_info}" />
   </a:panel>

>>>>>>> develop-5.1
</a:panel>