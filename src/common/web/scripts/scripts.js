// http://www.hunlock.com/blogs/Mastering_The_Back_Button_With_Javascript
window.onbeforeunload = function () {
   // This fucntion does nothing.  It won't spawn a confirmation dialog
   // But it will ensure that the page is not cached by the browser.

   // When page is submitted, uses sees an hourglass cursor
   $jQ('*').css('cursor', 'wait');
}

function isIE7() {
	if (/MSIE (\d+\.\d+);/.test(navigator.userAgent)){ //test for MSIE x.x;
		 var ieversion=new Number(RegExp.$1) // capture x.x portion and store as a number
		 if (ieversion>=7 && ieversion < 8) {
			 return true;
		 }
	}
	return false;
}

function zIndexWorkaround()
{
    // If the browser is IE,
    if(isIE7())
    {
    	var zIndexNumber = 5000;
        $jQ("#container div").each(function() {
                $jQ(this).css('zIndex', zIndexNumber);
                $jQ(this).children('span').each(function() {
                	$jQ(this).css('zIndex', zIndexNumber);
                	zIndexNumber -= 10;
                });
                zIndexNumber -= 10;
        });
    }
}

function fixIEDropdownMinWidth(container, items) {
	if(isIE7()) {
		var max = 0;
		var ul = $jQ(container);
		ul.css('visibility', 'hidden').css('display', 'block');
		$jQ(items).each(function() {
			var li = $jQ(this);
			if(li.outerWidth() > max) {
				max = li.outerWidth();
			}
		}).each(function() {
			$jQ(this).css('min-width', max+'px');
		});
		ul.css('display', 'none').css('visibility', 'visible');
	}
}

function fixIESelectMinWidth() {
	if(isIE7()) {
		$jQ("#container-content td select").not('.with-pager select').not(".modalpopup-content-inner select[name$='_results']").each(function() {
			var select = $jQ(this);
			if(select.outerWidth() < 165) {
				select.css('width', '170px');
			}
		});
	}
}

// KAAREL: Do not delete! Yet... :)
//function addFocusHighlight() {
//	$jQ("#container-content td input, #container-content td select, #container-content td textarea,")
//	.not(".modalpopup input, .modalpopup select, .modalpopup textarea").each(function() {
//        var elm = $jQ(this);
//        
//        elm.focus(function() {
//            $jQ(this).parents("tr").addClass("row-highlight");
//        });
//        
//        elm.blur(function() {
//            $jQ(this).parents("tr").removeClass("row-highlight");
//        });
//    });
//}

/**
 * @return input string where ":" and "." are escaped, so that the result could be used by jQuery
 */
function escapeId4JQ(idToEscape) {
   return idToEscape.replace(/:/g, "\\:").replace(/\./g, "\\.");
}

/**
 * Change the status of close button on function/series/volume/case details dialog
 * @param status - status of the function/series/volume/case
 */
function processFnSerVolCaseCloseButton(status){
   var closeBtn = $jQ("#"+escapeId4JQ("dialog:close-button"));
   var finishBtn = $jQ("#"+escapeId4JQ("dialog:finish-button"));
   var closeBtn2 = $jQ("#"+escapeId4JQ("dialog:close-button-2"));
   var finishBtn2 = $jQ("#"+escapeId4JQ("dialog:finish-button-2"));
   var finishDisabled = finishBtn.attr("disabled");
   if(status != "avatud"){
      closeBtn.remove();
      closeBtn2.remove();
   } else if(finishDisabled || status == "avatud"){
      closeBtn.attr("disabled", finishDisabled);
      closeBtn2.attr("disabled", finishDisabled);
   }
}

function disableAndRemoveButton(buttonId) {
   var finishButton = $jQ('#' + escapeId4JQ(buttonId));
   finishButton.remove();
}
/**
 * append selection of source element to the element with id ending with "toItemIdSuffix" and beginning with the same id prefix as selectBox
 * @author Ats Uiboupin
 */
function appendSelection(source, targetId) {
   var appendSepparator = ', ';
   var targetElem = $jQ("#" + escapeId4JQ(targetId));
   var lable = $jQ('#' + escapeId4JQ(source.attr("id")) + ' :selected').text(); // using label not value!
   var lastToItemValue = targetElem.val();
   if (lastToItemValue.length != 0) {
      lastToItemValue = lastToItemValue + appendSepparator;
   }
   targetElem.val(lastToItemValue + lable);
};

/**
 * Add autoComplete functionality to input using given values to for suggestion(allows to enter also values not given with <code>valuesArray</code>)
 * @param inputId - id of the input component that should have autoComplete functionality based on values from <code>valuesArray</code>
 * @param valuesArray - values to be suggested int the input
 * @author Ats Uiboupin
 */
function setInputAutoCompleteArray(inputId, valuesArray){
   var jQInput = $jQ("#"+escapeId4JQ(inputId));
   var autoCompleter = jQInput.autocompleteArray(valuesArray, { minChars: -1, suggestAll: 1, delay: 50 });
   jQInput.focus(function() {
     jQInput.keydown();
  });
}

$jQ(document).ready(function () {
   /**
    * Forward click event to autocomplete input.
    * (We wrap autocomplete inputs to fix IE bug related to input with background image and text shadowing)
    */
   $jQ(".suggest-wrapper").click(function (e) {
      $jQ(this).children("input").focus();
   });
});

function showFooterTitlebar() {
	var bar = $jQ("#footer-titlebar");
	
	if($jQ(window).height() < bar.offset().top) {
		bar.css('visibility', 'visible'); // vivibility is used, because display: none; gives offset (0, 0)
	} else {
		bar.css('display', 'none'); // doesn't need to take the space
	}
}

function setPageScrollY() {
	var scrollTop = $jQ(window).scrollTop();
	$jQ('form').append('<input type="hidden" name="scrollToY" value="'+ scrollTop +'" />');
}

$jQ(document).ready(function()
{
	// Darn IE7 bugs...
	fixIESelectMinWidth();
	fixIEDropdownMinWidth("#titlebar .extra-actions .dropdown-menu", "#titlebar .extra-actions .dropdown-menu li");
	fixIEDropdownMinWidth("footer-titlebar .extra-actions .dropdown-menu", "#footer-titlebar .extra-actions .dropdown-menu li");
	fixIEDropdownMinWidth(".title-component .dropdown-menu.in-title", ".title-component .dropdown-menu.in-title li");
	zIndexWorkaround();
	
	showFooterTitlebar();
//	addFocusHighlight();
	
	$jQ(".toggle-tasks").click(function(){
		var nextTr = $jQ(this).toggleClass("expanded").closest("tr").next()[0];
		if(nextTr.style.display == 'none') { // bug in IE8
		     $jQ(nextTr).show();
		} else {
		     $jQ(nextTr).hide();                       
		}
	});
	
	var lastActiveInput = null;
	$jQ("input").focus(function() {
		lastActiveInput = $jQ(this);
	});
	
	$jQ("form").submit(function(e) {
		if(lastActiveInput != null) {
			if(lastActiveInput.attr("type") == "submit") {
				return true;
			}
			
			// Check special cases
			// modal popups
			if(lastActiveInput.parents('.modalpopup-content-inner').length > 0) {
				var searchLink = lastActiveInput.parent().parent().closest("td").children(".search"); // SearchGenerator?
				if(searchLink.length < 1) { // MultiValueEditor?
					searchLink = lastActiveInput.parent().parent().closest("td").children("table").children("tbody").children("tr").children("td").children(".search");
				}
				if(searchLink.length < 1) { // TaskListGenerator?
					searchLink = lastActiveInput.closest("span").children("table").children("tbody").children("tr").children("td").children("span").children(".search");
				}
				searchLink.click(); // hack to override clearFormhiddenParams function. Works with mouse but not with click()...
				lastActiveInput.next().click();
			} else if(lastActiveInput.attr("id") == $jQ("form").attr("name") + ":quickSearch") { // quick search
				var submitButton = $jQ('#search.panel input[id$=quickSearchBtn]').click();
				if(!isIE7()) {
					return true;
				}
			}
				
			return false; // otherwise return false, users don't want to lose their data :)
		}
	});
	
   /**
    * Binder for alfresco properties that are generated with ClassificatorSelectorAndTextGenerator.class
    * Binds all elements that have class="selectBoundWithText" with corresponding textAreas/inputs(assumed to have same id prefix and suffix specified with TARGET_SUFFIX) 
    * @author Ats Uiboupin
    */
   $jQ(".selectBoundWithText").each(function (intIndex)
   {
      var TARGET_SUFFIX = "select_target"; // corresponding textAreas/input
      var selectId = $(this).id;
      var textAreaId = selectId.substring(0, selectId.lastIndexOf(':') + 1) + TARGET_SUFFIX;
      var existingValue = $jQ("#" + escapeId4JQ(textAreaId)).text();
      var initialValue = $jQ('#' + escapeId4JQ(selectId) + ' :selected').text()
      if (initialValue != "" && existingValue == "")
      {
         var targetElem = $jQ("#" + escapeId4JQ(textAreaId));
         targetElem.val(initialValue);
      }
      $jQ(this).bind("change", function()
      {
         appendSelection($jQ(this), textAreaId)
      });
   });
   
   $jQ(".review-note-trimmed-comment a").click(function() {
	   $jQ(this).parent().css("display", "none").next().css("display", "block");
	   return false;
   });
   
   $jQ(".review-note-comment a").click(function() {
	   $jQ(this).parent().css("display", "none").prev().css("display", "block");
	   return false;
   });
   
});

