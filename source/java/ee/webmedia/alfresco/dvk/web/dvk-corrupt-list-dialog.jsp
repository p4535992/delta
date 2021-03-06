<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel label="#{msg.dvkCorrupt_title}" id="files-panel" facetsId="dialog:dialog-body:files-panel-facets" styleClass="panel-100" progressive="true">

   <a:richList id="dvkCorruptList" viewMode="details" value="#{DvkCorruptListDialog.files}" var="r" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

<!-- 
      NB! all columns here(name, date, remove) are exact copies from file-block bean, so this place could be refactored 
 -->

      <%-- Name with URL link column --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <h:outputText id="col1-header" value="#{msg.imap_file_name}" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <h:panelGroup>
               <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
                  <a:actionLink id="col1-act1" value="#{r.name}" href="#{r.downloadUrl}" target="_blank" image="#{r.fileType16}" showLink="false"
                     styleClass="inlineAction" />
               </r:permissionEvaluator>
               <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
                  <h:graphicImage value="#{r.fileType16}" />
               </r:permissionEvaluator>
            </h:panelGroup>
         </f:facet>
         <r:permissionEvaluator value="#{r.node}" allow="viewDocumentFiles">
            <a:actionLink id="col1-act2" value="#{r.name}" href="#{r.downloadUrl}" target="_blank"/>
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
               styleClass="inlineAction no-underline" rendered="#{r.digiDocItem == false}" />
          </r:permissionEvaluator>
          <r:permissionEvaluator value="#{r.node}" deny="viewDocumentFiles">
          	 <h:outputText id="col3-txt" value="#{r.created}" rendered="#{r.digiDocItem == false}">
            	<a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         	</h:outputText>
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
</a:panel>