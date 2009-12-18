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

<%@ page import="org.alfresco.web.app.servlet.BaseServlet" %>
<%@ page import="org.alfresco.web.app.servlet.AuthenticationHelper" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>
<%@ page import="org.alfresco.web.ui.common.Utils" %>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="org.alfresco.web.bean.LoginBean" %>
<%@ page import="javax.faces.context.FacesContext" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="java.util.Locale" %>

<%@ page buffer="16kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<%
   Cookie authCookie = AuthenticationHelper.getAuthCookie(request);
   
   // remove the username cookie value if explicit logout was requested by the user
   if (session.getAttribute(AuthenticationHelper.SESSION_INVALIDATED) != null)
   {
      if (authCookie != null)
      {
         authCookie.setMaxAge(0);
         response.addCookie(authCookie);
      }
   }
   else
   {      
      // setup value used by JSF bean state ready for login page if we find the cookie
      if (authCookie != null && authCookie.getValue() != null)
      {
         session.setAttribute(AuthenticationHelper.SESSION_USERNAME, authCookie.getValue());
      }
   }
   
   // setup system locale from the Accept-Language header value
   Locale locale = BaseServlet.setLanguageFromRequestHeader(request, application);
%>

<r:page titleId="title_login">

<f:view>
<div id="login-wrapper" style="background-image: url(<%=request.getContextPath()%>/images/logo/logo-faded.png); background-repeat: no-repeat; background-attachment: fixed; height: 100%;">
<%
   FacesContext fc = FacesContext.getCurrentInstance();

   // set locale for JSF framework usage
   fc.getViewRoot().setLocale(locale);
   
   // set permissions error if applicable
   if (session.getAttribute(LoginBean.LOGIN_NOPERMISSIONS) != null)
   {
   	Utils.addErrorMessage(Application.getMessage(fc, LoginBean.MSG_ERROR_LOGIN_NOPERMISSIONS));
   	session.setAttribute(LoginBean.LOGIN_NOPERMISSIONS, null);
   }
%>
   
   <%-- load a bundle of properties I18N strings here --%>
   <r:loadBundle var="msg"/>

      <h:form acceptcharset="UTF-8" id="loginForm">
         <div id="login-container">
            <h:graphicImage url="/images/logo/logo.png" alt="#{ApplicationService.projectTitle}" title="#{ApplicationService.projectTitle}" />
            <h:outputText
            value="#{msg.login_details}" styleClass="login-instructions" /> <h:panelGrid
            cellpadding="0" cellspacing="0" columns="2"
            columnClasses="loginLabel,loginField">
            <h:outputText value="#{msg.username}: " />
            <h:inputText id="user-name" value="#{LoginBean.username}"
               validator="#{LoginBean.validateUsername}" />

            <h:outputText value="#{msg.password}: " />
            <h:inputSecret id="user-password" value="#{LoginBean.password}"
               validator="#{LoginBean.validatePassword}" />

            <h:outputText value="#{msg.language}:"
               rendered="#{LoginBean.multipleLanguageSelect}" />

            <h:selectOneMenu id="language"
               value="#{UserPreferencesBean.language}"
               onchange="document.forms['loginForm'].submit(); return true;"
               rendered="#{LoginBean.multipleLanguageSelect}">
               <f:selectItems value="#{UserPreferencesBean.languages}" />
            </h:selectOneMenu>
         </h:panelGrid>
         <div id="login-footer"><h:commandButton id="submit"
            styleClass="login-submit" action="#{LoginBean.login}"
            value="#{msg.login}" />
            <%--<a:actionLink value="ID-kaardiga"  />--%> 
            <%-- messages tag to show messages not handled by other specific message tags --%>
         <h:messages styleClass="login-error" /></div>

         <div id="no-cookies" style="display: none"><h:graphicImage
            url="/images/icons/info_icon.gif" width="16" height="16" /> <h:outputText
            value="#{msg.no_cookies}" /></div>
         </div>
         </div>
         <script>
               document.cookie="_alfTest=_alfTest"
               var cookieEnabled = (document.cookie.indexOf("_alfTest") != -1);
               if (cookieEnabled == false)
               {
                  document.getElementById("no-cookies").style.display = 'block';
               }
            </script>

      </h:form>
      </div>
   </f:view>

<script>

   if (document.getElementById("loginForm:user-name").value.length == 0)
   {
      document.getElementById("loginForm:user-name").focus();
   }
   else
   {
      document.getElementById("loginForm:user-password").focus();
   }

</script>

</r:page>

</body>