<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<%@page import="ee.webmedia.alfresco.volume.web.VolumeDetailsDialog"%>
<%@page import="org.alfresco.web.app.servlet.FacesHelper"%>
<%@page import="javax.faces.context.FacesContext"%>
<a:panel id="metadata-panel" label="#{msg.document_metadata}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="volume-metatada" value="#{VolumeDetailsDialog.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel"/>
</a:panel>

<f:verbatim>
<script type="text/javascript">
   function postProcessButtonState(){
      var status = "</f:verbatim><h:outputText value="#{VolumeDetailsDialog.currentNode.properties['{http://alfresco.webmedia.ee/model/volume/1.0}status']}" /><f:verbatim>";
      processFnSerVolCaseCloseButton(status);
   }
</script>
</f:verbatim>
<%
   final boolean isNew = ((VolumeDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "VolumeDetailsDialog")).isNew();
   if(isNew) {
%>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-close-button.jsp" />
<%
   }
%>