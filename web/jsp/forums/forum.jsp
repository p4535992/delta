<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<a:panel id="topics-panel" label="#{msg.browse_topics}" styleClass="with-pager">

<%-- Topics List --%>
<a:richList id="topicsList" binding="#{ForumsBean.forumRichList}" viewMode="#{ForumsBean.forumViewMode}" pageSize="#{ForumsBean.forumPageSize}"
styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
value="#{ForumsBean.topics}" var="r">

<%-- component to display if the list is empty --%>
<f:facet name="empty">
<h:outputFormat value="#{msg.no_topics}" escape="false" />
</f:facet>

<%-- Primary column for details view mode --%>
<a:column primary="true" style="padding:2px;text-align:left" rendered="#{ForumsBean.forumViewMode == 'details'}">
<f:facet name="header">
<a:sortLink label="#{msg.topic}" value="name" mode="case-insensitive" styleClass="header"/>
</f:facet>
<f:facet name="small-icon">
<a:actionLink value="#{r.name}" image="/images/icons/#{r.smallIcon}.gif" action="dialog:showTopic" actionListener="#{ForumsBean.setupTopicNavigatorCurrentNodeId}" showLink="false">
<f:param name="id" value="#{r.id}" />
</a:actionLink>
</f:facet>
<a:actionLink value="#{r.name}" action="dialog:showTopic" actionListener="#{ForumsBean.setupTopicNavigatorCurrentNodeId}">
<f:param name="id" value="#{r.id}" />
</a:actionLink>
</a:column>

<%-- Replies column for all view modes --%>
<a:column style="text-align:left">
<f:facet name="header">
<a:sortLink label="#{msg.replies}" value="replies" styleClass="header"/>
</f:facet>
<h:outputText value="#{r.replies}" />
</a:column>

<a:dataPager styleClass="pager" />
</a:richList>

</a:panel>

=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<a:panel id="topics-panel" label="#{msg.browse_topics}" styleClass="with-pager">

<%-- Topics List --%>
<a:richList id="topicsList" binding="#{ForumsBean.forumRichList}" viewMode="#{ForumsBean.forumViewMode}" pageSize="#{ForumsBean.forumPageSize}"
styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
value="#{ForumsBean.topics}" var="r">

<%-- component to display if the list is empty --%>
<f:facet name="empty">
<h:outputFormat value="#{msg.no_topics}" escape="false" />
</f:facet>

<%-- Primary column for details view mode --%>
<a:column primary="true" style="padding:2px;text-align:left" rendered="#{ForumsBean.forumViewMode == 'details'}">
<f:facet name="header">
<a:sortLink label="#{msg.topic}" value="name" mode="case-insensitive" styleClass="header"/>
</f:facet>
<f:facet name="small-icon">
<a:actionLink value="#{r.name}" image="/images/icons/#{r.smallIcon}.gif" action="dialog:showTopic" actionListener="#{ForumsBean.setupTopicNavigatorCurrentNodeId}" showLink="false">
<f:param name="id" value="#{r.id}" />
</a:actionLink>
</f:facet>
<a:actionLink value="#{r.name}" action="dialog:showTopic" actionListener="#{ForumsBean.setupTopicNavigatorCurrentNodeId}">
<f:param name="id" value="#{r.id}" />
</a:actionLink>
</a:column>

<%-- Replies column for all view modes --%>
<a:column style="text-align:left">
<f:facet name="header">
<a:sortLink label="#{msg.replies}" value="replies" styleClass="header"/>
</f:facet>
<h:outputText value="#{r.replies}" />
</a:column>

<a:dataPager styleClass="pager" />
</a:richList>

</a:panel>

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />