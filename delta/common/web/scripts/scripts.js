var delta = [];
delta['translations'] = [];

function addTranslation(key, value) {
   delta.translations[key] = value;
}

function getTranslation(key, defaultValue) {
   var translation = delta.translations[key];
   if(translation==null) {
      if(defaultValue!=undefined) {
         translation = defaultValue;
      } else {
         translation = "$$"+key+"$$";
      }
   }
   return translation;
}

//NB! All background AJAX calls must use $jQ.ajax({ mode: 'queue' !!!!
//This limits simultaneous AJAX calls to 1. This is needed because there may also be one "ajaxSubmit" request running
//And server limit of simultaneous connections in one session is 2, when more connections are made, older ones get dropped

var nextSubmitStaysOnSamePageFlag = false;
function nextSubmitStaysOnSamePage() {
   nextSubmitStaysOnSamePageFlag = true;
}

// http://www.hunlock.com/blogs/Mastering_The_Back_Button_With_Javascript
window.onbeforeunload = function () {
   // This fucntion does nothing.  It won't spawn a confirmation dialog
   // But it will ensure that the page is not cached by the browser.

   if(typeof $jQ.log !== "undefined"){
      $jQ.log('window.onbeforeunload');
   }

   if (nextSubmitStaysOnSamePageFlag) {
      nextSubmitStaysOnSamePageFlag = false;

      // Pause ajax queue for 20 seconds!
      $jQ.ajaxPause();
      window.setTimeout(function() {
         $jQ.ajaxResume();
      }, 20000);

   } else {
      // When page is submitted, user sees an hourglass cursor
      $jQ(".submit-protection-layer").show().focus();
      $jQ.ajaxDestroy(); // do not allow any new AJAX requests to start
      // if we are navigating to another page, and one AJAX request was cancelled already but is still working on server, then that makes 2 requests in server simultaneously
      // if any new AJAX requests would start, then RequestControlFilter connection limit 2 would be exceeded and user would see a blank page
   }
}

