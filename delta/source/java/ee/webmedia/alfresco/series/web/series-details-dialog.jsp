<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@page import="ee.webmedia.alfresco.series.model.SeriesModel"%>
<%@page import="ee.webmedia.alfresco.series.web.SeriesDetailsDialog"%>
<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>

<%@page import="org.alfresco.web.app.Application"%><a:panel id="metadata-panel" label="#{msg.document_metadata}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="ser-metatada" value="#{SeriesDetailsDialog.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/series/web/series-log-block.jsp" />

<f:verbatim>
<script type="text/javascript">
   function postProcessButtonState(){
      var status = '<%= StringEscapeUtils.escapeJavaScript((String)BeanHelper.getSeriesDetailsDialog().getCurrentNode().getProperties().get(SeriesModel.Props.STATUS)) %>';
      processFnSerVolCaseCloseButton(status);
   }
</script>
</f:verbatim>
<%
   final boolean isNew = BeanHelper.getSeriesDetailsDialog().isNew();
   if(isNew) {
%>
      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-close-button.jsp" />
<f:verbatim>
<script type="text/javascript">
   var jQSeriesIdentifier = $jQ("#"+escapeId4JQ("dialog:dialog-body:ser-metatada:prop_serx003a_seriesIdentifier:serx003a_seriesIdentifier"));
   var initialSeriesIdentifier = '<%= StringEscapeUtils.escapeJavaScript((String)BeanHelper.getSeriesDetailsDialog().getCurrentNode().getProperties().get(SeriesModel.Props.SERIES_IDENTIFIER)) %>';

   propSheetValidateSubmitFn.push(validateSeriesIdentifier);

   function validateSeriesIdentifier(){
      var newSeriesIdentifier = jQSeriesIdentifier.val().trim();
      if(newSeriesIdentifier == initialSeriesIdentifier){
         jQSeriesIdentifier.val(newSeriesIdentifier);
         var errMsg = '<%= MessageUtil.getMessageAndEscapeJS("series_seriesIdentifier_notChanged") %>';
         informUser(jQSeriesIdentifier.get(0), errMsg, true);
         return false;
      }
      return true;
   }
</script>
</f:verbatim>
<%
   }
%>
