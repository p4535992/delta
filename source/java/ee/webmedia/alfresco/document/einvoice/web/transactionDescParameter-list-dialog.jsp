<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="transactionDescParameters-panel" styleClass="with-pager" label="#{msg.transactionDescParameter_title}" >

   <%-- Spaces List --%>
   <a:richList id="transactionDescParameterList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.transactionDescParameters}" var="tdp" initialSortColumn="name" >

      <%-- Primary column for the name --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.transactionDescParameter_name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{tdp.name}" />
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.transactionDescParameter_mandatoryForOwner}" value="mandatoryForOwner" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:selectBooleanCheckbox id="col2-select-check" value="#{tdp.mandatoryForOwner}">
            <f:selectItem value="#{tdp.mandatoryForOwner}" />
         </h:selectBooleanCheckbox>
      </a:column>
      
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.transactionDescParameter_mandatoryForCostManager}" value="mandatoryForCostManager" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:selectBooleanCheckbox id="col3-select-check" value="#{tdp.mandatoryForCostManager}">
            <f:selectItem value="#{tdp.mandatoryForCostManager}" />
         </h:selectBooleanCheckbox>
      </a:column>    
      
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.transactionDescParameter_mandatoryForAccountant}" value="mandatoryForAccountant" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:selectBooleanCheckbox id="col4-select-check" value="#{tdp.mandatoryForAccountant}">
            <f:selectItem value="#{tdp.mandatoryForAccountant}" />
         </h:selectBooleanCheckbox>
      </a:column>   

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>