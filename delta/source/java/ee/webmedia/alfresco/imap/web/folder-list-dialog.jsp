<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="folder-panel" styleClass="panel-100 with-pager" label="#{DialogManager.bean.folderListTitle}" progressive="true" rendered="#{DialogManager.bean.showFolderList}">

   <%-- Folder List --%>
   <a:richList id="email-folderList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.folders}" var="f" refreshOnBind="true">

      <%-- Title --%>
      <a:column id="col1" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_docName}" value="name" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text-1" value="#{f.node.name}" actionListener="#{DialogManager.bean.setup}" >
            <f:param name="parentNodeRef" value="#{f.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <%-- Actions column --%>
      <a:column id="folder-act-col" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="act-col-txt" value="" />
         </f:facet>
         <a:actionLink id="act-col-act1" value="#{msg.delete}" actionListener="#{DeleteDialog.setupDeleteDialog}" action="dialog:deleteDialog" showLink="false"
            image="/images/icons/delete.gif" rendered="#{f.isEmpty}">
            <f:param name="nodeRef" value="#{f.nodeRef}"/>
            <f:param name="confirmMessagePlaceholder0" value="#{f.name}"/>
         </a:actionLink>
      </a:column>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />      
   </a:richList>
</a:panel>