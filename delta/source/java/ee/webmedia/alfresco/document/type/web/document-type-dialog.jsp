<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="documenttype-panel" label="#{msg.document_types}" styleClass="panel-100 with-pager">
   <a:richList id="documentTypeList" value="#{DialogManager.bean.documentTypes}" var="type" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" initialSortColumn="name">

      <a:column id="documentTypeUsedCol">
         <f:facet name="header">
            <a:outputText value="#{msg.document_type_used}" />
         </f:facet>
         <h:selectBooleanCheckbox value="#{type.used}" />
      </a:column>

      <a:column id="documentTypePublicAdrCol">
         <f:facet name="header">
            <a:outputText value="#{msg.document_type_public_adr}" />
         </f:facet>
         <h:selectBooleanCheckbox value="#{type.publicAdr}" />
      </a:column>

      <a:column id="documentTypeNameCol" primary="true">
         <f:facet name="header">
            <a:sortLink id="documentTypeNameCol-sort" label="#{msg.document_type_name}" value="name" mode="case-insensitive" />
         </f:facet>
         <h:inputText id="documentTypeNameCol-input" value="#{type.name}" size="45" />
      </a:column>

      <a:column id="documentTypeCommentCol">
         <f:facet name="header">
            <a:outputText value="#{msg.document_type_comment}" />
         </f:facet>
         <h:inputTextarea rows="1" cols="45" value="#{type.comment}" styleClass="expand19-200" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
