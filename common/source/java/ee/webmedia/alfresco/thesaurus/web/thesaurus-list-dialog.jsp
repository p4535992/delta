<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="thesauri-panel" styleClass="panel-100 with-pager" label="#{msg.thesauri}" progressive="true">

   <a:richList id="thesauriList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.thesauri}" var="r" initialSortColumn="name" refreshOnBind="true">

      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.thesaurus_name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-act2" value="#{r.name}" action="dialog:thesaurusDetailsDialog" actionListener="#{ThesaurusDetailsDialog.setup}" tooltip="#{msg.thesaurus_edit}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.thesaurus_description}" value="description" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.description}" />
      </a:column>
      
      <a:column id="col3" actions="true" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText value="" escape="false" />
         </f:facet>
         
         <a:actionLink id="col3-act1" value="#{msg.thesaurus_edit}" image="/images/icons/edit_properties.gif" showLink="false" action="dialog:thesaurusDetailsDialog" actionListener="#{ThesaurusDetailsDialog.setup}" tooltip="#{msg.thesaurus_edit}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
      
    </a:richList>
   
</a:panel>