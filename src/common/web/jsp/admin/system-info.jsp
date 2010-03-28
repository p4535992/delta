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

<%--
<a:panel label="#{msg.http_app_state}" id="http-application-state" progressive="true" expanded="false">
   <a:httpApplicationState id="has" />
</a:panel>

<a:panel label="#{msg.http_session_state}" id="http-session-state" progressive="true" expanded="false">
<a:httpSessionState id="hss" />
</a:panel>
 --%>

<a:panel label="#{msg.http_request_state}" id="http-request-state" progressive="true" expanded="false">
<a:httpRequestState id="hrs" />
</a:panel>

<a:panel label="#{msg.http_request_params}" id="http-request-params" progressive="true" expanded="false">
<a:httpRequestParams id="hrp" />
</a:panel>

<a:panel label="#{msg.http_request_headers}" id="http-request-headers" progressive="true" expanded="false">
<a:httpRequestHeaders id="hrh" />
</a:panel>

<a:panel label="#{msg.repository_props}" id="repo-props" progressive="true" expanded="false">
<a:repositoryProperties id="rp" />
</a:panel>

<a:panel label="#{msg.system_props}" id="system-props" progressive="true" 
expanded="false">
<a:systemProperties id="sp" />
</a:panel>