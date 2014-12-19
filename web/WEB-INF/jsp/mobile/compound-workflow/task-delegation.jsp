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
    <tag:datepicker labelId="workflow.task.dueDate" value="${ dueDateStr }" />

    <div class="buttongroup">
      <button type="button" id="addTaskButton" onclick="validateAndAdd()">
        <fmt:message key="workflow.task.delegation.button.add" />
      </button>
      <button id="okButton" type="submit" name="ok" value="Submit">
        <fmt:message key="workflow.task.delegation.button.delegate" />
      </button>
    </div>
    <%-- END: Addition form --%>

  </form:form>
  
  <script type="text/javascript">
    var translations = [];
    
    function addTranslation(key, translation) {
       translations[key] = translation;
    }
    
    addTranslation("workflow.task.owner", "<fmt:message key='workflow.task.owner' />");
    addTranslation("workflow.task.ownerGroup", "<fmt:message key='workflow.task.ownerGroup' />");
    addTranslation("workflow.task.prop.resolution", "<fmt:message key='workflow.task.prop.resolution' />");
    addTranslation("workflow.task.dueDate", "<fmt:message key='workflow.task.dueDate' />");
    addTranslation("workflow.task.delegation.missing.coOwner", "<fmt:message key='workflow.task.delegation.missing.coOwner' />");
    addTranslation("workflow.task.delegation.missing.owner", "<fmt:message key='workflow.task.delegation.missing.owner' />");
    
    function translate(key) {
       var translation = translations[key];
       if(translation == null) {
          translation = key;
       }
       return translation;
    }
    
    var choiceToBlock = {0: "owner", 1: "coOwners", 2: "informationTasks", 3: "opinionTasks"};
    
    function addTask() {
       var choice = parseInt($("#choice").val());
       if(choice == 0) {
          $("#owner span").remove();
       }
       var block = $("#" + choiceToBlock[choice]);
       
       var next = block.find(".rowCounter").length;
       var owners = $(".autocomplete").parent().find(".name");
       var ownerIds = $(".autocomplete").attr("value").split("¤¤");
       var res = $("#taskResolution").val();
       var dateStr = $(".datepicker").siblings("input").val();
       var dateTime = dateStr.length > 0 ? (dateStr + " 23:59") : "";
       var dateObj = null;
       if(dateStr.length > 0) {
         var dateParts = dateStr.split(".");
         dateObj = new Date(dateParts[2], dateParts[1]-1, dateParts[0], 23, 59);
       }
       
       owners.each(function(index, elem) {
          var taskHolder = $("<span />");
          var count = next + index;
          if(count > 0) {
             block.append($("<hr />"));
          }
          taskHolder.addClass("rowCounter");
          taskHolder.attr("data-row", count);
          taskHolder.css("position", "relative");

          var ownerElem = $(elem);
          var ownerName = ownerElem.html();

          var isGroup = "group" == ownerElem.attr("itemtype");
          var title = isGroup ? translate("workflow.task.ownerGroup") : translate("workflow.task.owner");
          var row1 = createValueRowWithTrashcan(title, ownerName, isGroup);
          var row2 = createReadMoreValueRow(translate("workflow.task.prop.resolution"), res);
          var row3 = createValueRow(translate("workflow.task.dueDate"), dateTime);
          
          taskHolder.append(row1);
          taskHolder.append(row2);
          taskHolder.append(row3);

          //hidden elements that are actually sent to server
          var prefix = "taskElementMap['" + choice + "'][" + count + "]";
          var userId = $("<input class='ownerId' type='hidden' />");
          userId.attr("name", prefix + ".ownerId");
          userId.attr("value", ownerIds[index]);

          var resolution = $("<input type='hidden' />");
          resolution.attr("name", prefix + ".resolution");
          resolution.attr("value", res);

          taskHolder.append(userId);
          taskHolder.append(resolution);

          if(dateStr.length > 0) {
              var date = $("<input type='hidden' />");
              date.attr("name", prefix + ".dueDate");
              date.attr("value", dateObj);
              taskHolder.append(date);
          }

          if(isGroup) {
             var groupMembers = $("<input class='groupMembers' type='hidden' />");
             groupMembers.attr("name", prefix + ".groupMembers");
             groupMembers.attr("value", ""); // values will be added in fillGroups()
             taskHolder.append(groupMembers);
             
             var modalId = "modal_" + choice + "_" + count;
             var modal = createModal(modalId, ownerName);
             var anchor = row1.find("a.modaltarget");
             anchor.attr("href", "#" + modalId);
             
             addFancyBox(anchor); // mDelta.js
             
             taskHolder.append(modal);
          }

          block.append(taskHolder);
       });
       block.show();
       fillGroups();
    }
    
    function fillGroups() {
       $(".rowCounter .unfilled").each(function() {
          toggleButtons(true);
          $(this).find(".modal-content").each(function() {
             var modalContent = $(this);
             modalContent.parents(".rowCounter").each(function() {
                var taskContainer = $(this);
                taskContainer.addClass("covered");
                taskContainer.spin("large", "#ff9000");
                var spinner = taskContainer.find(".spinner");
                var top = (taskContainer.height() / 2) + "px";
                spinner.css({"position": "relative", "top" : top });
             });
             var groupId = modalContent.parents(".rowCounter").find(".ownerId").first().attr("value");
             $.ajaxq("GroupMembersQueryQueue", {
                type: 'POST',
                queue: true,
                url: getContextPath() + '/m/ajax/search/groupmembers?q=' + groupId,
                dataType: 'json',
                contentType: 'application/json',
                success: function(response) {
                   modalContent.parent().removeClass("unfilled");
                   if(response.length == 0) {
                      modalContent.parents(".rowCounter").addClass("emptyGroup");
                   } else {
                      var ids = [];
                      $(response).each(function(i, groupMember) {
                         if(i > 0) {
                            modalContent.append($("<hr />").css("width", "100%"));
                         }
                         modalContent.append(createGroupMemberRow(groupMember));
                         ids.push(groupMember.userId);
                      });
                      var groupMembersHolder = modalContent.parents(".rowCounter").find(".groupMembers").first();
                      groupMembersHolder.attr("value", ids);
                      modalContent.parents(".rowCounter").addClass(modalContent.parent().attr("id"));
                   }
                   modalContent.parent().siblings(".spinner").remove();
                   modalContent.parents(".rowCounter").removeClass("covered");
                },
                error: function () {
                   var rowCounter = modalContent.parents(".rowCounter");
                   rowCounter.removeClass("unfilled");
                   rowCounter.removeClass("covered");
                   rowCounter.addClass("queryError");
                   modalContent.parent().siblings(".spinner").remove();
                }
             }).done(function() {
                toggleButtons(false);
             });
          });
       });
    }
    
    // disable delete buttons and submit button
    function toggleButtons(disable) {
       if(disable) {
          $("#okButton").attr("disabled", true);
          $(".rowCounter .remove").addClass("covered");
       } else {
          if(!$(".spinner").length) {
             $("#okButton").attr("disabled", false);
             $(".rowCounter .remove").removeClass("covered");
             $("#taskDelegationForm").trigger("allGroupsLoaded");
          }
       }
    }
    
    function createGroupMemberRow(userElement) {
       var valueRow = createValueRow(null, userElement.name);
       valueRow.attr("value", userElement.userId);
       var span = $("<span class='remove' />");
       var a = $("<a />");
       a.html("Eemalda");
       a.on("click", function() {
          var row = $(this).closest("p.valuerow");
          var userId = row.attr("value");
          var parent = row.parent();
          var modalId = $(this).parents("[id^=modal_]").first().attr("id");
          var groupMembers = $(".rowCounter." + modalId).find(".groupMembers").first();
          removeFromList(",", userId, groupMembers); // mDelta.js
          row.remove();
          parent.find("hr").remove();
          parent.children(".valuerow").each(function(i, elem){
             if(i > 0){
                $(elem).prepend($("<hr />").css("width", "100%"));
             }
          });
       });
       span.append(a);
       valueRow.addClass("rowactions");
       valueRow.find(".value").append(span);
       return valueRow;
    }
    
    function createReadMoreValueRow(label, str) {
       var div = $("<div class='readmore' />");
       var row = createValueRow(label, str);
       div.append(row);
       addShowMoreListener(div);
       return div;
    }
    
    function createValueRow(label, str) {
       return createValueRow(label, str, false);
    }
    
    function createValueRow(label, str, isGroup) {
       var valueRow = $("<p class='valuerow' />");
       var value = $("<span class='value' />");
       if(isGroup) {
          var a = $("<a class='modaltarget' />").html(str);
          value.append(a);
       } else {
          value.html(str);
       }
       if(label) {
          var desc = $("<span class='desc' />");
          desc.html(label + ":");
          valueRow.append(desc);
       }
       valueRow.append(value);
       return valueRow;
    }
    
    function createModal(id, groupName) {
       var div = $("<div class='unfilled modal-window' />");
       div.attr("id", id);
       div.css("display", "none");
       
       var header = $("<div class='modal-header' />");
       header.append($("<h1 />").html("Grupp: \"" + groupName + "\""));
       var content = $("<div class='modal-content' />'");
       
       var buttons = $("<div class='modal-buttons' />");
       var closeButton = $("<button type='button' onclick='javascript:$.fancybox.close()' />");
       closeButton.html("Sulge");
       buttons.append(closeButton);

       div.append(header);
       div.append(content);
       div.append(buttons);
       
       return div;
    }
    
    function createValueRowWithTrashcan(label, str, isGroup) {
       var valueRow = createValueRow(label, str, isGroup);
       var span = $("<span class='remove' />");
       var a = $("<a />");
       a.html("Eemalda");
       a.on("click", function() {
          var taskContainer = $(this).closest("span.rowCounter");
          var parent = taskContainer.parent();
          parent.find("hr").remove();
          taskContainer.remove();
          reorderTasks(parent);
       });
       span.append(a);
       valueRow.addClass("rowactions");
       valueRow.find(".value").append(span);
       return valueRow;
    }
    
    function reorderTasks(parent) {
       if(parent.find(".rowCounter").length == 0) {
          parent.removeAttr("style");
          parent.addClass("hidden");
       }
       parent.find(".rowCounter").each(function(i,elem){
          var task = $(elem);
          if(i > 0) {
             task.prepend("<hr />");
          }
          task.removeClass();
          task.addClass("rowCounter");
          task.attr("data-row", i);
          task.find("input").each(function(){
             var input = $(this);
             var name = input.attr("name");
             input.attr("name", name.replace(/\[\d*\]/, "["+ i +"]"));
          });
          var modal = task.find("[id^=modal_]");
          if(modal.length != 1 && task.find(".fancybox-placeholder").length == 1) { // modal has been opened and is currently replaced by placeholder
             modal = $(".fancybox-outer.fancybox-inner").find("[id^=modal_]");
          }
          if(modal.length == 1) {
             var oldId = modal.attr("id");
             var newId = oldId.substring(0, oldId.length - 1) + i;
             modal.attr("id", newId);
             task.addClass(newId);
             task.find(".modaltarget").attr("href", "#" + newId);
          }
       });
    }

    function validateAndAdd() {
       if(validate()) {
          addTask();
       }
    }
    
    var invalidFieldFound = false;
    
    function validate() {
       $(".invalid").each(function(i,elem) {
          $(elem).removeClass("invalid");
       });
       if($(".autocomplete").parent().find("li.token-input-token").length == 0) {
          $(".token-input-list").addClass("invalid");
       }
       var datePicker = $(".datepicker");
       if($("#choice").val() != "2" && datePicker.val().length == 0) { // 2 = teadmiseks
          datePicker.addClass("invalid");
       }
       if($(".invalid").length > 0) {
          invalidFieldFound = true;
          setupElementChangeListener();
       } else {
          $("#taskDelegationForm").off("change");
          invalidFieldFound = false;
       }
       return !invalidFieldFound;
    }
    
    function setupElementChangeListener() {
       $("#taskDelegationForm").change(function() {
          if(invalidFieldFound) {
             var invalid = $(".invalid");
             invalid.each(function(i, elem) {
                if($(elem).val().length > 0) {
                   $(elem).removeClass("invalid");
                }
             });
             if($(".autocomplete").parent().find("li.token-input-token").length > 0) {
                $(".token-input-list").removeClass("invalid");
             }
             if(invalid.length == 0) {
                invalidFieldFound = false;
             }
          }
       });
    }
    
    function addMessage(msg, type, clearPrevious) {
       if($("#mainNotifications").length != 0) {
          if(clearPrevious) {
             clearMessages();
          }
          var notification = $("<p />");
          notification.addClass(type);
          notification.html(msg);
          $("#mainNotifications").append(notification);
       } else {
          var notificationContainer = $("<div id='mainNotifications' class='notifications' />");
          var notification = $("<p />");
          notification.addClass(type);
          notification.html(msg);
          notificationContainer.append(notification);
          notificationContainer.insertAfter($("#pageTitle"));
       }
    }
    
    function clearMessages() {
       $("#mainNotifications").find("p").remove();
    }
    
    function allFieldsAreEmpty() {
       return $(".autocomplete").parent().find("li.token-input-token").length == 0 
          && $("#taskResolution").val().length == 0 
          && $(".datepicker").val().length == 0;
    }
    
    function hasOwner(choiches) {
       if($.inArray("0", choiches) != -1) { // user has responsible aspect
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

    var choice = 0;
    var choiches = [];
    var allTasksAdded = false;
    
    $(document).ready(function() {
       choice = $("#choice").val();
       var s = $(".autocomplete");
       if(choice == 0) {
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
          if(choice == 0) {
             suggester.addClass("singleEntry");
             setupSuggester(suggester, "/m/ajax/search/users");
          } else {
             suggester.removeClass("singleEntry");
             if(choice >= 2) {
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
          if(!allFieldsAreEmpty()) {
             if(!validate()) {
                return false;
             }
             if(!allTasksAdded) {
                addTask();
             }
             var validForm = hasOwner(choiches);
             allTasksAdded = false;
             if(validForm && $(".spinner").length > 0) { // wait for group members to load
                event.preventDefault();
                var form = $(this);
                $("#taskDelegationForm").on("allGroupsLoaded", function() {
                   allTasksAdded = true;
                   if(validForm) {
                      form.submit();
                   }
                });
             }
             return validForm;
          }
          return hasOwner(choiches);
       });
       
    });
  </script>

</tag:html>