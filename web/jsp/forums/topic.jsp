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

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<a:panel id="posts-panel" label="#{msg.browse_posts}" styleClass="with-pager">

<%-- Details - Posts --%>
<%-- Posts List --%>
<a:richList id="postsList" binding="#{ForumsBean.topicRichList}" viewMode="bubble" pageSize="#{ForumsBean.topicPageSize}"
styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
value="#{ForumsBean.posts}" var="r">

<%-- component to display if the list is empty --%>
<f:facet name="empty">
<h:outputFormat value="#{msg.no_posts}" escape="false" />
</f:facet>

<%-- Content column for all view modes --%>
<a:column primary="true">
<f:facet name="header">
<h:outputText value="#{msg.post}" />
</f:facet>
<h:outputText value="#{r.message}" escape="false" />
</a:column>

<%-- Author column for the details view mode --%>
<a:column style="text-align:left" rendered="#{ForumsBean.topicViewMode == 'details'}">
<f:facet name="header">
<a:sortLink label="#{msg.author}" value="creator" styleClass="header"/>
</f:facet>
<h:outputText value="#{r.creatorName}" />
</a:column>

<%-- Posted time column for details view mode --%>
<a:column style="text-align:left; white-space:nowrap"
rendered="#{ForumsBean.topicViewMode == 'details'}">
<f:facet name="header">
<a:sortLink label="#{msg.posted}" value="created" styleClass="header"/>
</f:facet>
<h:outputText value="#{r.created}">
<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
</h:outputText>
</a:column>

<%-- Posted time column for bubble view mode --%>
<a:column style="text-align:left; white-space:nowrap"
rendered="#{ForumsBean.topicViewMode == 'bubble'}">
<f:facet name="header">
<h:outputText value="#{msg.forum_posted_on}:" styleClass="header"/>
</f:facet>
<h:outputText value="&nbsp;" escape="false;"/>
<h:outputText value="#{r.created}">
<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
</h:outputText>
</a:column>

<%-- topic name column for bubble view mode --%>
<a:column style="text-align:left;"
rendered="#{ForumsBean.topicViewMode == 'bubble'}">
</a:column>

<%-- reply to column for bubble view mode --%>
<a:column style="text-align:left;"
rendered="#{ForumsBean.topicViewMode == 'bubble' && r.replyTo != null}">
<f:facet name="header">
<h:outputText value="#{msg.forum_replied_to_user}: " styleClass="header"/>
</f:facet>
<h:outputText value="&nbsp;" escape="false"/>
<h:outputText value="#{r.replyTo}" />
</a:column>


<%-- Actions column --%>
<a:column id="col1" actions="true" style="text-align:left">
<f:facet name="header">
<h:outputText value="#{msg.actions}"/>
</f:facet>

<%-- actions are configured in web-client-config-forum-actions.xml --%>
<r:actions id="actions" value="topic_actions_override" context="#{r}" showLink="false" styleClass="inlineAction" />
</a:column>

<a:dataPager styleClass="pager" />
</a:richList>

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

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<a:panel id="posts-panel" label="#{msg.browse_posts}" styleClass="with-pager">

<%-- Details - Posts --%>
<%-- Posts List --%>
<a:richList id="postsList" binding="#{ForumsBean.topicRichList}" viewMode="bubble" pageSize="#{ForumsBean.topicPageSize}"
styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
value="#{ForumsBean.posts}" var="r">

<%-- component to display if the list is empty --%>
<f:facet name="empty">
<h:outputFormat value="#{msg.no_posts}" escape="false" />
</f:facet>

<%-- Content column for all view modes --%>
<a:column primary="true">
<f:facet name="header">
<h:outputText value="#{msg.post}" />
</f:facet>
<h:outputText value="#{r.message}" escape="false" />
</a:column>

<%-- Author column for the details view mode --%>
<a:column style="text-align:left" rendered="#{ForumsBean.topicViewMode == 'details'}">
<f:facet name="header">
<a:sortLink label="#{msg.author}" value="creator" styleClass="header"/>
</f:facet>
<h:outputText value="#{r.creatorName}" />
</a:column>

<%-- Posted time column for details view mode --%>
<a:column style="text-align:left; white-space:nowrap"
rendered="#{ForumsBean.topicViewMode == 'details'}">
<f:facet name="header">
<a:sortLink label="#{msg.posted}" value="created" styleClass="header"/>
</f:facet>
<h:outputText value="#{r.created}">
<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
</h:outputText>
</a:column>

<%-- Posted time column for bubble view mode --%>
<a:column style="text-align:left; white-space:nowrap"
rendered="#{ForumsBean.topicViewMode == 'bubble'}">
<f:facet name="header">
<h:outputText value="#{msg.forum_posted_on}:" styleClass="header"/>
</f:facet>
<h:outputText value="&nbsp;" escape="false;"/>
<h:outputText value="#{r.created}">
<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
</h:outputText>
</a:column>

<%-- topic name column for bubble view mode --%>
<a:column style="text-align:left;"
rendered="#{ForumsBean.topicViewMode == 'bubble'}">
</a:column>

<%-- reply to column for bubble view mode --%>
<a:column style="text-align:left;"
rendered="#{ForumsBean.topicViewMode == 'bubble' && r.replyTo != null}">
<f:facet name="header">
<h:outputText value="#{msg.forum_replied_to_user}: " styleClass="header"/>
</f:facet>
<h:outputText value="&nbsp;" escape="false"/>
<h:outputText value="#{r.replyTo}" />
</a:column>


<%-- Actions column --%>
<a:column id="col1" actions="true" style="text-align:left">
<f:facet name="header">
<h:outputText value="#{msg.actions}"/>
</f:facet>

<%-- actions are configured in web-client-config-forum-actions.xml --%>
<r:actions id="actions" value="topic_actions_override" context="#{r}" showLink="false" styleClass="inlineAction" />
</a:column>

<a:dataPager styleClass="pager" />
</a:richList>

</a:panel>

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />