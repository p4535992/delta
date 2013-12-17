<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="orgstructs-panel" styleClass="panel-100 with-pager" label="#{msg.orgstructs_list}" progressive="true" facetsId="orgstructs-panel-facets">

   <%-- Main List --%>
   <a:richList id="orgstructsList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" styleClass="recordSet" headerStyleClass="recordSetHeader"
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.orgstructs}" var="os"
      initialSortColumn="name">

      <%-- Unit name --%>
      <a:column id="col1">
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.orgstruct_name}" value="name" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{os.name}" />
      </a:column>

      <%-- Super Unit name --%>
      <a:column id="col2">
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.orgstruct_supername}" value="superValueName" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{os.superValueName}" />
      </a:column>
      
      <%-- Organization path--%>
      <a:column id="col3">
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.orgstruct_organizationPath}" value="organizationDisplayPath" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{os.organizationDisplayPath}" />
      </a:column>      
      
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>