/**
 * Open file in read-only mode (TODO: with webdav, if file is office document)
 * @return false
 */
function webdavOpenReadOnly() {
   // TODO: at the moment it just alwais provides a download link even for office documents
//   // if the link represents an Office document and we are in IE try and
//   // open the file directly to get WebDAV editing capabilities
//   var agent = navigator.userAgent.toLowerCase();
//   if (agent.indexOf('msie') != -1) {
//         var wordDoc = new ActiveXObject('SharePoint.OpenDocuments.3');
//         //var wordDoc = new CreateObject("SharePoint.OpenDocuments.3");
//         if (wordDoc) {
//            /** iOpenFlag codes:
//            0 - When checked out, or when the document library does not require check out, the user can read or edit the document.
//            1 - When another user has checked it out, the user can only read the document.
//            2 - When the current user has checked it out, the user can only edit the document.
//            3 - When the document is not checked out and the document library requires that documents be checked out to be edited, the user can only read the document, or check it out and edit it.
//            4 - When the current user has checked it out, the user can only edit the local copy of the document.
//            */
//            var iOpenFlag = 0;
//            var szAppendId = "";
////            alert("starting to open(\nwindow"+window+"\n,'"+this.href+"'\n,"+iOpenFlag+"\n, '"+szAppendId+"'\n)");
//            var success = wordDoc.ViewDocument3(window, this.href, iOpenFlag, szAppendId); // Open document in memory from webdaw
////            alert("success?"+success);
//            return false;
//         }
//   } else {
//      alert("To open using webdaw you must have IE compatible browser(for example firefox with IEtab or Internet Explorer)");
//   }
//   alert("regular file download");
   window.open(this.href, '_blank');// regular file saveAs/open by downloading it to HD
   return false;
}
$jQ(document).ready(function () {
   /**
    * Bind all links with "webdav-readOnly" class to function webdavOpenReadOnly to open them in read-only mode for office documents
    */
   $jQ('a.webdav-readOnly').click(webdavOpenReadOnly);
});

// Functions for changing number of item displayed in RichList components
function applySizeSpaces(e)
{
   return applySize(e, 'spaces-apply');
}

function applySizeContent(e)
{
   return applySize(e, 'content-apply');
}

function applySize(e, field)
{ 
   var formId = document.forms[0].name;
   document.forms[formId][formId+':act'].value= formId + ':' + field;
   document.forms[formId].submit();

   return false;
}

//-----------------------------------------------------------------------------
// MUIG
//-----------------------------------------------------------------------------

/* Dialog
-------------------------------------------------- */
var openModalContent = null;
var titlebarIndex = null;

function showModal(target, height){
   if(isIE7()) {
	   titlebarIndex = $jQ("#titlebar").css("zIndex");
	   $jQ("#titlebar").css("zIndex", "-1");
	   $jQ("#mydetails-panel *, #pref-panel *, #man-panel *").css("zIndex", "-1");
   }
   target = escapeId4JQ(target);
	if ($jQ("#overlay").length == 0) {
		$jQ("#" + target).before("<div id='overlay'></div>");
	}
	if (openModalContent != null){
		$jQ("#" + openModalContent).hide();
	}
	openModalContent = target;

	$jQ("#overlay").css("display","block");
	$jQ("#" + target).css("display","block");
	if (height != null) {
	   $jQ("#" + target).css("height",height);
	}
	$jQ("#" + target).show();

	return false;
}

