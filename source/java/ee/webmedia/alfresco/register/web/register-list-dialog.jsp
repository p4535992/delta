<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="register-panel" styleClass="panel-100 with-pager" label="#{msg.register_list}" progressive="true">

   <%-- Spaces List --%>
   <a:richList id="registersList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.registers}" var="r" initialSortColumn="name">

      <%-- Primary column for the name --%>
      <a:column id="colName" primary="true">
         <f:facet name="header">
            <a:sortLink id="colName-sort" label="#{msg.register_name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <a:actionLink id="colName-act1" value="#{msg.register_edit}" tooltip="#{msg.register_edit}" image="/images/icons/view_properties.gif" action="dialog:registerDetailsDialog"
               showLink="false" actionListener="#{RegisterDetailsDialog.setupRegister}">
               <f:param name="id" value="#{r.id}" />
            </a:actionLink>
         </f:facet>
         <a:actionLink id="colName-act2" value="#{r.name}" action="dialog:registerDetailsDialog" actionListener="#{RegisterDetailsDialog.setupRegister}" tooltip="#{msg.register_edit}">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
      </a:column>

      <%-- Counter column --%>
      <a:column id="colCounter" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="colCounter-txt" value="#{msg.register_counter}" />
         </f:facet>
         <h:outputText id="register-counter" value="#{r.counter}" />
      </a:column>
      
      <%-- Is the register active --%>
      <a:column id="colActive" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="colActive-txt" value="#{msg.register_active}" />
         </f:facet>
         <h:outputText id="regActive" value="#{r.active}"><a:convertBoolean /></h:outputText>
      </a:column>
      
      <%-- Is the register resets automatically--%>
      <a:column id="colAutoReset" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="colAutoReset-txt" value="#{msg.register_autoReset_title}" />
         </f:facet>
         <h:outputText id="regAutoReset" value="#{r.autoReset}"><a:convertBoolean /></h:outputText>
      </a:column>

      <%-- Actions column --%>
      <a:column id="colActions" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="colActions-txt" value="#{msg.register_edit}" />
         </f:facet>
         <a:actionLink id="colActions-act1" value="#{msg.register_edit}" tooltip="#{msg.register_edit}" image="/images/icons/edit_properties.gif" action="dialog:registerDetailsDialog"
            showLink="false" actionListener="#{RegisterDetailsDialog.setupRegister}">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="register-panel" styleClass="panel-100 with-pager" label="#{msg.register_list}" progressive="true">

   <%-- Spaces List --%>
   <a:richList id="registersList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.registers}" var="r" initialSortColumn="name">

      <%-- Primary column for the name --%>
      <a:column id="colName" primary="true">
         <f:facet name="header">
            <a:sortLink id="colName-sort" label="#{msg.register_name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <a:actionLink id="colName-act2" value="#{r.name}" action="dialog:registerDetailsDialog" actionListener="#{RegisterDetailsDialog.setupRegister}" tooltip="#{msg.register_edit}">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
      </a:column>

      <%-- Counter column --%>
      <a:column id="colCounter" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="colCounter-txt" value="#{msg.register_counter}" />
         </f:facet>
         <h:outputText id="register-counter" value="#{r.counter}" />
      </a:column>
      
      <%-- Is the register active --%>
      <a:column id="colActive" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="colActive-txt" value="#{msg.register_active}" />
         </f:facet>
         <h:outputText id="regActive" value="#{r.active}"><a:convertBoolean /></h:outputText>
      </a:column>
      
      <%-- Is the register resets automatically--%>
      <a:column id="colAutoReset" style="text-align:right">
         <f:facet name="header">
            <h:outputText id="colAutoReset-txt" value="#{msg.register_autoReset_title}" />
         </f:facet>
         <h:outputText id="regAutoReset" value="#{r.autoReset}"><a:convertBoolean /></h:outputText>
      </a:column>

      <%-- Actions column --%>
      <a:column id="colActions" actions="true" style="text-align:right" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText id="colActions-txt" value="#{msg.register_edit}" />
         </f:facet>
         <a:actionLink id="colActions-act1" value="#{msg.register_edit}" tooltip="#{msg.register_edit}" image="/images/icons/edit_properties.gif" action="dialog:registerDetailsDialog"
            showLink="false" actionListener="#{RegisterDetailsDialog.setupRegister}">
            <f:param name="id" value="#{r.id}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
</a:panel>