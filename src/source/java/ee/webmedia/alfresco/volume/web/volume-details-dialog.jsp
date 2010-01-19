<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="metadata-panel" label="#{msg.document_metadata}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="volume-metatada" value="#{VolumeDetailsDialog.currentNode}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<f:verbatim>
<script type="text/javascript">

   var postProcessButtonStateBound = false;
   function postProcessButtonState(){
      var status = "</f:verbatim><h:outputText value="#{VolumeDetailsDialog.currentNode.properties['{http://alfresco.webmedia.ee/model/volume/1.0}status']}" /><f:verbatim>";
      var closeBtn = $jQ("#"+escapeId4JQ("dialog:close-button"));
      if(!postProcessButtonStateBound) {
         closeBtn.bind("click", function(e){
            nextButtonPressed = true;
            return validate();
   	     });
         postProcessButtonStateBound = true;
      }

      //var valid = processButtonState();
      //log("valid: "+valid);
      var finishBtn = $jQ("#"+escapeId4JQ("dialog:finish-button"));
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