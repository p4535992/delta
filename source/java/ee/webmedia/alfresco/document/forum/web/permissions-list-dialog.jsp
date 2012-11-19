<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<f:verbatim>
   <script type="text/javascript">
   var confirmMsg = '<%= MessageUtil.getMessageAndEscapeJS("forum_permissions_delete_confirm") %>';
   $jQ(document).ready(function() {
      prependOnclick($jQ("a.delete"), function(e) {
         var userOrGroup = $jQ(e).closest('tr').children().eq(0).text();
         return confirmWithPlaceholders(confirmMsg, userOrGroup);
      });
   });
   </script>
</f:verbatim>

<a:panel id="authorities-panel" label="#{msg.users_groups}" styleClass="with-pager">
   <a:richList id="authorities-list" binding="#{PermissionsListDialog.authoritiesRichList}" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
         styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
         value="#{PermissionsListDialog.authorities}" var="r" initialSortColumn="name" width="100%">

      <%-- Label column (user fullname / group display name) --%>
      <a:column primary="true" style="padding:2px;text-align:left;">
         <f:facet name="small-icon">
            <h:graphicImage url="#{r.icon}" />
         </f:facet>
         <f:facet name="header">
            <a:sortLink label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header"/>
         </f:facet>
         <h:outputText value="#{r.name}" />
      </a:column>
      <a:column>
         <f:facet name="header">
            <h:outputText value="#{msg.actions}" />
         </f:facet>
         <a:actionLink actionListener="#{PermissionsListDialog.removeAuthorityAndSave}" image="/images/icons/delete.gif" value="#{msg.remove}" showLink="false" styleClass="delete">
            <f:param name="authority" value="#{r.authority}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>
