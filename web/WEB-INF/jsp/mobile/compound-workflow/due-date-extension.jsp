<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<tag:html>

<form:form modelAttribute="dueDateExtensionForm" method="POST">
	<input id="compoundWorkflowRef" name="compoundWorkflowRef" value="${ dueDateExtensionForm.compoundWorkflowRef }" type="hidden" />
  <h1>
    <fmt:message key="workflow.task.dueDate.extension.title" />
  </h1>

  <fmt:formatDate value="${ dueDateExtensionForm.newDueDate }" pattern="dd MMMM, yyyy" var="dueDateStr" />
  <tag:datepicker name="newDueDate" labelId="workflow.task.dueDate.extension.new.dueDate" value="${ dueDateStr }" />

  <tag:formrow labelId="workflow.task.dueDate.extension.reason">
    <tag:textarea name="reason" id="dueDateExtension-reason" value="${ dueDateExtensionForm.reason }" />
  </tag:formrow>

  <tag:suggester name="userId" labelId="workflow.task.dueDate.extension.assignee" value="${ dueDateExtensionForm.userId }" allowSingleEntry="true" 
    initialUserId="${ dueDateExtensionForm.userId }" initialUserName="${ dueDateExtensionForm.userName }" />

  <fmt:formatDate value="${ dueDateExtensionForm.extensionDueDate }" pattern="dd MMMM, yyyy" var="extensionDueDateStr" />
  <tag:datepicker name="extensionDueDate" labelId="workflow.task.dueDate.extension.dueDate" value="${ extensionDueDateStr }" initialValue="${ dueDateExtensionForm.initialExtensionDueDate }" />

  <div class="buttongroup">
    <button id="okButton" type="submit" name="ok" value="Submit">
      <fmt:message key="workflow.task.dueDate.extension.ok" />
    </button>
    
  </div>

</form:form>
<script src="<c:url value="/mobile/js/delegation.js" />" type="text/javascript"></script>
    <script type="text/javascript">
               $(document).ready(function() {
                  setupSuggester($(".autocomplete"), "/m/ajax/search/users?withoutCurrentUser=true"); // mDelta.js
                  validate();
                  $(".datepicker, #dueDateExtension-reason, .autocomplete").change(validate);
                  $(".datepicker, #dueDateExtension-reason, .autocomplete").keyup(validate);

                  function validate() {
                     var datePickers = $(".datepicker");
                     var pickersFilled = true;
                     datePickers.each(function(index, elem) {
                        pickersFilled &= ($(elem).val().length > 0);
                     });
                     if (pickersFilled
                           && $("#dueDateExtension-reason").val().length > 0 
                           && $(".autocomplete").val().length > 0) {
                        $("#okButton").attr("disabled", false);
                     } else {
                        $("#okButton").attr("disabled", true);
                     }
                  }
                  
                  $("#dueDateExtensionForm").on("submit", function(event) {
                 	  var cwfRef = $("#compoundWorkflowRef").val();
                 	 var lockResult = false;
                      $.ajaxq('lock', {
                         type: 'POST',
                         queue: true,
                         url: getContextPath() + '/m/ajax/cwf/locktask',
                         async: false,
                         dataType: 'json',
                         data: JSON.stringify ({compoundWorkflowRef: cwfRef}),
                         contentType: 'application/json',
                         success: function(result) {
                            if(result.messages.length > 0) {
                               var combinedMessage = '';
                               $.each(result.messages, function(i, message) {
                                  combinedMessage += message + '\n';
                               });
                               addMessage(combinedMessage, "error", true);
                               lockResult = false;
                            } else {
                         	   lockResult = true; 
                            }
                         },
                         error: function() {
                            addMessage("Failed to get response from server", "error", true);
                            lockResult = false;
                         }
                      });
                 	 
                     if (!lockResult) {
                     	return false;
                     } else {
                    	 $(".datepicker").attr("disabled", "disabled");
                    	 return true;
                     }
                  });
               });
    </script>
</tag:html>