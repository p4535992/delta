<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel label="#{msg.scanned_folders_title}" id="files-panel" facetsId="dialog:dialog-body:files-panel-facets" styleClass="panel-100" progressive="true">

   <a:richList id="scannedFoldersList" viewMode="details" value="#{DialogManager.bean.files}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

      <%-- Name with URL link column --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <h:outputText id="col1-header" value="#{msg.imap_file_name}" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:panelGroup>
               <r:permissionEvaluator value="#{r.node}" allow="ReadContent">
                  <a:actionLink id="folderName" value="#{r.name} (#{r.nrOfChildren})" action="dialog:scannedDocsListDialog" tooltip="#{msg.scanned_folder_tooltip}"
                     showLink="false" actionListener="#{ScannedDocsListDialog.init}">
                     <f:param name="nodeRef" value="#{r.node.nodeRef}" />
                  </a:actionLink>
               </r:permissionEvaluator>

            </h:panelGroup>
         </f:facet>
      </a:column>

      <%-- Created Date column --%>
      <a:column id="col3">
         <f:facet name="header">
            <h:outputText id="col3-header" value="#{msg.folder_added_time}" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-txt" value="#{r.created}" rendered="#{r.digiDocItem == false}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

   </a:richList>
</a:panel>