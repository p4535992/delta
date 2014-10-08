<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/imap/web/folder-list-dialog.jsp" />

<a:panel label="#{msg.file_title}" id="files-panel" facetsId="dialog:dialog-body:files-panel-facets" styleClass="panel-100" progressive="true" rendered="#{DialogManager.bean.showFileList}">

   <a:richList id="filelistList" viewMode="details" value="#{DialogManager.bean.files}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true" rendered="#{DialogManager.bean.showFileList}">

      <%-- Name with URL link column --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <h:outputText id="col1-header" value="#{msg.imap_file_name}" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:panelGroup>
               <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
                  <a:actionLink id="col1-act1" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction no-underline" />
               </r:permissionEvaluator>
               <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
                  <h:graphicImage value="#{r.fileType16}" />
               </r:permissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
            <a:actionLink id="col1-act2" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" styleClass="inlineAction no-underline" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
            <h:outputText value="#{r.name}" />
         </r:permissionEvaluator>
      </a:column>

      <%-- Created Date column --%>
      <a:column id="col3">
         <f:facet name="header">
            <h:outputText id="col3-header" value="#{msg.file_added_time}" styleClass="header" />
         </f:facet>
               <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
                  <a:actionLink id="col3-act1" value="#{r.createdTimeStr}" href="#{r.downloadUrl}" target="_blank" showLink="false"
                     styleClass="inlineAction no-underline" />
               </r:permissionEvaluator>
               <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
               		<h:outputText id="col3-txt" value="#{r.created}" rendered="#{r.digiDocItem == false}">
            			<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
			         </h:outputText>
               </r:permissionEvaluator>
      </a:column>
      
      <%-- Owner name column. Rendered in AttachmentListDialog only--%>
      <a:column id="column-ownerName" rendered="#{DialogManager.bean == AttachmentListDialog}" >
         <f:facet name="header">
            <h:outputText id="column-ownerName-header" value="#{msg.imap_ownerName}" styleClass="header" />
         </f:facet>
               <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
                  <a:actionLink id="column-ownerName-act1" value="#{r.creator}" href="#{r.downloadUrl}" target="_blank" showLink="false"
                     styleClass="inlineAction no-underline" />
               </r:permissionEvaluator>
               <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
               		<h:outputText id="column-ownerName-txt" value="#{r.creator}" />
               </r:permissionEvaluator>
      </a:column>

      <%-- Remove column --%>
      <a:column id="col7">
         <r:permissionEvaluator value="#{r.node}" allow="editDocument">
            <a:actionLink id="col7-act" value="#{r.name}" actionListener="#{DeleteDialog.setupDeleteDialog}" action="dialog:deleteDialog" showLink="false"
               image="/images/icons/delete.gif" tooltip="#{msg.file_remove}">
               <f:param name="nodeRef" value="#{r.nodeRef}"/>
               <f:param name="confirmMessagePlaceholder0" value="#{r.name}"/>
            </a:actionLink>
         </r:permissionEvaluator>
      </a:column>

   </a:richList>
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/imap/web/folder-list-dialog.jsp" />

<a:panel label="#{msg.file_title}" id="files-panel" facetsId="dialog:dialog-body:files-panel-facets" styleClass="panel-100" progressive="true" rendered="#{DialogManager.bean.showFileList}">

   <a:richList id="filelistList" viewMode="details" value="#{DialogManager.bean.files}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true" rendered="#{DialogManager.bean.showFileList}">

      <%-- Name with URL link column --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <h:outputText id="col1-header" value="#{msg.imap_file_name}" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:panelGroup>
               <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
                  <a:actionLink id="col1-act1" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction no-underline" />
               </r:permissionEvaluator>
               <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
                  <h:graphicImage value="#{r.fileType16}" />
               </r:permissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
            <a:actionLink id="col1-act2" value="#{r.displayName}" href="#{r.downloadUrl}" target="_blank" styleClass="inlineAction no-underline" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
            <h:outputText value="#{r.name}" />
         </r:permissionEvaluator>
      </a:column>

      <%-- Created Date column --%>
      <a:column id="col3">
         <f:facet name="header">
            <h:outputText id="col3-header" value="#{msg.file_added_time}" styleClass="header" />
         </f:facet>
               <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
                  <a:actionLink id="col3-act1" value="#{r.createdTimeStr}" href="#{r.downloadUrl}" target="_blank" showLink="false"
                     styleClass="inlineAction no-underline" />
               </r:permissionEvaluator>
               <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
               		<h:outputText id="col3-txt" value="#{r.created}" rendered="#{r.digiDocItem == false}">
            			<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
			         </h:outputText>
               </r:permissionEvaluator>
      </a:column>
      
      <%-- Owner name column. Rendered in AttachmentListDialog only--%>
      <a:column id="column-ownerName" rendered="#{DialogManager.bean == AttachmentListDialog}" >
         <f:facet name="header">
            <h:outputText id="column-ownerName-header" value="#{msg.imap_ownerName}" styleClass="header" />
         </f:facet>
               <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
                  <a:actionLink id="column-ownerName-act1" value="#{r.creator}" href="#{r.downloadUrl}" target="_blank" showLink="false"
                     styleClass="inlineAction no-underline" />
               </r:permissionEvaluator>
               <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
               		<h:outputText id="column-ownerName-txt" value="#{r.creator}" />
               </r:permissionEvaluator>
      </a:column>

      <%-- Remove column --%>
      <a:column id="col7">
         <r:permissionEvaluator value="#{r.node}" allow="editDocument">
            <a:actionLink id="col7-act" value="#{r.name}" actionListener="#{DeleteDialog.setupDeleteDialog}" action="dialog:deleteDialog" showLink="false"
               image="/images/icons/delete.gif" tooltip="#{msg.file_remove}">
               <f:param name="nodeRef" value="#{r.nodeRef}"/>
               <f:param name="confirmMessagePlaceholder0" value="#{r.name}"/>
            </a:actionLink>
         </r:permissionEvaluator>
      </a:column>

   </a:richList>
>>>>>>> develop-5.1
</a:panel>