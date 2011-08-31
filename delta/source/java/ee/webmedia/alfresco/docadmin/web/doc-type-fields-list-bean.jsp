<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- TODO DLSeadist järjestuse muutmine, salvestamine
 --%>

<h:panelGroup id="docTypeMetadata-panel-facets">
   <f:facet name="title">
       <h:panelGroup><%-- just to group multiple elements into single child of facet --%>
<%-- TODO DLSeadist
         <a:actionLink image="/images/icons/add_item.gif" id="addExistingField" tooltip="#{msg.docType_metadataList_add_existingField_tooltip}" value="#{msg.docType_metadataList_add_existingField}" 
            actionListener="#{DocTypeFieldsListBean.addMetadataItem}" >
            <f:param name="itemType" value="existingField"/>
         </a:actionLink>
         <a:actionLink image="/images/icons/add_item.gif" id="addSysGroup" tooltip="#{msg.docType_metadataList_add_sysGroup_tooltip}" value="#{msg.docType_metadataList_add_sysGroup}" 
            actionListener="#{DocTypeFieldsListBean.addMetadataItem}" >
            <f:param name="itemType" value="sysGroup"/>
         </a:actionLink>
 --%>
         <a:actionLink image="/images/icons/add_item.gif" id="addField" tooltip="#{msg.docType_metadataList_add_field_tooltip}" value="#{msg.docType_metadataList_add_field}" 
            actionListener="#{DocTypeFieldsListBean.addMetadataItem}" >
            <f:param name="itemType" value="field"/>
         </a:actionLink>
<%-- TODO DLSeadist
         <a:actionLink image="/images/icons/add_item.gif" id="addGroup" tooltip="#{msg.docType_metadataList_add_group_tooltip}" value="#{msg.docType_metadataList_add_group}" 
            actionListener="#{DocTypeFieldsListBean.addMetadataItem}" >
            <f:param name="itemType" value="group"/>
         </a:actionLink>
 --%>
         <a:actionLink image="/images/icons/add_item.gif" id="addSep" tooltip="#{msg.docType_metadataList_add_separator_tooltip}" value="#{msg.docType_metadataList_add_separator}" 
            actionListener="#{DocTypeFieldsListBean.addMetadataItem}" >
            <f:param name="itemType" value="separator"/>
         </a:actionLink>
       </h:panelGroup>
   </f:facet>
</h:panelGroup>

<%-- FIXME DLSeadist test
<a:panel id="docTypeMetadata-panel" label="#{msg.doc_type_details_panel_metadata}" styleClass="panel-100 with-pager" progressive="true" facetsId="dialog:dialog-body:docTypeMetadata-panel-facets" >
 --%>
<a:panel id="docTypeMetadata-panel" label="#{msg.doc_type_details_panel_metadata} ver. #{DocTypeDetailsDialog.docType.latestVersion}" styleClass="panel-100 with-pager" progressive="true" facetsId="dialog:dialog-body:docTypeMetadata-panel-facets" >
   <a:richList id="documentTypeList" value="#{DocTypeFieldsListBean.metaFieldsList}" var="r" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" initialSortColumn="order" refreshOnBind="true">

      <a:column id="order" >
         <f:facet name="header">
            <a:outputText value="#{msg.docType_metadataList_order}" />
         </f:facet>
         <h:inputText value="#{r.order}" styleClass="tiny"/>
      </a:column>

      <a:column id="name">
         <f:facet name="header">
            <a:outputText value="#{msg.docType_metadataList_name}" />
         </f:facet>
         <a:booleanEvaluator value="#{r.type != 'doc_types_separationLine'}">
            <a:actionLink id="editField" tooltip="#{msg.docType_metadataList_edit_field_tooltip}" value="#{r.name} (#{r.fieldId.localName})" 
               actionListener="#{DocTypeFieldsListBean.editMetadataItem}" >
               <f:param name="nodeRef" value="#{r.nodeRef}"/>
            </a:actionLink>
         </a:booleanEvaluator>
      </a:column>

      <a:column id="type">
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
            <a:actionLink id="actionsCol-del" value="" actionListener="#{DocTypeFieldsListBean.removeMetaField}"
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