<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="help-text-results-panel" styleClass="panel-100 with-pager" label="#{msg.help_text_mgmt_title}" progressive="true">

   <a:richList id="helpTextList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      value="#{HelpTextListDialog.helpTexts}" var="r" refreshOnBind="true" initialSortColumn="name">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.help_text_name}" value="name" styleClass="header" />
         </f:facet>
         <a:actionLink value="#{r.name}" actionListener="#{HelpTextListDialog.edit}">
            <f:param name="textRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.help_text_code}" value="code" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.code}" />
      </a:column>

      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.help_text_type}" value="type" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text1" value="#{msg.help_text_name_dialog}" rendered="#{r.type == 'dialog'}" />
         <h:outputText id="col3-text2" value="#{msg.help_text_name_field}" rendered="#{r.type == 'field'}" />
         <h:outputText id="col3-text3" value="#{msg.help_text_name_documentType}" rendered="#{r.type == 'documentType'}" />
      </a:column>

      <%-- Actions column --%>
      <a:column id="col4" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="col4-txt" value="#{msg.workflow_compound_actions}" />
         </f:facet>
         <a:actionLink id="col4-act1" value="#{msg.help_text_edit_help}" tooltip="#{msg.help_text_edit_help}"
            actionListener="#{HelpTextListDialog.edit}" showLink="false" image="/images/icons/edit_properties.gif">
            <f:param name="textRef" value="#{r.nodeRef}" />
         </a:actionLink>
         <a:actionLink id="col4-act2" value="#{msg.help_text_delete_help}" tooltip="#{msg.help_text_delete_help}"
            actionListener="#{InformingDeleteNodeDialog.setupDelete}" action="dialog:informingDeleteNodeDialog" showLink="false" image="/images/icons/delete.gif">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
            <f:param name="containerTitleMsgKey" value="help_text_delete_title" />
            <f:param name="confirmMsgKey" value="help_text_delete_confirm" />
            <f:param name="deletableObjectNameProp" value="hlt:name" />
            <f:param name="successMsgKey" value="help_text_delete_success" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
</a:panel>