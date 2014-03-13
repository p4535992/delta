<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
	<script type="text/javascript">
	
	$jQ(document).ready(function(){
	   $jQ(".selectMainDoc").change(function() {
	      if ($jQ(this).attr('checked')){
	         $jQ(".selectMainDoc:not(:checked)").attr('disabled', true);
	      } else if ($jQ(".selectMainDoc:checked").size() == 0){
	         $jQ(".selectMainDoc").attr('disabled', false);
	      }
	   });
	   // disable all not selected on page load only if we have some selected checkboxes
	   if ($jQ(".selectMainDoc:checked").size() > 0){
	      $jQ(".selectMainDoc:not(:checked)").attr('disabled', true);
	   }
	});
	
	</script>
</f:verbatim> 

<h:panelGroup id="cwf-assocs-panel-facets" styleClass="nonfloating-element" >
   <f:facet name="title">
      <a:actionLink image="/images/icons/import.gif" id="act-link-0" showLink="false" tooltip="#{msg.compoundWorkflow_search_add_assoc}" value="" 
         actionListener="#{CompoundWorkflowAssocSearchBlock.searchDocs}" action="#docsearch-panel" rendered="#{CompoundWorkflowDialog.showAssocActions}" >
      </a:actionLink>
   </f:facet>
</h:panelGroup>

<a:panel id="cwf-assoc-panel" styleClass="panel-100 with-pager" label="#{CompoundWorkflowAssocListDialog.listTitle}" progressive="true" facetsId="dialog:dialog-body:cwf-assocs-panel-facets">

   <%-- Main List --%>
   <a:richList id="documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{CompoundWorkflowAssocListDialog.documents}" var="r" binding="#{CompoundWorkflowAssocListDialog.richList}" refreshOnBind="true" >
      
      <%-- Main document checkbox --%>
      <a:column id="col0" primary="true" >
         <f:facet name="header">
            <h:outputText id="col0-sort" value="#{msg.compoundWorkflow_object_list_mainDoc}" styleClass="header" />
         </f:facet>
         <h:selectBooleanCheckbox id="co0-checkbox" value="#{r.mainDocument}" styleClass="selectMainDoc" disabled="#{CompoundWorkflowAssocListDialog.disableDocSelect}" />
      </a:column>
      
      <%-- Document to sign checkbox --%>
      <a:column id="col01" primary="true" >
         <f:facet name="header">
            <h:outputText id="col01-sort" value="#{msg.compoundWorkflow_object_list_documentToSign}" styleClass="header" />
         </f:facet>
         <h:selectBooleanCheckbox id="co01-checkbox" value="#{r.documentToSign}" disabled="#{CompoundWorkflowAssocListDialog.disableSignSelect}" />
      </a:column>      
      
      <%-- regNumber --%>
      <a:column id="col1" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-link" value="#{r.regNumber}" action="#{DocumentDialog.action}" tooltip="#{r.regNumber}"
            actionListener="#{DocumentDialog.open}" rendered="#{r.showLink}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <h:outputText  id="col1-text" value="#{r.regNumber}" rendered="#{!r.showLink}" />
      </a:column>
      
      <%-- Registration date --%>
      <a:column id="col2" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-link" value="#{r.regDateTimeStr}" action="#{DocumentDialog.action}" tooltip="#{r.regDateTimeStr}"
          actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.showLink}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
          <h:outputText  id="col2-text" value="#{r.regDateTimeStr}" rendered="#{!r.showLink}" />
      </a:column>
      
      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_type}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-link" value="#{r.documentTypeName}" action="#{DocumentDialog.action}" tooltip="#{r.documentTypeName}"
          actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.showLink}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
          <h:outputText  id="col3-text" value="#{r.documentTypeName}" rendered="#{!r.showLink}" />
      </a:column>
      
      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_allRecipientsSenders}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-link" value="#{r.senderOrRecipient}" action="#{DocumentDialog.action}" tooltip="#{r.senderOrRecipient}"
          actionListener="#{DocumentDialog.open}" styleClass="tooltip condence50- no-underline" rendered="#{r.showLink}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
          <h:outputText  id="col4-text" value="#{r.senderOrRecipient}" rendered="#{!r.showLink}" />
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-link" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
            actionListener="#{DocumentDialog.open}" styleClass="tooltip condence20- no-underline" rendered="#{r.showLink}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <h:outputText  id="col6-text" value="#{r.docName}" rendered="#{!r.showLink}" />
      </a:column>
      
      <%-- Created --%>
      <a:column id="col61" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col61-sort" label="#{msg.document_dueDate}" value="created" styleClass="header" />
         </f:facet>
         <a:actionLink id="col61-link" value="#{r.dueDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.dueDateStr}"
            actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.showLink}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <h:outputText  id="col61-text" value="#{r.dueDateStr}" rendered="#{!r.showLink}" />
      </a:column>      

      <%-- Created --%>
      <a:column id="col7" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_created}" value="created" styleClass="header" />
         </f:facet>
         <a:actionLink id="col7-link" value="#{r.createdDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.createdDateStr}"
            actionListener="#{DocumentDialog.open}" styleClass="no-underline" rendered="#{r.showLink}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <h:outputText  id="col7-text" value="#{r.createdDateStr}" rendered="#{!r.showLink}" />
      </a:column>

      <%-- Files --%>
      <a:column id="col10" styleClass="doc-list-actions" >
         <f:facet name="header">
            <h:outputText id="col10-header" value="#{msg.document_allFiles}" styleClass="header" />
         </f:facet>
          <wm:customChildrenContainer id="compound-workflow-document-files" childGenerator="#{DocumentListDialog.documentRowFileGenerator}" parameterList="#{r.files}"/>
      </a:column>
      
      <a:column id="col-assoc-actions" actions="true" styleClass="actions-column2 #{r.cssStyleClass}">
            <a:actionLink id="col-actions" value="#{r.docName}" actionListener="#{DeleteAssocDialog.setupWorkflowAssoc}" action="dialog:deleteAssoc" showLink="false"
                  image="/images/icons/delete.gif" tooltip="#{msg.document_assocsBlockBean_delete}" rendered="#{CompoundWorkflowDialog.showAssocActions}">
                  <f:param name="sourceNodeRef" value="#{r.nodeRef}"/>
                  <f:param name="targetNodeRef" value="#{CompoundWorkflowAssocListDialog.workflowRef}"/>
                  <f:param name="type" value="#{CompoundWorkflowAssocListDialog.assocType}"/>
            </a:actionLink>
      </a:column>      

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager-cwf-assoc-block" styleClass="pager" />
   </a:richList>

</a:panel>