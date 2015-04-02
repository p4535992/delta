var delegation = (function($){
   var invalidFieldFound = false;
   
   var validateForm = function() {
      $(".invalid").each(function(i,elem) {
         $(elem).removeClass("invalid");
      });
      if($(".autocomplete").parent().find("li.token-input-token").length == 0) {
         $(".token-input-list").addClass("invalid");
      }
      validateDueDate();
      if($(".invalid").length > 0) {
         invalidFieldFound = true;
         setupElementChangeListener();
      } else {
         $("#taskDelegationForm").off("change");
         invalidFieldFound = false;
      }
      return !invalidFieldFound;
   }
   
   var validateDueDate = function() {};
   var validateAdditional = function() {return true;};
   
   var setupElementChangeListener = function() {
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
   
   var chooseTaskTypeAndSetChoice = function() {}
   
   var choice = "task";
   
   var addTask = function() {
      var block = chooseTaskTypeAndSetChoice();
      
      var next = block.find(".rowCounter").length;
      var owners = $(".autocomplete").parent().find(".name");
      var ownerIds = $(".autocomplete").attr("value").split("¤¤");
      var res = $("#taskResolution").val();
      var dateStr = $(".datepicker").siblings("input").val();
      var dateTime = dateStr.length > 0 ? (dateStr + " 23:59") : "";
      var dateObj = null;
      if (dateStr.length > 0) {
         var dateParts = dateStr.split(".");
         dateObj = new Date(dateParts[2], dateParts[1] - 1, dateParts[0], 23, 59);
      }
      
      owners.each(function(index, elem) {
         var taskHolder = $("<span />");
         var count = next + index;
         if (count > 0) {
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
         row3.addClass("visibleDueDate");
         
         taskHolder.append(row1);
         taskHolder.append(row2);
         taskHolder.append(row3);
         
         // hidden elements that are actually sent to server
         var prefix = "taskElementMap['" + choice + "'][" + count + "]";
         var userId = $("<input class='ownerId' type='hidden' />");
         userId.attr("name", prefix + ".ownerId");
         userId.attr("value", ownerIds[index]);
         
         var resolution = $("<input type='hidden' />");
         resolution.attr("name", prefix + ".resolution");
         resolution.attr("value", res);
         
         taskHolder.append(userId);
         taskHolder.append(resolution);
         
         if (dateStr.length > 0) {
            var date = $("<input type='hidden' />");
            date.attr("name", prefix + ".dueDate");
            date.attr("value", dateObj);
            taskHolder.append(date);
         }
         
         if (isGroup) {
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
   
   var fillGroups = function() {
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
               spinner.css({
                  "position" : "relative",
                  "top" : top
               });
            });
            var groupId = modalContent.parents(".rowCounter").find(".ownerId").first().attr("value");
            $.ajaxq("GroupMembersQueryQueue", {
               type : 'POST',
               queue : true,
               url : getContextPath() + '/m/ajax/search/groupmembers',
               dataType : 'json',
               data: JSON.stringify({query : groupId}),
               contentType : 'application/json',
               success : function(response) {
                  modalContent.parent().removeClass("unfilled");
                  if (response.length == 0) {
                     modalContent.parents(".rowCounter").addClass("emptyGroup");
                  } else {
                     var ids = [];
                     $(response).each(function(i, groupMember) {
                        if (i > 0) {
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
               error : function() {
                  var rowCounter = modalContent.parents(".rowCounter");
                  rowCounter.removeClass("unfilled");
                  rowCounter.removeClass("covered");
                  rowCounter.addClass("queryError");
                  modalContent.parent().siblings(".spinner").remove();
               }
            }).done(function() {
               toggleButtons(false);
            }).fail(function() {
               toggleButtons(false);
            });
         });
      });
   }
   
   // disable delete buttons and submit button
   var toggleButtons = function(disable) {
      if (disable) {
         $("#okButton").attr("disabled", true);
         $(".rowCounter .remove").addClass("covered");
      } else {
         if (!$(".rowCounter .spinner").length) {
            $("#okButton").attr("disabled", false);
            $(".rowCounter .remove").removeClass("covered");
            $("#taskDelegationForm").trigger("allGroupsLoaded");
         }
      }
   }
   
   var createGroupMemberRow = function(userElement) {
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
         parent.children(".valuerow").each(function(i, elem) {
            if (i > 0) {
               $(elem).prepend($("<hr />").css("width", "100%"));
            }
         });
      });
      span.append(a);
      valueRow.addClass("rowactions");
      valueRow.find(".value").append(span);
      return valueRow;
   }
   
   var createReadMoreValueRow = function(label, str) {
      var div = $("<div class='readmore' />");
      var row = createValueRow(label, str);
      div.append(row);
      addShowMoreListener(div);
      return div;
   }
   
   var createValueRow = function(label, str) {
      return createValueRow(label, str, false);
   }
   
   var createValueRow = function(label, str, isGroup) {
      var valueRow = $("<p class='valuerow' />");
      var value = $("<span class='value' />");
      if (isGroup) {
         var a = $("<a class='modaltarget' />").html(str);
         value.append(a);
      } else {
         value.html(str);
      }
      if (label) {
         var desc = $("<span class='desc' />");
         desc.html(label + ":");
         valueRow.append(desc);
      }
      valueRow.append(value);
      return valueRow;
   }
   
   var createModal = function(id, groupName) {
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
   
   var createValueRowWithTrashcan = function(label, str, isGroup) {
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
   
   var reorderTasks = function(parent) {
      if (parent.find(".rowCounter").length == 0) {
         parent.removeAttr("style");
         parent.addClass("hidden");
      }
      parent.find(".rowCounter").each(function(i, elem) {
         var task = $(elem);
         if (i > 0) {
            task.prepend("<hr />");
         }
         var isEmptyGroup = task.hasClass("emptyGroup");
         task.removeClass();
         task.addClass("rowCounter");
         if(isEmptyGroup) {
            task.addClass("emptyGroup");
         }
         task.attr("data-row", i);
         task.find("input").each(function() {
            var input = $(this);
            var name = input.attr("name");
            input.attr("name", name.replace(/\[\d*\]/, "[" + i + "]"));
         });
         var modal = task.find("[id^=modal_]");
         if (modal.length != 1 && task.find(".fancybox-placeholder").length == 1) { // modal has been opened and is currently replaced by placeholder
            modal = $(".fancybox-outer.fancybox-inner").find("[id^=modal_]");
         }
         if (modal.length == 1) {
            var oldId = modal.attr("id");
            var newId = oldId.substring(0, oldId.length - 1) + i;
            modal.attr("id", newId);
            task.addClass(newId);
            task.find(".modaltarget").attr("href", "#" + newId);
         }
      });
   }
   
   var restoreInputFormInitialState = function() {
      var datePicker = $(".datepicker");
      datePicker.val("");
      $("#taskResolution").val("");
      var suggester = $(".autocomplete");
      suggester.attr("value", "");
      suggester.parent().find("li.token-input-token").remove();
      if(suggester.hasClass("singleEntry")) {
         suggester.siblings("ul").remove();
         suggester.addClass("singleEntry");
         setupSuggester(suggester, "/m/ajax/search/users");
      }
      if(initialDueDateStr.length > 0) {
         datePicker.val(initialDueDateStr);
         datePicker.siblings("input[type='hidden']").val(initialDueDate);
      }
   }
   
   var getEmptyGroupNames = function() {
      var emptyGroups = [];
      $(".emptyGroup").each(function(){
         $(this).find(".modaltarget").each(function() {
            emptyGroups.push($(this).text());
         });
      });
      return emptyGroups;
   }
   
   var initialDueDateStr, initialDueDate;
   var confirmed = false;
   
   // Public methods
   var module = {};
   
   module.markConfirmed = function(value) {
      if(value === undefined) {
         confirmed = true;
      } else {
         confirmed = value;
      }
   }
   
   module.isConfirmed = function() {
      return confirmed;
   }

   module.addEmptyGroupsMessage = function() {
      var emptyGroups = getEmptyGroupNames();
      console.log("Found " + emptyGroups.length + " empty groups");
      if(emptyGroups.length > 0) {
         var translation = translate("empty.groups");
         console.log(translation);
         var message = translation.substr(0, translation.indexOf(":") + 2);
         var last = emptyGroups.length - 1;
         for(var i = 0; i < emptyGroups.length; i += 1) {
            message += (emptyGroups[i] + (i === last ? "" : ", "));
         }
         console.log(message);
         addMessage(message, "warning", true);
      }
   }
   
   module.hasOnlyEmptyGroups = function() {
      return $(".rowCounter").length == $(".emptyGroup").length;
   }
   
   module.init = function(conf) {
      validateDueDate = (conf.validateDueDate != undefined) ? conf.validateDueDate : validateDueDate;
      chooseTaskTypeAndSetChoice = (conf.chooseTaskTypeAndSetChoice != undefined) ? conf.chooseTaskTypeAndSetChoice : chooseTaskTypeAndSetChoice;
      validateAdditional = (conf.validateAdditional != undefined) ? conf.validateAdditional : validateAdditional;
      initialDueDateStr = $(".datepicker").val();
      initialDueDate = $(".datepicker").siblings("input[type='hidden']").val();
   }
   
   module.validateAndAdd = function() {
      if(validateForm() && validateAdditional()) {
         addTask();
         restoreInputFormInitialState();
         clearMessages();
         $("#taskDelegationForm").on("allGroupsLoaded", module.addEmptyGroupsMessage);
         return true;
      }
      return false;
   }
   
   module.setChoice = function(value) {
      choice = value;
   }
   
   return module;
   
}(jQuery));

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

function isOwnerOrResolutionFieldFilled() {
   return $(".autocomplete").parent().find("li.token-input-token").length > 0 
      || $("#taskResolution").val().length > 0;
}