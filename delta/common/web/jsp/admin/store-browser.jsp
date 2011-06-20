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
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<r:page titleId="title_admin_store_browser">

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <r:loadBundle var="msg"/>
   
   <%@ include file="admin-title.jsp" %>
   
   <h:form id="searchForm" styleClass="nodeBrowserForm">
      <h:commandLink action="#{AdminNodeBrowseBean.selectStores}">
          <h:outputText styleClass="mainSubText" value="Refresh view"/>
      </h:commandLink>
      
      <br/>
      <br/>
      <h:outputText styleClass="mainTitle" value="Stores"/>
      <br/>
      
      <h:dataTable id="stores" border="1" value="#{AdminNodeBrowseBean.stores}" var="store" styleClass="nodeBrowserTable">
          <h:column>
              <f:facet name="header">
                  <h:outputText value="Reference"/>
              </f:facet>
              <h:commandLink action="#{AdminNodeBrowseBean.selectStore}">
                  <h:outputText value="#{store}"/>
              </h:commandLink>
          </h:column>
      </h:dataTable>

      <hr/>
      <b><h:outputText styleClass="mainTitle" value="Skriptid"/></b><br/>

         <br/>
         <u>Dokumendi õiguste uuendamise skript (enne 2.5 versiooni)</u>
         <br/>
         <br/>
         <h:commandButton id="startDocumentPrivilegesUpdater" value="Käivita dokumendi õiguste skript" type="submit"
            actionListener="#{documentPrivilegesUpdater.executeUpdaterInBackground}"
            rendered="#{documentPrivilegesUpdater.updaterRunning == false}" />
         <h:commandButton id="stopDocumentPrivilegesUpdater" value="Peata dokumendi õiguste skript" type="submit"
            actionListener="#{documentPrivilegesUpdater.stopUpdater}"
            rendered="#{documentPrivilegesUpdater.updaterRunning == true}"
            disabled="#{documentPrivilegesUpdater.updaterStopping == true}" />
         <br/>
         <h:outputText value="Paus pärast iga dokumendi töötlemist (ms): "/>
         <h:inputText id="documentPrivilegesUpdaterSleepTime" value="#{documentPrivilegesUpdater.sleepTime}" size="4" />
         <h:commandButton id="updateDocumentPrivilegesUpdaterSleepTime" value="Uuenda" type="submit"
            actionListener="#{documentPrivilegesUpdater.updateSleepTime}" />

         <br/>
         <br/>
         <br/>
         <u>Dokumendi õiguste optimeerimise skript (alates 2.5 versioonist)</u>
         <br/>
         <br/>
         <h:commandButton id="startDocumentInheritPermissionsUpdater" value="Käivita dokumendi õiguste optimeerimise skript" type="submit"
            actionListener="#{documentInheritPermissionsUpdater.executeUpdaterInBackground}"
            rendered="#{documentInheritPermissionsUpdater.updaterRunning == false}" />
         <h:commandButton id="stopDocumentInheritPermissionsUpdater" value="Peata dokumendi õiguste optimeerimise skript" type="submit"
            actionListener="#{documentInheritPermissionsUpdater.stopUpdater}"
            rendered="#{documentInheritPermissionsUpdater.updaterRunning == true}"
            disabled="#{documentInheritPermissionsUpdater.updaterStopping == true}" />
         <br/>
         <h:outputText value="Paus pärast iga dokumendi töötlemist (ms): "/>
         <h:inputText id="documentInheritPermissionsUpdaterSleepTime" value="#{documentInheritPermissionsUpdater.sleepTime}" size="4" />
         <h:commandButton id="updateDocumentInheritPermissionsUpdaterSleepTime" value="Uuenda" type="submit"
            actionListener="#{documentInheritPermissionsUpdater.updateSleepTime}" />

         <br/>
         <br/>
         <br/>
         <u>Lepingute väljasaatmise kirjete tekitamise skript (ContractSendInfoUpdater)</u>
         <br/>
         <br/>
         <h:commandButton id="startContractSendInfoUpdater" value="Käivita dokumendi õiguste optimeerimise skript" type="submit"
            actionListener="#{contractSendInfoUpdater.executeUpdaterInBackground}"
            rendered="#{contractSendInfoUpdater.updaterRunning == false}" />
         <h:commandButton id="stopContractSendInfoUpdater" value="Peata dokumendi õiguste optimeerimise skript" type="submit"
            actionListener="#{contractSendInfoUpdater.stopUpdater}"
            rendered="#{contractSendInfoUpdater.updaterRunning == true}"
            disabled="#{contractSendInfoUpdater.updaterStopping == true}" />
         <br/>
         <h:outputText value="Paus pärast iga dokumendi töötlemist (ms): "/>
         <h:inputText id="contractSendInfoUpdaterSleepTime" value="#{contractSendInfoUpdater.sleepTime}" size="4" />
         <h:commandButton id="updateContractSendInfoUpdaterSleepTime" value="Uuenda" type="submit"
            actionListener="#{contractSendInfoUpdater.updateSleepTime}" />

      <hr/>
      <h:outputText styleClass="mainTitle" value="Arendajale testimiseks"/><br/>

         <a:actionLink value="TestingForDeveloper" actionListener="#{TestingForDeveloperBean.handleTestEvent}" >
              <f:param name="testP" value="11" />
         </a:actionLink>

      <hr/>
   </h:form>
</f:view>

</r:page>
