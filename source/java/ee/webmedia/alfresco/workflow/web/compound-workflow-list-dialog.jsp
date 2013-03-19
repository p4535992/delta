<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="compound-workflow-list-panel" styleClass="panel-100 with-pager" label="#{msg.workflow_compound_list}" progressive="true">

   <a:richList id="compoundWorkflowsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{CompoundWorkflowListDialog.compoundWorkflows}" var="r" initialSortColumn="createdDateTime" initialSortDescending="true">

      <%-- Title column --%>
      <a:column id="col1" primary="true" rendered="#{CompoundWorkflowListDialog.showTitle}">
         <f:facet name="header">
            <a:sortLink id="col1-lbl" label="#{msg.workflow_compound_title_column}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-act" value="#{r.title}" action="dialog:compoundWorkflowDialog" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}">
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Started date column --%>
      <a:column id="col1_1" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col1_1-lbl" label="#{msg.workflow_compound_startedDate_column}" value="startedDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1_1-act" value="#{r.startedDateStr}" action="dialog:compoundWorkflowDialog" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}">
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Status column --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-lbl" label="#{msg.workflow_compound_status}" value="status" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-act" value="#{r.status}" action="dialog:compoundWorkflowDialog" actionListener="#{CompoundWorkflowDialog.setupWorkflowFromList}">
            <f:param name="nodeRef" value="#{r.compoundWorkflow.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Object column --%>
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-lbl" label="#{msg.workflow_compound_object}" value="objectTitle" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text-doc" value="#{r.objectTitle}" action="#{DocumentDialog.action}" actionListener="#{DocumentDialog.open}" rendered="#{r.compoundWorkflow.documentWorkflow}">
            <f:param name="nodeRef" value="#{r.compoundWorkflow.parent}" />
         </a:actionLink>
         <a:actionLink id="col3-text-case-file" value="#{r.objectTitle}" actionListener="#{CaseFileDialog.openFromDocumentList}" rendered="#{r.compoundWorkflow.caseFileWorkflow}">
            <f:param name="nodeRef" value="#{r.compoundWorkflow.parent}" />
         </a:actionLink>
         <h:outputText id="col3-text-independent" value="#{r.objectTitle}" rendered="#{r.compoundWorkflow.independentWorkflow}"/>
      </a:column>     

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

</a:panel>