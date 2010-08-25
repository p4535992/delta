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

// http://www.hunlock.com/blogs/Mastering_The_Back_Button_With_Javascript
window.onbeforeunload = function () {
   // This fucntion does nothing.  It won't spawn a confirmation dialog
   // But it will ensure that the page is not cached by the browser.

   // When page is submitted, user sees an hourglass cursor
   $jQ(".submit-protection-layer").show();
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

      $jQ("td select").not('.with-pager select').not(".modalpopup-content-inner select[name$='_results']").not("#aoscModal-container-modalpopup select").each(function() {
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

function webdavOpen() {
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
      window.open(this.href, '_blank');
   }
   return false;
}

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

function propSheetValidateOnDocumentReady() {
   if (propSheetValidateBtnFn.length > 0 || propSheetValidateSubmitFn.length > 0) {
      document.getElementById(propSheetValidateFormId).onsubmit = propSheetValidateSubmit;
      document.getElementById(propSheetValidateFormId + ':' + propSheetValidateFinishId).onclick = function() { propSheetFinishBtnPressed = true; }
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

function ajaxSubmit(componentId, componentClientId, componentContainerId, formClientId, viewName, submittableParams) {
   // When page is submitted, user sees an hourglass cursor
   $jQ(".submit-protection-layer").show();

   var uri = getContextPath() + '/ajax/invoke/AjaxBean.submit?componentId=' + componentId + '&componentClientId=' + componentClientId + '&viewName=' + viewName;

   // Find all form fields that are inside this component
   var componentChildFormElements = $jQ('#' + escapeId4JQ(componentContainerId)).find('input,select,textarea');

   // Find additional hidden fields at the end of the page that HtmlFormRendererBase renders
   var hiddenFormElements = $jQ('input[type=hidden]').filter(function() {
      return componentClientId == this.name.substring(0, componentClientId.length) || $jQ.inArray(this.name, submittableParams) >= 0;
   });

   $jQ.ajax({
      type: 'POST',
      url: uri,
      data: componentChildFormElements.add(hiddenFormElements).serialize(),
      success: function (responseText) {
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

         $jQ(".submit-protection-layer").hide();
      },
      error: function ajaxError(request, textStatus, errorThrown) {
         var result = request.responseText.match(/<body>(.*)<\/body>/i);
         if (result) {
            $jQ(".submit-protection-layer").hide();
            $jQ('#wrapper').html(result[1]);
         } else {
            alert('Error during submit: ' + textStatus + "\nAfter clicking OK, the page will reload!");
            $jQ('#' + formClientId).submit();
         }
      },
      dataType: 'html'
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

//-----------------------------------------------------------------------------
// DOCUMENT-READY FUNCTIONS
//-----------------------------------------------------------------------------

// These things need to be performed only once after full page load
$jQ(document).ready(function() {
   showFooterTitlebar();
   
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
   
   extendCondencePlugin();
   // extendCondencePlugin() MUST be called before tooltips are added on the following lines,
   // as condence plugin will make a copy of element for condenced text that would not get tooltips if created later
   $jQ(".tooltip").tooltip({
	   track: true
   });

   // Realy simple history stuff for back button
   window.dhtmlHistory.initialize();
   window.dhtmlHistory.addListener(historyListener);
   window.dhtmlHistory.add(randomHistoryHash(), null);

   handleHtmlLoaded(null);
});

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
         minTrail: moreTxt.length
         }
       );
   });
}

// These things need to be performed
// 1) once after full page load
// *) each time an area is replaced inside the page
function handleHtmlLoaded(context) {
   applyAutocompleters();

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

   // Darn IE7 bugs...
   fixIESelectMinWidth(context);
   fixIEDropdownMinWidth("#titlebar .extra-actions .dropdown-menu", "#titlebar .extra-actions .dropdown-menu li", context);
   fixIEDropdownMinWidth("footer-titlebar .extra-actions .dropdown-menu", "#footer-titlebar .extra-actions .dropdown-menu li", context);
   fixIEDropdownMinWidth(".title-component .dropdown-menu.in-title", ".title-component .dropdown-menu.in-title li", context);
   zIndexWorkaround(context);
   
   if(isIE()) {
	   $jQ("option").each(function() {
		   $jQ(this).attr('title', $jQ(this).text());
	   });
   }

   /**
    * Open Office documents directly from server
    */
   $jQ('a.webdav-open', context).click(webdavOpen);
   $jQ('a.webdav-readOnly', context).click(webdavOpenReadOnly);

   $jQ(".modalwrap select option", context).tooltip();

   /**
    * Forward click event to autocomplete input.
    * (We wrap autocomplete inputs to fix IE bug related to input with background image and text shadowing)
    */
   $jQ(".suggest-wrapper", context).click(function (e) {
      $jQ(this).children("input").focus();
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
   
   $jQ(".admin-user-search-input", context).keyup(function(event) {
       updateButtonState();
       if (event.keyCode == '13') {
          $jQ(this).next().click();
       }
    });

	propSheetValidateOnDocumentReady();
}

//-----------------------------------------------------------------------------
//DIGITAL SIGNATURE
//-----------------------------------------------------------------------------

function processCert(cert, selectedCertNumber) {
 $jQ('#signApplet').hide();
 $jQ('#signWait').show();
 return oamSubmitForm('dialog','dialog:dialog-body:processCert',null,[['cert', cert], ['selectedCertNumber', selectedCertNumber]]);
}

function signDocument(signature) {   
 $jQ('#signApplet').hide();
 $jQ('#signWait').show();
  return oamSubmitForm('dialog','dialog:dialog-body:signDocument',null,[['signature', signature]]);
}

function cancelSign() {
 $jQ('#signApplet').hide();
 $jQ('#signWait').show();
 return oamSubmitForm('dialog','dialog:dialog-body:cancelSign',null,[[]]);
}

function driverError() {
}

//Some lines based on https://digidoc.sk.ee/include/JS/idCard.js
function loadSigningPlugin(operation, hashHex, selectedCertNumber, path) {

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
          var selectedCertNumber = plugin.selectedCertNumber;
          processCert(certHex, selectedCertNumber);
       } else {
          $jQ('#signWait').html('Sertifikaati ei valitud või sertifikaadid on registreerimata!');
       }

    } else if (operation == 'FINALIZE') {
       var signedHashHex = plugin.getSignedHash(hashHex, selectedCertNumber);
       if (signedHashHex) {
          signDocument(signedHashHex);
       } else {
          $jQ('#signWait').html('Allkirjastamine katkestati või ID-kaart ei ole lugejas!');
       }
    }
 }
 else
 {
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
