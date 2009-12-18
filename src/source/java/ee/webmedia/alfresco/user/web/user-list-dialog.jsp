<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript">

   window.onload = pageLoaded;
   
   function pageLoaded()
   {
      document.getElementById("dialog:dialog-body:search-text").focus();
      updateButtonState();
   }
   
   function updateButtonState()
   {
      if (document.getElementById("dialog:dialog-body:search-text").value.length == 0)
      {
         document.getElementById("dialog:dialog-body:search-btn").disabled = true;
      }
      else
      {
         document.getElementById("dialog:dialog-body:search-btn").disabled = false;
      }
   }
</script>
</f:verbatim>

<a:panel id="users-panel" styleClass="panel-100" label="#{msg.users_list}" progressive="true">

   
   <a:panel id="search-panel">
      <h:inputText id="search-text" value="#{UsersBeanProperties.searchCriteria}" size="35" maxlength="1024" onkeyup="updateButtonState();" onchange="updateButtonState();" />&nbsp;
      <f:verbatim>&nbsp;</f:verbatim>
      <h:commandButton id="search-btn" value="#{msg.search}" action="#{DialogManager.bean.search}" disabled="true" />
      <f:verbatim>&nbsp;</f:verbatim>
      <h:commandButton value="#{msg.show_all}" action="#{DialogManager.bean.showAll}" />
   </a:panel>
   
   <%-- Spaces List --%>
   <a:richList id="usersList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" styleClass="recordSet" headerStyleClass="recordSetHeader"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.users}" var="u"
      initialSortColumn="lastName" binding="#{DialogManager.bean.usersList}">
      
      <%-- Primary column with given name --%>
      <a:column primary="true" style="padding:2px;text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.first_name}" value="firstName" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:graphicImage url="/images/icons/person.gif" />
         </f:facet>
         <h:outputText value="#{u.firstName}" />
      </a:column>
      
      <%-- Column with surname --%>
      <a:column style="padding:2px;text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.last_name}" value="lastName" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:outputText value="#{u.lastName}" />
      </a:column>

      <%-- Unit column --%>
      <a:column style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.unit}" value="organizationId" styleClass="header" />
         </f:facet>
         <h:outputText value="#{u.unit}" />
      </a:column>

      <%-- Actions column --%>
      <a:column actions="true" style="text-align:right">
         <f:facet name="header">
            <h:outputText value="#{msg.user_look}" />
         </f:facet>
         <a:actionLink value="#{msg.user_look}" tooltip="#{msg.user_look}" image="/images/icons/user_console.gif" showLink="false" action="dialog:userDetailsDialog" actionListener="#{UserDetailsDialog.setupUser}">
            <f:param name="id" value="#{u.userName}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>