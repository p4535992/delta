<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="similar-results-panel" styleClass="panel-100 with-pager" label="#{msg.document_search_similar_documents}" progressive="true" rendered="#{DocumentDialog.search.count > 0}">
   <a:richList id="similar-documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DocumentDialog.search.documents}" var="r">

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog-columns.jsp" />

      <%-- iconLink to document details --%>
      <a:column id="col-actions" actions="true" styleClass="actions-column2 #{r.cssStyleClass}">
         <a:actionLink id="col-actions-act1" value="#{r.docName}" image="/images/icons/document-followup.png" showLink="false"
            actionListener="#{DocumentDialog.addFollowUpHandler}" tooltip="#{msg.document_search_add_followup}">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>