function isIE() {
	if (/MSIE (\d+\.\d+);/.test(navigator.userAgent)){ //test for MSIE x.x;
		return true;
	}
	return false;
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

function isIE8() {
	if (/MSIE (\d+\.\d+);/.test(navigator.userAgent)){ //test for MSIE x.x;
		 var ieversion=new Number(RegExp.$1) // capture x.x portion and store as a number
		 if (ieversion>7) {
			 return true;
		 }
	}
	return false;
}

function round(floatVal, decimals) {
   return (Math.round(floatVal * Math.pow(10, decimals)) / Math.pow(10, decimals)).toFixed(decimals);
}


function zIndexWorkaround(context)
{
   // If the browser is IE,
   if(isIE()) {

      var containerContext = context;
	  if (context) {
         if (!$jQ(context).parents().is('#container')) {
            return;
         }
      } else {
         containerContext = $jQ('#container');
      }

      if(isIE7()) {
         var zIndexNumber = 5000;
         $jQ("div", containerContext).each(function() {
            $jQ(this).css('zIndex', zIndexNumber);
            $jQ(this).children('span').each(function() {
               $jQ(this).css('zIndex', zIndexNumber);
               zIndexNumber -= 10;
            });
            zIndexNumber -= 10;
         });

         $jQ(".long-content-wrapper", context).mouseenter(function() {
            var td = $jQ(this).parent();
            var tr = td.parent();
            td.css({"z-index": "1000", "position": "relative"});
            td.siblings().css({"z-index": "-1", "position": "relative"});
            tr.siblings(".recordSetRow, .recordSetRowAlt").css({"z-index": "-1", "position": "relative"});
            var footer = tr.siblings(":last-child").children("td[align=center]");
            footer.children(".pager-wrapper").css("z-index", "-2");
            footer.children(".page-controls").css("z-index", "-1");
         }).mouseleave(function(index, el) {
            var td = $jQ(this).parent();
            var tr = td.parent();
            td.siblings().css("z-index", "0");
            tr.siblings(".recordSetRow, .recordSetRowAlt").css("z-index", "0");
            var footer = tr.siblings(":last-child").children("td[align=center]");
            footer.children(".pager-wrapper").css("z-index", "0");
            footer.children(".page-controls").css("z-index", "1");
         });
      }

      if(isIE8()) {
         $jQ(".long-content", context).css("top", "-17px");
      }
   }
}

function fixIEDropdownMinWidth(container, items, context) {
	if(isIE7()) {
		var max = 0;
		var ul = $jQ(container, context);
		ul.css('visibility', 'hidden').css('display', 'block');
		$jQ(items, context).each(function() {
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

function fixIESelectMinWidth(context) {
	if(isIE7()) {
      if (context) {
         if (!$jQ(context).parents().is('#container-content')) {
            return;
         }
      } else {
         context = $jQ('#container-content');
      }

      $jQ("td select").not('.with-pager select').not(".modalpopup-content-inner select[name$='_results']").not("#aoscModal-container-modalpopup select").not(".genericpicker-results").each(function() {
			var select = $jQ(this);
			if(select.outerWidth() < 165) {
				select.css('width', '170px');
			}
		});
	}
}

/**
 * @return input string where ":" and "." are escaped, so that the result could be used by jQuery
 */
function escapeId4JQ(idToEscape) {
   return idToEscape.replace(/:/g, "\\:").replace(/\./g, "\\.");
}

/**
 * Prepend given function to each element selected with jQBtnOrLink jQuery object.
 * @param jQBtnOrLink - jQuery object containing elements that need prepending function
 * @param prependFn - function to be called before existing onclick function is called
 * @return
 */
function prependOnclick(jQBtnOrLink, prependFn) {
   prependFunction(jQBtnOrLink, prependFn, "onclick");
}

function prependOnchange(jQHtmlElem, prependFn) {
   prependFunction(jQHtmlElem, prependFn, "onchange");
}

/**
 * Prepend given function to each element selected with jQHtmlElem jQuery object.
 * @param jQHtmlElem - jQuery object containing elements that need prepending function
 * @param prependFn - function to be called before existing function is called (function name given with eventAttributeName parameter)
 * @param eventAttributeName - attribute name that contains function that needs to be prepended
 * @return
 */
function prependFunction(jQHtmlElem, prependFn, eventAttributeName) {
   if(!eventAttributeName.startsWith("on")) {
      alert("invalid eventAttributeName '"+eventAttributeName+"'");
   }
   jQHtmlElem.each(function(index, domElem) {
      var jQElem = $jQ(this);
      var originalClickHandler = jQElem.attr(eventAttributeName);
      jQElem.attr(eventAttributeName, "return false;");
      var jQEventType = eventAttributeName.substring(2, eventAttributeName.length);
      jQElem.bind(jQEventType, function() {
         return prependFn(jQElem) && originalClickHandler();
      });
   });
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
var autocompleters = new Array();
function addAutocompleter(inputId, valuesArray){
   autocompleters.push(function() {
      var jQInput = $jQ("#"+escapeId4JQ(inputId));
      var autoCompleter = jQInput.autocompleteArray(valuesArray, { minChars: -1, suggestAll: 1, delay: 50, onItemSelect: function(li) { processButtonState(); } });
      autoCompleter.parent(".suggest-wrapper").click(function(){
         autoCompleter.trigger("suggest");
      });
      autoCompleter.bind("autoComplete", function(e, data){
         var ac = $jQ(this);
         if(!ac.parent().hasClass('noValChangeTooltip')) {
            var selected = ac.val();
            ac.attr("title", data.newVal);
         }
      });
      jQInput.focus(function() {
         jQInput.keydown();
      });
   });
}

function addSearchSuggest(clientId, containerClientId, pickerCallback, submitUri) {
   autocompleters.push(function () {
      var jQInput = $jQ("#"+escapeId4JQ(clientId));
      var uri = getContextPath() + "/ajax/invoke/AjaxSearchBean.searchSuggest";
      var suggest = jQInput.autocomplete(uri, {extraParams: {'pickerCallback' : pickerCallback}, minChars: 3, suggestAll: 1, delay: 50,
      onItemSelect: function (li) {
         processButtonState();
      },
      formatResult: function (data) {
         var end = data.indexOf("<");
         if (end > 0) {
            return data.substring(0, data.indexOf("<"));
         }
         return data;
      }
      });

      suggest.bind("autoComplete", function(e, data){
      	ajaxSubmit(clientId, containerClientId, [], submitUri, {'data' : data.newVal});
      });
      jQInput.focus(function() {
         jQInput.keydown();
      });
   });
}


// Autocompleters are applied here, this guarantees sequentiality
// When they were applied in random order from AJAX updating function, then something was broken
function applyAutocompleters() {
    $jQ.each(autocompleters, function() {
      this();
   });
   autocompleters = new Array();
}

function showFooterTitlebar() {
	var bar = $jQ("#footer-titlebar");

	if (bar.length > 0) {
   	if($jQ(window).height() < bar.offset().top) {
   		bar.css('visibility', 'visible'); // vivibility is used, because display: none; gives offset (0, 0)
   	} else {
   		bar.css('display', 'none'); // doesn't need to take the space
   	}
	}
}

function setPageScrollY() {
	var scrollTop = $jQ(window).scrollTop();
	$jQ('#wrapper form').append('<input type="hidden" name="scrollToY" value="'+ scrollTop +'" />');
}

function webdavOpen(url) {
   var showDoc = true;
   // if the link represents an Office document and we are in IE try and
   // open the file directly to get WebDAV editing capabilities
   var agent = navigator.userAgent.toLowerCase();
   if (agent.indexOf('msie') != -1)
   {
         var wordDoc = new ActiveXObject('SharePoint.OpenDocuments.1');
         if (wordDoc)
         {
            showDoc = !wordDoc.EditDocument(this.href);
         }
   }
   if (showDoc == true)
   {
      window.open(url, '_blank');
   }
   return false;
}

/**
 * Open file in read-only mode (TODO: with webdav, if file is office document)
 * @return false
 */
function webdavOpenReadOnly(url) {
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
   window.open(url, '_blank');// regular file saveAs/open by downloading it to HD
   return false;
}

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
   var formId = $jQ("#wrapper form").attr("name");
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
	target = escapeId4JQ(target);
	if ($jQ("#overlay").length == 0) {
		$jQ("#" + target).before("<div id='overlay'></div>");
	}
	if(isIE7()) {
		titlebarIndex = $jQ("#titlebar").css("z-index");
		$jQ("#titlebar").css("z-index", "-1");
	}
	if (openModalContent != null){
		$jQ("#" + openModalContent).hide();
	}
	openModalContent = target;

	$jQ("#overlay").css("display","block");
	$jQ("#" + target).css("display","block");
	$jQ("#" + target).find(".genericpicker-input").focus();
	if (height != null) {
	   $jQ("#" + target).css("height",height);
	}
	$jQ("#" + target).show();

	return false;
}

function hideModal(){
	if (openModalContent != null){
	  if(isIE7() && titlebarIndex != null) {
		  $jQ("#titlebar").css("zIndex", titlebarIndex);
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
      result = propSheetValidateSubmitCommon();
      if(!result){
         return result;
      }
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

function propSheetValidateSubmitCommon() {
   return validateDatePeriods();
}

function validateDatePeriods() {
   var endBeforeBegin = false;
   $jQ(".beginDate").each(function (index, beginDateElem) {
      // Get the date
      var bDate = $jQ(beginDateElem);
      var row = bDate.closest("tr");
      if (row == null) {
         return;
      }
      var endDate = getEndDate(beginDateElem, row);
      var oneDay = 24*60*60*1000; // hours*minutes*seconds*milliseconds
      if (endDate.val() != "") {
         var daysDiff = (endDate.datepicker('getDate') - bDate.datepicker('getDate') ) / oneDay;
         if(daysDiff<0){
            informUser(bDate, getTranslation('error_endDateBeforeBeginDate'), true);
            endBeforeBegin = true;
            return false; // break from each()
            }
         }
   });
   return !endBeforeBegin;
}

function getEndDate(beginDateElem, container){
   var endDates = container.find(".endDate");
   if(endDates.length>1){
      var dates = container.find(".beginDate, .endDate");
      for ( var i = 0; i < dates.length; i++) {
         if(dates[i]==beginDateElem && dates.length > i+1){
            return $jQ(dates[i+1]);
         }
      }
   } else {
      return endDates;
   }
}

function propSheetValidateOnDocumentReady() {
   if (propSheetValidateBtnFn.length > 0 || propSheetValidateSubmitFn.length > 0) {
      document.getElementById(propSheetValidateFormId).onsubmit = propSheetValidateSubmit;
      var finishBtn = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateFinishId);
      if(finishBtn){
         finishBtn.onclick = function() { propSheetFinishBtnPressed = true; }
      }
      var secondaryFinishButton = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateSecondaryFinishId);
      if(secondaryFinishButton != null) {
    	  secondaryFinishButton.onclick = function() { propSheetFinishBtnPressed = true; }
      }
      if (propSheetValidateNextId.length > 0) {
         var validateNextId = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateNextId);
         if (validateNextId != null){
            validateNextId.onclick = function() { propSheetNextBtnPressed = true; }
         }
      }
      processButtonState();
   }
}

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

    $jQ.ajax({
       type: 'POST',
       url: uri,
       mode: 'queue',
       success: requestUpdatePanelStateSuccess,
       error: requestUpdatePanelStateFailure,
       dataType: 'xml'
    });
}

function requestUpdatePanelStateSuccess(xml) {
   if (!xml) { // check that response is not empty
      return;
   }
   // Set new value to view state, so when form is submitted next time, correct state is restored.
   document.getElementById("javax.faces.ViewState").value = xml.documentElement.getAttribute('view-state');
}

function requestUpdatePanelStateFailure() {
    $jQ.log("Updating panel status in server side failed");
}

function ajaxError(request, textStatus, errorThrown) {
   var result = request.responseText.match(/<body>(.*)<\/body>/i);
   if (result) {
      $jQ(".submit-protection-layer").hide();
      $jQ('#wrapper').html(result[1]);
   } else {
      alert('Error during submit: ' + textStatus + "\nAfter clicking OK, the page will reload!");
      $jQ('#wrapper form').submit();
   }
}

function ajaxErrorHidden(request, textStatus, errorThrown) {
   $jQ.log('Error during AJAX query: ' + textStatus);
}

function ajaxSubmit(componentClientId, componentContainerId, submittableParams, uri) {
   ajaxSubmit(componentClientId, componentContainerId, submittableParams, uri, null);
}

function ajaxSubmit(componentClientId, componentContainerId, submittableParams, uri, payload) {
   // When page is submitted, user sees an hourglass cursor
   $jQ(".submit-protection-layer").show().focus();

   // Find all form fields that are inside this component
   var componentChildFormElements = $jQ('#' + escapeId4JQ(componentContainerId)).find('input,select,textarea');

   // Find additional hidden fields at the end of the page that HtmlFormRendererBase renders
   var hiddenFormElements = $jQ('input[type=hidden]').filter(function() {
      return componentClientId == this.name.substring(0, componentClientId.length) || $jQ.inArray(this.name, submittableParams) >= 0;
   });

   var postData = componentChildFormElements.add(hiddenFormElements).serialize();
   if (payload != null) {
      postData += "&" + $jQ.param(payload);
   }

   $jQ.ajax({
      type: 'POST',
      url: uri,
      data: postData,
      success: function (responseText) {
         ajaxSuccess(responseText, componentClientId, componentContainerId)
      },
      error: ajaxError,
      dataType: 'html'
   });
}

function ajaxSuccess(responseText, componentClientId, componentContainerId) {
   if (responseText) { // check that response is not empty
      // Split response
      var i = responseText.lastIndexOf('VIEWSTATE:');
      var html = responseText.substr(0, i);
      var viewState = responseText.substr(i + 10);

      // Update HTML
      $jQ('#' + escapeId4JQ(componentContainerId)).after(html).remove();

      // Update ViewState
      document.getElementById('javax.faces.ViewState').value = viewState;

      // Reset hidden fields
      var hiddenFormElements = $jQ('input[type=hidden]').filter(function() {
         return componentClientId == this.name.substring(0, componentClientId.length);
      }).each(function() {
         this.value = '';
      });

      // Reattach behaviour
      handleHtmlLoaded($jQ('#' + escapeId4JQ(componentContainerId)));
   }
   $jQ(".submit-protection-layer").hide();
}

//-----------------------------------------------------------------------------
//MENU ITEM COUNT UPDATE
//-----------------------------------------------------------------------------

// There is no concurrency in JS. One thread per page. Event handling is based on a queue.
// http://stackoverflow.com/questions/2078235/ajax-concurrency

function queueUpdateMenuItemCount(menuItemId, timeout) {
   if ($jQ('.menuItemCount[menuitemid=' + menuItemId + ']').length == 0) {
      return;
   }
   window.setTimeout(function () {
         updateMenuItemCount(menuItemId);
   }, timeout);
}

function updateMenuItemCount(menuItemId) {
   var uri = getContextPath() + '/ajax/invoke/MenuItemCountBean.updateCount?menuItemId=' + menuItemId;
   $jQ.ajax({
      type: 'POST',
      url: uri,
      mode: 'queue',
      success: function (responseText) {
         if ($jQ('.menuItemCount[menuitemid=' + menuItemId + ']').length == 0) {
            return;
         }
         if (responseText) { // check that response is not empty
            // if response is empty, then
            // * either user clicked on a link to navigate to another page and browser cancelled all in-progress AJAX requests but still fired this callback
            // * or the request was dropped by RequestControlFilter. normally should not happen

            if (responseText.length > 7) {
               // Response is too big, something is wrong. Probably session expired and response is CAS login page
               return;
            }

            var count = responseText == '0' ? '' : ' (' + responseText + ')';

            // Construct element text
            var text = $jQ($jQ('.menuItemCount[menuitemid=' + menuItemId + '] a')[0]).text();
            var i = text.lastIndexOf(' (');
            if (i == -1) {
               text += count;
            } else {
               text = text.substr(0, i) + count;
            }

            // Update on all elements with same menuitemid
            $jQ('.menuItemCount[menuitemid=' + menuItemId + '] a').text(text).attr('title', text);
         }

         //Continuous update disabled - if you stay on the same page, the counts are updated only once.
         //queueUpdateMenuItemCount(menuItemId, menuItemUpdateTimeout);
      },
      error: function(request, textStatus, errorThrown) {
         // Don't destroy ajax queue, let other request proceed
         ajaxErrorHidden(request, textStatus, errorThrown); // log messsage to FireBug
      },
      dataType: 'text'
   });
}

//-----------------------------------------------------------------------------
// Realy simple history stuff for back button
//-----------------------------------------------------------------------------

var historyListener = function(newLocation, historyData) {
   var debugHist = false;
   var curHashVal = historyStorage.get('CUR_HASH_VAL');
   if (debugHist) window.alert('Detected navigation event: \'' + newLocation + '\', history: \'' + historyData + '\', HASH: \'' + curHashVal + '\'');

   if (newLocation == 'files-panel') {
      // Special case for our only real anchor tag
      if (debugHist) window.alert('files-panel ei tee midagi!');
      window.dhtmlHistory.add(randomHistoryHash(), null);
   }
   else if (newLocation != '' && newLocation == curHashVal) {
      // Just a browser refresh of the same page, do nothing
      if (debugHist) window.alert('Given hash equals to current hash, no action!');
   }
   else if ($jQ('DIV.modalpopup.modalwrap DIV.modalpopup-header P.close A').filter(':visible:first').length > 0) {
     // There is a visible popup with close button
     if (debugHist) window.alert('Closing popup!');
     $jQ('DIV.modalpopup.modalwrap DIV.modalpopup-header P.close A').filter(':visible:first').click();
   }
   else if ($jQ('#' + escapeId4JQ('dialog:cancel-button')).filter(':visible:first').length > 0) {
     // There is a visible back/cancel button
     if (debugHist) window.alert('Clicking \'Tagasi\'!');
     $jQ('#' + escapeId4JQ('dialog:cancel-button')).filter(':visible:first').click();
   }
   else {
     if (debugHist) window.alert('Siit enam tagasiteed ei ole!');
     historyStorage.reset();
     historyStorage.put(window.dhtmlHistory.PAGELOADEDSTRING, true);
     window.dhtmlHistory.add(randomHistoryHash(), null);
   }
}

function randomHistoryHash() {
   var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
   var string_length = 8;
   var randomstring = '';
   for ( var i = 0; i < string_length; i++) {
      var rnum = Math.floor(Math.random() * chars.length);
      randomstring += chars.substring(rnum, rnum + 1);
   }
   historyStorage.put('CUR_HASH_VAL', randomstring);
   return randomstring;
}

window.dhtmlHistory.create( {
   debugMode : false,
   toJSON : function(o) {
      return $jQ.toJSON(o);
   },
   fromJSON : function(s) {
      return $jQ.parseJSON(s);
   }
});

function allowMultiplePageSizeChangers(){ // otherwise the last pageSizeChanger would overwrite value of all others
   var pageSizeSelects = $jQ("select[id$=selPageSize]");
   pageSizeSelects.each(function(i, select) {
   prependOnchange($jQ(select), function(){
      var jqThis = $jQ(select);
      pageSizeSelects.each(function(index, elem) {
          $jQ(elem).val(jqThis.val());
      });
      jqThis.parent().find('a').click();
      return true;
   });
   });
}

//-----------------------------------------------------------------------------
// DOCUMENT-READY FUNCTIONS
//-----------------------------------------------------------------------------

// These things need to be performed only once after full page load
$jQ(document).ready(function() {
   showFooterTitlebar();
   allowMultiplePageSizeChangers();
   $jQ(".admin-user-search-input").keyup(function(event) {
	   updateButtonState();
	   if (event.keyCode == 13) {
		     $jQ(this).next().click();
		     return false;
	   }
   });

   $jQ(".quickSearch-input").live('keydown', function(event) {
	   if (event.keyCode == 13) {
		    $jQ(this).next().click();
		    return false;
	   }
   });

   $jQ(".genericpicker-input").live('keydown', function (event) {
      if (event.keyCode == 13) {
	      $jQ(this).next().click();
			return false;
	   }
	});

   $jQ(".genericpicker-input").live('keyup', function (event) {
      var input = $jQ(this);
      var value = input.val();
      var callback = input.attr('datasrc');
      // If possible, submit filter value
      var filterValue = 0;
      var filter = input.prev();
      if (filter != null && filter.attr('value') != undefined) {
         filterValue = filter.attr('value');
      }

      if (!value) {
         return;
      }

      var backspace = event.keyCode == 8;
      var tbody = input.closest('tbody');
      var select = tbody.find('.genericpicker-results');
      if (value.length == 3 && !backspace) {
         $jQ.ajax({
            type: 'POST',
            url: getContextPath() + "/ajax/invoke/AjaxSearchBean.searchPickerResults",
            data: $jQ.param({'contains' : value, 'pickerCallback' : callback, 'filterValue' : filterValue}),
            mode: 'queue',
            success: function(responseText) {
               select.children().remove();
               var index = responseText.indexOf("|");
               select.attr("size", responseText.substring(0, index))
               select.append(responseText.substring(index + 1, responseText.length));
               tbody.find('.hidden').toggleClass('hidden');
            },
            error: ajaxError,
            dataType: 'html'
         });
      } else if (value.length > 3 || backspace) {
         select.children().each(function (i) {
            var option = $jQ(this);
            if (option.text().toLowerCase().indexOf(value.toLowerCase()) < 0) {
               option.hide();
            } else {
               option.show();
            }
         });
      }
   });

   $jQ(".errandReportDateBase").live('change', function (event) {
      // Get the date
      var elem = $jQ(this);
      if (elem != null) {
         // Find the report due date
         var errandEnd = elem.datepicker('getDate');
         var reportDue = new Date(errandEnd.getFullYear(), errandEnd.getMonth(), errandEnd.getDate() + 5);
         if (reportDue.getDay() == 6) { // Saturday
            reportDue = new Date(reportDue.getFullYear(), reportDue.getMonth(), reportDue.getDate() + 2);
         } else if (reportDue.getDay() == 0) { // Sunday
            reportDue = new Date(reportDue.getFullYear(), reportDue.getMonth(), reportDue.getDate() + 1);
         }
         // Set date
         elem.closest(".panel-border").find(".reportDueDate").datepicker('setDate',  reportDue);
      }
   });

   $jQ(".beginTotalCount,.endTotalCount").live('change', function (event) {
      // Get the date
      var elem = $jQ(this);
      if (elem == null) {
         return;
      }
      var row = elem.closest("tr");
      if (row == null) {
         return;
      }
      var totalDays = row.find(".totalDays");
      if(totalDays.length==0){
         return;
      }
      var oneDay = 24*60*60*1000; // hours*minutes*seconds*milliseconds
      if (elem.hasClass("beginTotalCount") && elem.val() != "") {
         var endDate = row.find(".endTotalCount");
         if (endDate.val() != "") {
            totalDays.val((endDate.datepicker('getDate') - elem.datepicker('getDate') + oneDay) / oneDay);
         }
      } else if (elem.hasClass("endTotalCount") && elem.val() != "") {
         var beginDate = row.find(".beginTotalCount");
         if (beginDate.val() != "") {
            totalDays.val((elem.datepicker('getDate') - beginDate.datepicker('getDate') + oneDay) / oneDay);
         }
      } else if (row.find(".beginTotalCount").val() == "" || row.find(".endTotalCount").val() == "") {
         totalDays.val("");
      }
  });

   $jQ(".eventBeginDate,.eventEndDate").live('change', function (event) {
      // Get the date
      var elem = $jQ(this);
      if (elem == null) {
         return;
      }


      var table = elem.closest("table");
      if (table == null) {
         return;
      }

      var row = table.closest("tr");
      if (row == null) {
         return;
      }


      var dateField = null;
      if (elem.hasClass("eventBeginDate")) {
         dateField = row.next().find(".errandBeginDate");
      } else if (elem.hasClass("eventEndDate")) {
         dateField = row.next().find(".errandEndDate");
      }
      if (dateField != null) {
         dateField.datepicker('setDate', elem.datepicker('getDate'));
      }
   });

   if(isIE()) {
	   // http://www.htmlcenter.com/blog/fixing-the-ie-text-selection-bug/
	   document.body.style.height = document.documentElement.scrollHeight + 'px';
   }

   if(isIE7()) {
	   $jQ(window).resize(function() {
		   var htmlWidth = $jQ("html").outerWidth(true);
		   var width =  htmlWidth < 920 ? 920 : htmlWidth;
		   $jQ("#wrapper").css("min-width", width + "px");
	   });
   }

   var suggesters = $jQ("span.suggest-wrapper>textarea");
   suggesters.live("change", function(e){
      var jqSuggester = $jQ(this);
      if(!jqSuggester.parent().hasClass('noValChangeTooltip')) {
         var selected = jqSuggester.val();
         jqSuggester.attr("title", jqSuggester.val());
      }
   });
   var selects = $jQ("select");
   selects.live("change", function(){
      var jqSelect = $jQ(this);
      if(!jqSelect.hasClass('noValChangeTooltip')) {
         setSelectTooltips(jqSelect);
      }
   });


   extendCondencePlugin();
   // extendCondencePlugin() MUST be called before tooltips are added on the following lines,
   // as condence plugin will make a copy of element for condenced text that would not get tooltips if created later
   $jQ(".tooltip").tooltip({
	   track: true
	   ,escapeHtml: true
	   ,tooltipContainerElemName: "p"
   });

   // Realy simple history stuff for back button
   window.dhtmlHistory.initialize();
   window.dhtmlHistory.addListener(historyListener);
   window.dhtmlHistory.add(randomHistoryHash(), null);


   jQuery(".dailyAllowanceDaysField, .dailyAllowanceRateField").live('change', function(event) {
      var elem = $jQ(this);
      // Calculate sum for current row
      var row = elem.closest("tr");
      var allowanceDays = parseInt(row.find(".dailyAllowanceDaysField").val());
      var allowanceRate = parseInt(row.find(".dailyAllowanceRateField").val());
      var sumField = row.find(".dailyAllowanceSumField");
      if(!allowanceDays || !allowanceRate) {
         return;
      }
      var sum = allowanceDays * (allowanceRate / 100) * sumField.attr("datafld");
      if (sum) {
         sumField.val(round(sum, 2));
      }

      // Sum all rows in this block and set total daily allowance sum
      var totalSum = 0;
      var sumFields = $jQ(".dailyAllowanceSumField", row.closest("table"));
      sumFields.each(function() {
         var sum = parseFloat($jQ(this).val());
         if(sum) {
            totalSum = totalSum + sum;
         }
      });

      row.closest("div").closest("tr").next().find(".dailyAllowanceTotalSumField").val(totalSum);
   });

   jQuery(".expectedExpenseSumField").live('keyup', function(event) {
      var elem = $jQ(this);
      var totalSum = 0;
      var sum = 0;
      var sumString;
      elem.closest("table").find(".expectedExpenseSumField").each(function () {
         sumString = $jQ(this).val();
         sumString = sumString.replace(",", ".");
         sum = parseFloat(sumString);
         if(sum) {
            totalSum += sum;
         }
      });

      var totalField = elem.closest("div").closest("tr").next().find(".expensesTotalSumField");
      totalField.val(totalSum);
   });

   jQuery(".invoiceTotalSum, .invoiceVat").live('change', function(event) {
      // assume there is only one field available for each value
      var invoiceTotalSum = getFloatOrNull($jQ(".invoiceTotalSum").val());
      var invoiceVat = getFloatOrNull($jQ(".invoiceVat").val());
      var invoiceSum = $jQ(".invoiceSum");
      if(isNaN(invoiceTotalSum) || isNaN(invoiceVat)){
         invoiceSum.val('');
         return;
      }
      invoiceSum.val(round(invoiceTotalSum - invoiceVat, 2));
   });

   toggleSubrow.init();
   toggleSubrowToggle.init();

   handleHtmlLoaded(null, selects);
});

var toggleSubrowToggle = {
      init : function(){
         $jQ(".trans-subrow-toggle").children("a").click(this.clickIt);
      },
      clickIt : function(){
         var table = $jQ(this).parent().nextAll("div.trans-scroll:first").find("table:first");
         var anchor = table.find("td.trans-toggle-subrow").find("a");
         var subrow = table.find(".trans-subrow,.trans-subrowAlt");

         if($jQ(this).hasClass("open")) {
            subrow.css("display","");
            anchor.addClass("open");
         }
         else {
            subrow.css("display","none");
            anchor.removeClass("open");
         }
         return false;
      }
};
var toggleSubrow = {
      init : function(){
         $jQ("td.trans-toggle-subrow").children("a").click(this.clickIt);
      },
      clickIt : function(){
         var anchor = $jQ(this);
         var row    = anchor.parents("tr:eq(0)");
         var subrow = row.next("tr.trans-subrow,.trans-subrowAlt");

         if(subrow.is(":hidden")) {
            subrow.css("display","");
            anchor.addClass("open");
         }
         else {
            subrow.css("display","none");
            anchor.removeClass("open");
         }
         return false;
      }
};

function getFloatOrNull(originalSumString){
   var sumString = originalSumString.replace(",", ".");
   sumString = sumString.replace(" ", "");
   if(sumString == ""){
      return 0;
   }
   return parseFloat(sumString);
}

function initSelectTooltips(selects) {
   selects.each(function(){
      var jqSelect = $jQ(this);
      if(!jqSelect.hasClass('noValChangeTooltip')) {
         var existingTooltip = jqSelect.attr("title");
         if(existingTooltip==null || existingTooltip.trim().length == 0) {
            setSelectTooltips(jqSelect);
         }
      }
   });
}

function setSelectTooltips(jqSelect){
   var selected = jqSelect.find("option:selected");
   if(!jqSelect.hasClass('noOptionTitle')){
      jqSelect.attr("title", selected.text());
   } else {
      jqSelect.attr("title", selected.attr("title"));
   }
}
/**
 * extend jQuery Condence plugin so that
 * 1) condencing to specific number of chars could be performed based on styleClass (number of characters must be specified in the styleclass right after the text "condence"):)
 * 2) showMore/showLess text could be left out if condence styleclass ends with "-"
 */
function extendCondencePlugin() {
   var condencers = jQuery("[class*=condence]" /*, context*/);
   condencers.each(function(){
      var p = this.className.match(/condence(\d+)?(\-)?/i);
      var condenceAtChar = p ? parseInt('0'+p[1], 10) : 200;
      var condenceAtChar = p ? parseInt('0'+p[1], 10) : 200;
      var condence = p ? parseInt('0'+p[1], 10) : 200;
      var moreTxt = "... ";
      if(!(p && p[2] == "-")){
         moreTxt = getTranslation('jQuery.condence.moreText');
      }
      jQuery(this).condense({
         moreSpeed: 0,
         lessSpeed: 0,
         moreText: moreTxt,
         lessText: getTranslation('jQuery.condence.lessText'),
         ellipsis: "",
         condensedLength: condenceAtChar,
         minTrail: moreTxt.length,
         strictTrim: true  // assume that condense content is text (i.e. doesn't contain html elements)
                           // and don't search for word breaks for triming text
         }
       );
   });
}

// These things need to be performed
// 1) once after full page load
// *) each time an area is replaced inside the page
function handleHtmlLoaded(context, selects) {
   applyAutocompleters();

   initSelectTooltips((selects==undefined) ? $jQ("select") : selects);

   //initialize all expanding textareas
   var expanders = jQuery("textarea[class*=expand]", context);
   expanders.TextAreaExpander();
   if(jQuery.browser.msie) {
      // trigger size recalculation if IE, because e.scrollHeight may be inaccurate before keyup() is called
      expanders.keyup();
      jQuery.fn.TextAreaExpander.ieInitialized = true;
   }

   // datepicker
   jQuery("input.date", context).not("input[readonly]").datepicker({ dateFormat: 'dd.mm.yy', changeMonth: true, changeYear: true, nextText: '', prevText: '', yearRange: '-100:+100', duration: '' });
   jQuery("input.sysdate", context).not("input[readonly]").datepicker({ dateFormat: 'dd.mm.yy', changeMonth: true, changeYear: true, nextText: '', prevText: '', defaultDate: +7, yearRange: '-100:+100', duration: '' });

   // trigger keyup event (for validation & textarea resize) on paste. Can't use live() because of IE
   $jQ("textarea, input[type='text']", context).bind("paste", function(){
      var input = $jQ(this);// pasted value not jet assigned
      setTimeout(function() {
         input.keyup();
      }, 100);
   });

   // Darn IE7 bugs...
   fixIESelectMinWidth(context);
   fixIEDropdownMinWidth("#titlebar .extra-actions .dropdown-menu", "#titlebar .extra-actions .dropdown-menu li", context);
   fixIEDropdownMinWidth("footer-titlebar .extra-actions .dropdown-menu", "#footer-titlebar .extra-actions .dropdown-menu li", context);
   fixIEDropdownMinWidth(".title-component .dropdown-menu.in-title", ".title-component .dropdown-menu.in-title li", context);
   zIndexWorkaround(context);

   if(isIE()) {
      var jqSelects = (selects==undefined) ? $jQ("select") : selects;
      jqSelects.each(function(){
         var jqSelect = $jQ(this);
         if(!jqSelect.hasClass('noOptionTitle')) {
            jqSelect.children().each(function() {
               $jQ(this).attr('title', $jQ(this).text());
            });
         }
      });
   }

   /**
    * Open Office documents directly from server
    */
   $jQ('a.webdav-open', context).click(function () {
      var path = $jQ(this).attr('href');
      var uri = getContextPath() + '/ajax/invoke/AjaxBean.isFileLocked?path=' + path;
      $jQ.ajax({
        type: 'POST',
        url: uri,
        mode: 'queue',
        success: function (responseText) {
          if (responseText.indexOf("NOT_LOCKED") > -1) {
             webdavOpen(path);
          } else if (confirm(getTranslation("webdav_openReadOnly").replace("#", responseText))) {
             webdavOpenReadOnly(path);
          } else {
             return false;
          }
        },
        error: ajaxError,
        datatype: 'html'
      });
      return false;
   });
   $jQ('a.webdav-readOnly', context).click(function () {
      webdavOpenReadOnly($jQ(this).attr('href'));
   });

   $jQ(".modalwrap select option", context).tooltip();

   /**
    * Forward click event to autocomplete input.
    * (We wrap autocomplete inputs to fix IE bug related to input with background image and text shadowing)
    */
   $jQ(".suggest-wrapper", context).click(function (e) {
      $jQ(this).children("textarea").focus();
   });

   $jQ(".toggle-tasks", context).click(function(){
      var nextTr = $jQ(this).toggleClass("expanded").closest("tr").next()[0];
      if(nextTr.style.display == 'none') { // bug in IE8
           $jQ(nextTr).show();
      } else {
           $jQ(nextTr).hide();
      }
   });

   if(context != null) {
	   $jQ("input", context).focus(function() {
		      lastActiveInput = $jQ(this);
	   });
   }

   /**
    * Binder for alfresco properties that are generated with ClassificatorSelectorAndTextGenerator.class
    * Binds all elements that have class="selectBoundWithText" with corresponding textAreas/inputs(assumed to have same id prefix and suffix specified with TARGET_SUFFIX)
    * @author Ats Uiboupin
    */
   $jQ(".selectBoundWithText", context).each(function (intIndex)
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

   /**
    * Add onChange functionality to jQuery change event (we can't use onChange attribute because of jQuery bug in IE)
    * @author Riina Tens
    */
   $jQ("[class^=selectWithOnchangeEvent]", context).each(function (intIndex, selectElement)
   {
      var classString = selectElement.className;
      var onChangeJavascript = classString.substring(classString.lastIndexOf('¤¤¤¤') + 4);
      if(onChangeJavascript != ""){
         if(classString.indexOf('selectWithOnchangeEventParam') == 0){
            $jQ(this).bind("change", function()
            {
               //assume onChangeJavascript contains valid function body
               eval("(function(currElId) {" + onChangeJavascript + "}) ('" + selectElement.id + "');");
            });
         }
         else{
            $jQ(this).bind("change", function()
            {
               eval("(function() {" + onChangeJavascript + "}) ();");
            });
         }
      }
   });

   $jQ(".admin-user-search-input", context).keyup(function(event) {
       updateButtonState();
       if (event.keyCode == '13') {
          $jQ(this).next().click();
       }
    });

   $jQ(".genericpicker-input:visible").focus();

	propSheetValidateOnDocumentReady();

	// this method should be called last in handleHtmlLoaded as it displays alerts and possibly submits page
	confirmWorkflow();
}

//-----------------------------------------------------------------------------
//DIGITAL SIGNATURE
//-----------------------------------------------------------------------------

function processCert(certHex, certId) {
 $jQ('#signApplet').hide();
 $jQ('#signWait').show();
 return oamSubmitForm('dialog','dialog:dialog-body:processCert',null,[['certHex', certHex], ['certId', certId]]);
}

function signDocument(signatureHex) {
 $jQ('#signApplet').hide();
 $jQ('#signWait').show();
  return oamSubmitForm('dialog','dialog:dialog-body:signDocument',null,[['signatureHex', signatureHex]]);
}

function cancelSign() {
 $jQ('#signApplet').hide();
 $jQ('#signWait').show();
 return oamSubmitForm('dialog','dialog:dialog-body:cancelSign',null,[[]]);
}

function driverError() {
}

//Some parts based on https://digidoc.sk.ee/include/JS/idCard.js
//Some parts based on https://id.smartlink.ee/plugin_tests/legacy-plugin/load-legacy.js
function loadSigningPlugin(operation, hashHex, certId, path) {

 if (isIE())
 {
    //activeX
    document.getElementById('pluginLocation').innerHTML = '<OBJECT id="IdCardSigning" codebase="' + path + '/applet/EIDCard.cab#Version=1,0,2,4" classid="clsid:FC5B7BD2-584A-4153-92D7-4C5840E4BC28"></OBJECT>';

    if (!this.isActiveXOK(document.getElementById('IdCardSigning')))
    {
       $jQ('#signWait').html('ID-kaardi draiverid ei ole paigaldatud!');
       return;
    }
    var plugin = document.getElementById('IdCardSigning');

    if (operation == 'PREPARE') {
       var certHex = plugin.getSigningCertificate();
       if (certHex) {
          var certId = plugin.selectedCertNumber;
          processCert(certHex, certId);
       } else {
          $jQ('#signWait').html('Sertifikaati ei valitud või sertifikaadid on registreerimata!');
       }

    } else if (operation == 'FINALIZE') {
       var signedHashHex = plugin.getSignedHash(hashHex, certId);
       if (signedHashHex) {
          signDocument(signedHashHex);
       } else {
          $jQ('#signWait').html('Allkirjastamine katkestati või ID-kaart ei ole lugejas!');
       }
    }
 }
 else if (navigator.userAgent.indexOf('Firefox') != -1)
 {
    navigator.plugins.refresh();
    if (!navigator.mimeTypes['application/x-idcard-plugin']) {
       $jQ('#signWait').html('ID-kaardi draiverid ei ole paigaldatud!');
       return;
    }

    var s = document.createElement('embed');
    s.id           = 'IdCardSigning';
    s.type         = 'application/x-idcard-plugin';
    s.style.width  = "1px";
    s.style.height = "1px";
    var b = document.getElementsByTagName("body")[0];
    b.appendChild(s); // why does it work when appended here?

    var plugin = document.getElementById('IdCardSigning');
    $jQ.log('Loaded Mozilla plugin ' + plugin.getVersion());

    if (operation == 'PREPARE') {
       var response = eval('' + plugin.getCertificates());
       if (response.returnCode != 0 || response.certificates.length < 1) {
           firefoxSigningPluginError(response.returnCode);
           return;
       }

       /* Find correct certificate */
       var reg = new RegExp("(^| |,)Non-Repudiation($|,)");
       var cert = null;
       for (var i in response.certificates) {
           cert = response.certificates[i];
           if (reg.exec(cert.keyUsage)) break;
       }

       if (cert) {
          var certHex = cert.cert;
          var certId = cert.id;
          processCert(certHex, certId);
       } else {
          $jQ('#signWait').html('Sertifikaati ei leitud!');
       }

    } else if (operation == 'FINALIZE') {
       var response = eval('' + plugin.sign(certId, hashHex));
       if (response.returnCode != 0) {
          firefoxSigningPluginError(response.returnCode);
          return;
       }

       var signedHashHex = response.signature;
       signDocument(signedHashHex);
    }
 }
 else
 {
    $jQ('#signWait').html('Digiallkirjastamine ei ole toetatud!');
/*
    //applet
    $jQ('#signWait').hide();
    $jQ('#pluginLocation').show();

    document.getElementById('pluginLocation').innerHTML = '<embed'
       + ' id="signApplet"'
       + ' type="application/x-java-applet;version=1.4"'
       + ' width="400"'
       + ' height="80"'
       + ' pluginspage="http://javadl.sun.com/webapps/download/GetFile/1.6.0_18-b07/windows-i586/xpiinstall.exe"'
       + ' java_code="SignApplet.class"'
       + ' java_codebase="' + path + '/applet"'
       + ' java_archive="SignApplet_sig.jar, iaikPkcs11Wrapper_sig.jar"'
       + ' NAME="SignApplet"'
       + ' MAYSCRIPT="true"'
       + ' LANGUAGE="EST"'
       + ' FUNC_SET_CERT="window.processCert"'
       + ' FUNC_SET_SIGN="window.signDocument"'
       + ' FUNC_CANCEL="window.cancelSign"'
       + ' FUNC_DRIVER_ERR="window.driverError"'
       + ' DEBUG_LEVEL="4"'
       + ' OPERATION="' + operation + '"'
       + ' HASH="' + hashHex + '"'
       + ' TOKEN_ID=""'
       + ' LEGACY_LIFECYCLE="true"'
       + '><noembed></noembed></embed>';
*/
 }
}

function firefoxSigningPluginError(returnCode) {
   $jQ.log('returnCode=' + returnCode);
   if (returnCode == 1) {
      $jQ('#signWait').html('Allkirjastamine katkestati!');
   } else if (returnCode == 12) {
      $jQ('#signWait').html('ID-kaart ei ole lugejas!');
   } else if (returnCode == 16) {
      $jQ('#signWait').html('Vale ID-kaart on lugejas!');
   } else {
      $jQ('#signWait').html('Allkirjastamine ebaõnnestus (vea kood ' + returnCode + ')!');
   }
}

//https://digidoc.sk.ee/include/JS/idCard.js
function isActiveXOK(plugin) {

 if (plugin == null)
    return false;

 if (typeof(plugin) == "undefined")
    return false;

 if (plugin.readyState != 4 )
    return false;

 if (plugin.object == null )
    return false;

 return true;
}

function sendToSapManually(){
   return showModal('entrySapNumber_popup');
}

function confirmWorkflow(){
   var confirmationMessagesSelect = $jQ("[class='workflow-confirmation-messages']").get(0);
   if(confirmationMessagesSelect == undefined){
      return false;
   }
   for (i = confirmationMessagesSelect.children.length - 1; i >= 0; i--) {
      if (!confirm(confirmationMessagesSelect.children[i].value)){
         return false;
      }
   }
   $jQ("[class='workflow-after-confirmation-link']").get(0).click();   
}