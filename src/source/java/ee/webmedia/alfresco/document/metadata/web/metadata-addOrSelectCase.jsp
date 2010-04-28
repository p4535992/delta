<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.app.Application"%>
<%@ page import="javax.faces.context.FacesContext"%>

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
      <h:inputHidden id="caseAssignmentNeeded" value="#{MetadataBlockBean.caseAssignmentNeeded}" ></h:inputHidden>
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
         <h:inputTextarea id="newCaseName" binding="#{MetadataBlockBean.newCaseHtmlInput}" styleClass="aoscModal-newCase assignCaseInput expand20-200" readonly="readonly" />
</h:panelGrid>
<f:verbatim>
</div>
</div>

<script type="text/javascript">
var registerButtonPressed = false;
var checksDone = false;
$jQ(document).ready(function(){
   initHtmlElements();
   clearCaseSelectionInputs();

   var finishButton = $jQ('#' + escapeId4JQ('dialog:finish-button') + ", "+'#' + escapeId4JQ('dialog:finish-button-2'));
   $jQ('#' + escapeId4JQ('dialog:documentRegisterButton')).bind("click", function(e){
      registerButtonPressed = true;
      if(finishButton.attr('disabled')) {
         alert("<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("document_mandatory_fields_are_empty")%>");
         return false;
      }
      return proccessSubmitOrRegisterButton();
   });

   finishButton.bind("click", function(e){
      return proccessSubmitOrRegisterButton();
   });

   /**
    * @return true, if case is selected, false otherwise
    */ 
   function proccessSubmitOrRegisterButton() {
      if(isCaseAssignmentNeeded()){
         clearCaseSelectionInputs();
         if(propSheetValidateSubmit()){
            showModal('aoscModal-container-modalpopup');
         }
         checksDone = true;
         return false;
      } else {
         if(!checksDone){
            checksDone = true;
            if(registerButtonPressed) {
               $jQ('#' + escapeId4JQ('dialog:documentRegisterButton')).click();
            } else {
               $jQ('#' + escapeId4JQ('dialog:finish-button')).click();
            }
            propSheetFinishBtnPressed = true;
         }
         return true;
      }
   }

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

   function initHtmlElements(){
	  createConfirmCaseSelectionBtn();
      relocateRadioButtons();
   }

   function createConfirmCaseSelectionBtn(){
      var lastRow = $jQ('.aoscModal-newCase.assignCaseInput').parent().parent();
      // can't use button, as it will behave diferently for some reason with IE(at least with IE8): 
      // statusbar will go to completed state before process application phase is completed(for example because of breakpoint at MetadataBlockBean#save())
      // and later page is reloaded without any indications on statusbar
      var confirmCaseSelectionBtn = $jQ('<a id="confirmCaseSelectionBtn" href="#" class="button disabled" >Kinnita</a>');
      confirmCaseSelectionBtn.click(function(e){
         if(confirmCaseSelectionBtn.hasClass("disabled")){
            return false;
         }
         realSubmit();
      });
      confirmCaseSelectionBtn.insertAfter(lastRow);
      confirmCaseSelectionBtn.wrapAll("<tr style='height:35px'/>").wrapAll("<td/>");
   }

   function relocateRadioButtons(){
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
         if(registerButtonPressed) {
        	$jQ('#' + escapeId4JQ('dialog:documentRegisterButton')).click();
         } else {
            $jQ('#' + escapeId4JQ('dialog:finish-button')).click();
         }
         return true; // case selected
      } else {
         // Assertion: code should normally not reach here - if it does, smth is probably wrong with code
         alert("<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("document_validationMsg_mandatory_case")%>");
         return false;
      }
   }

   function setCaseAssignedValue(value){
      var isEmpty = isEmptyValue(value)
      setCaseAssigned(!isEmpty);
      return !isEmpty;
   }

   function setCaseAssigned(selected){
      var confirmCaseBtn = $jQ('#confirmCaseSelectionBtn');
      if(selected){
         confirmCaseBtn.removeClass("disabled");
      } else {
         confirmCaseBtn.addClass("disabled");
         clearCaseSelectionInputs();
      }
   }

   function clearCaseSelectionInputs(){
      var newCaseInputElem = $jQ(".aoscModal-newCase.assignCaseInput");
      newCaseInputElem.html(""); // jsf sets inner html not value attribute, that might cause troubles
      var newCaseInput = newCaseInputElem.val("");
      var selectCaseInput = $jQ(".aoscModal-selectCase");
      if(selectCaseInput != null){
         $jQ(selectCaseInput).val($jQ('option:first', selectCaseInput).val());
      }
      $jQ("input[name='existingOrNew']").removeAttr("checked");
   }

   function isCaseAssignmentNeeded(){
      return !isCaseAssigned() && "true" == $jQ('#' + escapeId4JQ('dialog:dialog-body:caseAssignmentNeeded')).val();
   }
   
</script>
</f:verbatim>