$jQ(document).ready(function(){
   $jQ(".modalwrap select option").tooltip();
});

function hideModal(){
	if (openModalContent != null){
	  if(isIE7() && titlebarIndex != null) {
		  $jQ("#titlebar").css("zIndex", titlebarIndex); // aoscModal
	  }
	  $jQ("#" + openModalContent).hide();
      $jQ("#overlay").remove();
	}
	return false;
}

var propSheetValidateBtnFn = [];
var propSheetValidateSubmitFn = [];
var propSheetValidateFormId = '';
var propSheetValidateFinishId = '';
var propSheetValidateSecondaryFinishId = '';
var propSheetValidateNextId = '';
var propSheetFinishBtnPressed = false;
var propSheetNextBtnPressed = false;

// Should be called once per property sheet. If there are multiple propertySheets on the same
// page then the last caller overwrites formId, finishBtnId and nextBtnId, so those must be 
// equal to all property sheets on the same page.
function registerPropertySheetValidator(btnFn, submitFn, formId, finishBtnId, nextBtnId) {
   propSheetValidateBtnFn.push(btnFn);
   propSheetValidateSubmitFn.push(submitFn);
   propSheetValidateFormId = formId;
   propSheetValidateFinishId = finishBtnId;
   propSheetValidateSecondaryFinishId = finishBtnId + "-2";
   propSheetValidateNextId = nextBtnId;
}

function processButtonState() {
   for (var i = 0; i < propSheetValidateBtnFn.length; i++) {
      if (typeof propSheetValidateBtnFn[i] == 'function') { 
         propSheetValidateBtnFn[i]();
         var finishBtn = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateFinishId);
         var finishBtn2 = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateSecondaryFinishId);
         if (finishBtn == null || finishBtn.disabled == true) {
            break;
         }
      }
   }
   if (typeof postProcessButtonState == 'function') { postProcessButtonState(); }
}

function propSheetValidateSubmit() {
   var result = true;
   if (propSheetFinishBtnPressed || propSheetNextBtnPressed) {
      for (var i = 0; i < propSheetValidateSubmitFn.length; i++) {
         if (typeof propSheetValidateSubmitFn[i] == 'function') { 
            if (!propSheetValidateSubmitFn[i]()) {
               result = false;
               break;
            }
         }
      }
   }
   propSheetFinishBtnPressed = false;
   propSheetNextBtnPressed = false;
   return result;
}

$jQ(document).ready(function() {
   if (propSheetValidateBtnFn.length > 0 || propSheetValidateSubmitFn.length > 0) {
      document.getElementById(propSheetValidateFormId).onsubmit = propSheetValidateSubmit;
      document.getElementById(propSheetValidateFormId + ':' + propSheetValidateFinishId).onclick = function() { propSheetFinishBtnPressed = true; }
      var secondaryFinishButton = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateSecondaryFinishId);
      if(secondaryFinishButton != null) {
    	  secondaryFinishButton.onclick = function() { propSheetFinishBtnPressed = true; }
      }
      if (propSheetValidateNextId.length > 0) {
         document.getElementById(propSheetValidateFormId + ':' + propSheetValidateNextId).onclick = function() { propSheetNextBtnPressed = true; }
      }
      processButtonState();
   }
});

function togglePanel(divId) {
    $jQ(divId).toggle();
}

function togglePanelWithStateUpdate(divId, panelId, viewName) {
    togglePanel(divId);
    updateState(divId, panelId, viewName);
}

function updateState(divId, panelId, viewName) {
    var uri = getContextPath() + '/ajax/invoke/PanelStateBean.updatePanelState?panelId=' + panelId +
              '&panelState=' + $jQ(divId).is(":visible") + '&viewName=' + viewName;
    YAHOO.util.Connect.asyncRequest("GET", uri,
    {
        success: requestUpdatePanelStateSuccess
        ,failure: requestUpdatePanelStateFailure
    }, null);
}

function requestUpdatePanelStateSuccess(ajaxResponse) {
   var xml = ajaxResponse.responseXML.documentElement;
   // Set new value to view state, so when form is submitted next time, correct state is restored.
   document.getElementById("javax.faces.ViewState").value = xml.getAttribute('view-state');
}

function requestUpdatePanelStateFailure(ajaxResponse) {
    $jQ.log("Updating panel status in server side failed");
}
