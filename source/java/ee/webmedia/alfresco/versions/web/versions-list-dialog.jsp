<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>

<a:panel id="versions-panel" styleClass="panel-100 with-pager" label="#{msg.versions_list}" progressive="true">

   <%-- Main List --%>
   <a:richList id="versionsList" viewMode="details" pageSize="#{BrowseBean.pageSizeSpaces}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.versions}" var="r" initialSortColumn="versionNr" initialSortDescending="true">

      <%-- Version --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.version_version}" value="versionNr" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.version}" />
      </a:column>

      <%-- Author --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.version_author}" value="autor" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.author}" />
      </a:column>

      <%-- modified--%>
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.version_modified}" value="modified" styleClass="header" />
         </f:facet>
         <h:outputText id="itemDate" value="#{r.modified}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>
      
      <%-- comment--%>
      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.version_comment}" value="comment" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.comment}"/>
      </a:column>

      <%-- View --%>
      <a:column id="col4" actions="true" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="col4-sort" value="#{msg.version_view}" />
         </f:facet>
         <a:actionLink id="col4-act1" value="#{r.version}" href="#{r.downloadUrl}" target="_blank" showLink="false" image="#{r.fileType16}"
            styleClass="inlineAction" />
      </a:column>
      
      <%-- Actions --%>
      <a:column id="col6">
         <wm:docPermissionEvaluator id="col6-activateVersion-eval" value="#{r.node}" allow="editDocument">
            <a:actionLink id="col6-activateVersion" value="" actionListener="#{DialogManager.bean.activateVersion}" showLink="false"
               image="/images/icons/recover.gif" tooltip="#{msg.version_activate}" rendered="true" styleClass="version-activation-confirm">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </wm:docPermissionEvaluator>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />

<script>
	prependOnclick($jQ(".version-activation-confirm"), function(){
   		return confirm('<%= MessageUtil.getMessageAndEscapeJS("version_activate_confirm")%>');
	});
</script>