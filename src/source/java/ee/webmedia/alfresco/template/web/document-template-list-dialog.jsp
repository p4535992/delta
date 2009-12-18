<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="document-template-panel" styleClass="panel-100" label="#{msg.templates}" progressive="true">

   <%-- Spaces List --%>
   <a:richList id="registersList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" styleClass="recordSet" headerStyleClass="recordSetHeader"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.templates}" var="r" 
      initialSortColumn="name">
      
      <%-- Primary column for the name --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.template_name_doc}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <a:actionLink id="col1-act1" value="#{r.name}" tooltip="#{msg.template_download}" image="/images/icons/template.gif" showLink="false" href="#{r.downloadUrl}" />
         </f:facet>
         <a:actionLink id="col1-act2" value="#{r.name}" tooltip="#{msg.template_download}" href="#{r.downloadUrl}" />
      </a:column>

      <%-- Comment column --%>
      <a:column id="col2">
         <f:facet name="header">
            <h:outputText id="col2-txt" value="#{msg.template_comment}" />
         </f:facet>
         <h:outputText value="#{r.comment}" />
      </a:column>
      
      <%-- DocTypeId column --%>
      <a:column id="col3">
         <f:facet name="header">
            <h:outputText id="col3-txt" value="#{msg.template_document_type}" />
         </f:facet>
         <h:outputText id="docType-name" value="#{r.docTypeName}" />
      </a:column>
      
      <%-- Actions column --%>
      <a:column id="col4" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="col4-txt" value="#{msg.template_delete}" />
         </f:facet>
         <a:actionLink id="col7-act" value="#{msg.template_delete}" tooltip="#{msg.template_delete}" actionListener="#{BrowseBean.setupContentAction}" action="dialog:deleteFile" showLink="false"
            image="/images/icons/delete.gif">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
      </a:column>

      
   </a:richList>
  
</a:panel>