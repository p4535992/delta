<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="docsearch-panel-facets">
   <f:facet name="title">
      <a:actionLink id="close-docSearch" actionListener="#{DocumentDynamicDialog.hideSearchBlock}" value="#{msg.document_assocSearch_close}" image="/images/icons/close_panel.gif" showLink="false"/>
   </f:facet>
</h:panelGroup>

<a:panel label="#{SearchBlockBean.searchBlockTitle}" id="docsearch-panel" styleClass="panel-100" progressive="true"
      facetsId="dialog:dialog-body:docsearch-panel-facets">
   <h:panelGrid width="100%" id="docsearch-panelGrid" columns="1">
      <a:panel id="docsearch-button">
         <f:verbatim>
            <script type="text/javascript">
               function _ifenter2(event) { if (event && event.keyCode == 13) {$jQ('#docsearch-button.panel input[id$=quickSearchBtn2]').click();return false;} else {return true;} }
            </script>
         </f:verbatim>
         <h:panelGrid id="docsearch-button-panelgrid" styleClass="docSearch-inputs" columns="2" columnClasses="propertiesLabel" >
            <h:outputText id="text1" value="#{msg.document_search_input} " />
            <h:inputText id="docsearchField" value="#{SearchBlockBean.searchValue}" maxlength="50" onkeypress="return _ifenter2(event)" />
            <h:outputText id="text2" value="#{msg.document_regDateTime2}" />
            <h:outputText id="text3">
               <h:outputText id="text4" value="#{msg.from} " />
               <h:inputText id="regDateTimeBegin" value="#{SearchBlockBean.regDateTimeBegin}" styleClass="date">
                  <f:converter converterId="ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter" />
               </h:inputText>
               <h:outputText id="text5" value=" #{msg.to} " />
               <h:inputText id="regDateTimeEnd" value="#{SearchBlockBean.regDateTimeEnd}" styleClass="date">
                  <f:converter converterId="ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter" />
               </h:inputText>
            </h:outputText>
            <h:outputText id="text6" value="#{msg.document_docType}" />
            <h:selectManyListbox value="#{SearchBlockBean.selectedDocumentTypes}" size="5" >
               <f:selectItems value="#{DocumentSearchBean.documentTypes}" />
            </h:selectManyListbox>
            <h:commandButton id="quickSearchBtn2" value="#{msg.search}" type="submit" actionListener="#{SearchBlockBean.setup}" action="#docsearch-panel" />
         </h:panelGrid>
      </a:panel>

      <a:panel id="docsearch-results-panel" styleClass="panel-100 with-pager" label="#{msg.search}" progressive="true" rendered="#{SearchBlockBean.count > 0}">
         <a:richList id="search-documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
            altRowStyleClass="recordSetRowAlt" width="100%" value="#{SearchBlockBean.documents}" var="r" refreshOnBind="true" >

            <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog-columns.jsp" />

            <%-- iconLink to document details --%>
            <a:column id="col-actions" actions="true" styleClass="actions-column2 #{r.cssStyleClass}">
               <a:actionLink id="col-actions-act1" value="#{r.docName}" image="/images/icons/document-followup.png" showLink="false" action="#assocs-block-panel"
                  actionListener="#{DocumentDynamicDialog.addFollowUpHandler}" tooltip="#{msg.document_search_add_followup}" rendered="#{not DocumentDynamicDialog.showDocsAndCasesAssocs}" >
                  <f:param name="nodeRef" value="#{r.node.nodeRef}" />
               </a:actionLink>
               <a:actionLink id="col-actions-act2" value="#{r.docName}" image="/images/icons/document-reply.png" showLink="false" action="#assocs-block-panel"
                  actionListener="#{DocumentDynamicDialog.addReplyHandler}" tooltip="#{msg.document_search_add_reply}" rendered="#{not DocumentDynamicDialog.showDocsAndCasesAssocs}" >
                  <f:param name="nodeRef" value="#{r.node.nodeRef}" />
               </a:actionLink>
<%-- Start assocs that must be saved when clicked --%>               
               <a:actionLink id="col-actions-addAssocDoc" value="#{r.docName}" image="/images/icons/import.png" showLink="false" action="#assocs-block-panel"
                  actionListener="#{SearchBlockBean.addAssocDocHandler}" tooltip="#{msg.document_assocAdd}" rendered="#{(r.cssStyleClass != 'case') and ('true' == DocumentDynamicDialog.showDocsAndCasesAssocs)}" >
                  <f:param name="nodeRef" value="#{r.node.nodeRef}" />
               </a:actionLink>

               <a:actionLink id="col-actions-addAssocDoc2Case" value="#{r.docName}" image="/images/icons/import.png" showLink="false" action="#assocs-block-panel"
                  actionListener="#{SearchBlockBean.addAssocDoc2CaseHandler}" tooltip="#{msg.document_assocAdd}" rendered="#{(DocumentDynamicDialog.showDocsAndCasesAssocs == 'true') and (r.cssStyleClass == 'case')}" >
                  <f:param name="nodeRef" value="#{r.node.nodeRef}" />
               </a:actionLink>
<%-- End assocs that must be saved when clicked --%>               
               
               <a:actionLink id="col-actions-act3" value="#{r.docName}" image="/images/icons/document-attachment.png" showLink="false" action="#assocs-block-panel"
                  actionListener="#{DocumentDynamicDialog.addFilesHandler}" tooltip="#{msg.document_search_add_files}" rendered="#{(not DocumentDynamicDialog.showDocsAndCasesAssocs) and (not DocumentDynamicDialog.document.incomingInvoice)}" >
                  <f:param name="nodeRef" value="#{r.node.nodeRef}" />
               </a:actionLink>
            </a:column>

            <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
            <a:dataPager id="pager1" styleClass="pager" />
         </a:richList>
      </a:panel>

   </h:panelGrid>
</a:panel>
