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
   var headerCheckBoxes = $jQ(".grHeader.groupless td input[type='checkbox']:enabled", privilegesTable);
   headerCheckBoxes.change(headerCheckBoxChanged);
   var group = $jQ(".tbGroup", privilegesTable);
   var updateHeaderCheckboxStateDisabled = false;

   // FIXME PRIV2 Grupi checkbox'ide sõltuvused ei toimi hetkel
   // (grupi kasutajaid linnutama ei pea, aga sama rea peal võiksid sõltuvad checkbox'id ka linnutatud saama).
   // pole kriitiline viga, kuna salvestamisel nagunii salvestatakse ka õiguse sõltuvused


   function headerCheckBoxChanged() {
      if (updateHeaderCheckboxStateDisabled) {
         return;
      }
      setScreenProtected(true, "uuendatakse õiguste tabelit - headerCheckBoxChanged");
      updateHeaderCheckboxStateDisabled = true;
      var originalCheckBox = this;
      var jqCheckBox = $jQ(originalCheckBox);
      var isChecked = jqCheckBox.is(":checked");
      var byClassSelector = "." + getAllClasses(jqCheckBox).join('.');
      var curGroupCurPermCBs = $jQ(byClassSelector + ":enabled", jqCheckBox.closest("tbody"));
      curGroupCurPermCBs.prop('checked', isChecked);
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
      // relatedCBs.prop('checked', isChecked);
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
               relatedCB.prop('checked', isChecked);
               addDependantPrivileges(relatedCB);
            }
         });
      }
   }

   // hack that manually triggers onmouseover/mouseout events for disabled checkboxes
   var unShownTooltips = $jQ("input[type='checkbox'].tooltip").filter(":disabled");
   $jQ.each(unShownTooltips, function(i, cb){
        var checkBox = $jQ(cb);
        var parent = checkBox.parent();
        var box = $jQ('<input>');
        box.addClass('dummy');
        box.attr('type', 'checkbox');
        
        box.css('opacity', '0');
        box.css('position', 'relative');
        checkBox.css('position', 'relative');
        
        var r = parseInt(checkBox.css('margin-right'), 10);
        var leftVal = checkBox.width()/2 + r/2;
        
        box.css('left', '-'+leftVal+'px');
        checkBox.css('left', leftVal+'px');
        
        box.appendTo(parent);
        box.mouseover(function(e) {
           box.siblings("input").trigger(e);
        });
        box.mouseout(function(e) {
           box.siblings("input").trigger(e);
        });
   });

   // confirm removing
   prependOnclick($jQ(".removePerson"), function(e) {
      return confirmWithPlaceholders(confirmMsgRemovePerson, getUserOrGroupDisplayName(e));
   });
   prependOnclick($jQ(".removeGroupWithUsers"), function(e) {
      return confirmWithPlaceholders(confirmMsgRemoveGroupWithUsers, getUserOrGroupDisplayName(e));
   });
   prependOnclick($jQ(".inlineGroupUsers"), function(e) {
      return confirmWithPlaceholders(confirmMsgInlineGroupUsers, getUserOrGroupDisplayName(e));
   });

   function getUserOrGroupDisplayName(e){
      return $jQ(e).closest('tr').children().eq(1).text();
   }

   // START: update header checkboxes of all permissions of groupless group
   function updateHeaderCheckboxState(firstUpdate) {
      if (updateHeaderCheckboxStateDisabled) {
         return; // at the end of reacting to headerCheckBox click this function will be called once
      }
      var groupRows = group.children();
      var groupHeaderRow = groupRows.eq(0);
      var groupUserRows = groupRows.slice(1);
      if (firstUpdate) {
         var groupBodyCBs = groupUserRows.find("td input[type='checkbox']:not(.dummy)");
         bindOnChangeUpdateHeaderCheckboxState(groupBodyCBs);
      }
      var groupHeaderCheckboxes = groupHeaderRow.find("td input[type='checkbox']:not(.dummy)");
      groupHeaderCheckboxes.each(function() {
         var groupPermissionHeaderCB = $jQ(this);
         var sameGroupAndPermissionCBs = getSameGroupAndPermissionCBs(groupPermissionHeaderCB);
         var allChecked = true;
         for ( var i = 1; i < sameGroupAndPermissionCBs.length; i++) {
            var permissionCB = sameGroupAndPermissionCBs.eq(i);
            var checked = permissionCB.is(":checked");
            if (allChecked && !checked) {
               allChecked = false;
            }
         }
         groupPermissionHeaderCB.prop('checked', allChecked);
      });
   }

   function getSameGroupAndPermissionCBs(groupPermissionHeaderCB){
      var stClassesArr = getAllClasses(groupPermissionHeaderCB);
      for ( var i = 0; i < stClassesArr.length; i++) {
         var styleClass = stClassesArr[i];
         if(styleClass.startsWith("permission_")){
            return group.find("input." + styleClass);
         }
      }
      throw "didn't find permission styleclass from "+stClassesArr;
   }

   function getAllClasses(jqElem){
      return jqElem.attr('class').split(' ');
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
                        relatedPermCBToUpdate.prop('checked', true);
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