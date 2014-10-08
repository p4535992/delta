<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/imap/web/folder-list-dialog.jsp" />

<%-- This JSP is used from multiple dialogs, that's why DialogManager.bean reference is used --%>   
<a:panel id="document-panel" styleClass="panel-100 with-pager" label="#{DialogManager.bean.listTitle}" progressive="true">   

   <%-- Document List --%>
   <a:richList id="email-documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.documents}" var="r" refreshOnBind="true" rendered="#{DialogManager.bean.showFileList}">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog-columns.jsp" />
      
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
