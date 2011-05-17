<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

   <a:panel styleClass="column panel-100" id="dimension-data" label="#{msg.dimension_data}">
      <h:panelGrid id="dimension-data-panel" styleClass="table-padding" border="0" width="100%" columnClasses="propertiesLabel," columns="3" >
         <h:outputText id="dimension-comment-label" value="#{msg.dimension_comment}:" style="padding-left:8px" />
         <h:inputTextarea id="dimension-comment" value="#{DimensionDetailsDialog.dimension.comment}" styleClass="expand19-200" />
      </h:panelGrid>
   </a:panel>

<a:panel id="dimensions-panel" styleClass="panel-100 with-pager" label="#{msg.dimensions_values_list}" progressive="true">

   <a:richList id="dimensionDetailsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      value="#{DialogManager.bean.dimensionValues}" var="dimValue" initialSortColumn="valueName" binding="#{DialogManager.bean.richList}">

      <%-- Name column --%>
      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.dimension_value_name}" value="valueName" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{dimValue.valueName}" />
      </a:column>

      <%-- Value column --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.dimension_value_value}" value="value" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{dimValue.value}" />
      </a:column>

      <%-- Comment column --%>
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.dimension_value_comment}" value="valueComment" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:inputTextarea id="col3-in-txt" value="#{dimValue.valueComment}" styleClass="expand19-200" />
      </a:column>

      <%-- BeginDateTime column --%>
      <a:column id="col4">
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.dimension_value_beginDateTime}" value="beginDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{dimValue.beginDateTime}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- EndDateTime column --%>
      <a:column id="col5">
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.dimension_value_endDateTime}" value="endDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{dimValue.endDateTime}">
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Active column --%>
      <a:column id="col6">
         <f:facet name="header">
            <h:outputText id="col6-sort" value="#{msg.dimension_value_active}" />
         </f:facet>
         <h:selectBooleanCheckbox id="col6-select-check" value="#{dimValue.active}">
            <f:selectItem value="#{dimValue.active}" />
         </h:selectBooleanCheckbox>
      </a:column>

      <%-- Default value column --%>
      <a:column id="col7">
         <f:facet name="header">
            <h:outputText id="col7-sort" value="#{msg.dimension_default_value}" />
         </f:facet>
         <h:selectBooleanCheckbox id="col7-select-check" value="#{dimValue.defaultValue}">
            <f:selectItem value="#{dimValue.defaultValue}" />
         </h:selectBooleanCheckbox>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>