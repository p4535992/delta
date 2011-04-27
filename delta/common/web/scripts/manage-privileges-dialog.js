$jQ(document).ready(function() {
   $jQ("td.expanded, td.collapsed").each(function() {
      setVisibility($jQ(this));
   });
   /**
    * When plus/minus icon of the users group is pressed, toggle visibility of group members
    */
   $jQ("td.expanded, td.collapsed").click(function(event) {
      var plusMinusIcon = $jQ(this);
      if (plusMinusIcon.hasClass("expanded")) {
         plusMinusIcon.removeClass("expanded").addClass("collapsed");
         var memberTrs = plusMinusIcon.parent().nextAll();
      } else if (plusMinusIcon.hasClass("collapsed")) {
         plusMinusIcon.removeClass("collapsed").addClass("expanded");
         var memberTrs = plusMinusIcon.parent().nextAll();
      }
      setVisibility(plusMinusIcon);
   });

   function setVisibility(plusMinusIcon) {
      var memberTrs = plusMinusIcon.parent().nextAll();
      if (plusMinusIcon.hasClass("expanded")) {
         memberTrs.show();
      } else if (plusMinusIcon.hasClass("collapsed")) {
         memberTrs.hide();
      }
   }

   // check/uncheck checkboxes of all group rows when group header checkbox is changed
   // and synchronize checkboxes of the same person on each group
   $jQ(".privileges td input[type='checkbox']:enabled").change(function() {
      var jqCheckBox = $jQ(this)
      var isChecked = jqCheckBox.is(":checked");
      var allClasses = jqCheckBox.attr('class');
      var classes = allClasses.split(' ');
      var byClassSelector = "." + classes.join('.');
      var relatedCBs;
      var isGroup = jqCheckBox.closest("tr").hasClass("grHeader");
      if (isGroup) {
         relatedCBs = $jQ(byClassSelector + ":enabled", jqCheckBox.closest("tbody"));
         var excluded = $jQ(".grHeader input[type='checkbox']");
         relatedCBs.not(excluded);
      } else {
         relatedCBs = $jQ(byClassSelector + ":enabled");
      }
      relatedCBs.attr('checked', isChecked);
      if (isGroup) {
         relatedCBs.each(function(index) {
            var checkBox = jqCheckBox.get(index);
            if (checkBox != this) {
               $jQ(this).trigger('change');
            }
         });
      }
   });

   // hack that manually triggers onmouseover/mouseout events for disabled checkboxes(based on mouse movement in parent element)
   var unShownTooltips = $jQ("input[type='checkbox'].tooltip").filter(":disabled").parent();
   unShownTooltips.mouseover(function(e) {
      e.stopPropagation();
      $jQ(this).children().trigger(e);
   });
   unShownTooltips.mouseout(function(e) {
      e.stopPropagation();
      $jQ(this).children().trigger(e);
   });

   // confirm removing
   prependOnclick($jQ(".deletePerson,.deleteGroup"), function(e) {
      var userOrGroup = $jQ(e).closest('tr').children().eq(1).text();
      return confirm(confirmMsg.replace('{0}', userOrGroup));
   });

   // START: update header checkboxes of all groups
   function updateHeaderCheckboxState(firstUpdate) {
      var groups = $jQ(".privileges .tbGroup");
      groups.each(function() {
         var group = $jQ(this);
         var groupRows = group.children();
         var groupHeaderRow = groupRows.eq(0);
         if (firstUpdate) {
            var groupBodyCBs = groupRows.nextAll(groupHeaderRow).find("td input[type='checkbox']");
            bindOnChangeUpdateHeaderCheckboxState(groupBodyCBs);
         }
         var groupHeaderCheckboxes = groupHeaderRow.find("td input[type='checkbox']");
         groupHeaderCheckboxes.each(function() {
            var groupPermissionHeaderCB = $jQ(this);
            var permissionSC = groupPermissionHeaderCB.attr("class");
            var sameGroupAndPermissionCBs = group.find("input." + permissionSC);
            var allChecked = true;
            for ( var i = 1; i < sameGroupAndPermissionCBs.length; i++) {
               var permissionCB = sameGroupAndPermissionCBs.eq(i);
               var checked = permissionCB.is(":checked");
               if (allChecked && !checked) {
                  allChecked = false;
               }
            }
            groupPermissionHeaderCB.attr('checked', allChecked);
         });
      });
   }

   function addDependantPrivileges(curCB) {
      if (curCB.is(":checked")) {
         for ( var curCBPriv in privDependencies) {
            var privName = privDependencies[curCBPriv];
            if (typeof privName == 'object' && curCB.hasClass("permission_" + curCBPriv)) {
               for ( var privToUpdateIndex in privName) {
                  if (privName.hasOwnProperty(privToUpdateIndex)) {
                     var privToUpdate = privName[privToUpdateIndex];
                     var relatedPermCBToUpdate = curCB.closest("tr").find("td input[type='checkbox'].permission_" + privToUpdate + ":enabled");
                     if (!relatedPermCBToUpdate.is(":checked")) {
                        relatedPermCBToUpdate.attr('checked', true);
                        relatedPermCBToUpdate.trigger('change');
                     }
                  }
               }
               break;
            }
         }
      }
   }

   function bindOnChangeUpdateHeaderCheckboxState(groupBodyCBs) {
      groupBodyCBs.each(function() {
         $jQ(this).change(function(eventObject) {
            updateHeaderCheckboxState(false);
            addDependantPrivileges($jQ(this));
         });
      });
   }

   updateHeaderCheckboxState(true);
   bindOnChangeUpdateHeaderCheckboxState($jQ("tbody.groupless input[type='checkbox']"));
   // END: update header checkboxes of all groups

});