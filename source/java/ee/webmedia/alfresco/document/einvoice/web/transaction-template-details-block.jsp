<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel styleClass="column panel-100" id="dimension-data" label="#{msg.dimension_data}">
   <h:panelGrid id="transaction-template-data-panel" styleClass="table-padding" border="0" width="100%" columnClasses="propertiesLabel," columns="2" >
      <h:outputText id="transaction-template-name-label" value="#{msg.transactionTemplate_name}: " style="padding-left:8px" />
      <h:inputText id="transaction-template-name" value="#{TransactionsTemplateDetailsDialog.transactionTemplate.name}" styleClass="expand19-200" />
      <h:outputText id="transaction-template-active-label" value="#{msg.transactionTemplate_active}: " style="padding-left:8px" />
      <h:selectBooleanCheckbox id="col4-select-check" value="#{TransactionsTemplateDetailsDialog.transactionTemplate.active}">
         <f:selectItem value="#{TransactionsTemplateDetailsDialog.transactionTemplate.active}" />
      </h:selectBooleanCheckbox>      
   </h:panelGrid>
</a:panel>

<h:panelGroup id="template-transaction-panel-group" binding="#{DialogManager.bean.transactionPanelGroup}"/>
