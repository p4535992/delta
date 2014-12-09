<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<tag:html>

<form:form modelAttribute="dueDateExtensionForm" method="POST">

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
                  $("#dueDateExtensionForm").submit(function(){
                     $(".datepicker").attr("disabled", "disabled");
                  })
               });
    </script>
  </div>

</form:form>

</tag:html>