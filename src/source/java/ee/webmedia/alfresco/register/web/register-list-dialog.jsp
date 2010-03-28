<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="register-panel" styleClass="panel-100 with-pager" label="#{msg.register_list}" progressive="true">

   <%-- Spaces List --%>
   <a:richList id="registersList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.registers}" var="r" initialSortColumn="name">

      <%-- Primary column for the name --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.register_name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <a:actionLink id="col1-act1" value="#{msg.register_edit}" tooltip="#{msg.register_edit}" image="/images/icons/view_properties.gif" action="dialog:registerDetailsDialog"
               showLink="false" actionListener="#{RegisterDetailsDialog.setupRegister}">
               <f:param name="id" value="#{r.id}" />
            </a:actionLink>
         </f:facet>
         <a:actionLink id="col1-act2" value="#{r.name}" action="dialog:registerDetailsDialog" actionListener="#{RegisterDetailsDialog.setupRegister}" tooltip="#{msg.register_edit}">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
      </a:column>

      <%-- Counter column --%>
      <a:column id="col2" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="col2-txt" value="#{msg.register_counter}" />
         </f:facet>
         <h:outputText id="register-counter" value="#{r.counter}" />
      </a:column>
      
      <%-- Prefix column --%>
      <a:column id="col3" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="col3-txt" value="#{msg.register_prefix}" />
         </f:facet>
         <h:outputText id="register-prefix" value="#{r.prefix}" />
      </a:column>
      
      <%-- Suffix column --%>
      <a:column id="col4" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="col4-txt" value="#{msg.register_suffix}" />
         </f:facet>
         <h:outputText id="register-suffix" value="#{r.suffix}" />
      </a:column>

      <%-- Is the register active --%>
      <a:column id="col5" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="col5-txt" value="#{msg.register_active}" />
         </f:facet>
         <h:outputText id="regActive" value="#{msg.yes}" rendered="#{r.active}" />
         <h:outputText id="regNotActive" value="#{msg.no}" rendered="#{!r.active}"  />
      </a:column>

      <%-- Actions column --%>
      <a:column id="col6" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="col6-txt" value="#{msg.register_edit}" />
         </f:facet>
         <a:actionLink id="col6-act1" value="#{msg.register_edit}" tooltip="#{msg.register_edit}" image="/images/icons/edit_properties.gif" action="dialog:registerDetailsDialog"
            showLink="false" actionListener="#{RegisterDetailsDialog.setupRegister}">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>