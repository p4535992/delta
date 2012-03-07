<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="help-text-results-panel" styleClass="panel-100 with-pager" label="#{msg.help_text_mgmt_title}" progressive="true">

   <a:richList id="helpTextList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      value="#{HelpTextListDialog.helpTexts}" var="r" refreshOnBind="true">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.help_text_name}" value="name" styleClass="header" />
         </f:facet>
         <a:actionLink value="#{r.properties['{http://alfresco.webmedia.ee/model/helpText/1.0}type']}" actionListener="#{HelpTextListDialog.edit}">
            <f:param name="textRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.help_text_code}" value="content" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.properties['{http://alfresco.webmedia.ee/model/helpText/1.0}code']}" />
      </a:column>

      <a:column id="col3">
         <a:actionLink id="col3-del" value="#{msg.help_text_delete_help}" actionListener="#{HelpTextListDialog.delete}" showLink="false" image="/images/icons/delete.gif"
            tooltip="#{msg.help_text_delete_help}">
            <f:param name="textRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
</a:panel>