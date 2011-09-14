<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="docTypeReplyAssocs-panel-facets">
   <f:facet name="title">
      <a:actionLink image="/images/icons/add_item.gif" id="addReply" value="#{msg.docType_assocsList_add_reply}" tooltip="#{msg.docType_assocsList_add_reply_tooltip}"
         actionListener="#{DocTypeDetailsDialog.assocsListBean.addAssoc}" >
         <f:param name="assocType" value="REPLY"/>
      </a:actionLink>
   </f:facet>
</h:panelGroup>

<a:panel id="docTypeReplyAssocs-panel" label="#{msg.doc_type_details_panel_assocs_reply}" styleClass="panel-100 with-pager" progressive="true" facetsId="dialog:dialog-body:docTypeReplyAssocs-panel-facets" >
   <a:richList id="docTypeReplyAssocsList" value="#{DocTypeDetailsDialog.assocsListBean.replyAssocs}" var="r" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

      <a:column id="docTypeName" >
         <f:facet name="header">
            <a:outputText value="#{msg.docType_assocsList_docTypeName}" />
         </f:facet>
         <a:actionLink id="editAssoc" tooltip="#{msg.docType_assocsList_edit_assoc_tooltip}" value="#{r.docTypeName}" 
            actionListener="#{DocTypeDetailsDialog.assocsListBean.editAssoc}" >
            <f:param name="nodeRef" value="#{r.nodeRef}"/>
         </a:actionLink>
      </a:column>
      <a:column id="docTypeId" >
         <f:facet name="header">
            <a:outputText value="#{msg.docType_assocsList_docTypeName}" />
         </f:facet>
         <h:outputText value="#{r.docType}"/>
      </a:column>

<%-- TODO DLSeadist CL 166386
      <a:column id="actionsCol" actions="true">
         <a:booleanEvaluator value="#{r.removableFromList}">
            <a:actionLink id="actionsCol-del" value="" actionListener="#{DialogManager.bean.fieldsListBean.removeMetaField}"
             showLink="false" image="/images/icons/delete.gif" tooltip="#{msg.docType_assocsList_action_remove}" styleClass="remove_#{r.type}" >
                  <f:param name="nodeRef" value="#{r.nodeRef}"/>
            </a:actionLink>
         </a:booleanEvaluator>
      </a:column>
 --%>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pagerAssocs" styleClass="pager" />
   </a:richList>
</a:panel>
