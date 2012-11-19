<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- 
TODO: ALSeadist CL_TASK 181259 181260 DocTypeDetailsDialog
 --%>
<h:panelGroup id="docTypeMetadata-panel-facets">
   <f:facet name="title">
      <a:booleanEvaluator value="#{DialogManager.bean.dynamicTypeDetailsDialog.showingLatestVersion}">
         <a:booleanEvaluator value="#{DialogManager.bean != FieldGroupDetailsDialog || FieldGroupDetailsDialog.showAddExistingField}">
            <wm:search id="searchFieldDef" pickerCallback="#{DialogManager.bean.fieldsListBean.searchFieldDefinitions}" setterCallback="#{DialogManager.bean.fieldsListBean.addExistingField}"
               searchLinkLabel="docType_metadataList_add_existingField" dialogTitleId="docType_metadataList_add_existingField_pickerTitle" value="#{DialogManager.bean.fieldsListBean.dummy}" ajaxParentLevel="100" searchLinkTooltip="docType_metadataList_add_existingField_tooltip"
            />
         </a:booleanEvaluator>
         <a:booleanEvaluator value="#{DialogManager.bean == DocTypeDetailsDialog || DialogManager.bean == CaseFileTypeDetailsDialog}">
            <wm:search id="searchFieldGroup" pickerCallback="#{DialogManager.bean.fieldsListBean.searchFieldGroups}" setterCallback="#{DialogManager.bean.fieldsListBean.addExistingFieldGroup}"
               searchLinkLabel="docType_metadataList_add_existingFieldGroup" dialogTitleId="docType_metadataList_add_existingFieldGroup_pickerTitle" value="#{DialogManager.bean.fieldsListBean.dummy}" ajaxParentLevel="100" searchLinkTooltip="docType_metadataList_add_existingFieldGroup_tooltip"
            />
         </a:booleanEvaluator>
         <a:actionLink image="/images/icons/add_item.gif" id="addField" tooltip="#{msg.docType_metadataList_add_field_tooltip}" value="#{msg.docType_metadataList_add_field}" 
            actionListener="#{DialogManager.bean.fieldsListBean.addMetadataItem}" rendered="#{DialogManager.bean.addFieldVisible}">
            <f:param name="itemType" value="field"/>
         </a:actionLink>
         <a:booleanEvaluator value="#{DialogManager.bean == DocTypeDetailsDialog || DialogManager.bean == CaseFileTypeDetailsDialog}">
            <a:actionLink image="/images/icons/add_item.gif" id="addGroup" tooltip="#{msg.docType_metadataList_add_group_tooltip}" value="#{msg.docType_metadataList_add_group}" 
               actionListener="#{DialogManager.bean.fieldsListBean.addMetadataItem}" >
               <f:param name="itemType" value="group"/>
            </a:actionLink>
            <a:actionLink image="/images/icons/add_item.gif" id="addSep" tooltip="#{msg.docType_metadataList_add_separator_tooltip}" value="#{msg.docType_metadataList_add_separator}" 
               actionListener="#{DialogManager.bean.fieldsListBean.addMetadataItem}" >
               <f:param name="itemType" value="separator"/>
            </a:actionLink>
         </a:booleanEvaluator>
      </a:booleanEvaluator>
   </f:facet>
</h:panelGroup>

<a:panel id="docTypeMetadata-panel" label="#{DialogManager.bean.dynamicTypeDetailsDialog.metaFieldsListLabel}" styleClass="panel-100 with-pager" progressive="true" facetsId="dialog:dialog-body:docTypeMetadata-panel-facets" >
   <a:richList id="documentTypeList" value="#{DialogManager.bean.fieldsListBean.metaFieldsList}" var="r" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

      <a:column id="order" >
         <f:facet name="header">
            <a:outputText value="#{msg.docType_metadataList_order}" />
         </f:facet>
         <h:inputText value="#{r.order}" styleClass="tiny" rendered="#{DialogManager.bean.dynamicTypeDetailsDialog.showingLatestVersion}" >
            <wm:convertIntWithMsg />
         </h:inputText>
         <h:outputText value="#{r.order}" styleClass="tiny" rendered="#{!DialogManager.bean.dynamicTypeDetailsDialog.showingLatestVersion}" />
      </a:column>

      <a:column id="name">
         <f:facet name="header">
            <a:outputText value="#{msg.docType_metadataList_name}" />
         </f:facet>
         <a:booleanEvaluator value="#{r.type == 'doc_types_field'}">
            <a:actionLink id="editField" tooltip="#{msg.docType_metadataList_edit_field_tooltip}" value="#{r.nameAndFieldId}" 
               actionListener="#{DialogManager.bean.fieldsListBean.editField}" >
               <f:param name="nodeRef" value="#{r.nodeRef}"/>
            </a:actionLink>
         </a:booleanEvaluator>
         <a:booleanEvaluator value="#{r.type == 'doc_types_fieldGroup'}">
            <a:actionLink id="editFieldGroup" tooltip="#{msg.docType_metadataList_edit_field_tooltip}" value="#{r.name}" 
               actionListener="#{DialogManager.bean.fieldsListBean.editFieldGroup}" >
               <f:param name="nodeRef" value="#{r.nodeRef}"/>
            </a:actionLink>
         </a:booleanEvaluator>
      </a:column>

      <a:column id="type" rendered="#{DialogManager.bean == DocTypeDetailsDialog || DialogManager.bean == CaseFileTypeDetailsDialog}">
         <f:facet name="header">
            <a:outputText value="#{msg.docType_metadataList_type}" />
         </f:facet>
         <h:outputText value="#{msg[r.type]}"/>
      </a:column>

      <a:column id="additInfo">
         <f:facet name="header">
            <a:outputText value="#{msg.docType_metadataList_additInfo}" />
         </f:facet>
         <a:booleanEvaluator value="#{r.type != 'doc_types_separationLine'}">
            <h:outputText value="#{r.additionalInfo}"/>
         </a:booleanEvaluator>
      </a:column>

      <a:column id="actionsCol" actions="true">
         <a:booleanEvaluator value="#{r.removableFromList}">
            <a:actionLink id="actionsCol-del" value="" actionListener="#{DialogManager.bean.fieldsListBean.removeMetaField}" rendered="#{DialogManager.bean.dynamicTypeDetailsDialog.showingLatestVersion}"
             showLink="false" image="/images/icons/delete.gif" tooltip="#{msg.docType_metadataList_action_remove}" styleClass="remove_#{r.type}" >
                  <f:param name="nodeRef" value="#{r.nodeRef}"/>
            </a:actionLink>
         </a:booleanEvaluator>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pagerMetadata" styleClass="pager" />
   </a:richList>
</a:panel>

<f:verbatim>
<script type="text/javascript">
   // confirm removing
   function confirmRemoveFieldOrGroup(msg, e){
      var fieldOrGroupName = $jQ(e).closest('tr').children().eq(1).text();
      return confirmWithPlaceholders(msg, fieldOrGroupName);
   }
   prependOnclick($jQ(".remove_doc_types_field"), function(e) {
     var msg = '<%= MessageUtil.getMessageAndEscapeJS("docType_metadataList_action_remove_confirm_field") %>';
     return confirmRemoveFieldOrGroup(msg, e);
   });
   prependOnclick($jQ(".remove_doc_types_fieldGroup"), function(e) {
     var msg = '<%= MessageUtil.getMessageAndEscapeJS("docType_metadataList_action_remove_confirm_fieldGroup") %>';
     return confirmRemoveFieldOrGroup(msg, e);
   });
</script>
</f:verbatim>