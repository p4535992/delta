<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<a:booleanEvaluator value="#{ClassificatorDetailsDialog.savedClassificator}">
<%@ include file="searchPanel.jsp" %>
</a:booleanEvaluator>
<a:panel id="classificator-details" styleClass="panel-100" label="#{msg.classificator_metadata}" progressive="true" >
<a:booleanEvaluator value="#{ClassificatorDetailsDialog.unsavedClassificator}">
   <a:panel id="new-classificator-message" styleClass="message">
      <h:graphicImage url="/images/icons/info_icon.gif" width="16" height="16" alt="" />
      <h:outputText value="#{msg.classificators_create_classificator_confirm}" />
   </a:panel>
   </a:booleanEvaluator>
   <r:propertySheetGrid id="classificator-details-props" value="#{ClassificatorDetailsDialog.classificatorNode}" columns="1"
      mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
<a:booleanEvaluator value="#{ClassificatorDetailsDialog.savedClassificator}">
<a:panel id="classificators-panel" styleClass="panel-100 with-pager" label="#{msg.classificators_values_list}" progressive="true">

   <%-- Classificator Values List --%>
   <a:richList id="classificatorsDetailsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{ClassificatorDetailsDialog.classificatorValues}" var="clValue" initialSortColumn="order"
      binding="#{ClassificatorDetailsDialog.richList}" refreshOnBind="true">

      <%-- Name column --%>
      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.classificator_value_name}" value="valueName" styleClass="header" />
         </f:facet>
         <h:inputTextarea id="col1-in-txt" value="#{clValue.valueName}" readonly="#{clValue.readOnly}"
            styleClass="expand19-200 #{(clValue.lastNameValidationSuccess) ? '' : 'error' }" />
      </a:column>

      <%-- Order column --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.classificator_value_order}" value="order" styleClass="header" />
         </f:facet>
         <h:inputText id="col2-in-txt" value="#{clValue.orderText}" size="1" styleClass="#{(clValue.lastOrderValidationSuccess) ? '' : 'error ' }small" />
      </a:column>
      
      <%-- Description column --%>
      <a:column id="col3">
         <f:facet name="header">
            <h:outputText id="col3-sort" value="#{msg.classificator_value_description}" />
         </f:facet>
         <h:inputTextarea id="col3-in-txt" value="#{clValue.classificatorDescription}" readonly="#{clValue.readOnly}" styleClass="expand19-200" />
      </a:column>

      <%-- valueData column --%>
      <a:column id="col7">
         <f:facet name="header">
            <h:outputText id="col7-sort" value="#{msg.classificator_value_valueData}" />
         </f:facet>
         <h:inputTextarea id="col7-in-txt" value="#{clValue.valueData}" readonly="#{clValue.readOnly}" styleClass="expand19-200 classificatorTextArea" />
      </a:column>

      <%-- byDefault column --%>
      <a:column id="col4">
         <f:facet name="header">
            <h:outputText id="col3-sort" value="#{msg.classificator_value_bydefault}" />
         </f:facet>
         <h:selectBooleanCheckbox id="col3-select-check" value="#{clValue.byDefault}">
            <f:selectItem value="#{clValue.byDefault}" />
         </h:selectBooleanCheckbox>
      </a:column>

      <%-- Active column --%>
      <a:column id="col5">
         <f:facet name="header">
            <h:outputText id="col4-sort" value="#{msg.classificator_value_active}" />
         </f:facet>
         <h:selectBooleanCheckbox id="col4-select-check" value="#{clValue.active}">
            <f:selectItem value="#{clValue.active}" />
         </h:selectBooleanCheckbox>
      </a:column>

      <%-- Remove column --%>
      <a:column id="col6" rendered="#{ClassificatorDetailsDialog.addRemoveValuesAllowed}">
         <f:facet name="header">
            <h:outputText id="col5-sort" value="#{msg.classificator_value_remove}" />
         </f:facet>
         <a:actionLink id="col5-act1" value="#{msg.classificator_value_remove}" image="/images/icons/delete.gif" showLink="false"
            actionListener="#{ClassificatorDetailsDialog.removeValue}" rendered="#{!clValue.readOnly}">
            <f:param name="nodeRef" value="#{clValue.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>
</a:booleanEvaluator>
</a:panel>

