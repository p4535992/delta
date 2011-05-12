<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<%@page import="ee.webmedia.alfresco.volume.model.VolumeModel"%>
<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="ee.webmedia.alfresco.volume.web.VolumeDetailsDialog"%>
<a:panel id="metadata-panel" label="#{msg.document_metadata}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="volume-metatada" value="#{VolumeDetailsDialog.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel"/>
</a:panel>

<f:verbatim>
<script type="text/javascript">
   function postProcessButtonState(){
      var status = '<%= StringEscapeUtils.escapeJavaScript((String)BeanHelper.getVolumeDetailsDialog().getCurrentNode().getProperties().get(VolumeModel.Props.STATUS)) %>';
      processFnSerVolCaseCloseButton(status);
   }

   var volumeMarkInput = $jQ("#"+escapeId4JQ("dialog:dialog-body:volume-metatada:prop_volx003a_volumeMark:volx003a_volumeMark"));
   var initialVolumeMark = volumeMarkInput.val();

   $jQ(document).ready(function () {
      var jQVolumeType = $jQ("#"+escapeId4JQ("dialog:dialog-body:volume-metatada:prop_volx003a_volumeType:volx003a_volumeType"));
      changeVolumeMarkReadOnly(jQVolumeType);
      prependOnchange(jQVolumeType, changeVolumeMarkReadOnly);
   });

   function changeVolumeMarkReadOnly(selectBox){
      var selectedVal = selectBox.children("option:selected").val();
      if(selectedVal == "objektipõhine"){
         volumeMarkInput.val(initialVolumeMark);
         volumeMarkInput.attr("disabled", "disabled");
      } else {
         volumeMarkInput.removeAttr("disabled");
      }
      processButtonState();
   }

</script>
</f:verbatim>
<%
   final boolean isNew = BeanHelper.getVolumeDetailsDialog().isNew();
   if(isNew) {
%>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-close-button.jsp" />
<%
   }
%>