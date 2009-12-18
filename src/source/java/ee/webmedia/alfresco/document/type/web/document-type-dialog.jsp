<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="documenttype-panel" label="#{msg.document_types}" styleClass="panel-100">
   <a:richList id="documentTypeList" value="#{DialogManager.bean.documentTypes}" var="type" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      initialSortColumn="name">

      <a:column id="documentTypeUsedCol">
         <f:facet name="header">
            <a:outputText value="#{msg.document_type_used}" />
         </f:facet>
         <h:selectBooleanCheckbox value="#{type.used}" />
      </a:column>

      <a:column id="documentTypeNameCol" primary="true">
         <f:facet name="header">
            <a:sortLink id="documentTypeNameCol-sort" label="#{msg.document_type_name}" value="name" mode="case-insensitive" />
         </f:facet>
         <h:outputText value="#{type.name}" />
      </a:column>

      <a:column id="documentTypeCommentCol">
         <f:facet name="header">
            <a:outputText value="#{msg.document_type_comment}" />
         </f:facet>
         <h:inputTextarea rows="1" cols="45" value="#{type.comment}" styleClass="expand5-200" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>

<f:verbatim>
   <script type="text/javascript">
      $jQ(document).ready(function () {
         var finishButton = $jQ('#' + escapeId4JQ('dialog:cancel-button'));
         finishButton.remove();
      });
   </script>
</f:verbatim>
