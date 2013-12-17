<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%@page import="org.alfresco.web.app.Application"%>
<%@page import="javax.faces.context.FacesContext"%>

<a:panel id="functions-panel" styleClass="panel-100 with-pager" label="#{msg.functions_list}" progressive="true">

   <%-- Main List --%>
   <a:richList id="functionsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{DialogManager.bean.functions}" var="r">

      <%-- Mark --%>
      <a:column id="col2" primary="true">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.function_mark}" value="mark" styleClass="header" />
         </f:facet>
         <a:actionLink id="col2-text" value="#{r.mark}" action="dialog:seriesListDialog" tooltip="#{msg.series_list_info}"
            showLink="false" actionListener="#{SeriesListDialog.showAll}" styleClass="no-underline" >
            <f:param name="functionNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Title--%>
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.function_title}" value="title" styleClass="header" />
         </f:facet>

         <a:actionLink id="col3-text" value="#{r.title}" action="dialog:seriesListDialog" tooltip="#{msg.series_list_info}"
            showLink="false" actionListener="#{SeriesListDialog.showAll}" >
            <f:param name="functionNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Type --%>
      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.function_type}" value="type" styleClass="header" />
         </f:facet>
           <a:actionLink id="col1-text" value="#{r.type}" action="dialog:seriesListDialog" tooltip="#{msg.series_list_info}"
            showLink="false" actionListener="#{SeriesListDialog.showAll}" styleClass="no-underline" >
            <f:param name="functionNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Status --%>
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.function_status}" value="status" styleClass="header" />
         </f:facet>
           <a:actionLink id="col4-text" value="#{r.status}" action="dialog:seriesListDialog" tooltip="#{msg.series_list_info}"
            showLink="false" actionListener="#{SeriesListDialog.showAll}" styleClass="no-underline" >
            <f:param name="functionNodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Details actions column --%>
      <a:column id="col5" actions="true" styleClass="actions-column" rendered="#{UserService.documentManager}" >
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink id="col5-act1" value="#{r.title}" image="/images/icons/edit_properties.gif" action="dialog:functionsDetailsDialog" showLink="false"
            actionListener="#{FunctionsDetailsDialog.select}" tooltip="#{msg.functions_details_info}">
            <f:param name="nodeRef" value="#{r.nodeRef}" />
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<f:verbatim>
   <script type="text/javascript">
      prependOnclick($jQ(".docList_export"), function(){
         return confirm('<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("docList_export_confirmProceed")%>');
      });
      $jQ(".docList_createNewYearBasedVolumes").click(function(){
         alert('<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("docList_createNewYearBasedVolumes_confirmProceed")%>');
      });
      prependOnclick($jQ(".docList_deleteAllDocuments"), function(){
         return confirm('<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("docList_deleteAllDocuments_confirmProceed")%>');
      });
      prependOnclick($jQ(".docList_importPP"), function(){
          return confirm('<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("doclist_import_PP_confirmProceed")%>');
       });
   </script>
</f:verbatim>