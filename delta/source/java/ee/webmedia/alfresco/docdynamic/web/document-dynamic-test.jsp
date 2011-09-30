<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="draft-new-panel" label="Loo uus dokument mustandite alla" styleClass="panel-100" progressive="true">
   <h:commandButton id="testType1" value="Uus dokument tüüpi 'type1'" type="submit" actionListener="#{DocumentDynamicDialog.createDraft}" >
      <f:param id="testType1param" name="documentTypeId" value="type1"/>
   </h:commandButton>
         <h:outputText value=" " />
   <h:commandButton id="testType2" value="Uus dokument tüüpi 'type2'" type="submit" actionListener="#{DocumentDynamicDialog.createDraft}" >
      <f:param id="testType2param" name="documentTypeId" value="type2"/>
   </h:commandButton>
         <h:outputText value=" " />
   <h:commandButton id="testType3" value="Uus dokument tüüpi 'type3'" type="submit" actionListener="#{DocumentDynamicDialog.createDraft}" >
      <f:param id="testType3param" name="documentTypeId" value="type3"/>
   </h:commandButton>
</a:panel>

<a:panel id="draft-list-panel" label="Dokumendid mustandite all" styleClass="panel-100 with-pager" progressive="true">
   <a:richList id="documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DocumentDynamicTestDialog.drafts}" var="r" binding="#{DocumentDynamicTestDialog.draftsList}">

      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="Tegevused" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-text-1" value="Vaata" actionListener="#{DocumentDynamicDialog.openView}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <h:outputText value=" " />
         <a:actionLink id="col1-text-2" value="Muuda" actionListener="#{DocumentDynamicDialog.openEdit}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <h:outputText value=" " />
         <a:actionLink id="col1-text-3" value="Kustuta" actionListener="#{DocumentDynamicTestDialog.deleteDocument}" >
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <h:outputText value=" " />
         <a:actionLink id="col1-text-4" href="#{r.url}" value="URL" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="documentTypeId" value="documentTypeId" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.documentTypeId}" />
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="versionNr" value="documentTypeVersionNr" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.documentTypeVersionNr}" />
      </a:column>

      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.creator}" value="creator" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.node.properties['cm:creator']}" />
      </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.created}" value="created" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.node.properties['cm:created']}" >
            <a:convertXMLDate pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.modifier}" value="modifier" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-text" value="#{r.node.properties['cm:modifier']}" />
      </a:column>

      <a:column id="col7">
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.modified}" value="modified" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.node.properties['cm:modified']}" >
            <a:convertXMLDate pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>
