<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="classificators-panel" styleClass="panel-100" label="#{msg.classificators_list}" progressive="true">

   <%-- Spaces List --%>
   <a:richList id="classificatorsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" styleClass="recordSet" headerStyleClass="recordSetHeader"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.classificators}" var="classificator"
      initialSortColumn="name">

      <%-- Primary column for the name --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.classificator_name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <a:actionLink id="col1-act1" value="#{classificator.name}" image="/images/icons/space-icon-default-16.gif"
               action="dialog:classificatorDetailsDialog" showLink="false" actionListener="#{ClassificatorDetailsDialog.select}">
               <f:param name="nodeRef" value="#{classificator.nodeRef}" />
            </a:actionLink>
         </f:facet>
         <a:actionLink id="col1-act2" value="#{classificator.name}" action="dialog:classificatorDetailsDialog" actionListener="#{ClassificatorDetailsDialog.select}">
            <f:param name="nodeRef" value="#{classificator.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Adding allowed, yes or no --%>
      <a:column id="col2" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="col2-txt" value="#{msg.classificator_add_allowed}" />
         </f:facet>
         <a:booleanEvaluator id="classificatorCanAddEvaluator" value="#{classificator.addRemoveValues}">
            <h:outputText id="itemValid" value="#{msg.classificator_yes}" />
         </a:booleanEvaluator>
         <a:booleanEvaluator id="classificatorCannotAddEvaluator" value="#{!classificator.addRemoveValues}">
            <h:outputText id="itemNotValid" value="#{msg.classificator_no}" />
         </a:booleanEvaluator>
      </a:column>

      <%-- Actions column --%>
      <a:column id="col3" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="col3-txt" value="#{msg.classificator_actions}" />
         </f:facet>
         <a:actionLink id="col3-act1" value="#{classificator.name}" image="/images/icons/edit_properties.gif" tooltip="#{msg.classificator_actions}" action="dialog:classificatorDetailsDialog"
            showLink="false" actionListener="#{ClassificatorDetailsDialog.select}">
            <f:param name="nodeRef" value="#{classificator.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>