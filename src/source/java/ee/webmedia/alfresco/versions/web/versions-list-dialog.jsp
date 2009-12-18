<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="versions-panel" border="white" bgcolor="white" styleClass="panel-100" label="#{msg.versions_list}" progressive="true"
   facetsId="versions-panel-facets">

   <%-- Main List --%>
   <a:richList id="versionsList" viewMode="details" pageSize="#{BrowseBean.pageSizeSpaces}" styleClass="recordSet" headerStyleClass="recordSetHeader"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.versions}" var="r" initialSortColumn="version"
      initialSortDescending="true">

      <%-- Version --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.version_version}" value="version" styleClass="header" />
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

      <%-- View --%>
      <a:column id="col4" actions="true" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="col4-sort" value="#{msg.version_view}" />
         </f:facet>
         <a:actionLink id="col4-act1" value="#{r.version}" href="#{r.downloadUrl}" target="new" showLink="false" image="#{r.fileType16}"
            styleClass="inlineAction" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>