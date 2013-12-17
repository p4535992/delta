<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/imap/web/folder-list-dialog.jsp" />

<a:panel id="document-panel" styleClass="panel-100 with-pager" label="#{DialogManager.bean.listTitle}" progressive="true" rendered="#{DialogManager.bean.showFileList}">   

   <%-- Document List --%>
   <a:richList id="email-documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.documents}" var="r" refreshOnBind="true" rendered="#{DialogManager.bean.showFileList}">

      <%-- Sender/owner, in incoming email list only --%>
      <a:column id="col4" primary="true" styleClass="#{r.cssStyleClass}" rendered="#{DialogManager.bean == IncomingEmailListDialog}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_sender}" value="sender" styleClass="header" />
         </f:facet>
         <a:actionLink id="col4-text" value="#{r.senderNameOrEmail}" action="#{DocumentDialog.action}" tooltip="#{r.sender}"
          actionListener="#{DocumentDialog.open}" styleClass="tooltip no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
      </a:column>
      
      <%-- All Recipients, in sent email list only --%>
      <a:column id="col5" primary="true" styleClass="#{r.cssStyleClass}" rendered="#{DialogManager.bean == SentEmailListDialog}" >
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.document_recipients}" value="recipients" styleClass="header" />
         </f:facet>
         <a:actionLink id="col5-text" value="#{r.recipients}" action="#{DocumentDialog.action}" tooltip="#{r.recipients}"
          actionListener="#{DocumentDialog.open}" styleClass="tooltip condence50- no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
           </a:actionLink>
      </a:column>      
      
      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text-1" value="#{r.docName}" action="#{DocumentDialog.action}" tooltip="#{r.docName}"
            actionListener="#{DocumentDialog.open}" styleClass="tooltip no-underline" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="column-ownerName" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="column-ownerName-sort" label="#{msg.imap_ownerName}" value="ownerName" styleClass="header" />
         </f:facet>
         <a:actionLink id="column-ownerName-text" value="#{r.ownerName}" action="#{DocumentDialog.action}" tooltip="#{r.ownerName}" styleClass="no-underline tooltip condence20-"
            actionListener="#{DocumentDialog.open}">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Files --%>
      <a:column id="col10" primary="true" styleClass="doc-list-actions">
         <f:facet name="header">
            <h:outputText id="col10-header" value="#{msg.document_allFiles}" styleClass="header" />
         </f:facet>
         <wm:customChildrenContainer childGenerator="#{DialogManager.bean.documentRowFileGenerator}" parameterList="#{r.files}"/>
      </a:column>
         
      <%-- Actions column --%>
      <a:column id="act-col" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="act-col-txt" value="" />
         </f:facet>
         <a:actionLink id="act-col-act1" value="#{msg.delete}" actionListener="#{DeleteDialog.setupDeleteDialog}" action="dialog:deleteDialog" showLink="false"
            image="/images/icons/delete.gif">
            <f:param name="nodeRef" value="#{r.node.nodeRef}"/>
            <f:param name="confirmMessagePlaceholder0" value="#{r.docName}"/>
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager2" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
