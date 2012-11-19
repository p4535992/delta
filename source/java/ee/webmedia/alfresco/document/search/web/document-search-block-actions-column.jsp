<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- iconLink to document details --%>
<a:column id="col-actions" actions="true" styleClass="actions-column2 #{r.cssStyleClass}">
   <a:actionLink id="col-actions-act1" value="#{r.docName}" image="/images/icons/document-followup.png" showLink="false" action="#assocs-block-panel"
      actionListener="#{DocumentDynamicDialog.addFollowUpHandler}" tooltip="#{msg.document_search_add_followup}" rendered="#{not DialogManager.bean.showDocsAndCasesAssocs}">
      <f:param name="nodeRef" value="#{r.node.nodeRef}" />
   </a:actionLink>
   <a:actionLink id="col-actions-act2" value="#{r.docName}" image="/images/icons/document-reply.png" showLink="false" action="#assocs-block-panel"
      actionListener="#{DocumentDynamicDialog.addReplyHandler}" tooltip="#{msg.document_search_add_reply}" rendered="#{not DialogManager.bean.showDocsAndCasesAssocs}">
      <f:param name="nodeRef" value="#{r.node.nodeRef}" />
   </a:actionLink>
   <%-- Start assocs that must be saved when clicked --%>
   <a:actionLink id="col-actions-addAssocDoc" value="#{r.docName}" image="/images/icons/import.png" showLink="false" action="#assocs-block-panel"
      actionListener="#{SearchBlockBean.addAssocDocHandler}" tooltip="#{msg.document_assocAdd}"
      rendered="#{'true' == DialogManager.bean.showDocsAndCasesAssocs}">
      <f:param name="nodeRef" value="#{r.node.nodeRef}" />
   </a:actionLink>
   <%-- End assocs that must be saved when clicked --%>

   <a:actionLink id="col-actions-act3" value="#{r.docName}" image="/images/icons/document-attachment.png" showLink="false" action="#assocs-block-panel"
      actionListener="#{DocumentDynamicDialog.addFilesHandler}" tooltip="#{msg.document_search_add_files}"
      rendered="#{(not DialogManager.bean.showDocsAndCasesAssocs) and (not DialogManager.bean.document.incomingInvoice)}">
      <f:param name="nodeRef" value="#{r.node.nodeRef}" />
   </a:actionLink>
</a:column>