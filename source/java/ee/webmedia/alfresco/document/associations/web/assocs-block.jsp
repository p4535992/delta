<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.document.associations.web.AssocsBlockBean" %>

<h:panelGroup id="assocs-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator id="assocs-permission-evaluator" value="#{DocumentDialogHelperBean.node}" allow="viewDocumentMetaData">
         <a:actionLink image="/images/icons/import.gif" id="col3-text" showLink="false" tooltip="#{msg.document_assocAdd}" value="" 
            actionListener="#{DialogManager.bean.searchDocsAndCases}" action="#docsearch-panel" rendered="#{DialogManager.bean.showAddAssocsLink}" >
         </a:actionLink>
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>

<a:panel id="assocs-block-panel" label="#{msg.document_assocsBlockBean_panelTitle} (#{AssocsBlockBean.assocsCount})" styleClass="panel-100 with-pager" progressive="true"
   expanded="#{DialogManager.bean.assocsBlockExpanded}" facetsId="dialog:dialog-body:assocs-panel-facets">

   <a:richList id="assocsList" viewMode="details" value="#{AssocsBlockBean.docAssocInfos}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true" pageSize="#{BrowseBean.pageSizeContent}" 
      initialSortColumn="regDateTime" initialSortDescending="true">

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
            <a:convertXMLDate type="both" pattern="dd.MM.yyyy HH:mm" />
         </h:outputText>
      </a:column>
      
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-header" label="#{msg.document_assocsBlockBean_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.title}" tooltip="#{r.title}" showLink="false"
            actionListener="#{DocumentDynamicDialog.openFromDocumentList}" rendered="#{r.document}"
            styleClass="#{AssocsBlockBean.titleStyle}">
            <f:param id="col4-act-param" name="nodeRef" value="#{r.otherNodeRef}" />
         </a:actionLink>
         <a:actionLink id="col4-link2docList" value="#{r.title}" action="dialog:documentListDialog" tooltip="#{r.title}"
            showLink="false" actionListener="#{DocumentListDialog.setup}" rendered="#{r.case}"
            styleClass="#{AssocsBlockBean.titleStyle}">
            <f:param id="col4-doclist-act-param" name="caseNodeRef" value="#{r.otherNodeRef}" />
         </a:actionLink>
         <a:actionLink id="col2-act" value="#{r.title}" action="dialog:compoundWorkflowDialog" tooltip="#{r.title}" 
            actionListener="#{CompoundWorkflowDialog.setupWorkflow}" rendered="#{r.workflow}"
            styleClass="#{AssocsBlockBean.titleStyle}">
            <f:param id="col2-act-param" name="nodeRef" value="#{r.otherNodeRef}" />
         </a:actionLink>
         <a:actionLink id="col4-link2volume" value="#{r.title}" action="dialog:caseDocListDialog" tooltip="#{r.title}"
            showLink="false" actionListener="#{CaseDocumentListDialog.showAll}" rendered="#{r.volume && !r.caseFileVolume}"
            styleClass="#{AssocsBlockBean.titleStyle}">
            <f:param id="col4-volumeNodeRef" name="volumeNodeRef" value="#{r.otherNodeRef}" />
         </a:actionLink>
         <a:actionLink id="col4-link2caseFile" value="#{r.title}" tooltip="#{r.title}"
            showLink="false" actionListener="#{CaseFileDialog.openFromDocumentList}" rendered="#{r.caseFileVolume}"
            styleClass="#{AssocsBlockBean.titleStyle}">
            <f:param id="col4-nodeRef" name="nodeRef" value="#{r.otherNodeRef}" />
         </a:actionLink>         
      </a:column>

      <a:column id="col3" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col3-header" label="#{msg.document_assocsBlockBean_type}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.type}" title="#{r.type}" styleClass="tooltip condence20- no-underline"/>
      </a:column>

       <a:column id="col15" rendered="#{AssocsBlockBean.showAssociationSenderReceiver}">
           <f:facet name="header">
               <a:sortLink id="col15-header" label="#{msg.document_assocsBlockBean_recipientsSenders}" value="senderOrRecipient" styleClass="header" />
           </f:facet>
           <h:outputText id="col15-txt" value="#{r.senderOrRecipient}" title="#{r.senderOrRecipient}" styleClass="tooltip condence20- no-underline"/>
       </a:column>

       <a:column id="col7" >
           <f:facet name="header">
               <a:sortLink id="col7-header" label="#{msg.document_assocsBlockBean_workflowOwnerName}" value="workflowOwnerName" styleClass="header" />
           </f:facet>
           <h:outputText id="col7-txt" value="#{r.workflowOwnerName}" />
       </a:column>

      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-header" label="#{msg.document_assocsBlockBean_assoc}" value="assocType" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-txt" value="#{r.assocType.valueName}" />
      </a:column>

       <a:column id="col16" rendered="#{AssocsBlockBean.showAssociationFiles}">
           <f:facet name="header">
               <h:outputText id="col16-header" value="#{msg.document_assocsBlockBean_files}" styleClass="header" />
           </f:facet>
           <wm:customChildrenContainer id="assocs-workflow-document-files" childGenerator="#{DocumentListDialog.documentRowFileGenerator}" parameterList="#{r.assocDocument}"/>
       </a:column>
      
      <a:column id="col6" actions="true">
         <f:facet name="header">
            <h:outputText id="col6-header" value="#{msg.document_assocsBlockBean_actions}" styleClass="header" />
         </f:facet>
         <r:permissionEvaluator id="assocs-list-permission-evaluator" value="#{DocumentDynamicDialog.node}" allow="editDocument">
            <a:actionLink id="col6-act" rendered="#{r.allowDelete}" value="#{r.title}" actionListener="#{DeleteAssocDialog.setupAssoc}" action="dialog:deleteAssoc" showLink="false"
                  image="/images/icons/delete.gif" tooltip="#{msg.document_assocsBlockBean_delete}">
                  <f:param name="sourceNodeRef" value="#{r.sourceNodeRef}"/>
                  <f:param name="targetNodeRef" value="#{r.targetNodeRef}"/>
                  <f:param name="type" value="#{r.assocTypeQName}"/>
            </a:actionLink>
         </r:permissionEvaluator>

         <r:permissionEvaluator id="assocs-list-compare-permission-evaluator" value="#{r.effectiveNodeRef}" allow="viewDocumentMetaData">
            <a:actionLink id="compare-documents-link" value="#{msg.document_assocsBlockBean_compare}" rendered="#{r.documentToDocumentAssoc && DocumentDynamicDialog.document.documentTypeId == r.typeId}"
            image="/images/icons/search_results.gif" target="_blank" showLink="false"
            href="/printTable?tableMode=DOCUMENT_FIELD_COMPARE&doc1=#{DialogManager.bean.node.nodeRef}&doc2=#{r.effectiveNodeRef}" />
         </r:permissionEvaluator>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

</a:panel>
