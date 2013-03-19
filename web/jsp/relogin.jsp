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

<%@ page import="org.alfresco.web.app.servlet.AuthenticationHelper" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="ee.webmedia.alfresco.user.service.SimpleAuthenticationFilter" %>
<%@ page import="ee.webmedia.alfresco.common.service.ApplicationService" %>
<%@ page import="org.alfresco.web.app.servlet.FacesHelper"%>
<%@ page import="javax.faces.context.FacesContext"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<%
    // remove the username cookie value if explicit logout was requested by the user
   if (session.getAttribute(AuthenticationHelper.SESSION_INVALIDATED) != null)
   {
      Cookie authCookie = AuthenticationHelper.getAuthCookie(request);
      if (authCookie != null)
      {
         authCookie.setMaxAge(0);
         response.addCookie(authCookie);
      }
   }

   if (session.getAttribute(SimpleAuthenticationFilter.AUTHENTICATION_EXCEPTION) == null) {
      ApplicationService applicationService = (ApplicationService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "ApplicationService");
      response.sendRedirect(applicationService.getLogoutRedirectUrl());
   }

%>


<r:page titleId="title_relogin">

<f:view>
<div id="login-wrapper" style="background-image: url(<%=request.getContextPath()%>/images/logo/logo-faded.png); background-repeat: no-repeat; background-attachment: fixed; height: 100%;">
   <%-- load a bundle of properties I18N strings here --%>
   <r:loadBundle var="msg"/>
   
   <h:form acceptcharset="UTF-8" id="loggedOutForm" >
   <div id="login-container">
            <h:graphicImage url="#{ApplicationService.logoUrl}" alt="#{ApplicationService.headerText}" title="#{ApplicationService.headerText}" />
             
            <%
               if (session.getAttribute(SimpleAuthenticationFilter.AUTHENTICATION_EXCEPTION) != null) {
            %>
	              <h:outputText value="#{msg.error_login_user}" styleClass="login-instructions center" />
                  <a:actionLink href="#{ApplicationService.logoutRedirectUrl}" value="#{msg.logout}" />
            <%
               } else {
            %>
	              <h:outputText value="#{msg.loggedout_details}" styleClass="login-instructions center" />
                  <a:actionLink href="/faces/jsp/dashboards/container.jsp" value="#{msg.relogin}" />
            <%
               }
            %>

            <%-- messages tag to show messages not handled by other specific message tags --%>
            <h:messages style="padding-top:8px; color:red; font-size:10px" />
    </div>
   </h:form>
</div>
</f:view>
</r:page>