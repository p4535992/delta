<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="dimensions-panel" styleClass="with-pager" label="#{msg.dimensions_list}" >

   <%-- Spaces List --%>
   <a:richList id="dimensionsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.dimensions}" var="dimension" initialSortColumn="name" >

      <%-- Primary column for the name --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.dimension_name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <a:actionLink id="col1-act1" value="#{dimension.name}" action="dialog:dimensionDetailsDialog" actionListener="#{DimensionDetailsDialog.select}">
               <f:param name="nodeRef" value="#{dimension.node.nodeRef}" />
            </a:actionLink>
         </f:facet>
      </a:column>

      <%-- Comment column --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.dimension_comment}" value="comment" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{dimension.comment}" />
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

<a:panel id="dimensions-panel" styleClass="with-pager" label="#{msg.dimensions_list}" >

   <%-- Spaces List --%>
   <a:richList id="dimensionsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.dimensions}" var="dimension" initialSortColumn="name" >

      <%-- Primary column for the name --%>
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.dimension_name}" value="name" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <f:facet name="small-icon">
            <a:actionLink id="col1-act1" value="#{dimension.name}" action="dialog:dimensionDetailsDialog" actionListener="#{DimensionDetailsDialog.select}">
               <f:param name="nodeRef" value="#{dimension.node.nodeRef}" />
            </a:actionLink>
         </f:facet>
      </a:column>

      <%-- Comment column --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.dimension_comment}" value="comment" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{dimension.comment}" />
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

>>>>>>> develop-5.1
</a:panel>