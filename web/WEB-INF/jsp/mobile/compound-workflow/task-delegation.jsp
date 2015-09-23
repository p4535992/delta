<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<tag:html>

  <form:form modelAttribute="taskDelegationForm" method="POST">

    <h2>
      <fmt:message key="${taskDelegationForm.translations['pageTitle']}" />
    </h2>
    
    <div id="assignees" >
       <h3><fmt:message key="${taskDelegationForm.translations['assigneesTitle']}" /></h3>
    </div>
    
    <h2>
      <fmt:message key="${taskDelegationForm.translations['formTitle']}" />
    </h2>
    
    <input id="compoundWorkflowRef" name="compoundWorkflowRef" value="${ taskDelegationForm.compoundWorkflowRef }" type="hidden" />
    <input id="taskType" name="taskType" value="${ taskDelegationForm.taskType }" type="hidden" />
    <input id="choice" name="choice" value="${ taskDelegationForm.choice }" type="hidden" />
    
    <%-- START: Addition form --%>
    <tag:suggester labelId="${taskDelegationForm.translations['assignee']}" value="" />

    <tag:formrow labelId="workflow.task.resolution.long">
      <tag:textarea id="taskResolution" value="" />
    </tag:formrow>

    <fmt:formatDate value="${ taskDelegationForm.dueDate }" pattern="dd MMMM, yyyy" var="dueDateStr" />
    <tag:datepicker labelId="workflow.task.dueDate" value="${ dueDateStr }" initialValue="${ taskDelegationForm.taskDueDate }"/>

    <div class="buttongroup">
      <button id="addTaskButton" type="button" onclick="validateAndAdd()" >
        <fmt:message key="workflow.task.delegation.button.add.to.list" />
      </button>
      <button id="okButton" type="submit" name="ok" value="Submit">
        <fmt:message key="${taskDelegationForm.translations['okButtonTitle']}" />
      </button>
    </div>
    <%-- END: Addition form --%>

  </form:form>
  
  <script src="<c:url value="/mobile/js/delegation.js" />" type="text/javascript"></script>  
  <script type="text/javascript">
  
  addTranslation("workflow.task.owner", "<fmt:message key='workflow.task.owner' />");
  addTranslation("workflow.task.ownerGroup", "<fmt:message key='workflow.task.ownerGroup' />");
  addTranslation("workflow.task.prop.resolution", "<fmt:message key='workflow.task.prop.resolution' />");
  addTranslation("workflow.task.dueDate", "<fmt:message key='workflow.task.dueDate' />");
  addTranslation("delegation.missing.owner", "<fmt:message key='${taskDelegationForm.translations[\'missingOwner\']}' />");
  addTranslation("workflow.task.delegation.dueDate.in.past", "<fmt:message key='workflow.task.delegation.dueDate.in.past' />");
  addTranslation("delegation.dueDate.after.original.dueDate", "<fmt:message key='${taskDelegationForm.translations[\'futureDate\']}' />");
  addTranslation("empty.groups", "<fmt:message key='workflow.task.delegation.found.empty.groups' />");
  
  function validateAndAdd() {
     delegation.validateAndAdd();
  }
  
  function hasOwner() {
     if($("#assignees .rowCounter").length == 0) {
        addMessage(translate("delegation.missing.owner"), "warning", true);
        return false;
     }
     return true;
  }
  
  function getInitialTaskDueDate() {
     var initialDueDateStr = $(".datepicker.picker__input").attr("data-value");
     return dateStringToDate(initialDueDateStr);
  }
  
  function dateStringToDate(dateStr) {
     if(dateStr) {
        var dateParts = dateStr.split(".");
        return new Date(dateParts[2], dateParts[1] - 1, dateParts[0], 23, 59);
     }
  }
  
  function isSelectedDateBeforeOrEqualToInitialTaskDueDate() {
     var selectedDateStr = $(".datepicker").siblings("input").val();
     var selectedDateObj = dateStringToDate(selectedDateStr);
     var initialDueDate = getInitialTaskDueDate();
     if (selectedDateObj && initialDueDate && initialDueDate < selectedDateObj) {
        addMessage(translate("delegation.dueDate.after.original.dueDate"), "warning", true);
        return false;
     }
     return true;
  }
  
  function validateDueDates() {
     var now = new Date();
     var pastDateFound = false;
     var futureDateFound = false;
     var initialDueDate = getInitialTaskDueDate();
     
     $("#assignees input[name$='.dueDate']").each(function() {
        var dueDate = $(this).attr("value");
        var date = new Date(dueDate);
        if(!pastDateFound && date < now) {
           pastDateFound = true;
        }
        if(initialDueDate && initialDueDate < date) {
           futureDateFound = true;
           return false;
        }
     });
     if(futureDateFound) {
        addMessage(translate("delegation.dueDate.after.original.dueDate"), "warning", true);
        return false;
     }
     return pastDateFound ? confirm(translate("workflow.task.delegation.dueDate.in.past")) : true;
  }
  
  function getAllDueDates() {
     var dueDates = [];
     $(".visibleDueDate .value").each(function() {
        var dueDate = $(this).text();
        if(dueDate) {
           dueDates.push(dueDate);
        }
     });
     return dueDates;
  }
  
  function getAllTaskOwners() {
     var owners = [];
     $(".rowCounter input.ownerId").each(function() {
        owners.push($(this).val());
     });
     $(".rowCounter input.groupMembers").each(function() {
        var members = $(this).val();
        $.each(members.split(","), function(i, member) {
           owners.push(member);
        });
     });
     return owners;
  }
  
  function coverFormWithSpinner() {
     var form = $("#taskDelegationForm");
     form.addClass("covered");
     form.spin("large", "#ff9000");
     var spinner = form.find(".spinner");
     var top = (form.height() / 2) + "px";
     spinner.css({
        "position" : "relative",
        "top" : top
     });
  }
  
  function getConfirmationMessages(setupSpinner) {
     if(!!setupSpinner) {
        coverFormWithSpinner();
     }
     if($(".spinner").not("#taskDelegationForm > .spinner").length) { // waiting for groups to load
        $("#taskDelegationForm").on("allGroupsLoaded", function(){
           if(delegation.hasOnlyEmptyGroups()) {
              removeFormSpinner();
              delegation.addEmptyGroupsMessage();
              return;
           }
           getConfirmationMessages(false);
        });
        return;
     }
     
     if(delegation.hasOnlyEmptyGroups()) {
        removeFormSpinner();
        delegation.addEmptyGroupsMessage();
        return;
     }
     
     var cwfRef = $("#compoundWorkflowRef").val();
     var taskType = $("#taskType").val();
     var dueDates = getAllDueDates();
     var taskOwners = getAllTaskOwners();
     
     $.ajaxq('validate', {
        type: 'POST',
        queue: true,
        url: getContextPath() + '/m/ajax/delegation/confirmation',
        dataType: 'json',
        data: JSON.stringify ({compoundWorkflowRef: cwfRef,
                              'taskType': taskType,
                              'dueDates': dueDates,
                              'taskOwners': taskOwners }),
        contentType: 'application/json',
        success: function(result) {
           if(result.messages.length > 0) {
              var combinedMessage = '';
              $.each(result.messages, function(i, message) {
                 combinedMessage += message + '\n';
              });
              if(confirm(combinedMessage)) {
                 $("#taskDelegationForm").trigger("delegationConditionsAccepted");
              } 
              return;
           }
           $("#taskDelegationForm").trigger("delegationConditionsAccepted"); // no messages
        },
        error: function() {
           addMessage("Failed to get response from server", "error", true);
           removeFormSpinner();
        }
     }).done(removeFormSpinner);
  }
  
  function removeFormSpinner() {
     $("#taskDelegationForm .spinner").remove();
     $("#taskDelegationForm").removeClass("covered");
  }
  
  function submitAfterConfirmation(form) {
     getConfirmationMessages(true);
     $("#taskDelegationForm").on("delegationConditionsAccepted", function() {
        delegation.markConfirmed();
        form.submit();
     });
  }
  
  $(document).ready(function() {
     setupSuggester($(".autocomplete"), "/m/ajax/search/all");
     var disableDueDateValidation = $(".datepicker").val().length == 0;
     
     delegation.init({
        validateDueDate: function() {
           if(disableDueDateValidation) {
              return;
           }
           var datePicker = $(".datepicker");
           if(datePicker.val().length == 0) {
              datePicker.addClass("invalid");
           }
        },
        chooseTaskTypeAndSetChoice: function() { return $("#assignees"); },
        validateAdditional: isSelectedDateBeforeOrEqualToInitialTaskDueDate
     });
     var delegatableTaskType = $("#choice").val();
     delegation.setChoice(delegatableTaskType);
     
     $("#taskDelegationForm").on("submit", function(event) {
        if(delegation.isConfirmed()) {
           return true;
        }
        var valid = true;
        if(isOwnerOrResolutionFieldFilled()) {
           valid = delegation.validateAndAdd();
        }
        valid = valid && hasOwner() && validateDueDates();
        if(valid) {
           event.preventDefault();
           submitAfterConfirmation($(this));
        }
        return false;
     });
  });
  </script>

</tag:html>