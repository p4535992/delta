<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
         
   <%-- Users in Group list --%>
   <a:panel id="users-panel" label="#{msg.users}" styleClass="with-pager" rendered="#{not empty DialogManager.bean.users}">
   
      <a:richList id="users-list" binding="#{DialogManager.bean.usersRichList}" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
         altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.users}" var="r" initialSortColumn="name">
         
         <%-- Primary column for details view mode --%>
         <a:column primary="true" style="padding:2px;text-align:left;">
            <f:facet name="small-icon">
               <h:graphicImage alt="#{r.name}" value="/images/icons/person.gif" />
            </f:facet>
            <f:facet name="header">
               <a:sortLink label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header"/>
            </f:facet>
            <h:outputText value="#{r.name}" />
         </a:column>
         
         <%-- Username column --%>
         <a:column width="120" style="text-align:left">
            <f:facet name="header">
               <a:sortLink label="#{msg.username}" value="userName" styleClass="header"/>
            </f:facet>
            <h:outputText value="#{r.userName}" />
         </a:column>
         
         <%-- Actions column --%>
         <a:column actions="true" style="text-align:left">
            <f:facet name="header">
               <h:outputText value="#{msg.actions}" rendered="#{UserService.groupsEditingAllowed && !GroupUsersListDialog.disableActions}" />
            </f:facet>
            <a:actionLink value="#{msg.remove}" image="/images/icons/remove_user.gif" showLink="false" styleClass="inlineAction" actionListener="#{DialogManager.bean.removeUser}"  rendered="#{UserService.groupsEditingAllowed  && !GroupUsersListDialog.disableActions && r.structUnitBased == 'false'}">
               <f:param name="id" value="#{r.id}" />
            </a:actionLink>
         </a:column>
         
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
         <a:dataPager id="pager2" styleClass="pager" />
      </a:richList>
   </a:panel>
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
         
   <%-- Users in Group list --%>
   <a:panel id="users-panel" label="#{msg.users}" styleClass="with-pager" rendered="#{not empty DialogManager.bean.users}">
   
      <a:richList id="users-list" binding="#{DialogManager.bean.usersRichList}" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
         altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.users}" var="r" initialSortColumn="name">
         
         <%-- Primary column for details view mode --%>
         <a:column primary="true" style="padding:2px;text-align:left;">
            <f:facet name="small-icon">
               <h:graphicImage alt="#{r.name}" value="/images/icons/person.gif" />
            </f:facet>
            <f:facet name="header">
               <a:sortLink label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header"/>
            </f:facet>
            <h:outputText value="#{r.name}" />
         </a:column>
         
         <%-- Username column --%>
         <a:column width="120" style="text-align:left">
            <f:facet name="header">
               <a:sortLink label="#{msg.username}" value="userName" styleClass="header"/>
            </f:facet>
            <h:outputText value="#{r.userName}" />
         </a:column>
         
         <%-- Actions column --%>
         <a:column actions="true" style="text-align:left">
            <f:facet name="header">
               <h:outputText value="#{msg.actions}" rendered="#{UserService.groupsEditingAllowed && !GroupUsersListDialog.disableActions}" />
            </f:facet>
            <a:actionLink value="#{msg.remove}" image="/images/icons/remove_user.gif" showLink="false" styleClass="inlineAction" actionListener="#{DialogManager.bean.removeUser}"  rendered="#{UserService.groupsEditingAllowed  && !GroupUsersListDialog.disableActions && r.structUnitBased == 'false'}">
               <f:param name="id" value="#{r.id}" />
            </a:actionLink>
         </a:column>
         
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
         <a:dataPager id="pager2" styleClass="pager" />
      </a:richList>
   </a:panel>
>>>>>>> develop-5.1
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />