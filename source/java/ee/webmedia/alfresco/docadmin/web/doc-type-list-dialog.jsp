<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="docType-panel" label="#{DialogManager.bean.listTitle}" styleClass="panel-100 with-pager">
   <%-- 
   DialogManager.bean is used to refer to subclass of DynamicTypeListDialog - 
   either DocTypeListDialog or CaseFileTypeListDialog
    --%>
   <a:richList id="docTypeList" value="#{DialogManager.bean.types}" refreshOnBind="true" var="type" viewMode="details" 
      pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" initialSortColumn="name">

      <a:column id="docTypeUsedCol">
         <f:facet name="header">
            <a:outputText value="#{msg.document_type_used}" />
         </f:facet>
         <h:outputText value="#{type.used}"><a:convertBoolean /></h:outputText>
      </a:column>

      <a:column id="docTypePublicAdrCol" rendered="#{DialogManager.bean == DocTypeListDialog}">
         <f:facet name="header">
            <a:outputText value="#{msg.document_type_public_adr}" />
         </f:facet>
         <h:outputText value="#{type.publicAdr}"><a:convertBoolean /></h:outputText>
      </a:column>

      <a:column id="docTypeNameCol" primary="true">
         <f:facet name="header">
            <a:sortLink id="docTypeNameCol-sort" label="#{msg.document_type_name}" value="name" mode="case-insensitive" />
         </f:facet>
         <a:actionLink id="docTypeNameCol-input" value="#{type.name}" actionListener="#{DialogManager.bean.showDetails}">
            <f:param name="nodeRef" value="#{type.nodeRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="docTypeIdCol" primary="true">
         <f:facet name="header">
            <a:sortLink id="docTypeIdCol-sort" label="#{msg.document_type_id}" value="id" mode="case-insensitive" />
         </f:facet>
         <h:outputText value="#{type.id}"/>
      </a:column>

      <a:column id="docTypeCommentCol">
         <f:facet name="header">
            <a:outputText value="#{msg.document_type_comment}" />
         </f:facet>
         <h:outputText value="#{type.comment}" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
