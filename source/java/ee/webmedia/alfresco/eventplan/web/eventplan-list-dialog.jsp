<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="eventplans-panel" styleClass="panel-100 with-pager" label="#{msg.eventplan_list_title2}" progressive="true">

   <%-- Main List --%>
   <a:richList id="versionsList" viewMode="details" pageSize="#{BrowseBean.pageSizeSpaces}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.plans}" var="r" initialSortColumn="name">

      <%-- 1. Name --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.eventplan_name}" value="name" styleClass="header" />
         </f:facet>
         <a:actionLink value="#{r.name}" actionListener="#{EventPlanDialog.editEventPlan}" action="dialog:eventPlanDialog">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- 2. Appraised --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.eventplan_isAppraised}" value="appraised" styleClass="header" />
         </f:facet>
         <h:outputText id="appraised" value="#{r.appraised}">
            <a:convertBoolean />
         </h:outputText>
      </a:column>

      <%-- 3. HasArchivalValue--%>
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.eventplan_hasArchivalValue}" value="hasArchivalValue" styleClass="header" />
         </f:facet>
         <h:outputText id="hasArchivalValue" value="#{r.hasArchivalValue}">
            <a:convertBoolean />
         </h:outputText>
      </a:column>
      
      <%-- 4. RetainPeriod --%>
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.eventplan_retaintionPeriodLabel}" value="retaintionPeriodLabel" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.retaintionPeriodLabel}"/>
      </a:column>

      <%-- 5. FirstEvent --%>
      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.eventplan_firstEventLabel}" value="firstEventLabel" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.firstEventLabel}"/>
      </a:column>

      <%-- 6. ArchivingNote --%>
      <a:column id="col6">
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.eventplan_archivingNoteShort}" value="archivingNote" styleClass="header" />
         </f:facet>
         <h:outputText id="col6-text" value="#{r.archivingNoteShort}"/>
      </a:column>

      <%-- 7. View action --%>
      <a:column id="col7" actions="true" styleClass="actions-column">
         <a:actionLink value="" actionListener="#{EventPlanDialog.editEventPlan}" action="dialog:eventPlanDialog" showLink="false" image="/images/icons/edit_properties.gif">
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
