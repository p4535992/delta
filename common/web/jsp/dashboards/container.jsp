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

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>
<%@ page import="javax.faces.context.FacesContext" %>

<r:page titleId="title_my_alfresco">

<f:view>
   <%
       FacesContext fc = FacesContext.getCurrentInstance();

               // set locale for JSF framework usage
               fc.getViewRoot().setLocale(Application.getLanguage(fc));
   %>
   
   <%-- load a bundle of properties with I18N strings --%>
   <r:loadBundle var="msg"/>

   <h:form acceptcharset="UTF-8" id="dashboard">

         <%-- Title bar --%>
         <%@ include file="../parts/titlebar.jsp"%>

         <%-- Main area --%>
         <a:panel id="container">
         
         <%-- Shelf --%> 
         <%@ include file="../parts/shelf.jsp"%> 
         
         <%-- Work Area --%>
         
         <a:panel id="content">
         <%-- Breadcrumb --%>
         <%@ include file="../parts/breadcrumb.jsp"%>
            
         <%-- Status and Actions --%>
         <a:panel id="titlebar"><%-- Status and Actions inner contents table --%>
         <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
            
            <f:verbatim>
            <h2 class="title-icon">
            </f:verbatim>
               <h:graphicImage url="/images/icons/dashboard_large.gif" width="32" height="32" alt="" />
               <h:outputText value="#{msg.dashboard_info}" />
            <f:verbatim>
            </h2>
            </f:verbatim>
            
            <%--
            <a:panel id="description">
               <h:outputText value="#{msg.dashboard_description}" />
            </a:panel>
             --%>

            <a:panel id="additional">
               <a:actionLink value="#{msg.configure}" image="/images/icons/configure-dashboard.png" action="wizard:configureDashboard"
                  rendered="#{NavigationBean.isGuest == false || DashboardWizard.allowGuestConfig}" />
            </a:panel>

         </a:panel>

         <%-- Details --%> <f:subview id="dash-body">
            <jsp:include
               page="<%=Application.getDashboardManager().getLayoutPage()%>" />
         </f:subview> <a:errors message="#{msg.error_dashboard}" styleClass="message" errorClass="error-message" infoClass="info-message" />
         </a:panel>
         <f:verbatim><div class="clear"></div></f:verbatim>
         </a:panel>
         <%@ include file="../parts/footer.jsp"%>
      </h:form>

</f:view>

</r:page>