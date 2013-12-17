<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="thesaurus-import-file-panel" styleClass="panel-100" label="#{msg.thesaurus_choose_file}" progressive="true" rendered="#{empty ThesaurusImportDialog.fileName}">
   <h:panelGrid id="upload_panel" columns="2" cellpadding="2" cellspacing="2" border="0" width="100%"
      columnClasses="panelGridLabelColumn,panelGridValueColumn,panelGridRequiredImageColumn">
      <h:outputText id="out_schema" value="#{msg.file_location}:" style="padding-left:8px" />
      <r:upload id="uploader" value="#{ThesaurusImportDialog.fileName}" framework="dialog" />
   </h:panelGrid>
</a:panel>

<a:panel id="thesaurus-import-review-panel" styleClass="panel-100" label="#{msg.thesaurus_import_review_list}" progressive="true" rendered="#{not empty ThesaurusImportDialog.fileName}">
   <h:dataTable id="thesaurusImportList" value="#{ThesaurusImportDialog.importableThesauri}" var="r" width="100%" rowClasses="recordSetRow,recordSetRowAlt">
      <h:column id="name">
         <f:facet name="header">
            <h:outputText id="name-header" value="#{msg.thesaurus_name}" />
         </f:facet>
         <h:outputText value="#{r.first}" />
      </h:column>

      <h:column>
         <f:facet name="header">
            <h:outputText value="#{msg.thesaurus_import_changes}" />
         </f:facet>
         <h:outputText value="#{msg[r.second]}" />
      </h:column>
   </h:dataTable>
</a:panel>

<a:panel styleClass="panel-100" id="thesaurus-import-file" label="#{msg.thesaurus_import_file}" rendered="#{not empty ThesaurusImportDialog.fileName}">
   <h:panelGroup>
      <a:actionLink image="/images/icons/delete.gif" value="#{msg.remove}" action="#{ThesaurusImportDialog.reset}" showLink="false" id="link1" />
      <h:outputText id="text3" value="#{ThesaurusImportDialog.fileName}" styleClass="dialogpanel-title filename" />
   </h:panelGroup>
</a:panel>
