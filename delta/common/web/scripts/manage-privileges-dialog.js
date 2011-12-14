$jQ(document).ready(function() {
   setScreenProtected(true, "luuakse õiguste tabelit");
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

   var privilegesTable = $jQ(".privileges");
   var headerCheckBoxes = $jQ(".grHeader td input[type='checkbox']:enabled", privilegesTable);
   headerCheckBoxes.change(headerCheckBoxChanged);
   var groups = $jQ(".tbGroup", privilegesTable);
   var updateHeaderCheckboxStateDisabled = false;

   function headerCheckBoxChanged() {
      if (updateHeaderCheckboxStateDisabled) {
         return;
      }
      setScreenProtected(true, "uuendatakse õiguste tabelit - headerCheckBoxChanged");
      updateHeaderCheckboxStateDisabled = true;
      var originalCheckBox = this;
      var jqCheckBox = $jQ(originalCheckBox);
      var isChecked = jqCheckBox.is(":checked");
      var allClasses = jqCheckBox.attr('class');
      var classes = allClasses.split(' ');
      var byClassSelector = "." + classes.join('.');
      var curGroupCurPermCBs = $jQ(byClassSelector + ":enabled", jqCheckBox.closest("tbody"));
      curGroupCurPermCBs.attr('checked', isChecked);
      curGroupCurPermCBs.each(function(index) {
         if (this != originalCheckBox) {
            $jQ(this).change();
         }
      });
      updateHeaderCheckboxStateDisabled = false;
      updateHeaderCheckboxState(false);
      setScreenProtected(false);
   }

   var bodyCheckBoxes = $jQ("td input[type='checkbox']:enabled", privilegesTable).filter(function(index) {
      return !$jQ(this).closest("tr").hasClass("grHeader");
   });
   bodyCheckBoxes.change(bodyCheckBoxChanged);
   function bodyCheckBoxChanged() {
      var jqCheckBox = $jQ(this);
      var isChecked = jqCheckBox.is(":checked");
      var allClasses = jqCheckBox.attr('class');

      var classes = allClasses.split(' ');
      // rest of this method used to be following 3 lines, but they are 20 times slower than next 25 lines
      // var byClassSelector = "." + classes.join('.');
      // var relatedCBs = $jQ(byClassSelector + ":enabled", privilegesTable);
      // relatedCBs.attr('checked', isChecked);
      var user = null;
      var permission = null;
      for ( var i in classes) {
         var styleClass = classes[i];
         if (styleClass.indexOf("userId_") === 0) {
            user = styleClass;
         }
         if (styleClass.indexOf("permission_") === 0) {
            permission = styleClass;
         }
         if (user && permission) {
            break;
         }
      }
      var userRows = $jQ("tr." + user, privilegesTable);
      if (userRows.length > 1) {
         var curCBRowEl = jqCheckBox.closest("tr").get(0);
         userRows.each(function() {
            if (this != curCBRowEl) {
               var otherGroupSameUserRow = $jQ(this);
               var relatedCB = otherGroupSameUserRow.find("." + permission + ":enabled");
               relatedCB.attr('checked', isChecked);
               addDependantPrivileges(relatedCB);
            }
         });
      }
   }

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
      return confirmWithPlaceholders(confirmMsg, userOrGroup);
   });

   // START: update header checkboxes of all groups
   function updateHeaderCheckboxState(firstUpdate) {
      if (updateHeaderCheckboxStateDisabled) {
         return; // at the end of reacting to headerCheckBox click this function will be called once
      }
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
         $jQ(this).change(function() {
            addDependantPrivileges($jQ(this));
            updateHeaderCheckboxState(false);
         });
      });
   }

   updateHeaderCheckboxState(true);
   bindOnChangeUpdateHeaderCheckboxState($jQ("tbody.groupless input[type='checkbox']"));
   // END: update header checkboxes of all groups

   setScreenProtected(false);
});