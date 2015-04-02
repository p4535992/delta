<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<tag:html>

  <%-- Delegation history block --%>
  <tag:expanderBlock blockId="workflow-delegation-history-block" titleId="delegation.history.block.title" titleDetails="${delegationHistoryTaskCount}" expanded="false" independent="true" rendered="${not empty delegationHistoryTaskCount}">
    <jsp:include page="/WEB-INF/jsp/mobile/compound-workflow/delegation-history-block-content.jsp" />
  </tag:expanderBlock>

  <form:form modelAttribute="taskDelegationForm" method="POST">

    <h2>
      <fmt:message key="workflow.task.delegation.title" />
    </h2>
    
    <%-- START: Tasks --%>
    <div id="owner" class="hidden">
       <h3><fmt:message key="workflow.task.delegation.subblock.owner" /></h3>
    </div>
    
    <div id="coOwners" class="hidden">
       <h3><fmt:message key="workflow.task.delegation.subblock.coOwner" /></h3>
    </div>
    
    <div id="informationTasks" class="hidden">
       <h3><fmt:message key="workflow.task.delegation.subblock.informationTasks" /></h3>
    </div>
    
    <div id="opinionTasks" class="hidden">
       <h3><fmt:message key="workflow.task.delegation.subblock.opinionTasks" /></h3>
    </div>
    <%-- END: Tasks --%>
    
    <h2>
      <fmt:message key="workflow.task.delegation.addition.substitle" />
    </h2>
    
    <%-- START: Addition form --%>
    <tag:formrow labelId="workflow.task.type">          
      <form:select path="choice" items="${ taskDelegationForm.choices }" />
    </tag:formrow>

    <tag:suggester labelId="workflow.task.owner" value="" />

    <tag:formrow labelId="workflow.task.resolution.long">
      <tag:textarea id="taskResolution" value="" />
    </tag:formrow>

    <fmt:formatDate value="${ taskDelegationForm.dueDate }" pattern="dd MMMM, yyyy" var="dueDateStr" />
    <tag:datepicker labelId="workflow.task.dueDate" value="${ dueDateStr }" initialValue="${ taskDelegationForm.taskDueDate }"/>

    <div class="buttongroup">
      <button type="button" id="addTaskButton" onclick="validateAndAdd()">
        <fmt:message key="workflow.task.delegation.button.add.to.list" />
      </button>
      <button id="okButton" type="submit" name="ok" value="Submit">
        <fmt:message key="workflow.task.delegation.button.delegate" />
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
    addTranslation("workflow.task.delegation.missing.coOwner", "<fmt:message key='workflow.task.delegation.missing.coOwner' />");
    addTranslation("workflow.task.delegation.missing.owner", "<fmt:message key='workflow.task.delegation.missing.owner' />");
    addTranslation("empty.groups", "<fmt:message key='workflow.task.delegation.found.empty.groups' />");
    
    function hasOwner(choiches) {
       if($.inArray("ASSIGNMENT_RESPONSIBLE", choiches) != -1) { // user has responsible aspect
          if($("#owner .rowCounter").length == 0) {
             var msg = translate("workflow.task.delegation.missing.owner");
             addMessage(msg, "warning", true);
             return false;
          }
       } else {
          if($("#coOwners .rowCounter").length == 0) {
             var msg = translate("workflow.task.delegation.missing.coOwner");
             addMessage(msg, "warning", true);
             return false;
          }
       }
       clearMessages();
       return true;
    }

    var choiches = [];
    
    function validateAndAdd() {
       delegation.validateAndAdd();
    }
    
    $(document).ready(function() {
       
       delegation.init({
          chooseTaskTypeAndSetChoice: function() {
             var choiceToTaskTypeBlock = {"ASSIGNMENT_RESPONSIBLE": "owner", "ASSIGNMENT_NOT_RESPONSIBLE": "coOwners", "INFORMATION": "informationTasks", "OPINION": "opinionTasks"};
             choice = $("#choice").val();
             delegation.setChoice(choice);
             if (choice === "ASSIGNMENT_RESPONSIBLE") {
                $("#owner span").remove();
             }
             return $("#" + choiceToTaskTypeBlock[choice]);
          },
          validateDueDate: function() {
             var datePicker = $(".datepicker");
             if($("#choice").val() != "INFORMATION" && datePicker.val().length == 0) {
                datePicker.addClass("invalid");
             }
          }
       });
       
       var choice = $("#choice").val();
       var s = $(".autocomplete");
       if(choice == "ASSIGNMENT_RESPONSIBLE") {
          s.addClass("singleEntry");
       }
       setupSuggester(s, "/m/ajax/search/users");
       $("#choice").data("previous", $("#choice").val());
       $("#choice").change(function() {
          choice = $("#choice").val();
          var prev = $(this).data("previous");
          $(this).data("previous", choice);
          var suggester = $(".autocomplete");
          suggester.attr("value", "");
          suggester.siblings("ul").remove();
          if(choice === "ASSIGNMENT_RESPONSIBLE") {
             suggester.addClass("singleEntry");
             setupSuggester(suggester, "/m/ajax/search/users");
          } else {
             suggester.removeClass("singleEntry");
             if(choice === "INFORMATION" || choice === "OPINION") {
                setupSuggester(suggester, "/m/ajax/search/all");
             } else {
                setupSuggester(suggester, "/m/ajax/search/users");
             }
          }
       });
       
       $("#choice option").each(function() {
          choiches.push($(this).val());
       });
       
       $("#taskDelegationForm").on("submit", function(event) {
          if(delegation.isConfirmed()) {
             return true;
          }
          var valid = true;
          if(isOwnerOrResolutionFieldFilled()) {
             valid = delegation.validateAndAdd();
          }
          valid = valid && hasOwner(choiches);
          
          if(valid) {
             if($(".spinner").length > 0) {
                event.preventDefault(); // wait for group members to load
                var form = $(this);
                $("#taskDelegationForm").on("allGroupsLoaded", function() {
                   if(valid) {
                      if(delegation.hasOnlyEmptyGroups()) {
                         return false;
                      }
                      delegation.markConfirmed();
                      form.submit();
                   }
                });
             } else {
                return true;
             }
          }
          
          return false;
       });
       
    });
  </script>

</tag:html>