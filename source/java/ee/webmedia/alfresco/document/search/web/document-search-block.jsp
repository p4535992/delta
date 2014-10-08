<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="ee.webmedia.alfresco.document.search.web.AbstractSearchBlockBean" %>
<%@ page import="ee.webmedia.alfresco.document.search.web.BlockBeanProviderProvider" %>

<h:panelGroup id="docsearch-panel-facets">
   <f:facet name="title">
      <a:actionLink id="close-docSearch" actionListener="#{DialogManager.bean.search.hideSearchBlock}" value="#{msg.document_assocSearch_close}" image="/images/icons/close_panel.gif" showLink="false"/>
   </f:facet>
</h:panelGroup>
      
<a:panel label="#{DialogManager.bean.search.searchBlockTitle}" id="docsearch-panel" styleClass="panel-100" progressive="true"
      facetsId="dialog:dialog-body:docsearch-panel-facets">
<<<<<<< HEAD
   <h:panelGrid width="100%" id="docsearch-panelGrid" >
=======
   <h:panelGrid width="100%" id="docsearch-panelGrid" columns="1">
>>>>>>> develop-5.1
      <a:panel id="docsearch-button">
         <f:verbatim>
            <script type="text/javascript">
               function _searchAssocs(event) { if (event && event.keyCode == 13) {$jQ('#docsearch-button.panel input[id$=quickSearchBtn2]').click();return true;} else {return false;} }
             </script>
         </f:verbatim>
		   <r:propertySheetGrid id="assoc-object-search-filter" value="#{DialogManager.bean.search.filter}" columns="1" mode="edit" externalConfig="true"
 		      labelStyleClass="propertiesLabel" binding="#{DialogManager.bean.search.propertySheet}" config="#{DialogManager.bean.search.propertySheetConfigElement}" var="searchNode"/>
			<h:commandButton id="quickSearchBtn2" value="#{msg.search}" type="submit" actionListener="#{DialogManager.bean.search.setup}" action="#docsearch-panel" styleClass="searchAssocOnEnter" />
      </a:panel>

      <a:panel id="docsearch-results-panel" styleClass="panel-100 with-pager" label="#{msg.search}" progressive="true" rendered="#{DialogManager.bean.search.showSearch}">
         <a:richList id="search-documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow"
            altRowStyleClass="recordSetRowAlt" width="100%" value="#{DialogManager.bean.search.documents}" var="r" refreshOnBind="true" >

            <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog-columns.jsp" />
            <jsp:include page="<%=(((BlockBeanProviderProvider) Application.getDialogManager().getBean()).getSearch()).getActionColumnFileName()%>" />

            <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
            <a:dataPager id="pager1" styleClass="pager" />
         </a:richList>
      </a:panel>

   </h:panelGrid>
</a:panel>
