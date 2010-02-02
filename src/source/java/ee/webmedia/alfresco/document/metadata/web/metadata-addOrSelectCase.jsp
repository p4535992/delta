<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<div id="aoscModal-container-modalpopup" class="modalpopup modalwrap">
<div class="modalpopup-header clear">
   <h1>
</f:verbatim>
   <h:outputLabel value="#{msg.document_aoscModal_title}" />
<f:verbatim>
   </h1>
   <p class="close">
      <a onclick="setCaseAssigned(false);hideModal();return false;" href="#"></f:verbatim><h:outputLabel value="#{msg.close_window}" /><f:verbatim></a></p></div>

<div id="aoscModal-container">
         <input type="radio" name="existingOrNew" id="aoscModal-selectCase-radio" value="aoscModal-selectCase-radio" class="aoscModal-selectCase" />
</f:verbatim>
<h:panelGrid>
      <a:outputText id="aoscModal-selectCase-lbl" value="#{msg.document_aoscModal_existingCaseLabel}" styleClass="aoscModal-selectCase" />
      <h:selectOneMenu value="" id="aoscModal-selectCase" valueChangeListener="#{MetadataBlockBean.caseOfVolumeSelected}" styleClass="aoscModal-selectCase assignCaseInput" >
         <f:selectItems value="#{MetadataBlockBean.casesOfSelectedVolume}" />
      </h:selectOneMenu>
</h:panelGrid>
<f:verbatim>
         <input type="radio" name="existingOrNew" id="aoscModal-newCase-radio" value="aoscModal-newCase-radio" class="aoscModal-newCase" />
</f:verbatim>
<h:panelGrid id="aoscModal-newCase-controls">
         <h:outputText id="aoscModal-newCase-lbl" value="#{msg.document_aoscModal_newCaseLabel}" styleClass="aoscModal-newCase" />
         <h:inputTextarea id="newCaseName" binding="#{MetadataBlockBean.newCaseHtmlInput}" styleClass="aoscModal-newCase assignCaseInput expand15-200" readonly="readonly" />
         <h:commandButton id="confirmCaseSelectionBtn" onclick="realSubmit()" value="#{msg.confirm}" disabled="true" />
</h:panelGrid>
<f:verbatim>
</div>
</div>

