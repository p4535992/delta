<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="assocs-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator id="assocs-permission-evaluator" value="#{DocumentDialog.node}" allow="editDocumentMetaData">
         <a:actionLink image="/images/icons/import.gif" id="col3-text" showLink="false" tooltip="#{msg.document_assocAdd}" value="" 
            actionListener="#{DocumentDialog.searchDocsAndCases}" action="#docsearch-panel" rendered="#{MetadataBlockBean.mode eq 'view'}" >
         </a:actionLink>
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>

<a:panel id="assocs-block-panel" label="#{msg.document_assocsBlockBean_panelTitle}" styleClass="panel-100 with-pager" progressive="true"
   expanded="false" facetsId="dialog:dialog-body:assocs-panel-facets">

   <a:richList id="assocsList" viewMode="details" value="#{AssocsBlockBean.docAssocInfos}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true" pageSize="#{BrowseBean.pageSizeContent}" initialSortColumn="type">

      <a:column id="col1" >
         <f:facet name="header">
            <a:sortLink id="col1-header" label="#{msg.document_assocsBlockBean_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-txt" value="#{r.regNumber}" />
      </a:column>   
      
     <a:column id="col2" >
         <f:facet name="header">
            <a:sortLink id="col2-header" label="#{msg.document_assocsBlockBean_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.regDateTime}" >
            <a:convertXMLDate type="both" pattern="dd.MM.yyyy" />
         </h:outputText>
      </a:column>

      <a:column id="col3" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.document_assocsBlockBean_type}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.type}" />
      </a:column>


      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-header" label="#{msg.document_assocsBlockBean_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.title}" action="#{DocumentDialog.action}" tooltip="#{msg.document_details_info}" showLink="false"
            actionListener="#{DocumentDialog.open}" rendered="#{not r.case}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col4-link2docList" value="#{r.title}" action="dialog:documentListDialog" tooltip="#{msg.document_assocsBlockBean_documentListInfo}"
            showLink="false" actionListener="#{DocumentListDialog.setup}" rendered="#{r.case && !r.maaisCase}">
            <f:param name="caseNodeRef" value="#{r.caseNodeRef}" />
         </a:actionLink>
         <h:outputText id="col4-maaisText" value="#{r.title}" rendered="#{r.case && r.maaisCase}" />
      </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-header" label="#{msg.document_assocsBlockBean_assoc}" value="assocType" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-txt" value="#{r.assocType.valueName}" />
      </a:column>
      
      <a:column id="col6" actions="true">
         <f:facet name="header">
            <h:outputText id="col6-header" value="#{msg.document_assocsBlockBean_actions}" styleClass="header" />
         </f:facet>
         <r:permissionEvaluator id="assocs-list-permission-evaluator" value="#{DocumentDialog.node}" allow="editDocumentMetaData">
            <a:actionLink id="col6-act" rendered="#{r.assocType.valueName == 'tavaline'}" value="#{r.title}" actionListener="#{DeleteAssocDialog.setupAssoc}" action="dialog:deleteAssoc" showLink="false"
                  image="/images/icons/delete.gif" tooltip="#{msg.document_assocsBlockBean_delete}">
                  <f:param name="nodeRef" value="#{r.nodeRef}"/>
                  <f:param name="caseNodeRef" value="#{r.caseNodeRef}"/>
                  <f:param name="documentRef" value="#{AssocsBlockBean.document.nodeRef}"/>
                  <f:param name="source" value="#{r.source}" />
            </a:actionLink>
         </r:permissionEvaluator>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

</a:panel>
