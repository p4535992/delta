<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/web/limited-message-panel.jsp" />

<a:panel id="volume-result-panel" styleClass="panel-100 with-pager" label="#{VolumeSearchResultsDialog.listTitle}" progressive="true">

   <%-- Main List --%>
   <a:richList id="volumeSearchResultsList" styleClass="duplicate-header" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" binding="#{VolumeSearchResultsDialog.richList}" value="#{VolumeSearchResultsDialog.volumes}" var="r" >
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
