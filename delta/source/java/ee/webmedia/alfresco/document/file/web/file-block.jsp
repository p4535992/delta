<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="files-panel-facets">
   <f:facet name="title">
      <r:actions id="acts_add_content" value="addFileMenu" context="#{DialogManager.bean.node}" showLink="false"/>
   </f:facet>
</h:panelGroup>

<a:panel label="#{msg.file_title}" id="files-panel" facetsId="dialog:dialog-body:files-panel-facets" styleClass="panel-100" progressive="true">

   <a:richList id="filelistList" viewMode="details" value="#{DialogManager.bean.file.files}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">
      
      <%-- Name with URL link column --%>
      <a:column id="col1" primary="true" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col1-header" value="#{msg.file_name}" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:panelGroup>
               <r:permissionEvaluator value="#{r.node}" allow="ReadContent">
                  <a:actionLink id="col1-act1" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction webdav-open" />
               </r:permissionEvaluator>
               <r:permissionEvaluator value="#{r.node}" deny="ReadContent">
                  <h:graphicImage value="#{r.fileType16}" />
               </r:permissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <r:permissionEvaluator value="#{r.node}" allow="ReadContent">
            <a:actionLink id="col1-act2" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" styleClass="webdav-open" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.node}" deny="ReadContent">
            <h:outputText value="#{r.displayName}" />
         </r:permissionEvaluator>
      </a:column>
      
      <%-- Created By column --%>
      <a:column id="col2" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col2-header" value="#{msg.file_added_by}" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-txt" value="#{r.creator}" />
      </a:column>
      
      <%-- Created Date column --%>
      <a:column id="col3" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col3-header" value="#{msg.file_added_time}" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.created}" rendered="#{r.digiDocItem == false}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Modified By column --%>
      <a:column id="col4" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col4-header" value="#{msg.file_modified_by}" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-txt" value="#{r.modifier}" rendered="#{r.digiDocItem == false}" />
      </a:column>

      <%-- Modified Date column --%>
      <a:column id="col5" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col5-header" value="#{msg.file_modified_time}" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-txt" value="#{r.modified}" rendered="#{r.digiDocItem == false}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Size --%>
      <a:column id="col6" rendered="#{r.activeAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col6-header" value="#{msg.file_size}" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-txt" value="#{r.size}" rendered="#{r.digiDocItem == false}">
            <a:convertSize />
         </h:outputText>
      </a:column>

      <%-- Remove and Version column --%>
      <a:column id="col7" rendered="#{r.activeAndNotDigiDoc}">
         <r:permissionEvaluator value="#{r.node}" allow="WriteProperties">
            <a:actionLink id="col7-act3" value="#{r.name}" actionListener="#{FileBlockBean.toggleActive}" showLink="false"
               image="/images/icons/document-convert.png" tooltip="#{msg.file_toggle_deactive} " rendered="#{!DocumentDialog.notEditable}">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.node}" allow="ReadContent">
            <a:actionLink id="col7-act2" value="#{r.name}" actionListener="#{VersionsListDialog.select}" action="dialog:versionsListDialog" showLink="false"
               image="/images/icons/version_history.gif" rendered="#{r.versionable && !DocumentDialog.notEditable}" tooltip="#{msg.file_version_history}">
               <f:param name="fileName" value="#{r.name}" />
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.node}" allow="DeleteNode">
            <a:actionLink id="col7-act" value="#{r.name}" actionListener="#{BrowseBean.setupContentAction}" action="dialog:deleteFile" showLink="false"
               image="/images/icons/delete.gif" tooltip="#{msg.file_remove}" rendered="#{UserService.documentManager && !DocumentDialog.inprogressCompoundWorkflows && !DocumentDialog.notEditable}">
               <f:param name="id" value="#{r.id}" />
               <f:param name="ref" value="#{r.nodeRef}" />
            </a:actionLink>
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.node}" allow="WriteContent">
            <a:actionLink id="col7-act4" value="#{r.name}" actionListener="#{FileBlockBean.transformToPdf}" showLink="false"
               image="/images/filetypes/pdf.gif" tooltip="#{msg.file_generate_pdf}" rendered="#{r.transformableToPdf && !DocumentDialog.notEditable}">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </r:permissionEvaluator>
      </a:column>
      
      <a:column id="col1-ddoc" primary="true" rendered="#{r.activeDigiDoc}">
         <a:panel id="ddoc-inner-panel" styleClass="digidoc-panel">
            <h:dataTable id="ddocList" value="#{r.dataItems}" var="v">
               <h:column id="col1-inner">
                  <r:permissionEvaluator value="#{r.node}" allow="ReadContent">
                     <a:actionLink id="inner-act1" value="#{v.displayName}" tooltip="#{v.name}" href="#{v.downloadUrl}" target="_blank" image="#{v.fileType16}" showLink="false" styleClass="inlineAction" />
                     <a:actionLink id="inner-act2" value="#{v.displayName}" tooltip="#{v.name}" href="#{v.downloadUrl}" target="_blank" />
                  </r:permissionEvaluator>
                  <r:permissionEvaluator value="#{r.node}" deny="ReadContent">
                     <h:graphicImage id="inner-act1-deny" value="#{v.fileType16}" />
                     <h:outputText id="inner-act2-deny" value="#{v.displayName}" title="#{v.name}" />
                  </r:permissionEvaluator>
               </h:column>
            </h:dataTable>
         </a:panel>
      </a:column>

      <a:column id="col2-ddoc" colspan="6" rendered="#{r.activeDigiDoc}">
         <a:panel id="ddoc-inner-panel2" styleClass="digidoc-panel">
            <h:dataTable id="siglistList" value="#{r.signatureItems}" var="v" width="100%">
               <h:column id="col2-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.file_signer}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt1" value="#{v.name}" />
               </h:column>
               <h:column id="col3-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.ddoc_signature_idcode}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt2" value="#{v.legalCode}" />
               </h:column>
               <h:column id="col4-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.ddoc_signature_date}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt3" value="#{v.signingTime}">
                     <a:convertXMLDate type="both" pattern="dd.MM.yyyy" />
                  </h:outputText>
               </h:column>
               <h:column id="col5-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.ddoc_signature_status}" styleClass=" header" />
                  </f:facet>
                  <a:booleanEvaluator id="itemValidEvaluator" value="#{v.valid}">
                     <h:outputText id="inner-txt4" value="#{msg.ddoc_signature_valid}" style="color: green;" />
                  </a:booleanEvaluator>
                  <a:booleanEvaluator id="itemNotValidEvaluator" value="#{v.notValid}">
                     <h:outputText id="inner-txt5" value="#{msg.ddoc_signature_invalid}" style="color: red;" />
                  </a:booleanEvaluator>
               </h:column>
            </h:dataTable>
         </a:panel>
      </a:column>


   </a:richList>
