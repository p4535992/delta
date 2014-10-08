<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>


<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="locks-panel" label="#{msg.lock_manage_list}" styleClass="panel-100 with-pager">
   <a:richList id="locksList" value="#{ManageLocksDialog.locks}" var="l" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" refreshOnBind="true">

      <a:column id="docRegNrCol" primary="true">
         <f:facet name="header">
            <a:sortLink id="docRegNr-sort" label="#{msg.document_regNumber}" value="docRegNr" mode="case-insensitive" />
         </f:facet>
         <h:outputText value="#{l.docRegNr}" />
      </a:column>

      <a:column id="docRegDateCol">
         <f:facet name="header">
            <a:sortLink id="docRegDateCol-sort" label="#{msg.document_regDateTime3}" value="docRegDate" mode="case-insensitive" />
         </f:facet>
         <a:outputText id="docRegDate-text" value="#{l.docRegDate}">
            <a:convertXMLDate type="both" pattern="#{msg.date_pattern}" />
         </a:outputText>
      </a:column>

      <a:column id="docNameCol">
         <f:facet name="header">
            <a:sortLink id="docNameCol-sort" label="#{msg.document_docName}" value="docName" mode="case-insensitive" />
         </f:facet>
         <a:actionLink id="docNameCol-text" value="#{l.docName}" tooltip="#{l.docName}"
            actionListener="#{DocumentDynamicDialog.openFromDocumentList}" styleClass="tooltip condence20- no-underline" >
            <f:param name="nodeRef" value="#{l.docNodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="fileNameCol">
         <f:facet name="header">
            <a:sortLink id="fileNameCol-sort" label="#{msg.file_name}" value="fileName" mode="case-insensitive" />
         </f:facet>
         <a:actionLink id="col1-act1" value="#{l.fileName}" href="#{l.fileUrl}" target="_blank" showLink="true" styleClass="inlineAction" />
      </a:column>
      
      <a:column id="lockedByCol">
         <f:facet name="header">
            <a:sortLink id="lockedByCol-sort" label="#{msg.lock_locked_by}" value="lockedBy" mode="case-insensitive" />
         </f:facet>
         <a:outputText id="lockedBy-text" value="#{l.lockedBy}" />
      </a:column>
      
      <%-- show details --%>
      <a:column id="actionsCol" actions="true" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="act1" value="#{msg.lock_release}" image="/images/icons/revert.gif" showLink="false"
            actionListener="#{ManageLocksDialog.release}" tooltip="#{msg.lock_release}" rendered="#{l.file}">
            <f:param name="object" value="#{l.object}" />
            <f:param name="all" value="false" />
         </a:actionLink>
         <a:actionLink id="act2" value="#{msg.lock_release_all}" image="/images/icons/revert_all.gif" showLink="false"
            actionListener="#{ManageLocksDialog.release}" tooltip="#{msg.lock_release_all}" rendered="#{l.document}">
            <f:param name="object" value="#{l.object}" />
            <f:param name="all" value="true" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-cancel-button.jsp" />