<f:verbatim>
<script type="text/javascript">
$jQ("input[type='text'].error").each(function (index, domEle) {
   var val = domEle.value;
   if(val == "") {
      var input = $jQ(domEle);
      input.keyup(function(event) {
          var newValue = $jQ(this).val();
          if(newValue.length > 0){
             input.removeClass("error");
          } else {
             input.addClass("error");
          }
      });
   }
});
</script>
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<a:booleanEvaluator value="#{ClassificatorDetailsDialog.savedClassificator}">
<%@ include file="searchPanel.jsp" %>
</a:booleanEvaluator>
<a:panel id="classificator-details" styleClass="panel-100" label="#{msg.classificator_metadata}" progressive="true" >
<a:booleanEvaluator value="#{ClassificatorDetailsDialog.unsavedClassificator}">
   <a:panel id="new-classificator-message" styleClass="message">
      <h:graphicImage url="/images/icons/info_icon.gif" width="16" height="16" alt="" />
      <h:outputText value="#{msg.classificators_create_classificator_confirm}" />
   </a:panel>
   </a:booleanEvaluator>
   <r:propertySheetGrid id="classificator-details-props" value="#{ClassificatorDetailsDialog.classificatorNode}" columns="1"
      mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
<a:booleanEvaluator value="#{ClassificatorDetailsDialog.savedClassificator}">
<a:panel id="classificators-panel" styleClass="panel-100 with-pager" label="#{msg.classificators_values_list}" progressive="true">

   <%-- Classificator Values List --%>
   <a:richList id="classificatorsDetailsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{ClassificatorDetailsDialog.classificatorValues}" var="clValue" initialSortColumn="order"
      binding="#{ClassificatorDetailsDialog.richList}" refreshOnBind="true">

      <%-- Name column --%>
      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.classificator_value_name}" value="valueName" styleClass="header" />
         </f:facet>
         <h:inputTextarea id="col1-in-txt" value="#{clValue.valueName}" readonly="#{clValue.readOnly}"
            styleClass="expand19-200 #{(clValue.lastNameValidationSuccess) ? '' : 'error' }" />
      </a:column>

      <%-- Order column --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.classificator_value_order}" value="order" styleClass="header" />
         </f:facet>
         <h:inputText id="col2-in-txt" value="#{clValue.orderText}" size="1" styleClass="#{(clValue.lastOrderValidationSuccess) ? '' : 'error ' }small" />
      </a:column>
      
      <%-- Description column --%>
      <a:column id="col3">
         <f:facet name="header">
            <h:outputText id="col3-sort" value="#{msg.classificator_value_description}" />
         </f:facet>
         <h:inputTextarea id="col3-in-txt" value="#{clValue.classificatorDescription}" readonly="#{clValue.readOnly}" styleClass="expand19-200" />
      </a:column>

      <%-- valueData column --%>
      <a:column id="col7">
         <f:facet name="header">
            <h:outputText id="col7-sort" value="#{msg.classificator_value_valueData}" />
         </f:facet>
         <h:inputTextarea id="col7-in-txt" value="#{clValue.valueData}" readonly="#{clValue.readOnly}" styleClass="expand19-200 classificatorTextArea" />
      </a:column>

      <%-- byDefault column --%>
      <a:column id="col4">
         <f:facet name="header">
            <h:outputText id="col3-sort" value="#{msg.classificator_value_bydefault}" />
         </f:facet>
         <h:selectBooleanCheckbox id="col3-select-check" value="#{clValue.byDefault}">
            <f:selectItem value="#{clValue.byDefault}" />
         </h:selectBooleanCheckbox>
      </a:column>

      <%-- Active column --%>
      <a:column id="col5">
         <f:facet name="header">
            <h:outputText id="col4-sort" value="#{msg.classificator_value_active}" />
         </f:facet>
         <h:selectBooleanCheckbox id="col4-select-check" value="#{clValue.active}">
            <f:selectItem value="#{clValue.active}" />
         </h:selectBooleanCheckbox>
      </a:column>

      <%-- Remove column --%>
      <a:column id="col6" rendered="#{ClassificatorDetailsDialog.addRemoveValuesAllowed}">
         <f:facet name="header">
            <h:outputText id="col5-sort" value="#{msg.classificator_value_remove}" />
         </f:facet>
         <a:actionLink id="col5-act1" value="#{msg.classificator_value_remove}" image="/images/icons/delete.gif" showLink="false"
            actionListener="#{ClassificatorDetailsDialog.removeValue}" rendered="#{!clValue.readOnly}">
            <f:param name="nodeRef" value="#{clValue.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>
</a:booleanEvaluator>
</a:panel>

<f:verbatim>
<script type="text/javascript">
$jQ("input[type='text'].error").each(function (index, domEle) {
   var val = domEle.value;
   if(val == "") {
      var input = $jQ(domEle);
      input.keyup(function(event) {
          var newValue = $jQ(this).val();
          if(newValue.length > 0){
             input.removeClass("error");
          } else {
             input.addClass("error");
          }
      });
   }
});
</script>
>>>>>>> develop-5.1
</f:verbatim>