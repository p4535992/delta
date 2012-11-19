<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="caseFile-document-workflows-panel" styleClass="panel-100 with-pager" label="#{msg.caseFile_documents_compoundWorkflows}" progressive="true" expanded="false" >

   <%-- Main List --%>
   <a:richList id="documentWorkflowList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{CaseFileDialog.documentWorkflows}" var="r" refreshOnBind="true" >
      
      <%-- regNumber --%>
      <a:column id="col1"  >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-link" value="#{r.regNumber}" action="#{DocumentDialog.action}" tooltip="#{r.regNumber}" actionListener="#{DocumentDialog.open}" 
         	styleClass="no-underline">
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Registration date --%>
      <a:column id="col2" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-link" value="#{r.document.regDateTimeStr}" action="#{DocumentDialog.action}" tooltip="#{r.document.regDateTimeStr}" actionListener="#{DocumentDialog.open}" 
         	styleClass="no-underline"  >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
          </a:actionLink>
      </a:column>
      
      <%-- Document name --%>
      <a:column id="col3"  >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.caseFile_documentToWorkflow_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-link" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}" actionListener="#{DocumentDialog.open}" 
         	styleClass="no-underline">
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>      
      
      <%-- Document type --%>
      <a:column id="col4" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.caseFile_documentToWorkflow_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-link" value="#{r.documentTypeName}" action="#{DocumentDialog.action}" tooltip="#{r.documentTypeName}"
          actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
          </a:actionLink>
      </a:column>
      
      <%-- Compound workflow title --%>
      <a:column id="col5"  >
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.caseFile_documentToWorkflow_compoundWorkflowTitle}" value="compoundWorkflowTitle" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-link" value="#{r.compoundWorkflowTitle}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowTitle}" actionListener="#{DocumentDialog.open}" 
         	styleClass="no-underline">
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Compound workflow state --%>
      <a:column id="col6"  >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.caseFile_documentToWorkflow_compoundWorkflowState}" value="compoundWorkflowState" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-link" value="#{r.compoundWorkflowState}" action="#{DocumentDialog.action}" tooltip="#{r.compoundWorkflowState}" actionListener="#{DocumentDialog.open}" 
         	styleClass="no-underline">
            <f:param name="nodeRef" value="#{r.document.nodeRef}" />
         </a:actionLink>
      </a:column>      
 

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager-caseFile-document-workflows-panel" styleClass="pager" />
   </a:richList>

</a:panel>