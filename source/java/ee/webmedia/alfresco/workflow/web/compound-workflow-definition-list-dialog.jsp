<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="compound-workflow-list-panel" styleClass="panel-100 with-pager" label="#{msg.workflow_compound_list}" progressive="true">

   <a:richList id="compoundWorkflowsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{CompoundWorkflowDefinitionListDialog.workflows}" var="r" initialSortColumn="name">

      <%-- Name column --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-lbl" label="#{msg.workflow_compound_name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-act" value="#{r.nameAndUserFullName}" action="dialog:compoundWorkflowDefinitionDialog" actionListener="#{CompoundWorkflowDefinitionDialog.setupWorkflow}"
            tooltip="#{msg.workflow_compound_edit}">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Document types column --%>
      <a:column id="col2" rendered="#{CompoundWorkflowDefinitionListDialog.showDocumentColumn}">
         <f:facet name="header">
            <h:outputText id="col2-lbl" value="#{msg.workflow_compound_type}" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.documentTypes}">
            <f:converter converterId="ee.webmedia.alfresco.document.type.web.DocumentTypeConverter" />
         </h:outputText>
      </a:column>
      
      <%-- case file types column --%>
      <a:column id="col3" rendered="#{CompoundWorkflowDefinitionListDialog.showCaseFileColumn}">
         <f:facet name="header">
            <h:outputText id="col3-lbl" value="#{msg.workflow_compound_caseFileType}" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.caseFileTypes}">
            <f:converter converterId="ee.webmedia.alfresco.document.type.web.CaseFileTypeConverter" />
         </h:outputText>
      </a:column>      

      <%-- Actions column --%>
      <a:column id="col4" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="col4-txt" value="#{msg.workflow_compound_actions}" />
         </f:facet>
         <a:actionLink id="col4-act2" value="#{msg.workflow_compound_delete}" tooltip="#{msg.workflow_compound_delete}"
            actionListener="#{InformingDeleteNodeDialog.setupDelete}" action="dialog:informingDeleteNodeDialog" showLink="false" image="/images/icons/delete.gif">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
            <f:param name="containerTitleMsgKey" value="delete_compound_workflow" />
            <f:param name="confirmMsgKey" value="delete_compound_workflow_confirm" />
            <f:param name="deletableObjectNameProp" value="wfc:name" />
            <f:param name="successMsgKey" value="workflow_compound_delete_compound_success" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

</a:panel>