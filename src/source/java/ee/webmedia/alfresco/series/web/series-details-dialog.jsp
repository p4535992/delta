<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="metadata-panel" label="#{msg.document_metadata}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="ser-metatada" value="#{SeriesDetailsDialog.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<f:verbatim>
<script type="text/javascript">

   var postProcessButtonStateBound = false;
   function postProcessButtonState(){
      var finBtnId = "dialog:finish-button";
      var closeBtnId = "dialog:close-button";
      var status = "</f:verbatim><h:outputText value="#{SeriesDetailsDialog.currentNode.properties['{http://alfresco.webmedia.ee/model/series/1.0}status']}" /><f:verbatim>";
      var closeBtn = $jQ("#"+escapeId4JQ(closeBtnId));
      if(!postProcessButtonStateBound) {
         closeBtn.bind("click", function(e){
            nextButtonPressed = true;
            return validate();
   	     });
         postProcessButtonStateBound = true;
      }

      var finishBtn = $jQ("#"+escapeId4JQ(finBtnId));
      var finishDisabled = finishBtn.attr("disabled");
      if(finishDisabled){
         closeBtn.attr("disabled", finishDisabled);
      } else {
         if(status != "suletud"){
         	closeBtn.attr("disabled", finishDisabled);
         }
      }
   }
</script>
</f:verbatim>

