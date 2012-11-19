<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="case-file-list-panel" styleClass="panel-100 with-pager" label="#{msg.casefile_list_title}" progressive="true">

   <a:richList id="caseFileListDialog" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{CaseFileListDialog.caseFiles}" var="r" initialSortColumn="validFrom" initialSortDescending="true">
      
      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-lbl" label="#{msg.casefile_title_column}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-act" value="#{r.title}" actionListener="#{CaseFileDialog.openFromDocumentList}" styleClass="tooltip condence50- no-underline">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-lbl" label="#{msg.casefile_type_column}" value="type" styleClass="header" />
         </f:facet>
         <a:outputText value="#{r.type}" id="col2-text"/>
      </a:column>
      
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-lbl" label="#{msg.casefile_duedate_column}" value="dueDate" styleClass="header" />
         </f:facet>
         <a:outputText value="#{r.dueDateStr}" id="col3-text"/>
      </a:column>
      
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-lbl" label="#{msg.casefile_workflow_status_column}" value="workflowsStatus" styleClass="header" />
         </f:facet>
         <a:outputText value="#{r.workflowsStatus}" id="col4-text"/>
      </a:column>
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>

</a:panel>