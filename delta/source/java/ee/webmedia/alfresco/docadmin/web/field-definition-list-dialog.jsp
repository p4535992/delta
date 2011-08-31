<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- TODO DLSeadist test --%>
<h:panelGroup id="docTypeFields-panel-facets">
   <f:facet name="title">
       <a:actionLink id="nameCol-input2" value="FIXME: testimisels link - lisa uus eelseadistatud andmeväli" action="dialog:fieldDetailsDialog" actionListener="#{FieldDetailsDialog.addNew}" image="/images/icons/add_item.gif" />
   </f:facet>
</h:panelGroup>

<a:panel id="docTypeFields-panel" label="#{msg.fieldDefinitions_list}" styleClass="panel-100 with-pager" facetsId="dialog:dialog-body:docTypeFields-panel-facets" >
   <a:richList id="docTypeFieldsList" value="#{FieldDefinitionListDialog.fieldDefinitions}" refreshOnBind="true" var="fd" viewMode="details" 
      pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" initialSortColumn="name">

      <a:column id="nameCol" primary="true">
         <f:facet name="header">
            <a:sortLink id="nameCol-sort" label="#{msg.fieldDefinitions_list_name}" value="name" mode="case-insensitive" />
         </f:facet>
         <a:actionLink id="name" value="#{fd.name}" action="dialog:fieldDetailsDialog" actionListener="#{FieldDetailsDialog.editField}">
            <f:param name="nodeRef" value="#{fd.nodeRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="idCol">
         <f:facet name="header">
            <a:sortLink id="idCol-sort" label="#{msg.fieldDefinitions_list_fieldId}" value="fieldId" mode="case-insensitive" />
         </f:facet>
         <h:outputText value="#{fd.fieldId.localName}"/>
      </a:column>

      <a:column id="systematicCol">
         <f:facet name="header">
            <a:outputText value="#{msg.fieldDefinitions_list_systematic}" />
         </f:facet>
         <h:outputText value="#{fd.systematic}"><a:convertBoolean /></h:outputText>
      </a:column>

      <a:column id="docTypesCol">
         <f:facet name="header">
            <a:outputText value="#{msg.fieldDefinitions_list_docTypes}" />
         </f:facet>
         <h:outputText value="#{fd.docTypes}" ><a:convertMultiValue /></h:outputText>
      </a:column>

      <a:column id="parameterOrderInDocSearchCol">
         <f:facet name="header">
            <a:sortLink id="parameterOrderInDocSearchCol-sort" label="#{msg.fieldDefinitions_list_parameterOrderInDocSearch}" value="parameterOrderInDocSearch" mode="case-insensitive" />
         </f:facet>
         <h:inputText value="#{fd.parameterOrderInDocSearch}" styleClass="tiny" disabled="#{!fd.parameterInDocSearch}">
            <wm:convertIntWithMsg msgKey="fieldDefinitions_list_parameterOrderInDocSearch_error" />
         </h:inputText>
      </a:column>

      <a:column id="parameterOrderInVolSearchCol">
         <f:facet name="header">
            <a:sortLink id="parameterOrderInVolSearchCol-sort" label="#{msg.fieldDefinitions_list_parameterOrderInVolSearch}" value="parameterOrderInVolSearch" mode="case-insensitive" />
         </f:facet>
         <h:inputText value="#{fd.parameterOrderInVolSearch}" styleClass="tiny" disabled="#{!fd.parameterInVolSearch}" >
            <wm:convertIntWithMsg msgKey="fieldDefinitions_list_parameterOrderInVolSearch_error" />
         </h:inputText>
      </a:column>

      <a:column id="volTypesCol" rendered="#{VolumeService.caseVolumeEnabled}">
         <f:facet name="header">
            <a:outputText value="#{msg.fieldDefinitions_list_volTypes}" />
         </f:facet>
         <h:outputText value="#{fd.volTypes}" ><a:convertMultiValue /></h:outputText>
      </a:column>

      <a:column id="actionsCol" actions="true">
         <a:booleanEvaluator value="#{empty fd.docTypes && empty fd.volTypes && !fd.systematic}">
            <a:actionLink id="actionsCol-del" value="#{fd.name}" actionListener="#{FieldDefinitionListDialog.deleteField}"
             showLink="false" image="/images/icons/delete.gif" tooltip="#{msg.fieldDefinitions_list_action_delete}" styleClass="deleteFieldDef">
               <f:param name="nodeRef" value="#{fd.nodeRef}" />
            </a:actionLink>
         </a:booleanEvaluator>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />

<f:verbatim>
<script type="text/javascript">
   prependOnclick($jQ(".deleteFieldDef"), function(e) {
     var msg = '<%= MessageUtil.getMessageAndEscapeJS("fieldDefinitions_list_action_delete_confirm") %>';
     var name = $jQ(e).closest('tr').children().eq(0).text();
     var id = $jQ(e).closest('tr').children().eq(1).text();
     return confirmWithPlaceholders(msg, name, id);
   });
</script>
</f:verbatim>
