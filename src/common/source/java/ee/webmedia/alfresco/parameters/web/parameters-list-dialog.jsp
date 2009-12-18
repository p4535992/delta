<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>


<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/ajax/yahoo/dom/dom-min.js"> </script>
<%--
   jquery.textarea-expander binds to all textareas, that have classname containing "expand"
 --%>

<a:panel id="parameters-panel" label="#{msg.parameters_list}" styleClass="panel-100">
   <a:richList id="parametersList" value="#{ParametersListDialog.parameters}" var="sParameter" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      initialSortColumn="paramName">

      <a:column id="paramNameCol" primary="true">
         <f:facet name="header">
            <a:sortLink id="paramNameCol-sort" label="#{msg.parameters_name}" value="paramName" mode="case-insensitive" />
         </f:facet>
         <h:outputText value="#{sParameter.paramName}" styleClass="#{(sParameter.lastValidationSuccessful) ? '' : 'error' }" />
      </a:column>

      <a:column id="paramTypeCol">
         <f:facet name="header">
            <a:sortLink id="paramTypeCol-sort" label="#{msg.parameters_type}" value="typeMsg" mode="case-insensitive" />
         </f:facet>
         <a:outputText id="paramTypeOT" value="#{msg[sParameter.typeMsg]}" />
      </a:column>

      <a:column id="paramValCol">
         <f:facet name="header">
            <a:outputText value="#{msg.parameters_value}" />
         </f:facet>
         <h:inputTextarea rows="1" cols="45" value="#{sParameter.paramValue}" styleClass="#{(sParameter.lastValidationSuccessful) ? '' : 'error' } expand5-200" />
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