<script type="text/javascript">
var volumeContainsCases = true;
$jQ(document).ready(function(){
   relocateRadioButtons();
   clearCaseSelectionInputs();

   $jQ('#' + escapeId4JQ('dialog:finish-button') + ', #' + escapeId4JQ('dialog:document_register_button')).bind("click", function(e){
      if(!volumeContainsCases || isCaseAssigned()){
         volumeContainsCases = true;
         return true; // case selected, proceed
      }
      requestVolumeContainsCases();// ajax call. Based on input show case selection modal
      return false; // case not selected
    });

   var class_selectCase = "aoscModal-selectCase";
   var class_newCase = "aoscModal-newCase";
   var class_assignCaseInput = "assignCaseInput";
   var radioSuffix = "-radio";
   var selectCaseItems = $jQ("."+class_selectCase);
   var newCaseItems = $jQ("."+class_newCase);
   var assignSelect = $jQ("."+class_selectCase+"."+class_assignCaseInput);
   var assignNew = $jQ("."+class_newCase+"."+class_assignCaseInput);
   assignNew.attr('disabled', 'disabled');
   selectCaseItems.bind("click", function(e){
      var radio = $jQ("#"+class_selectCase+radioSuffix);
      radio.attr('checked', 'checked');
      selectCaseItems.removeAttr('readonly');
      assignNew.attr('disabled', 'disabled');
      assignNew.val("");
   });
   newCaseItems.bind("click", function(e){
      var radio = $jQ("#"+class_newCase+radioSuffix);
      radio.attr('checked', 'checked');
      newCaseItems.removeAttr('disabled');
      assignSelect.attr('readonly', 'readonly');
      assignSelect.val("");
   });

   selectCaseItems.change(function () {
      $jQ("."+class_selectCase+" option:selected").each(function () {
         setCaseAssignedValue($jQ(this).text());
      });
    });

   assignNew.change(function () {
      setCaseAssignedValue($jQ(this).val());
   });
   assignNew.bind("keyup", function () {
      setCaseAssignedValue($jQ(this).val());
   });

});

   function relocateRadioButtons(){
      //TODO: nupud õigetesse kohtadesse
      // relocate selectCase radiobutton
      var selectCaseRadio = $jQ("input[type='radio'].aoscModal-selectCase");
      selectCaseRadio.insertBefore("span.aoscModal-selectCase");
      // relocate newCase radiobutton
      var newCaseRadio = $jQ("input[type='radio'].aoscModal-newCase");
      newCaseRadio.insertBefore("span.aoscModal-newCase");
   }
   function isCaseAssigned(){
      var selectCaseVal = $jQ(".aoscModal-selectCase option:selected").text();
      var assigned;
      if(!isEmptyValue(selectCaseVal)){
         assigned = setCaseAssignedValue(selectCaseVal);
      }
      var newCaseVal = $jQ(".aoscModal-newCase.assignCaseInput").val();
      if(!isEmptyValue(newCaseVal)){
         assigned = setCaseAssignedValue(newCaseVal);
      }
      return assigned;
   }
   function isEmptyValue(value){
      value = $jQ.trim(value);
      var defaultSelection = "[defaultSelection]";
      var isEmpty = true;
      if(value != null && value.length > 0 && value!=defaultSelection) {
         isEmpty = false;
      }
      return isEmpty;
   }
   
   function realSubmit(){
      if(isCaseAssigned()){
         $jQ('#' + escapeId4JQ('dialog:finish-button')).click();
         return true; // case selected
      } else {
         return false;
      }
   }

   function setCaseAssignedValue(value){
      var isEmpty = isEmptyValue(value)
      setCaseAssigned(!isEmpty);
      return !isEmpty;
   }

   function setCaseAssigned(selected){
      var confirmCaseBtn = $jQ('#' + escapeId4JQ('dialog:dialog-body:confirmCaseSelectionBtn'));
      if(selected){
         confirmCaseBtn.removeAttr("disabled");
      } else {
         confirmCaseBtn.attr("disabled", "disabled");
         clearCaseSelectionInputs();
      }
   }

   function clearCaseSelectionInputs(){
      var newCaseInput = $jQ(".aoscModal-newCase.assignCaseInput").val("");
      var selectCaseInput = $jQ(".aoscModal-selectCase");
      if(selectCaseInput != null){
         $jQ(selectCaseInput).val($jQ('option:first', selectCaseInput).val());
      }
      $jQ("input[name='existingOrNew']").removeAttr("checked");
   }
   
   function requestVolumeContainsCases() {
      YAHOO.util.Connect.asyncRequest("GET", getContextPath() + '/ajax/invoke/MetadataBlockBean.volumeContainsCasesClientHandler', 
            { 
               success: requestVolumeContainsCasesRefreshSuccess
               ,failure: requestVolumeContainsCasesRefreshFailure
            }, 
            null);
   }

   function requestVolumeContainsCasesRefreshSuccess(ajaxResponse) {
      var xml = ajaxResponse.responseXML.documentElement;
      if(xml.getAttribute('volume-selection-changed') == "true" && xml.getAttribute('contains-cases') == "true"){
         volumeContainsCases = true;
         showModal('aoscModal-container-modalpopup');
      } else {
         volumeContainsCases = false;
         $jQ('#' + escapeId4JQ('dialog:finish-button')).click();
      }
   }
   
   function requestVolumeContainsCasesRefreshFailure(ajaxResponse) {
      // alert("response: "+ajaxResponse.responseText); //XXX: mida veaolukorras teha võiks?
   }
   
</script>
</f:verbatim>