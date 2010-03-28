<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="assocs-panel-facets">
   <f:facet name="title">
      <a:actionLink image="/images/icons/import.gif" id="col2-text" showLink="false" tooltip="#{msg.document_assocAdd}" value="" 
         actionListener="#{DocumentDialog.searchDocsAndCases}" rendered="#{MetadataBlockBean.mode eq 'view'}" >
      </a:actionLink>
   </f:facet>
</h:panelGroup>

<a:panel id="assocs-block-panel" label="#{msg.document_assocsBlockBean_panelTitle}" styleClass="panel-100 with-pager" progressive="true"
   expanded="#{AssocsBlockBean.expanded}" expandedActionListener="#{AssocsBlockBean.expandedAction}" facetsId="dialog:dialog-body:assocs-panel-facets">

   <a:richList id="assocsList" viewMode="details" value="#{AssocsBlockBean.docAssocInfos}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true" pageSize="#{BrowseBean.pageSizeContent}" initialSortColumn="type">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.document_assocsBlockBean_type}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-txt" value="#{r.type}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.document_assocsBlockBean_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text" value="#{r.title}" action="dialog:document" tooltip="#{msg.document_details_info}" showLink="false"
            actionListener="#{DocumentDialog.open}" rendered="#{not r.case}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col2-link2docList" value="#{r.title}" action="dialog:documentListDialog" tooltip="#{msg.document_assocsBlockBean_documentListInfo}"
            showLink="false" actionListener="#{DocumentListDialog.setup}" rendered="#{r.case}">
            <f:param name="caseNodeRef" value="#{r.caseNodeRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.document_assocsBlockBean_assoc}" value="assocType" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.assocType.valueName}" />
      </a:column>
      
      <a:column id="col4" actions="true">
         <f:facet name="header">
            <h:outputText id="col4-header" value="#{msg.document_assocsBlockBean_actions}" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-act" rendered="#{r.assocType.valueName == 'tavaline'}" value="#{r.title}" actionListener="#{DeleteAssocDialog.setupAssoc}" action="dialog:deleteAssoc" showLink="false"
               image="/images/icons/delete.gif" tooltip="#{msg.document_assocsBlockBean_delete}">
               <f:param name="nodeRef" value="#{r.nodeRef}"/>
               <f:param name="caseNodeRef" value="#{r.caseNodeRef}"/>
               <f:param name="documentRef" value="#{AssocsBlockBean.document.nodeRef}"/>
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

</a:panel>
