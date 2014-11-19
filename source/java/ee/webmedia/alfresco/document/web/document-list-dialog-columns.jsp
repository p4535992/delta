<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
      
      <%-- regNumber --%>
      <a:column id="col1" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
         <a:actionLink id="col1-text1" value="#{r.akString}" style="font-weight: bold;" action="#{r.getAction}" tooltip="#{r.regNumber}"
            actionListener="#{r.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col1-text2" value="#{r.regNumber}" action="#{r.getAction}" tooltip="#{r.regNumber}"
            actionListener="#{r.open}" >
=======
         <a:actionLink id="col1-text" value="#{r.regNumber}" action="#{DocumentDialog.action}" tooltip="#{r.regNumber}"
            actionListener="#{DocumentDialog.open}" >
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Registration date --%>
      <a:column id="col2" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
         <a:actionLink id="col2-text" value="#{r.regDateTimeStr}" action="#{r.getAction}" tooltip="#{r.regDateTimeStr}" styleClass="no-underline"
            actionListener="#{r.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text-1" value="#{r.docName}" action="#{r.getAction}" tooltip="#{r.docName}"
            actionListener="#{r.open}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <!-- TODO if row item is not document, but case, create link to documents list of case (row item is subclass of Document, to be shown as listItem in document list) -->
      </a:column>

      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_type}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text" value="#{r.documentTypeName}" action="#{r.getAction}" tooltip="#{r.documentTypeName}"
          actionListener="#{r.open}" styleClass="no-underline" >
=======
         <a:actionLink id="col2-text" value="#{r.regDateTimeStr}" action="#{DocumentDialog.action}" tooltip="#{r.regDateTimeStr}" styleClass="no-underline"
          actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
      </a:column>
      
      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col3-text" value="#{r.documentTypeName}" action="#{DocumentDialog.action}" tooltip="#{r.documentTypeName}"
          actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
      </a:column>
      
<<<<<<< HEAD
      <%-- Volume --%>
      <a:column id="col3_2" primary="true" styleClass="#{r.cssStyleClass}" rendered="#{!(DialogManager.bean == CaseDocumentListDialog || DialogManager.bean == CaseFileDialog) && SearchBlockBean.documentVolumeColumnVisible}">
         <f:facet name="header">
            <a:sortLink id="col3_2-sort" label="#{msg.volume}" value="volume" styleClass="header"/>
         </f:facet>
         <a:actionLink id="col3_2-link2cases" value="#{r.documentVolume.volumeMarkAndTitle}" action="dialog:caseDocListDialog" tooltip="#{r.documentVolume.volumeMarkAndTitle}"
            showLink="false" actionListener="#{CaseDocumentListDialog.showAll}" rendered="#{r.documentVolume != null && !r.documentVolume.dynamic}" styleClass="tooltip condence20- no-underline" >
            <f:param name="volumeNodeRef" value="#{r.documentVolume.node.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col3_2-caseFile" value="#{r.documentVolume.volumeMarkAndTitle}" tooltip="#{r.documentVolume.volumeMarkAndTitle}"
            showLink="false" actionListener="#{CaseFileDialog.openFromDocumentList}" rendered="#{r.documentVolume != null && r.documentVolume.dynamic}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.documentVolume.node.nodeRef}" />
         </a:actionLink>          
      </a:column>

      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender_recipient}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.sender}" action="#{r.getAction}" tooltip="#{r.sender}"
          actionListener="#{r.open}" styleClass="tooltip condence20- no-underline" >
