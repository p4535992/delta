<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="ee.webmedia.alfresco.document.web.BaseDocumentListDialog" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
      
      <%-- Received DateTime --%>
      <a:column id="col0_5" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="col0_5-sort" label="#{msg.document_receivedDateTime}" value="properties;cm:created" styleClass="header" />
         </f:facet>
         <a:actionLink id="col0_5-text" value="#{r.createdDateTimeStr}" action="#{DocumentDialog.action}" tooltip="#{r.createdDateTimeStr}" styleClass="no-underline"
          actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
          </a:actionLink>
      </a:column>
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog-columns.jsp" >
         <jsp:param name="showOrgStructColumn" value="<%=((BaseDocumentListDialog)Application.getDialogManager().getBean()).isShowOrgStructColumn() %>" />
         <jsp:param name="showComplienceDateColumn" value="<%=((BaseDocumentListDialog)Application.getDialogManager().getBean()).isShowComplienceDateColumn() %>" />
      </jsp:include>
      
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
      
      
      
