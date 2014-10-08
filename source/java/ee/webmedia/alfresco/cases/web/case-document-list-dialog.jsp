<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="case-panel" styleClass="panel-100 with-pager" label="#{DialogManager.bean.caseListTitle}" progressive="true" rendered="#{DialogManager.bean.parent.containsCases}">

   <%-- Main List --%>
   <a:richList id="casesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{CaseDocumentListDialog.cases}" var="r" initialSortColumn="title">

      <%-- Title --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.case_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-link" value="#{r.title} (#{r.containingDocsCount})" action="dialog:documentListDialog" tooltip="#{msg.case_document_list_info}" showLink="false"
            actionListener="#{DocumentListDialog.setup}">
            <f:param name="caseNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- status --%>
      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.case_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.status}" />
      </a:column>

      <%-- show details --%>
      <a:column id="col7" actions="true" styleClass="actions-column" rendered="#{UserService.documentManager}">
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col7-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="dialog:caseDetailsDialog" showLink="false"
            actionListener="#{CaseDetailsDialog.showDetails}" tooltip="#{msg.case_details_info}">
            <f:param name="caseNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog.jsp" />

<h:panelGroup id="case-assoc-block-group" rendered="#{CaseDocumentListDialog.showAssocsBlock}" >
	<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/associations/web/assocs-block.jsp" />
</h:panelGroup>
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="case-panel" styleClass="panel-100 with-pager" label="#{DialogManager.bean.caseListTitle}" progressive="true" rendered="#{DialogManager.bean.parent.containsCases}">

   <%-- Main List --%>
   <a:richList id="casesList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{CaseDocumentListDialog.cases}" var="r" initialSortColumn="title">

      <%-- Title --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.case_title}" value="title" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-link" value="#{r.title} (#{r.containingDocsCount})" action="#{DocumentListDialog.action}" tooltip="#{msg.case_document_list_info}" showLink="false"
            actionListener="#{DocumentListDialog.setup}">
            <f:param name="caseNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- status --%>
      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.case_status}" value="status" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.status}" />
      </a:column>

      <%-- show details --%>
      <a:column id="col7" actions="true" styleClass="actions-column" rendered="#{UserService.documentManager}">
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col7-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="#{CaseDetailsDialog.action}" showLink="false"
            actionListener="#{CaseDetailsDialog.showDetails}" tooltip="#{msg.case_details_info}">
            <f:param name="caseNodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog.jsp" />

<h:panelGroup id="case-assoc-block-group" rendered="#{CaseDocumentListDialog.showAssocsBlock}" >
	<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/associations/web/assocs-block.jsp" />
</h:panelGroup>
>>>>>>> develop-5.1
