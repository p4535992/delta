<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>


<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/ajax/yahoo/dom/dom-min.js"> </script>

<a:panel id="parameters-panel" label="#{msg.parameters_list}" styleClass="panel-100 with-pager">
   <a:richList id="parametersList" value="#{ParametersListDialog.parameters}" var="sParameter" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" initialSortColumn="paramName" >

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
         <f:facet name="csvExport">
            <f:param name="csvExport" value="false" />
         </f:facet>
         <a:outputText id="paramTypeOT" value="#{msg[sParameter.typeMsg]}" />
      </a:column>

      <a:column id="paramDescCol">
         <f:facet name="header">
            <a:sortLink id="paramDescCol-sort" label="#{msg.parameters_desc}" value="paramDescription" mode="case-insensitive" />
         </f:facet>
         <h:inputTextarea rows="1" cols="45" value="#{sParameter.paramDescription}" styleClass="#{sParameter.paramName} #{(sParameter.lastValidationSuccessful) ? '' : 'error' } expand19-200 medium" />
      </a:column>

      <a:column id="paramValCol">
         <f:facet name="header">
            <a:outputText value="#{msg.parameters_value}" />
         </f:facet>
         <h:inputTextarea rows="1" cols="45" value="#{sParameter.paramValue}" styleClass="#{sParameter.paramName} #{(sParameter.lastValidationSuccessful) ? '' : 'error' } expand19-200 medium" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