=======
      <%-- Sender/owner --%>
      <a:column id="col4" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.sender}" action="#{DocumentDialog.action}" tooltip="#{r.sender}"
          actionListener="#{DocumentDialog.open}" styleClass="tooltip condence20- no-underline" >
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
      </a:column>
      
      <% 
         Boolean showOrgStructColumn = false;
         String showOrgStructStr = request.getParameter("showOrgStructColumn");
         if(showOrgStructStr != null){
             showOrgStructColumn = Boolean.parseBoolean(showOrgStructStr);
         }
         String showOrgStructColumnStr = showOrgStructColumn ? "true" : "false";
      %>
      <%-- Organization structure --%>
      <a:column id="col4_1" primary="true" styleClass="#{r.cssStyleClass}" rendered="<%=showOrgStructColumnStr%>" >
         <f:facet name="header">
            <a:sortLink id="col4_1-sort" label="#{msg.document_ownerStructUnit}" value="ownerOrgStructUnit" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
        	<a:actionLink id="col4_1-text-1" value="#{r.ownerOrgStructUnit}" action="#{r.getAction}" tooltip="#{r.ownerOrgStructUnit}"
          actionListener="#{r.open}" styleClass="tooltip condence50- no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
           </a:actionLink>
      </a:column>      

      <%-- Owner --%>      
      <a:column id="col4_2" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col4_2-sort" label="#{msg.document_owner}" value="ownerName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4_2-text" value="#{r.ownerName}" action="#{r.getAction}" tooltip="#{r.ownerName}"
            actionListener="#{r.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
=======
        	<a:actionLink id="col4_1-text-1" value="#{r.ownerOrgStructUnit}" action="#{DocumentDialog.action}" tooltip="#{r.ownerOrgStructUnit}"
          actionListener="#{DocumentDialog.open}" styleClass="tooltip condence50- no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
           </a:actionLink>
      </a:column>      
      
      <%-- All Recipients --%>
      <a:column id="col5" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.document_allRecipients}" value="allRecipients" styleClass="header" />
         </f:facet>
        	<a:actionLink id="col5-text" value="#{r.allRecipients}" action="#{DocumentDialog.action}" tooltip="#{r.allRecipients}"
          actionListener="#{DocumentDialog.open}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
           </a:actionLink>
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text-1" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
            actionListener="#{DocumentDialog.open}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
         <!-- if row item is not document, but case, create link to documents list of case (row item is subclass of Document, to be shown as listItem in document list) -->
      </a:column>

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
      <%-- DueDate --%>
      <a:column id="col7" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_dueDate}" value="dueDate" styleClass="header" />
         </f:facet>
<<<<<<< HEAD
         <a:actionLink id="col7-text" value="#{r.dueDateStr}" action="#{r.getAction}" tooltip="#{r.dueDateStr}"
            actionListener="#{r.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <% 
         Boolean showComplienceDateColumn = false;
         String showComplienceDateStr = request.getParameter("showComplienceDateColumn");
         if(showComplienceDateStr != null){
             showComplienceDateColumn = Boolean.parseBoolean(showComplienceDateStr);
         } else {
             showComplienceDateColumn = Boolean.TRUE;
         }
         String showComplienceDateColumnStr = showComplienceDateColumn ? "true" : "false";
      %>      

      <%-- Complience Date --%>
      <a:column id="col9" primary="true" styleClass="#{r.cssStyleClass}" rendered="<%=showComplienceDateColumnStr%>" >
         <f:facet name="header">
            <a:sortLink id="col9-sort" label="#{msg.document_complienceDate}" value="complienceDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col9-text" value="#{r.complienceDateStr}" action="#{r.getAction}" tooltip="#{r.complienceDateStr}"
            actionListener="#{r.open}" styleClass="no-underline" >
=======
         <a:actionLink id="col7-text" value="#{r.dueDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.dueDateStr}"
            actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Complience Date --%>
      <a:column id="col9" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col9-sort" label="#{msg.document_complienceDate}" value="complienceDate" styleClass="header" />
         </f:facet>
         <a:actionLink id="col9-text" value="#{r.complienceDateStr}" action="#{DocumentDialog.action}" tooltip="#{r.complienceDateStr}"
            actionListener="#{DocumentDialog.open}" styleClass="no-underline" >
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Files --%>
      <a:column id="col10" primary="true" styleClass="doc-list-actions">
         <f:facet name="header">
            <h:outputText id="col10-header" value="#{msg.document_allFiles}" styleClass="header" />
         </f:facet>
          <f:facet name="csvExport">
              <a:param value="false"/>
          </f:facet>
<<<<<<< HEAD
          <wm:customChildrenContainer id="document-list-files" childGenerator="#{DocumentListDialog.documentRowFileGenerator}" parameterList="#{r.files}"/>
=======
          <wm:customChildrenContainer childGenerator="#{DocumentListDialog.documentRowFileGenerator}" parameterList="#{r.files}"/>
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
      </a:column>
