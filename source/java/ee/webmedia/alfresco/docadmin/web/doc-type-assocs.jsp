<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%!
private String getMethodBinding(String followUpOrReply, String methodName){
    return "#{"+getAssocsListBeanExpression(followUpOrReply, methodName)+"}";
}

private String getAssocsListBeanExpression(String followUpOrReply, String methodName){
    return "DocTypeDetailsDialog."+followUpOrReply+"AssocsListBean."+methodName;
}
%>
<%
String followUpOrReply = (String) request.getAttribute("followUpOrReply");
String panelId = "docType-"+followUpOrReply+"Assocs-panel";

String facetId = "docType"+ followUpOrReply +"Assocs-panel-facets";
String facetId2 = "dialog:dialog-body:"+facetId;
String panelLabel = MessageUtil.getMessage("doc_type_details_panel_assocs_"+followUpOrReply);
String addAssocLabel = MessageUtil.getMessage("docType_assocsList_add_"+followUpOrReply);
String addAssocTooltip = MessageUtil.getMessage("docType_assocsList_add_"+followUpOrReply+"_tooltip");
%>
<wm:panelGroup id="<%=facetId%>">
   <f:facet name="title">
      <a:actionLink image="/images/icons/add_item.gif" value="<%=addAssocLabel%>" tooltip="<%=addAssocTooltip%>" 
         actionListener='<%=getMethodBinding(followUpOrReply, "addAssoc")%>' styleClass="triggerPropSheetValidation" rendered="#{DocTypeDetailsDialog.showingLatestVersion}" />
   </f:facet>
</wm:panelGroup>

<a:panel id="<%=panelId%>" label="<%=panelLabel%>" styleClass="panel-100 with-pager" progressive="true" facetsId="<%=facetId2 %>" >
   <a:richList value='<%=getMethodBinding(followUpOrReply, "assocs")%>' var="r" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

      <a:column id="docTypeName" >
         <f:facet name="header">
            <a:outputText value="#{msg.docType_assocsList_docTypeName}" />
         </f:facet>
         <a:actionLink id="editAssoc" tooltip="#{msg.docType_assocsList_edit_assoc_tooltip}" value="#{r.docTypeName}" 
            actionListener='<%=getMethodBinding(followUpOrReply, "editAssoc")%>' >
            <f:param name="nodeRef" value="#{r.nodeRef}"/>
         </a:actionLink>
      </a:column>

      <a:column id="docTypeId" >
         <f:facet name="header">
            <a:outputText value="#{msg.fieldDefinitions_list_fieldId}" />
         </f:facet>
         <h:outputText value="#{r.docType}"/>
      </a:column>

      <a:column id="actionsCol" actions="true">
         <a:actionLink id="actionsCol-delConf" value="" actionListener="#{DeleteDialog.setupDeleteDialog}" action="dialog:deleteDialog"
          showLink="false" image="/images/icons/delete.gif" tooltip="#{msg.docType_assocsList_action_remove}" rendered="#{DocTypeDetailsDialog.showingLatestVersion}" >
               <f:param name="nodeRef" value="#{r.nodeRef}"/>
               <wm:param name="deleteAfterConfirmHandler" value='<%=getAssocsListBeanExpression(followUpOrReply, "deleteAssoc")%>'/>
               <f:param name="confirmMessagePlaceholder0" value="#{DocTypeDetailsDialog.docType.nameAndId}"/>
               <f:param name="confirmMessagePlaceholder1" value="#{r.docTypeName}"/>
               <f:param name="confirmMessagePlaceholder2" value="#{r.docType}"/>
               <f:param name="dialogsToClose" value="1"/>
               <wm:param name="alreadyDeletedHandler" value='<%=getAssocsListBeanExpression(followUpOrReply,"doAfterDelete")%>' />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pagerAssocs" styleClass="pager" />
   </a:richList>
</a:panel>
