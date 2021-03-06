<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/imap/web/folder-list-dialog.jsp" />

<a:panel label="#{msg.file_title}" id="files-panel" facetsId="dialog:dialog-body:files-panel-facets" styleClass="panel-100" progressive="true">

   <a:richList id="filelistList" viewMode="details" value="#{DialogManager.bean.files}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true" rendered="#{DialogManager.bean.showFileList}">

      <%-- Name with URL link column --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <h:outputText id="col1-header" value="#{msg.imap_file_name}" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:panelGroup>
               <wm:docPermissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
                  <a:actionLink id="col1-act1" value="#{r.name}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction" />
               </wm:docPermissionEvaluator>
               <wm:docPermissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
                  <h:graphicImage value="#{r.fileType16}" />
               </wm:docPermissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <wm:docPermissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
            <a:actionLink id="col1-act2" value="#{r.name}" href="#{r.downloadUrl}" target="_blank"/>
         </wm:docPermissionEvaluator>
         <wm:docPermissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
            <h:outputText value="#{r.name}" />
         </wm:docPermissionEvaluator>
      </a:column>

      <%-- Created Date column --%>
      <a:column id="col3">
         <f:facet name="header">
            <h:outputText id="col3-header" value="#{msg.file_added_time}" styleClass="header" />
         </f:facet>
         	<wm:docPermissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
               <a:actionLink id="col3-act1" value="#{r.createdTimeStr}" href="#{r.downloadUrl}" target="_blank" showLink="false"
                  styleClass="inlineAction no-underline" />
            </wm:docPermissionEvaluator>
            <wm:docPermissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
               <h:outputText id="col3-txt" value="#{r.created}" rendered="#{r.digiDocItem == false}">
           			<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         		</h:outputText>
             </wm:docPermissionEvaluator>
      </a:column>

      <%-- Remove column --%>
      <a:column id="col7">
         <a:actionLink id="col7-act" value="#{r.name}" actionListener="#{DeleteDialog.setupDeleteDialog}" action="dialog:deleteDialog" showLink="false"
            image="/images/icons/delete.gif" tooltip="#{msg.file_remove}" rendered="#{UserService.documentManager}" >
            <f:param name="nodeRef" value="#{r.nodeRef}"/>
            <f:param name="confirmMessagePlaceholder0" value="#{r.name}"/>
         </a:actionLink>
      </a:column>

   </a:richList>
</a:panel>