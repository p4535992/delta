<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<%@page import="ee.webmedia.alfresco.cases.web.CaseDetailsDialog"%>
<%@page import="org.alfresco.web.app.servlet.FacesHelper"%>
<%@page import="javax.faces.context.FacesContext"%>
<a:panel id="metadata-panel" label="#{msg.document_metadata}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="case-metatada" value="#{CaseDetailsDialog.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<f:verbatim>
<script type="text/javascript">

   function postProcessButtonState(){
      var status = "</f:verbatim><h:outputText value="#{CaseDetailsDialog.currentNode.properties['{http://alfresco.webmedia.ee/model/case/1.0}status']}" /><f:verbatim>";
      processFnSerVolCaseCloseButton(status);
   }
</script>
</f:verbatim>
<%
   final boolean isNew = ((CaseDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "CaseDetailsDialog")).isNew();
   if(isNew) {
%>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-close-button.jsp" />
<%
   }
%>
