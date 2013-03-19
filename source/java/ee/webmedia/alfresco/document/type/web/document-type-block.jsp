<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel label="#{msg.document_type_block}" id="types-panel" styleClass="panel-100" progressive="true" rendered="#{DocumentDynamicDialog.showTypeBlock}">
   <h:panelGrid id="types-grid" columns="2" columnClasses="propertiesLabel">
      <h:outputText id="type-txt" value="#{msg.document_docType}" />
      <a:panel id="submit-doc-type">
         <h:selectOneMenu id="doc-types-select" value="#{DocumentDynamicDialog.documentType.id}" styleClass="#{DocumentDynamicDialog.onChangeStyleClass}">
            <f:selectItems id="doc-types-select-items" value="#{DocumentDynamicDialog.documentTypeListItems}" />
         </h:selectOneMenu>
         <a:actionLink id="submit-doc-type-link" value="" actionListener="#{DocumentDynamicDialog.selectedDocumentTypeChanged}" styleClass="hidden" />
      </a:panel>
   </h:panelGrid>
</a:panel>
