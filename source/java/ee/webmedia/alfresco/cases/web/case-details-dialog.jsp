<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<%@page import="ee.webmedia.alfresco.cases.model.CaseModel"%>
<%@page import="ee.webmedia.alfresco.cases.web.CaseDetailsDialog"%>
<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<a:panel id="metadata-panel" label="#{msg.document_metadata}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="case-metatada" value="#{CaseDetailsDialog.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" binding="#{CaseDetailsDialog.propertySheet}"/>
</a:panel>

<f:verbatim>
<script type="text/javascript">

   function postProcessButtonState(){
      var status = '<%= StringEscapeUtils.escapeJavaScript((String)BeanHelper.getCaseDetailsDialog().getCurrentNode().getProperties().get(CaseModel.Props.STATUS)) %>';
      processFnSerVolCaseCloseButton(status);
   }
</script>
</f:verbatim>
<%
   final boolean isNew = BeanHelper.getCaseDetailsDialog().isNew();
   if(isNew) {
%>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-close-button.jsp" />
<%
   }
%>