</a:panel>



<a:panel label="#{msg.file_inactive_title}" id="inactive-files-panel" styleClass="panel-100" progressive="true" expanded="false">

   <a:richList id="inactiveFilelistList" viewMode="details" value="#{DialogManager.bean.file.files}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

      <%-- Name with URL link column --%>
      <a:column id="col21" primary="true" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col21-header" value="#{msg.file_name}" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:panelGroup>
               <r:permissionEvaluator value="#{r.node}" allow="ReadContent">
                  <a:actionLink id="col21-act1" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction webdav-open" />
               </r:permissionEvaluator>
               <r:permissionEvaluator value="#{r.node}" deny="ReadContent">
                  <h:graphicImage value="#{r.fileType16}" />
               </r:permissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <r:permissionEvaluator value="#{r.node}" allow="ReadContent">
            <a:actionLink id="col21-act2" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" styleClass="webdav-open" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.node}" deny="ReadContent">
            <h:outputText value="#{r.displayName}" />
         </r:permissionEvaluator>
      </a:column>

      <%-- Created By column --%>
      <a:column id="col22" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col22-header" value="#{msg.file_added_by}" styleClass="header" />
         </f:facet>
         <h:outputText id="col22-txt" value="#{r.creator}" />
      </a:column>
      
      <%-- Created Date column --%>
      <a:column id="col23" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col23-header" value="#{msg.file_added_time}" styleClass="header" />
         </f:facet>
         <h:outputText id="col23-txt" value="#{r.created}" rendered="#{not r.digiDocItem}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Modified By column --%>
      <a:column id="col24" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col24-header" value="#{msg.file_modified_by}" styleClass="header" />
         </f:facet>
         <h:outputText id="col24-txt" value="#{r.modifier}" rendered="#{r.digiDocItem == false}" />
      </a:column>

      <%-- Modified Date column --%>
      <a:column id="col25" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col25-header" value="#{msg.file_modified_time}" styleClass="header" />
         </f:facet>
         <h:outputText id="col25-txt" value="#{r.modified}" rendered="#{r.digiDocItem == false}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Size --%>
      <a:column id="col26" rendered="#{r.notActiveAndNotDigiDoc}">
         <f:facet name="header">
            <h:outputText id="col26-header" value="#{msg.file_size}" styleClass="header" />
         </f:facet>
         <h:outputText id="col26-txt" value="#{r.size}" rendered="#{r.digiDocItem == false}">
            <a:convertSize />
         </h:outputText>
      </a:column>

      <%-- Remove and Version column --%>
      <a:column id="col27" rendered="#{r.notActiveAndNotDigiDoc}">
         <r:permissionEvaluator value="#{r.node}" allow="WriteProperties">
            <a:actionLink id="col27-act3" value="#{r.name}" actionListener="#{FileBlockBean.toggleActive}" showLink="false"
               image="/images/icons/document-convert.png" tooltip="#{msg.file_toggle_active}">
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.node}" allow="ReadContent">
            <a:actionLink id="col27-act2" value="#{r.name}" actionListener="#{VersionsListDialog.select}" action="dialog:versionsListDialog" showLink="false"
               image="/images/icons/version_history.gif" rendered="#{r.versionable}" tooltip="#{msg.file_version_history}">
               <f:param name="fileName" value="#{r.name}" />
               <f:param name="nodeRef" value="#{r.nodeRef}" />
            </a:actionLink>
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.node}" allow="DeleteNode">
            <a:actionLink id="col27-act" value="#{r.name}" actionListener="#{BrowseBean.setupContentAction}" action="dialog:deleteFile" showLink="false"
               image="/images/icons/delete.gif" tooltip="#{msg.file_remove}" rendered="#{UserService.documentManager && !DocumentDialog.inprogressCompoundWorkflows}">
               <f:param name="id" value="#{r.id}" />
               <f:param name="ref" value="#{r.nodeRef}" />
            </a:actionLink>
         </r:permissionEvaluator>
      </a:column>
      
      <a:column id="col21-ddoc" primary="true" rendered="#{r.notActiveAndDigiDoc}">
         <a:panel id="ddoc-inner-panel" styleClass="digidoc-panel">
            <h:dataTable id="ddocList" value="#{r.dataItems}" var="v">
               <h:column id="col21-inner">
                  <r:permissionEvaluator value="#{r.node}" allow="ReadContent">
                     <a:actionLink id="inner-act1" value="#{v.displayName}" tooltip="#{v.name}" href="#{v.downloadUrl}" target="_blank" image="#{v.fileType16}" showLink="false" styleClass="inlineAction" />
                     <a:actionLink id="inner-act2" value="#{v.displayName}" tooltip="#{v.name}" href="#{v.downloadUrl}" target="_blank" />
                  </r:permissionEvaluator>
                  <r:permissionEvaluator value="#{r.node}" deny="ReadContent">
                     <h:graphicImage id="inner-act1-deny" value="#{v.fileType16}" />
                     <h:outputText id="inner-act2-deny" value="#{v.displayName}" title="#{v.name}" />
                  </r:permissionEvaluator>
               </h:column>
            </h:dataTable>
         </a:panel>
      </a:column>

      <a:column id="col22-ddoc" colspan="6" rendered="#{r.notActiveAndDigiDoc}">
         <a:panel id="ddoc-inner-panel2" styleClass="digidoc-panel">
            <h:dataTable id="siglistList" value="#{r.signatureItems}" var="v" width="100%">
               <h:column id="col2-inner">
                  <f:facet name="header">
                     <h:outputText id="col22-inner-header" value="#{msg.file_signer}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt1" value="#{v.name}" />
               </h:column>
               <h:column id="col23-inner">
                  <f:facet name="header">
                     <h:outputText id="col2-inner-header" value="#{msg.ddoc_signature_idcode}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt2" value="#{v.legalCode}" />
               </h:column>
               <h:column id="col24-inner">
                  <f:facet name="header">
                     <h:outputText id="col22-inner-header" value="#{msg.ddoc_signature_date}" styleClass=" header" />
                  </f:facet>
                  <h:outputText id="inner-txt23" value="#{v.signingTime}">
                     <a:convertXMLDate type="both" pattern="dd.MM.yyyy" />
                  </h:outputText>
               </h:column>
               <h:column id="col25-inner">
                  <f:facet name="header">
                     <h:outputText id="col22-inner-header" value="#{msg.ddoc_signature_status}" styleClass=" header" />
                  </f:facet>
                  <a:booleanEvaluator id="itemValidEvaluator" value="#{v.valid}">
                     <h:outputText id="inner-txt4" value="#{msg.ddoc_signature_valid}" style="color: green;" />
                  </a:booleanEvaluator>
                  <a:booleanEvaluator id="itemNotValidEvaluator" value="#{v.notValid}">
                     <h:outputText id="inner-txt5" value="#{msg.ddoc_signature_invalid}" style="color: red;" />
                  </a:booleanEvaluator>
               </h:column>
            </h:dataTable>
         </a:panel>
      </a:column>

   </a:richList>
</a:panel>
