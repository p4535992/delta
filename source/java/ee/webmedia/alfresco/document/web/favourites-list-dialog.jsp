<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog.jsp" />


<a:panel id="compound-workflow-panel" styleClass="panel-100 with-pager" label="#{msg.compound_workflows}" progressive="true" rendered="#{DialogManager.bean.hasWorkflows}">

   <%-- Main List --%>
   <a:richList id="compoundWorkflowList" styleClass="duplicate-header" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.workflows}" var="r" refreshOnBind="true" >
      
      <%-- Title --%>
      <a:column id="col1" rendered="#{WmWorkflowService.workflowTitleEnabled}">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-text-1" value="#{r.compoundWorkflow.title}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflow.title}"
            actionListener="#{CompoundWorkflowDialog.setupWorkflow}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Started Date --%>
      <a:column id="col2" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.cw_search_ignition_date}" value="startedDateStr" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.compoundWorkflow.startedDateStr}" />
      </a:column>

      
      <%-- Status --%>
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.status}" value="status" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text-1" value="#{r.compoundWorkflow.status}" action="dialog:compoundWorkflowDialog" tooltip="#{r.compoundWorkflow.status}"
            actionListener="#{CompoundWorkflowDialog.setupWorkflow}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Object --%>
      <a:column id="col4" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.workflow_compound_object}" value="objectTitle" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text-doc" value="#{r.objectTitle}" action="#{DocumentDialog.action}" actionListener="#{DocumentDialog.open}" rendered="#{r.compoundWorkflow.documentWorkflow}">
            <f:param name="nodeRef" value="#{r.compoundWorkflow.parent}" />
         </a:actionLink>
         <a:actionLink id="col4-text2" value="#{r.objectTitle}" action="dialog:compoundWorkflowDialog" tooltip="#{r.objectTitle}"
          actionListener="#{CompoundWorkflowDialog.setupWorkflow}" styleClass="no-underline"  rendered="#{r.compoundWorkflow.independentWorkflow && r.objectTitle!='Dokumendita'}">
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
          </a:actionLink>
         <a:actionLink id="col4-text-case-file" value="#{r.objectTitle}" actionListener="#{CaseFileDialog.openFromDocumentList}" rendered="#{r.compoundWorkflow.caseFileWorkflow}">
            <f:param name="nodeRef" value="#{r.compoundWorkflow.parent}" />
         </a:actionLink>
         <h:outputText id="col4-text-independent" value="#{r.objectTitle}" rendered="#{r.compoundWorkflow.independentWorkflow && r.objectTitle=='Dokumendita'}"/>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager22" styleClass="pager" />
   </a:richList>
</a:panel>

<a:panel id="case-file-list-panel" styleClass="panel-100 with-pager" label="#{msg.casefile_favorite_list_title}" progressive="true" rendered="#{DialogManager.bean.hasCaseFiles}">

   <a:richList id="caseFileListDialog" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.caseFiles}" var="r" initialSortColumn="validFrom" initialSortDescending="true">
      
      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-lbl" label="#{msg.casefile_volumemark_column}" value="volumeMark" styleClass="header" />
         </f:facet>
         <a:outputText value="#{r.volumeMark}" id="col1-text"/>
      </a:column>
      
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-lbl" label="#{msg.casefile_title_column}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-act" value="#{r.title}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="tooltip condence50- no-underline">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-lbl" label="#{msg.casefile_owner_column}" value="ownerName" styleClass="header" />
         </f:facet>
         <a:outputText value="#{r.ownerName}" id="col3-text"/>
      </a:column>
      
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-lbl" label="#{msg.casefile_validfrom_column}" value="validFrom" styleClass="header" />
         </f:facet>
         <a:outputText value="#{r.validFromStr}" id="col4-text"/>
      </a:column>
      
      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-lbl" label="#{msg.casefile_validto_column}" value="validTo" styleClass="header" />
         </f:facet>
         <a:outputText value="#{r.validToStr}" id="col5-text"/>
      </a:column>
      
      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-lbl" label="#{msg.casefile_status_column}" value="status" styleClass="header" />
         </f:facet>
         <a:outputText value="#{r.status}" id="col6-text"/>
      </a:column>
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

</a:panel>
