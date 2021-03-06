

var delta = [];
delta['translations'] = [];

/** Vertical scrollbar appears when browser window is narrower than minScreenWidth */
var minScreenWidth = 1020;

function setMinScreenWidth(minWidth){
   minScreenWidth = minWidth;
}

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
      setScreenProtected(true, "palun oodake, lehelt lahkutakse");
      $jQ.ajaxDestroy(); // do not allow any new AJAX requests to start
      // if we are navigating to another page, and one AJAX request was cancelled already but is still working on server, then that makes 2 requests in server simultaneously
      // if any new AJAX requests would start, then RequestControlFilter connection limit 2 would be exceeded and user would see a blank page
   }
};

/**
 * @param version
 * @returns if <code>version</code> is not given, then actual major version, otherwise true or false.
 */
function isIE(version) {
   if(!$jQ.browser.msie) {
      return false;
   }
   var actVer = parseInt($jQ.browser.version, 10);
   if(version){
      return actVer == version;
   } else {
      return actVer; // number will also evaluate to true
   }
}
// järgnevad funktsioonid võiks eemaldada kõige uuemas harus, et vältida probleeme vahepeal lisandunud koodi osas
/**@deprecated eemaldatakse varsti*/
function isIE7(){return isIE(7);}
/**@deprecated eemaldatakse varsti*/
function isIE8(){return isIE(8);}

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

      if(isIE(7)) {
         var zIndexNumber = 5000;
         $jQ("div,ul", containerContext).each(function() {
            $jQ(this).css('zIndex', zIndexNumber);
            $jQ(this).children('span,li').each(function() {
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

      if(isIE(8)) {
         $jQ(".long-content", context).css("top", "-17px");
      }
   }
}

function fixIEDropdownMinWidth(container, items, context) {
   if(isIE(7)) {
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
   if(isIE(7)) {
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

function confirmWithPlaceholders(msgWithPlaceholders /*, placeHolderValues ...*/) {
   var translatedMsg = msgWithPlaceholders;
   for( var i = 1; i < arguments.length; i++ ) {
      translatedMsg = translatedMsg.replace('{'+(i-1)+'}', arguments[i]);
  }
  return confirm(translatedMsg);
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
         if(typeof(originalClickHandler) == "function"){
            return prependFn(jQElem) && originalClickHandler();
         } else {
            return prependFn(jQElem) && eval("(function() {" + originalClickHandler + "})();");
         }
      });
   });
}

/**
 * Change the status of close button on function/series/volume/case details dialog
 * @param status - status of the function/series/volume/case
 */
function processFnSerVolCaseCloseButton(status){
   var closeBtns = getCloseButtons();
   var finishDisabled = getFinishButtons().prop("disabled");
   if(status != "avatud"){
      closeBtns.remove();
   } else if(finishDisabled || status == "avatud"){
      closeBtns.prop("disabled", finishDisabled);
   }
}

function getFinishButtons() {
   return $jQ(escapeId4JQ("#dialog:finish-button, #dialog:finish-button-2"));
}

function getCloseButtons() {
   return $jQ(escapeId4JQ("#dialog:close-button, #dialog:close-button-2"));
}

function clickFinishButton() {
   return $jQ(escapeId4JQ("#dialog:finish-button")).click();
}

function disableAndRemoveButton(buttonId) {
   var finishButton = $jQ('#' + escapeId4JQ(buttonId));
   finishButton.remove();
}
/**
 * append selection of source element to the element with id ending with "toItemIdSuffix" and beginning with the same id prefix as selectBox
 */
function appendSelection(source, targetId) {
   var selectItem = $jQ('#' + escapeId4JQ(source.attr("id")) + ' :selected');
   if ($jQ('#' + escapeId4JQ(source.attr("id")) + ' :selected').val() == "") {
      return;
   }
   var targetElem = $jQ("#" + escapeId4JQ(targetId));
   var lastToItemValue = targetElem.val();
   if (lastToItemValue.length != 0) {
      lastToItemValue = lastToItemValue + ', ';
   }
   var lable = selectItem.text(); // using label not value!
   targetElem.val(lastToItemValue + lable);
   targetElem.focus(); // expand the text field 
   source.focus();
};

/**
 * Add autoComplete functionality to input using given values to for suggestion(allows to enter also values not given with <code>valuesArray</code>)
 * @param inputId - id of the input component that should have autoComplete functionality based on values from <code>valuesArray</code>
 * @param valuesArray - values to be suggested int the input
 */
var autocompleters = new Array();
function addAutocompleter(inputId, valuesArray){
   autocompleters.push(function() {
      var jQInput = $jQ("#"+escapeId4JQ(inputId));
      var autoCompleter = jQInput.autocompleteArray(valuesArray, { minChars: -1, suggestAll: 1, delay: 50, onItemSelect: function(li) { processButtonState(); } });
      autoCompleter.parent(".suggest-wrapper").click(function(){
         jQInput.focus();
      });
      autoCompleter.bind("autoComplete", function(e, data){
         var ac = $jQ(this);
         if(!ac.parent().hasClass('noValChangeTooltip')) {
            ac.attr("title", data.newVal);
         }
      });
      jQInput.focus(function() {
         jQInput.keydown();
      });
   });
}

// Active dimension values cache for date dimensionSelectorDate.
// Cache should be emptied when dimensionSelectorDate value changes
var dimensionSelectorDefaultValues = {};
var lastDimensionQueryDates = {};

function addUIAutocompleter(input, valuesArray, dimensionName, dimensionQueryDate, filterName, linkId){
   autocompleters.push(function() {
      var inputId = "#"+escapeId4JQ(input);
      var jQInput = $jQ(inputId);
      var dimensionKey = dimensionName;
      var localFilterName = filterName;
      if(filterName != null && filterName != undefined && filterName.length){
         dimensionKey = dimensionKey + filterName;
      }
      if(valuesArray.length > 0){
         dimensionSelectorDefaultValues[dimensionKey] = valuesArray;
      }
      lastDimensionQueryDate = null;
      if(dimensionQueryDate){
         lastDimensionQueryDate = new Date(getDateFromString(dimensionQueryDate));
      }
      lastDimensionQueryDates[dimensionKey] = lastDimensionQueryDate;
      var autocomplete = jQInput.uiAutocomplete({
         minLength: 0,
         appendTo: "#wrapper", // this is needed to display custom tooltips above autocomplete list
         source: function( request, response ) {
            var term = request.term;
            var entryDateInput = $jQ(".entryDate"); //doesn't matter if entryDate field is not present, in that case just don't use it for filtering
            var entryDate = null;
            if(entryDateInput.length > 0){
               entryDate = entryDateInput.datepicker('getDate');
            }
            if(term.length < 3 && isSameDate(entryDate, lastDimensionQueryDates[dimensionKey])){
               response(dimensionSelectorDefaultValues[dimensionKey]);
            } else {
               var uri = getContextPath() + "/ajax/invoke/AjaxSearchBean.searchDimensionValues";
               request.dimensionName = dimensionName;
               request.predefinedFilterName = filterName;
               if(entryDate != null){
                  request.entryDate = entryDate.getDate() + "." + entryDate.getMonth() + "." + entryDate.getFullYear();
               }
               $jQ.ajax({
                  type: 'POST',
                  mode: 'queue',
                  data: request,
                  url: uri,
                  dataType: 'json',
                  success: function( data, status, xhr ) {
                     if(term.length < 3){
                        //cache data for new date
                        lastDimensionQueryDates[dimensionKey] = entryDate;
                        dimensionSelectorDefaultValues[dimensionKey] = data;
                     }
                     response(data);
                  }
              });
            }
         },
         focus: function( event, ui ) {
            return false;
         },
         select: function( event, ui ) {
            var input = $jQ(inputId);
            input.val(ui.item.value);
            //assume using customized tooltips; if needed could make tooltip attribute name configurable (title/tooltipText)
            input.attr("tooltipText", ui.item.description);
            var lnk = linkId;
            if(input.val() != '' && linkId != ''){
               $jQ("#" + escapeId4JQ(linkId)).click();
            }
            return false;
         },
         position: {
            my: "right top",
            at: "right bottom",
            collision: "none"
         }
      });
      autocomplete.data("uiAutocomplete")._renderItem = function( ul, item ) {
         var renderedItem = $jQ( "<li><a title=\"" + item.description + "\">" + item.value + "<br>" + item.label + "</a></li>" );
         renderedItem.data("item.uiAutocomplete", item );
         renderedItem.appendTo( ul );
         return renderedItem;
      };
      autocomplete.focus(function(){
         var input = $jQ(inputId);
         if(input.uiAutocomplete("option", "executeOnFocus")){
            input.uiAutocomplete("search");
         } else {
            input.uiAutocomplete("option", "executeOnFocus", true);
         }
      });
      autocomplete.bind("paste", function(){
         var input = $jQ(inputId);// pasted value not jet assigned
         setTimeout(function() {
            input.uiAutocomplete("search");
         }, 100);
      });
   });
}

// FIXME: this is not working!
function getDateFromString(dateString){
   if(dateString){
      var dateParts = dateString.split(".");
      if(dateParts.length == 3){
         if (dateParts[1].charAt(0) == "0"){
            dateParts[1] = dateParts[1].substr(1);
         }
         if (dateParts[0].charAt(0) == "0"){
            dateParts[0] = dateParts[0].substr(1);
         }
         var date = new Date(parseInt(dateParts[2]), parseInt(dateParts[1]), parseInt(dateParts[0]));
         return date;
      }
   }
   return null;
}

function isSameDate(date1, date2){
   if((date1 == null || date1 == undefined) && (date2 == null || date2 == undefined)){
      return true;
   }
   if(date1 && date2){
      return date1.getFullYear() == date2.getFullYear() && date1.getMonth() == date2.getMonth() && date1.getDate() == date2.getDate();
   }
}

function addSearchSuggest(clientId, containerClientId, pickerCallback, submitUri) {
   addSearchSuggest(clientId, containerClientId, pickerCallback, null, submitUri, null);
}

function addSearchSuggest(clientId, containerClientId, pickerCallback, pickerCallbackParams, submitUri) {
   addSearchSuggest(clientId, containerClientId, pickerCallback, pickerCallbackParams, submitUri, null);
}

function addSearchSuggest(clientId, containerClientId, pickerCallback, pickerCallbackParams, submitUri, autoCompleteCallback) {
   autocompleters.push(function addAutocompleter() {
      var jQInput = $jQ("#"+escapeId4JQ(clientId));
      var uri = getContextPath() + "/ajax/invoke/AjaxSearchBean.searchSuggest";
      var suggest = jQInput.autocomplete(uri, {extraParams: {'pickerCallback' : pickerCallback, 'pickerCallbackParams' : pickerCallbackParams}, matchContains: 1, minChars: 3, suggestAll: 1, delay: 50,
      onItemSelect: function (li) {
         processButtonState();
      },
      formatResult: function formatSuggestResult(data) {
         var end = data.indexOf("<");
         if (end > 0) {
            return data.substring(0, data.indexOf("<"));
         }
         return data;
      }
      });

      suggest.bind("autoComplete", function handleAutocomplete(event, data) {
         handleEnterKeySkip = true;
         setScreenProtected(true, "FIXME: palun oodake, ühendus serveriga");
         // Get other field values from updateable container and append AC data
         var postData = getContainerFields(containerClientId, clientId, []);
         postData += "&" + $jQ.param({'data' : data.newVal});
         $jQ.ajax({
            type: 'POST',
            url: submitUri,
            mode: 'queue',
            data: postData,
            success: function autocompleteSuccess(responseText) {
               if (autoCompleteCallback) {
                  autoCompleteCallback.call(data.newVal);
               }
               ajaxSuccess(responseText, clientId, containerClientId);
               setScreenProtected(false);
               handleEnterKeySkip = false;
            },
            error: ajaxError,
            dataType: 'html'
         });
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

function showDuplicatedTableHeader(context) {
   var table = $jQ("table.duplicate-header", context);

   if (table.length > 0) {
      if($jQ(window).height() < table.offset().top + table.outerHeight()) {
         table.append("<tfoot></tfoot>");

         var footer = table.children("tfoot");
         var row = table.children("thead").children("tr");
         row.clone().appendTo(footer);
      }
   }
}

function setPageScrollY() {
   var scrollTop = $jQ(window).scrollTop();
   $jQ('#wrapper form').append('<input type="hidden" name="scrollToY" value="'+ scrollTop +'" />');
}

function getOffice13Link(url) {
   var programs = {
         "word" : ["doc", "dot", "docx", "docm", "dotx", "dotm"],
         "excel" : ["xls", "xlt", "xlm", "xlsx", "xlsm", "xltx", "xltm"],
         "powerpoint" : ["ppt", "pps", "pptx", "pptm", "potx", "potm", "ppam", "ppsx", "ppsm", "sldx", "sldm"]
   }

   var link = null;
   var extension = url.substring(url.lastIndexOf(".") + 1, url.length);
   for (var program in programs) {
      if (jQuery.inArray(extension, programs[program]) > -1) {
         link = "ms-" + program + ":ofe|u|" + url;
         break;
      }
   }

   return link;
}

function webdavOpen(url) {
   var showDoc = true;
   var openDocumentCallback = function(url, readOnly, refresh) {
      if (readOnly) {
         webdavOpenReadOnly(url);
      } else {
         window.open(url, '_blank');
         if (!refresh) {
            return;
         }

         var selector = "a.webdav-open.icon-link[href='"+url+"']";
         var link = $jQ(selector);
         var tableId = link.closest("table").attr("id");
         if (tableId) {
            var linkId = link.attr("id");
            // Request new HTML for the surrounding table to show unlock action
            var ajaxUri = getContextPath() + "/ajax/invoke/AjaxBean.submit";
            ajaxSubmit(linkId, tableId, [], ajaxUri, {"viewName": "/jsp/dialog/container.jsp", "componentClientId": linkId, "containerClientId": tableId});
         }
      }
   };

   var openOfficeUrl = url.substring(0, "vnd.sun.star.webdav://".length) == "vnd.sun.star.webdav://";
   if (openOfficeUrl) {
      showDoc = false;
      openDocumentCallback(url, false, false); // Open in a new window to maintain compatibility with other configurations
   }

   // Try to open using MSOffice and WebDAV capabilities
   if (showDoc && window.ActiveXObject !== undefined) {
      try {
         // If we are able to instantiate this component, we have Office 2013 installed
         var isOffice2013Installed = new ActiveXObject("SharePoint.OpenDocuments.5");
         var link = getOffice13Link(url);
         if (link) {
            window.open(link, '_blank');
            showDoc = false;
         }
      } catch (e) {
         try {
            showDoc = !(new ActiveXObject("SharePoint.OpenDocuments").EditDocument(url));
         } catch (e2) {
            // Continue and try to open the document
         }
      }
   }

   if (showDoc) {
      openDocumentCallback(url, false, true); // Open in a new window to maintain compatibility with other configurations
   }
   userMessageChecker.init();
   window.name += "checkMessages";
   return false;
}

function requestNewMessages(xml){
   if(!xml){
      return;
   }
   xml = xml.documentElement;
   if(xml.getAttribute('success') == 'true'){
      alert(xml.getAttribute('message'));
   }
}
var numberOfInstances = 0;
var userMessageChecker = {
   init : function(){
      if(numberOfInstances != 0){
         return; // allow only one instance
      }
      numberOfInstances++;
      $jQ(window).focus(this.requestMessages);
   },
   requestMessages : function(){
      var uri = getContextPath() + '/ajax/invoke/NotificationBean.getCurrentUserNotifications';
      $jQ.ajax({
         type: 'POST',
         url: uri,
         mode: 'queue',
         success: requestNewMessages,
         dataType: 'xml'
      });
   }
};
/**
 * Open file in read-only mode (TODO: with webdav, if file is office document)
 * @return false
 */
function webdavOpenReadOnly(url) {
   var uri = getContextPath() + '/ajax/invoke/AjaxBean.getDownloadUrl?path=' + url;

   $jQ.ajax({
      type: 'POST',
      url: uri,
      data: 'url=' + url, // path is already escaped, so disable jquery escaping by giving it a string directly
      mode: 'queue',
      success: function openForDownload(responseText) {
         window.open(responseText, '_blank');// regular file saveAs/open by downloading it to HD
      },
      error: ajaxError,
      datatype: 'html'
    });
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
   if(isIE(7)) {
      titlebarIndex = $jQ("#titlebar").css("z-index");
      $jQ("#titlebar").css("z-index", "-1");
   }
   if (openModalContent != null){
      $jQ("#" + openModalContent).hide();
   }
   openModalContent = target;

   $jQ("#overlay").css("display","block");
   var modal = $jQ("#" + target);
   modal.css("display","block");
   var parentModal = modal.parent().closest(".modalwrap");

   if (parentModal) {
      parentModal.show(); // regulates display property
      height = parentModal.height();
   }

   if (height != null) {
      modal.css("min-height", height);
   }
   modal.show();
   modal.find(".genericpicker-input").focus();
   return false;
}

function hideModal(){
   if (openModalContent != null) {
      if(isIE(7) && titlebarIndex != null) {
         $jQ("#titlebar").css("zIndex", titlebarIndex);
      }
      var modal = $jQ("#" + openModalContent);
      modal.hide();
      var parentModal = modal.parent().closest(".modalwrap");
      if (parentModal.length < 1) {
         $jQ("#overlay").remove();
      }
   }
   return false;
}

function selectGroupForModalSearch(selectId, hiddenId, filterId){
   var selectedValue =$jQ("#" + escapeId4JQ(selectId)).val();
   var hidden = $jQ("#" + escapeId4JQ(hiddenId));
   var filter = $jQ("#" + escapeId4JQ(filterId));
   hidden.val(selectedValue);
   filter.val(parseInt(filter.val())-1);
}

function groupModalFilterChange(){
   var filterId = "#" + escapeId4JQ(this.id);
   var selectorId = filterId.replace("_filter","_groupSelector");
   if($jQ(selectorId) == null) {
      return;
   }
   $jQ(filterId.replace("_filter","_selectedGroup")).val("");
   $jQ(filterId.replace("_filter","_selectedGroupText")).remove();
   var resultsId = filterId.replace("_filter","_results");
   var userGroups = $jQ(resultsId);
   if(userGroups == null) {
      return;
   }
   if(this.value == "2") {
      userGroups.live('change', function(){
         if($jQ(filterId).val() == 2 && $jQ(resultsId).children("option:selected").length == 1) {
            $jQ(selectorId).removeAttr('disabled');
         } else {
            $jQ(selectorId).attr('disabled','disabled');
         }
      });
   } else {
      userGroups.unbind('change');
      $jQ(selectorId).attr('disabled','disabled');
   }
}

var propSheetValidateSubmitFn = [];
var propSheetValidateFormId = '';
var propSheetValidateFinishId = '';
var propSheetValidateSecondaryFinishId = '';
var propSheetValidateNextId = '';
var propSheetValidateSecondaryNextId = '';
var propSheetFinishBtnPressed = false;
var propSheetNextBtnPressed = false;

// Should be called once per property sheet. If there are multiple propertySheets on the same
// page then the last caller overwrites formId, finishBtnId and nextBtnId, so those must be
// equal to all property sheets on the same page.
function registerPropertySheetValidator(submitFn, formId, finishBtnId, nextBtnId) {
   propSheetValidateSubmitFn.push(submitFn);
   propSheetValidateFormId = formId;
   propSheetValidateFinishId = finishBtnId;
   propSheetValidateSecondaryFinishId = finishBtnId + "-2";
   propSheetValidateNextId = nextBtnId;
   propSheetValidateSecondaryNextId = nextBtnId + "-2";
}

function processButtonState() {
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

function triggerPropSheetValidation(){
   propSheetNextBtnPressed = true;
   return propSheetValidateSubmit();
}

function propSheetValidateSubmitCommon() {
   return (!window.propSheetValidateCustom || propSheetValidateCustom()) && validateDatePeriods();
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
      if (!endDate) {
         return;
      }
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

function getEndDate(beginDateElem, container) {
   var endDates = container.find(".endDate");
   if (endDates.length>1) {
      var dates = container.find(".beginDate, .endDate");
      for (var i = 0; i < dates.length; i++) {
         if (dates[i] == beginDateElem && dates.length > i+1) {
            return $jQ(dates[i+1]);
         }
      }
      return null;
   } else if (endDates.length == 0) {
      return null;
   } else {
      return endDates;
   }
}

function propSheetValidateOnDocumentReady() {
   if (propSheetValidateSubmitFn.length > 0) {
      document.getElementById(propSheetValidateFormId).onsubmit = propSheetValidateSubmit;
      var finishBtn = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateFinishId);
      if(finishBtn){
         finishBtn.onclick = function() { propSheetFinishBtnPressed = true; };
      }
      var secondaryFinishButton = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateSecondaryFinishId);
      if(secondaryFinishButton != null) {
        secondaryFinishButton.onclick = function() { propSheetFinishBtnPressed = true; };
      }
      if (propSheetValidateNextId.length > 0) {
         var validateNextId = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateNextId);
         if (validateNextId != null){
            validateNextId.onclick = function() { propSheetNextBtnPressed = true; };
         }
      }
      if (propSheetValidateSecondaryNextId.length > 0) {
         var validateSecondaryNextId = document.getElementById(propSheetValidateFormId + ':' + propSheetValidateSecondaryNextId);
         if (validateSecondaryNextId != null){
            validateSecondaryNextId.onclick = function() { propSheetNextBtnPressed = true; };
         }
      }
      processButtonState();
   }
}

function propSheetValidateRegisterOnDocumentReady() {
   if (propSheetValidateSubmitFn.length > 0) {
      document.getElementById(propSheetValidateFormId).onsubmit = propSheetValidateSubmit;
      setButtonPropSheetFinish(propSheetValidateFormId + ':documentRegisterButton');
      setButtonPropSheetFinish(propSheetValidateFormId + ':documentRegisterButton-2');
      processButtonState();
   }
}

function setButtonPropSheetFinish(elementId){
   var button = document.getElementById(elementId);
   if(button){
      button.onclick = function() { propSheetFinishBtnPressed = true; };
   }
}

function togglePanel(divId) {
    $jQ(divId).toggle();
}

function togglePanelWithStateUpdate(divId, panelId, viewName) {
    togglePanel(divId);
    updateState(divId, panelId, viewName);
}

/**
 * Toggle submit-protection-layer on/off based on <code>isProtected</code>.
 * When submit-protection-layer is on, then screen is protected against clicking and user sees an hourglass cursor
 * @param isProtected
 */
function setScreenProtected(isProtected, reason) {
   var layer = $jQ(".submit-protection-layer");
   if(!isProtected){
//      $jQ("#submit-protection-msg").text("");
      layer.hide();
   } else {
//      $jQ("#submit-protection-msg").text(reason);
      layer.show().focus();
   }
}

function updateState(divId, panelId, viewName) {
   setScreenProtected(true, "FIXME: palun oodake, ühendus serveriga");
    var uri = getContextPath() + '/ajax/invoke/PanelStateBean.updatePanelState?panelId=' + panelId +
              '&panelState=' + $jQ(divId).is(":visible") + '&viewName=' + viewName;

    $jQ.ajax({
       type: 'POST',
       url: uri,
       data: addViewStateElement(null).serialize(),
       mode: 'queue',
       success: requestUpdatePanelStateSuccess,
       error: requestUpdatePanelStateFailure,
       dataType: 'html'
    });
}

function requestUpdatePanelStateSuccess(responseText) {
   try {
      if (!responseText) { // check that response is not empty
         return;
      }
      if (isAjaxViewStateError(responseText)) {
         handleAjaxViewStateError(responseText);
         return;
      }
      // Set new value to view state, so when form is submitted next time, correct state is restored.
      var viewState = responseText.substr('VIEWSTATE:'.length);
      document.getElementById("javax.faces.ViewState").value = viewState;
   } finally {
      setScreenProtected(false);
   }
}

function requestUpdatePanelStateFailure() {
   try {
      $jQ.log("Updating panel status in server side failed");
   } finally {
      setScreenProtected(false);
   }
}

function ajaxError(request, textStatus, errorThrown) {
   var result = request.responseText.match(/<body>(.*)<\/body>/i);
   if (result) {
      setScreenProtected(false);
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
   setScreenProtected(true, "FIXME: palun oodake, ühendus serveriga");

   var postData = getContainerFields(componentContainerId, componentClientId, submittableParams);
   if (payload != null) {
      postData += "&" + $jQ.param(payload);
   }

   $jQ.ajax({
      type: 'POST',
      url: uri,
      data: postData,
      success: function (responseText) {
         ajaxSuccess(responseText, componentClientId, componentContainerId);
      },
      error: ajaxError,
      dataType: 'html'
   });
}

function getContainerFields(componentContainerId, componentClientId, submittableParams) {
// Find all form fields that are inside this component
   var componentChildFormElements = $jQ('#' + escapeId4JQ(componentContainerId)).find('input,select,textarea');

   // Find additional hidden fields at the end of the page that HtmlFormRendererBase renders
   var hiddenFormElements = $jQ('input[type=hidden]').filter(function() {
      return componentClientId == this.name.substring(0, componentClientId.length) || $jQ.inArray(this.name, submittableParams) >= 0;
   });

   componentChildFormElements = componentChildFormElements.add(hiddenFormElements);
   componentChildFormElements = addViewStateElement(componentChildFormElements);
   return componentChildFormElements.serialize();
}

function addViewStateElement(elements){
   var viewState = $jQ('#javax\\.faces\\.ViewState');
   if(elements != null){
      elements = elements.add(viewState);
   } else {
      elements = viewState;
   }
   return elements;
}

function ajaxSuccess(responseText, componentClientId, componentContainerId) {
   if (responseText) { // check that response is not empty
      if (isAjaxViewStateError(responseText)) {
         try {
            return handleAjaxViewStateError(responseText);
         } finally {
            setScreenProtected(false);
         }
      }
      // Split response
      var i = responseText.lastIndexOf('VIEWSTATE:');
      if (i < 0){
         try {
            window.location.href = window.location.protocol + "://" + window.location.hostname + "/" + window.location.pathname;
            return false;
         } finally {
            setScreenProtected(false);
         }
      }
      var html = responseText.substr(0, i);
      var hiddenInputsIndex = responseText.lastIndexOf("HIDDEN_INPUT_NAMES_JSON:");
      var viewState = responseText.substr(i + 'VIEWSTATE:'.length, hiddenInputsIndex);

      var hiddenInputNames = $jQ.parseJSON(responseText.substr(hiddenInputsIndex+"HIDDEN_INPUT_NAMES_JSON:".length));
      if(hiddenInputNames){
         var hiddenInputsContainer = $jQ("#hiddenInputsContainer");
         hiddenInputNames.each(function(elem){
            var hiddenInput = hiddenInputsContainer.find("input[name='"+elem+"']");
            if(hiddenInput.length == 0){
               $jQ('<input type="hidden" name="'+elem+'" value="" />').appendTo(hiddenInputsContainer);
            } else {
               hiddenInput.val("");
            }
         });
      }


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

      try {
         // Reattach behaviour
         handleHtmlLoaded($jQ('#' + escapeId4JQ(componentContainerId)), false);
      } catch (e) {
         alert("Failed to update page! "+e);
      } finally {
         setScreenProtected(false);
      }
   } else {
      setScreenProtected(false);
   }
}

function isAjaxViewStateError(responseText){
   try {
      return responseText.lastIndexOf('ERROR_VIEW_STATE_CHANGED') > -1;
   } catch (e){
      return false;
   }
}

function handleAjaxViewStateError(responseText) {
   var redirectUrl = $jQ.parseJSON(responseText.substr("ERROR_VIEW_STATE_CHANGED:".length));
   window.location.href = redirectUrl;
   return false;
}

// -----------------------------------------------------------------------------
//MENU ITEM COUNT UPDATE
//-----------------------------------------------------------------------------

// There is no concurrency in JS. One thread per page. Event handling is based on a queue.
// http://stackoverflow.com/questions/2078235/ajax-concurrency

function updateItemsCountIfNeeded(id, updateMethod){
   var menuElement = $jQ('#' + escapeId4JQ(id));
   if (!menuElement.hasClass("menuItemCountUpdated")){
      updateItemsCount(updateMethod);
      menuElement.addClass("menuItemCountUpdated");
   }
}

function updateMenuItemsCount() {
   updateItemsCount('MenuItemCountBean.updateMenuItemsCount');
}

function updateItemsCount(updateMethod) {
   var uri = getContextPath() + '/ajax/invoke/' + updateMethod;
   $jQ.ajax({
      type: 'POST',
      url: uri,
      mode: 'queue',
      success: function (json) {
         if(!json || json.length > 30) {
            return;
         }
         $jQ.each(json, function(idx, obj) {
            var menuItemId = obj.menuItem;
            if($jQ('.menuItemCount[menuitemid=' + menuItemId + ']').length == 0) {
               return true;
            }
            var responseText = obj.count;
            var count = responseText == '0' ? '' : ' (' + responseText + ')';

            // Construct element text
            var text = $jQ($jQ('.menuItemCount[menuitemid=' + menuItemId + '] a')[0]).text();
            var i = text.lastIndexOf(' (');
            if (i == -1) {
               text += count;
            } else {
               text = text.substr(0, i) + count;
            }

            // Construct element title
            var title = $jQ($jQ('.menuItemCount[menuitemid=' + menuItemId + '] a')[0]).attr('title');
            var i = title.lastIndexOf(' (');
            if (i == -1) {
               title += count;
            } else {
               title = title.substr(0, i) + count;
            }

            // Update on all elements with same menuitemid
            var menuItem = $jQ('.menuItemCount[menuitemid=' + menuItemId + '] a');
            menuItem.text(text).attr('title', title);
            if (count.length > 2) {
               menuItem.parent().removeClass("hiddenMenuItem");
            }
         });
      },
      error: function(request, textStatus, errorThrown) {
         // Don't destroy ajax queue, let other request proceed
         ajaxErrorHidden(request, textStatus, errorThrown); // log messsage to FireBug
      },
      dataType: 'json'
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
};

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

/**
 * Returns true if event was handled.
 */
var handleEnterKeySkip = false;
function handleEnterKey(event) {
   if (handleEnterKeySkip) {
      return;
   }

   var target = $jQ(event.target);
   var targetTag = event.target.tagName.toLowerCase();

   // Allow normal behaviour for textareas
   if ("textarea" == targetTag) {
      return false;
   }

   // Submit search for (constrained)quickSearch by clicking the next button
   var targetId = target.attr('id');
   if (targetId && endsWith(targetId.toLowerCase(), "quicksearch")) {
      target.next().click();
      return true;
   }

   // if enter is pressed in association search block, it has higher priority than specificAction and defaultAction
   if(target.hasClass("searchAssocOnEnter") && _searchAssocs(event)){
      return true;
   }

   // Are there any specific actions (modal search, dialog search)?
   var specificActions = $jQ('.specificAction').filter(":visible");
   if (specificActions.length == 1) { // Do nothing if multiple actions match
      // Special case for generic pickers. If we have selected something and press enter, we should select instead of searching
      if ("select" == targetTag) {
         var picker = target.parents(".generic-picker");
         if (picker.length > 0) {
            picker.first().find(".picker-add").click();
            return true;
         }
      }

      specificActions.click();
      return true;
   }

   // Default actions are usually dialog finishImpl buttons
   var defaultActions = $jQ('.defaultAction').filter(":visible");
   if (defaultActions.length == 1) { // Do nothing if multiple actions match
      defaultActions.click();
      return true;
   }

   return false;
}

//-----------------------------------------------------------------------------
// DOCUMENT-READY FUNCTIONS
//-----------------------------------------------------------------------------

// These things need to be performed only once after full page load
$jQ(document).ready(function() {
   try {
      initWithScreenProtected();
      if(window.name.indexOf("checkMessages") != -1){
         userMessageChecker.init();
      }
   } catch (e) {
      alert("Failed to initialize page! "+e);
   } finally {
      setScreenProtected(false);
   }
});

function initWithScreenProtected() {
   showFooterTitlebar();
   allowMultiplePageSizeChangers();

   // Collect all enter presses at document root (NB! live() doesn't work!)
   $jQ(document).keypress(function (event) {
      if (event.keyCode == 13 && handleEnterKey(event)) {
         event.stopPropagation(); // You shall not PASS!
         return false;
      }
   });

   $jQ(".toggle-tbody").live("mousedown", function (event) {
      $jQ(this).closest("tbody").next().toggle();
      $jQ(this).toggleClass("plus").toggleClass("minus");
   });

   $jQ(".genericpicker-input").live('keyup', throttle(function (event) {
      var input = $jQ(this);
      var filter = input.prev();
      var hidden = input.next();
      var filterValue;
      var hiddenValue;
      if (filter != null && filter.val() != undefined) {
         filterValue = filter.val();
      }
      if (hidden != null && hidden.attr('value') != undefined) {
         hiddenValue = hidden.attr('value');
      }

      var successCallback = function(responseText) {
         var tbody = $jQ(this);
         var select = tbody.find('.genericpicker-results');
         select.children().remove();
         var index = responseText.indexOf("|");
         select.attr("size", responseText.substring(0, index));
         select.append(responseText.substring(index + 1, responseText.length));
         var resultCount = select.children().length;

         if (select.attr("data-initialresults") == undefined) { // After the first fresh search, record the initial result count
            select.attr("data-initialresults", resultCount);
         }

         // Check if resultset is limited and show/hide message accordingly
         if (resultCount == select.attr("data-rowlimit")) {
            tbody.find('.modalResultsLimited').show();
         } else {
            tbody.find('.modalResultsLimited').hide();
         }

         tbody.find('tr.hidden').toggleClass('hidden');
      };

      // Workaround for IE/WebKit, since it cannot hide option elements...
      var backSpaceCallback = function(inputValue, callbackContext){
         var tbody = $jQ(callbackContext);
         var select = tbody.find('.genericpicker-results');
         select.children('span').each(function (i) {
            var option = $jQ(this).find('option');
            if (option.text().toLowerCase().indexOf(inputValue.toLowerCase()) > -1) {
               option.prop('disabled', '').show();
               $jQ(this).replaceWith(option).show();
            }
         });
      };

      doSearch(input, filterValue, hiddenValue, event, successCallback, backSpaceCallback, input.closest('tbody'));
   }, 500));

   var reSearch = true;
   function doSearch(input, filterValue, hiddenValue, event, successCallback, backSpaceCallback, callbackContext) {
      var callback = input.attr('datasrc');
      if(!callback){
         alert("no search callback found");
      }
      var value = input.val();
      if (!value) {
         reSearch = true;
         return;
      }
      if(!filterValue){
         filterValue = 1; // UserContactGroupSearchBean.USERS_FILTER
      }

      var filterByStructUnit = "false";
      var split = callback.split("|");
      if (split.length == 3) {
         filterValue = split[0];
         callback = split[1];
         filterByStructUnit = split[2];
      } else if (split.length == 2) {
         callback = split[0];
         filterByStructUnit = split[1];
      }

      var tbody = $jQ(callbackContext);
      var select = tbody.find('.genericpicker-results');
      var opts = select.children('option');

      // Determine if we are dealing with limited resultset
      var limited = false;
      if (value.length < 3) {
         select.removeAttr("data-initialresults"); // Reset the initial result count
      } else {
         limited = select.attr("data-initialresults") == select.attr("data-rowlimit");
      }

      var backspace = event.keyCode == 8;
      if (value.length > 2 && reSearch && !backspace || limited) {
         $jQ.ajax({
            type: 'POST',
            url: getContextPath() + "/ajax/invoke/AjaxSearchBean.searchPickerResults",
            data: $jQ.param({'contains' : value, 'pickerCallback' : callback, 'filterValue' : filterValue, 'hiddenValue' : hiddenValue, "filterByStructUnit" : filterByStructUnit}),
            mode: 'queue',
            context: callbackContext,
            success: successCallback,
            error: ajaxError,
            dataType: 'html'
         });
         reSearch = false;
      } else if (value.length > 3 && !backspace) {
         opts.each(function (i) {
            var option = $jQ(this);
            if (option.text().toLowerCase().indexOf(value.toLowerCase()) < 0) {
               option.attr('disabled', 'disabled').hide();
               option.wrap('<span />').hide();
            }
         });
      } else if (value.length > 2 || backspace) {
         backSpaceCallback(value, callbackContext);
      }

      if (value.length < 3) {
         reSearch = true;
      }
   };

   $jQ(".genericpicker-filter").live('change', function (event) {
      var filter = $jQ(this);
      var tbody = filter.closest('tbody');
      var select = tbody.find('.genericpicker-results');
      var input = filter.next();
      if (input.val().length < 3) {
         select.empty();
      } else {
         input.next().click();
      }
   });

   $jQ(".errandReportDateBase").live('change', function (event) {
      // Get the date
      var elem = $jQ(this);
      if (elem != null) {
         // Find the report due date
         var errandEnd = elem.datepicker('getDate');
         var reportDue = "";
         if (errandEnd) {
            reportDue = new Date(errandEnd.getFullYear(), errandEnd.getMonth(), errandEnd.getDate() + 5);
            if (reportDue.getDay() == 6) { // Saturday
               reportDue = new Date(reportDue.getFullYear(), reportDue.getMonth(), reportDue.getDate() + 2);
            } else if (reportDue.getDay() == 0) { // Sunday
               reportDue = new Date(reportDue.getFullYear(), reportDue.getMonth(), reportDue.getDate() + 1);
            }
         }
         // Set date
         elem.closest(".panel-border").find(".reportDueDate").datepicker('setDate',  reportDue);
      }
   });

   $jQ(".driveCompensationRate").live('change', function(event) {
      $jQ(".driveKm").change(); // Trigger updates
   });

   $jQ(".driveOdoBegin,.driveOdoEnd").live('change', function (event) {
      var elem = $jQ(this);
      var begin = elem.hasClass("driveOdoBegin");
      var other = begin ? $jQ(elem.parent().next().children()[0]) : $jQ(elem.parent().prev().children()[0]);
      if (!elem || !other) {
         return;
      }

      var driveKm = elem.closest("tr").find(".driveKm");
      var val1 = elem.val();
      var val2 = other.val();
      if (!val1 || !val2) {
         driveKm.val("");
      } else {
         driveKm.val(begin ? val2 - val1 : val1 - val2); // Show negative value, when data is incorrect
      }
      driveKm.change();
   });

   $jQ(".driveKm").live('change', function (event) {
      var kmElem = $jQ(this);
      var propSheet = kmElem.closest("table").parent().closest("table");
      var rateElem = propSheet.find(".driveCompensationRate");

      if (kmElem == null || rateElem == null) {
         return;
      }

      var compCalc = $jQ(kmElem.parent().next().children()[0]);

      var kmVal = kmElem.val();
      var rateVal = rateElem.val();
      if (!isNumeric(kmVal, true) || !isNumeric(rateVal)) {
         compCalc.val("");
      } else {
         compCalc.val(round((kmVal * rateVal), 2));
      }
      compCalc.change();

      // Update sum
      var totalKmElem = $jQ(propSheet.find(".driveTotalKm")[0]);
      if (!totalKmElem) {
         return;
      }
      var totalKm = 0;
      kmElem.closest("table").find(".driveKm").each(function () {
         if (isNumeric(this.value)) {
            totalKm += parseInt(this.value);
         }
      });

      totalKmElem.val(totalKm);
      totalKmElem.change();
   });

   $jQ(".driveTotalKm").live('change', function (event) {
      var kmElem = $jQ(this);
      var propSheet = kmElem.closest("table");
      var rateElem = propSheet.find(".driveCompensationRate").first();
      var compElem = propSheet.find(".driveTotalCompensation").first();
      if (!rateElem || !compElem) {
         return;
      }

      var kmVal = kmElem.val();
      var rateVal = rateElem.val();
      if (!isNumeric(kmVal, true) || !isNumeric(rateVal)) {
         compElem.text("");
      } else {
         compElem.text(round((kmVal * rateVal), 2));
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
      if (dateField != null && !dateField.val()) { // Only update if a value isn't specified
         dateField.datepicker('setDate', elem.datepicker('getDate'));
         dateField.change();
      }
   });

   if(isIE()) {
      // http://www.htmlcenter.com/blog/fixing-the-ie-text-selection-bug/
      document.body.style.height = document.documentElement.scrollHeight + 'px';
   }

   if(isIE(7)) {
      $jQ(window).resize(function() {
         var htmlWidth = $jQ("html").outerWidth(true);
         var width =  htmlWidth < minScreenWidth ? minScreenWidth : htmlWidth;
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
   // Adding tooltips is currently executed inside handleHtmlLoaded

   // Realy simple history stuff for back button
   window.dhtmlHistory.initialize();
   window.dhtmlHistory.addListener(historyListener);
   window.dhtmlHistory.add(randomHistoryHash(), null);


   jQuery(".dailyAllowanceDaysField:input, .dailyAllowanceRateField:input").live('change', function(event) {
      var elem = $jQ(this);
      // Calculate sum for current row
      var row = elem.closest("tr");
      var allowanceDays = parseInt(row.find(".dailyAllowanceDaysField").closest("input").val());
      var allowanceRate = parseInt(row.find(".dailyAllowanceRateField").closest("select").val());
      var sumField = row.find(".dailyAllowanceSumField").closest("input");
      var sum = 0;
      if (allowanceDays && allowanceRate) {
         var sum = allowanceDays * (allowanceRate / 100) * sumField.attr("datafld");
      }
      if (sum) {
         sumField.val(round(sum, 2));
      } else {
         sumField.val(0);
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

      row.closest("div").closest("tr").next().find(".dailyAllowanceTotalSumField").first().text(totalSum);
   });

   jQuery(".expectedExpenseSumField:input").live('keyup', function(event) {
      var elem = $jQ(this);
      var totalSum = 0;
      var sum = 0;
      var sumString;
      elem.closest("table").find(".expectedExpenseSumField").each(function () {
         sumString = $jQ(this).val();
         sumString = sumString.replace(/ /g,'');
         sumString = sumString.replace(",", ".");
         sum = parseFloat(sumString);
         if(sum) {
            totalSum += sum;
         }
      });

      var totalField = elem.closest("div").closest("tr").next().find(".expensesTotalSumField").first();
      totalField.text(totalSum);
   });

   jQuery(".invoiceTotalSum, .invoiceVat").live('change', function(event) {
      // assume there is only one field available for each value
      var invoiceTotalSum = getFloatOrNull($jQ(".invoiceTotalSum").val());
      var invoiceVat = getFloatOrNull($jQ(".invoiceVat").val());
      var invoiceSum = $jQ(".invoiceSum");
      if(isNaN(invoiceTotalSum) || isNaN(invoiceVat)){
         invoiceSum.val('');
      } else {
         invoiceSum.val(round(invoiceTotalSum - invoiceVat, 2));
      }
      transFooterTotalSumElem = jQuery("#footer-sum-2:first");
      setTransTotalSumColor(transFooterTotalSumElem, invoiceTotalSum, getFloatOrNull(transFooterTotalSumElem.text()));
   });

   jQuery(".errandReportSumField").live('change', function(event) {
      var elem = $jQ(this);
      var totalSum = 0;
      var sum = 0;
      var sumString;
      elem.closest("table").find(".errandReportSumField").each(function () {
         sumString = $jQ(this).val();
         sum = getFloatOrNull(sumString);
         if(sum) {
            totalSum += sum;
         }
      });

      var totalField = elem.closest("div").closest("tr").next().find(".errandReportTotalSumField");
      totalField.val(round(totalSum, 2));
   });

   jQuery(".errandSummaryDebitField").live('change', function(event) {
      var elem = $jQ(this);
      var totalSum = 0;
      var sum = 0;
      var sumString;
      elem.closest("table").find(".errandSummaryDebitField").each(function () {
         sumString = $jQ(this).val();
         sum = getFloatOrNull(sumString);
         if(sum) {
            totalSum += sum;
         }
      });

      var totalField = elem.closest("div").closest("table").find(".errandSummaryDebitTotalField");
      totalField.val(round(totalSum, 2));
   });

   jQuery(".errandSummaryCreditField").live('change', function(event) {
      var elem = $jQ(this);
      var totalSum = 0;
      var sum = 0;
      var sumString;
      elem.closest("table").find(".errandSummaryCreditField").each(function () {
         sumString = $jQ(this).val();
         sum = getFloatOrNull(sumString);
         if(sum) {
            totalSum += sum;
         }
      });

      var totalField = elem.closest("div").closest("table").find(".errandSummaryCreditTotalField");
      totalField.val(round(totalSum, 2));
   });

   jQuery(".driveTotalKmField:input, .driveCompensationRateField:input").live('change', function(event) {
      var driveTotalCompensation = $jQ(".driveTotalCompensationField");
      var driveTotalKmField = getFloatOrNull($jQ(".driveTotalKmField").val());
      var driveCompensationRate = getFloatOrNull($jQ(".driveCompensationRateField").val());
      if(driveTotalKmField && driveCompensationRate) {
         driveTotalCompensation.text(round((driveTotalKmField * driveCompensationRate), 2));
      }
   });

   jQuery(".trans-row-sum-input").live('change', recalculateInvoiceSums);
   jQuery(".trans-row-vat-code-input").live('change', recalculateInvoiceSums);

   jQuery(".trans-row-entry-content-input").keyup(function(){
      textCounter($(this), 50);
   });
   jQuery(".trans-row-entry-content-input").keydown(function(){
      textCounter($(this), 50);
   });

   toggleSubrow.init();
   toggleSubrowToggle.init();

   jQuery(".task-due-date-date").live("change", processTaskDueDateDate);
   jQuery(".clearGroupRowDate").live("change", clearGroupRowDate);
   jQuery(".groupRowDate").live("change", groupRowDateChange);
   jQuery(".changeSendOutMode").live("change", changeSendOutMode);
   jQuery(".resetSendOutGroupSendMode").live("change", resetSendOutGroupSendMode);

   if (isIE()) {
     jQuery("#footer a, .mailto").click(function() { nextSubmitStaysOnSamePage() });
   }

   handleHtmlLoaded(null, setInputFocus, selects);
};

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
         var subrowToggles = $jQ("td.trans-toggle-subrow").children("a");
         subrowToggles.click(this.clickIt);
         subrowToggles.click();
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

function recalculateInvoiceSums(){
   var sumWithoutVat = 0;
   var vatSum = 0;
   var hasInvalidSumInput = false;
   var hasInvalidVatInput = false;
   var originalTaxInfoOptions = jQuery(".trans-tax-original-info-selector").find("option");
   var taxCodeOptions = jQuery(".trans-tax-code-selector").find("option");
// assume there is only one transaction table
   var transTable = jQuery(".trans-main-table:first");
   transTable.find(".trans-recordSetRow2,.trans-recordSetRowAlt2").each(function(){
      var mainRow = $jQ(this);
      var sumInput = mainRow.find(".trans-row-sum-input:first");
      var vatPercentage = 0;
      var rowSumWithoutVat = getFloatOrNull(sumInput.val());
      if(isNaN(rowSumWithoutVat)){
         hasInvalidSumInput = true;
         return false;
      } else {
         sumWithoutVat += rowSumWithoutVat;
         var vatCodeInput = mainRow.next(".trans-subrow,.trans-subrowAlt").find(".trans-row-vat-code-input:first");
         var vatCode = vatCodeInput.find("option:selected").val();
         if(vatCode){
            var sumInputId = sumInput.attr('id');
            var sumInputNumber = sumInputId.substring(sumInputId.lastIndexOf('-') + 1)
            var originalVatCodeInfo = originalTaxInfoOptions.filter("[value='" + sumInputNumber + "']:first");
            if(originalVatCodeInfo.length > 0){
               var originalVatCodeInfoText = originalVatCodeInfo.text();
               var originalVatCode = originalVatCodeInfoText.substring(0, originalVatCodeInfoText.lastIndexOf("¤"));
               if(originalVatCode == vatCode){
                  vatPercentage = getFloatOrNull(originalVatCodeInfoText.substring(originalVatCodeInfoText.lastIndexOf("¤") + 1));
               } else {
                  vatPercentage = getFloatOrNull(taxCodeOptions.filter("[value='" + vatCode + "']:first").text());
               }
            } else {
               vatPercentage = getFloatOrNull(taxCodeOptions.filter("[value='" + vatCode + "']:first").text());
            }
         }
         if(isNaN(vatPercentage)){
            hasInvalidVatInput = true;
            return false;
         }
         vatSum += rowSumWithoutVat * vatPercentage / 100;
      }
   });
   var msg1 = jQuery("#footer-error-message-0");
   var msg2 = jQuery("#footer-error-message-1");
   if (!hasInvalidSumInput && !hasInvalidVatInput){
      jQuery("#footer-sum-0").text(round(sumWithoutVat, 2));
      jQuery("#footer-sum-1").text(round(vatSum, 2));
      var transFooterTotalSumElem = jQuery("#footer-sum-2:first");
      var transFooterTotalSum = round(sumWithoutVat + vatSum, 2);
      transFooterTotalSumElem.text(transFooterTotalSum);
      setTransTotalSumColor(transFooterTotalSumElem, getFloatOrNull(jQuery(".invoiceTotalSum").val()), getFloatOrNull(transFooterTotalSum));
      msg1.hide();
      msg2.hide();
   } else {
      jQuery("#footer-sum-0").text("");
      jQuery("#footer-sum-1").text("");
      jQuery("#footer-sum-2").text("");
      if(hasInvalidSumInput){
         msg1.show();
         msg2.hide();
      } else {
         msg1.hide();
         msg2.show();
      }
   }
}

function setTransTotalSumColor(transFooterTotalSumElem, invoiceTotalSum, transFooterTotalSum){
   if(invoiceTotalSum == '' || isNaN(invoiceTotalSum) || Math.abs(transFooterTotalSum - invoiceTotalSum) > 0.001){
      transFooterTotalSumElem.css("color", "red");
   } else {
      transFooterTotalSumElem.css("color", "");
   }
}

function textCounter(input, maxlimit) {
   var value = input.value;
   if (value.length > maxlimit){
      input.value = value.substring(0, maxlimit);
   }
}

// return number for valid numeric string,
// 0 for blank string and NaN for all other values
function getFloatOrNull(originalSumString){
   if (!originalSumString) {
      return NaN;
   }
   var sumString = originalSumString.replace(",", ".");
   sumString = sumString.replace(/ /g, "");
   if(sumString == ""){
      return 0;
   }
   if(isNumeric(sumString)){
      return parseFloat(sumString);
   }
   return NaN;
}

// use to avoid javascript parsing strings like "55 krooni" to number 55
// (conversion that java validation wouldn't allow)
function isNumeric(numberStr){
   return isNumeric(numberStr, false);
}
function isNumeric(numberStr, integer){
   if (!numberStr) {
      return false;
   }
   var validChars = "0123456789";
   if (!integer) {
      validChars += ".";
   }
   var additionalFirstChars = "+-";
   for (i = 0; i < numberStr.length; i++){
      var currentChar = numberStr.charAt(i);
      if (validChars.indexOf(currentChar) == -1) {
         if(i !== 0 || additionalFirstChars.indexOf(currentChar) == -1){
            return false;
         }
      }
   }
   return true;
}
function processTaskDueDateDate(){
   processTaskDueDateDateInput($jQ(this));
}

function processTaskDueDateDateInput(dueDateInput){
   var taskRow = dueDateInput.closest("tr");
   var taskDueDateTime = taskRow.find(".task-due-date-time");
   var taskDueDateDays = taskRow.find(".task-due-date-days");
   if(dueDateInput.val() == ""){
      taskDueDateTime.val("");
   } else {
      if(taskDueDateTime.val() == ""){
         taskDueDateTime.val("23:59");
      }
   }
   taskDueDateDays.val("");
}

function clearGroupRowDate(){
   var dueDateInput = $jQ(this);
   var taskRow = dueDateInput.closest("tr");
   while (true) {
      taskRow = taskRow.prev();
      if(taskRow.length == 0) {
         return;
      }

      var groupDateInputs = taskRow.find(".groupRowDate");
      if (groupDateInputs.length > 0) {
         groupDateInputs.each(function () {
            $jQ(this).val("");
         });

         return;
      }
   }
}

function groupRowDateChange() {
   var input = $jQ(this);
   var row = input.closest("tr");
   var dateVal = row.find(".date")[0].value;
   var timeVal = row.find(".time")[0].value;

   while (true) {
      row = row.next();
      if(row.length == 0) {
         return;
      }

      var dateInput = row.find(".clearGroupRowDate.date");
      var timeInput = row.find(".clearGroupRowDate.time");
      if (dateInput.length < 1 || timeInput.length < 1) {
         return; // Out of thid group
      }
      dateInput[0].value = dateVal;
      timeInput[0].value = timeVal;
   }
}

function changeSendOutMode() {
   var value = this.value;
   if (value == "") {
      return;
   }

   $jQ(this).closest("tbody").next().find("select").each(function () {
      this.value = value;
   });
}

function resetSendOutGroupSendMode() {
   var select = $jQ(this);
   select.closest("tbody").prev().find(".changeSendOutMode").each(function () {
      this.value = "";
   });
    toggleSendOutIdCodeVisibility();
}

function setReadonly(element, readonly){
   element.attr("readonly", readonly);
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
   if(jqSelect.hasClass('noOptionTitle')){
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
      var condence = p ? parseInt('0'+p[1], 10) : 200;
      var moreTxt = "... ";
      if(!(p && p[2] == "-")){
         moreTxt = getTranslation('jQuery.condence.moreText');
      }
      var isStrictTrim = jQuery(this).hasClass("strictTrim");
      jQuery(this).condense({
         moreSpeed: 0,
         lessSpeed: 0,
         moreText: moreTxt,
         lessText: getTranslation('jQuery.condence.lessText'),
         ellipsis: "",
         condensedLength: condenceAtChar,
         minTrail: moreTxt.length,
         strictTrim: isStrictTrim  // assume that condense content is not text (html, except links, is escaped)
                           // and does search for word breaks for triming text
         }
       );
   });
}

function setMinEndDate(owner, dateElem, triggerEndDateChange){
   if (dateElem.attr("class").indexOf("beginDate") < 0) return;
   var beginDate = dateElem.val();
   if (beginDate == null || beginDate.trim().length == 0) return;
   var row = dateElem.closest("tr");
   if (row == null) return;
   var endDate = getEndDate(owner, row);
   if (endDate == null) return;
   var endDatePicker = endDate.data("datepicker");
   if (endDatePicker == null) return;
   var date = jQuery.datepicker.parseDate(endDatePicker.settings.dateFormat, beginDate, endDatePicker.settings);
   if (date == null) return;
   endDate.datepicker("option", "minDate", date);
   if (triggerEndDateChange) {
      endDate.change();
   }
}

function initExpanders(context){

   //initialize all expanding textareas
   var expanders = jQuery("textarea[class*=expand]", context);
   expanders.TextAreaExpander();
   if(isIE()) {
      // trigger size recalculation if IE, because e.scrollHeight may be inaccurate before keyup() is called
      expanders.keyup();
      jQuery.fn.TextAreaExpander.ieInitialized = true;
   }

}

function toggleSendOutIdCodeVisibility() {
    var idCodeFieldVisible = false;
    $jQ(".resetSendOutGroupSendMode").each(function() {
        var value = $jQ(this).val();
        if (value == "Riigiportaal eesti.ee") {
            $jQ(".hiddenIdCode").removeClass("hiddenIdCode").addClass("visibleIdCode");
            $jQ(".sendOutGroup").attr("colspan", "3");
            idCodeFieldVisible = true;
            return false; // break out of the loop
        }
    });
    if (!idCodeFieldVisible) {
        $jQ(".visibleIdCode").removeClass("visibleIdCode").addClass("hiddenIdCode");
        $jQ(".sendOutGroup").attr("colspan", "2");
    }
}

// These things need to be performed
// 1) once after full page load
// *) each time an area is replaced inside the page
function handleHtmlLoaded(context, setFocus, selects) {

   showDuplicatedTableHeader(context);

   $jQ(".tooltip", context).tooltip({
      track: true
      ,escapeHtml: true
      ,tooltipContainerElemName: "p"
   });

   var ieVer = isIE();
   initExpanders(context);

   if(ieVer) { // Darn IE bugs...
      zIndexWorkaround(context);

      var jqSelects = (selects==undefined) ? $jQ("select") : selects;
      jqSelects.each(function(){
         var jqSelect = $jQ(this);
         if(jqSelect.hasClass('noOptionTitle')) {
            jqSelect.children().each(function() {
               $jQ(this).attr('title', $jQ(this).text());
            });
         } else {
            jqSelect.children().each(function() {
               var i = $jQ(this);
               if (i.attr('title') == undefined) {
                  i.attr('title', i.text());
               }
            });
            jqSelect.attr("title", jqSelect.find("option:selected").attr("title"));
         }
      });
      if(ieVer==7){
         fixIESelectMinWidth(context);
         fixIEDropdownMinWidth("#titlebar .extra-actions .dropdown-menu", "#titlebar .extra-actions .dropdown-menu li", context);
         fixIEDropdownMinWidth("footer-titlebar .extra-actions .dropdown-menu", "#footer-titlebar .extra-actions .dropdown-menu li", context);
         fixIEDropdownMinWidth(".title-component .dropdown-menu.in-title", ".title-component .dropdown-menu.in-title li", context);
      }
   }

   /**
    * Open Office documents directly from server
    */
   $jQ('.webdav-open', context).click(function (event) {
      event.preventDefault();
      // 1) this.href = 'https://dhs.example.com/dhs/webdav/xxx/yyy/zzz/abc.doc'
      // 2) $jQ(this).attr('href') = '/dhs/webdav/xxx/yyy/zzz/abc.doc'
      var path = this.href; // SharePoint ActiveXObject methods need to get full URL
      checkFileLock(path, function webdavOpenCallback(filePath, status) {
         if (status.code < 1) {
            if (status.code == 0 || confirm(getTranslation("webdav_openReadOnly").replace("#", status.data))) {
               webdavOpenReadOnly(filePath);
            }
         } else if (status.code == 1) {
            webdavOpen(filePath);
         } else if (status.code == 2) {
            alert("Faili ei saa avada, dokument on kustutatud");
         } else if (status.code == 3) {
            alert("Faili ei saa avada, fail on kustutatud");
         }
      });

      return false;
   });

   $jQ('a.webdav-readOnly', context).click(function () {
      webdavOpenReadOnly(this.href);
      return false;
   });

   jQuery(".dailyAllowanceDaysField, .dailyAllowanceRateField, .errandReportDateBase, .eventBeginDate, .eventEndDate", context).change();
   jQuery(".expectedExpenseSumField", context).keyup();
   jQuery(".errandReportSumField, .errandSummaryDebitField, .errandSummaryCreditField, .driveTotalKmField", context).change();
   $jQ('.triggerPropSheetValidation', context).each(function () {
      prependOnclick($jQ(this), triggerPropSheetValidation);
   });

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   // Functions that should be executed before removing submit-protection layer should be above.
   // For example activities related to clicking links or buttons
   ////////////////////////////////////////////////////////////////////////////////////////////////////
   setScreenProtected(false);
   ////////////////////////////////////////////////////////////////////////////////////////////////////
   // Functions that could be delayed a bit after submit-protection layer is removed could be bellow.
   // For example because user is probably not fast enough to react to removing submit-protection layer
   ////////////////////////////////////////////////////////////////////////////////////////////////////
   $jQ(".genericpicker-input:visible").focus();

   var container = $jQ("#"+escapeId4JQ('container-content'));
   if (setFocus) {
      $jQ("input:text,textarea", container).filter(':visible:enabled[readonly!="readonly"].focus').first().focus();
   }
   applyAutocompleters();
   toggleSendOutIdCodeVisibility();

   // datepicker
   var activeDatePickers = jQuery("input.date", context).not("input[readonly]");
   activeDatePickers.focusout(function(){
      var jqDate=$jQ(this);
      var strDate = jqDate.val().trim();
      var result;
      var shortPattern = /^\d{4}$/;
      var longPattern = /^\d{8}$/;
      if(shortPattern.test(strDate)){
         result = strDate.substring(0,2) + "." + strDate.substring(2,4) + "." + new Date().getFullYear();
      } else if(longPattern.test(strDate)){
         result = strDate.substring(0,2) + "." + strDate.substring(2,4) + "." + strDate.substring(4);
      } else {
         result = strDate;
      }
      jqDate.val(result);
   });
   var dp_dates = activeDatePickers.datepicker({
      dateFormat: 'dd.mm.yy',
      changeMonth: true,
      changeYear: true,
      nextText: '',
      prevText: '',
      yearRange: '-100:+100',
      duration: '',
      showAnim: '',
      onSelect: function( selectedDate ) {
         var dateElem = jQuery(this);
         dateElem.trigger("change");
         var onchange = '' + dateElem.attr("onchange");
         if (onchange.indexOf('ajaxSubmit(') == -1) {
            setMinEndDate(this, dateElem, true);
         }
      }
   });

   activeDatePickers.each(function()
         {
            var dateElem = jQuery(this);
            var onchange = '' + dateElem.attr("onchange");
            setMinEndDate(this, dateElem, onchange.indexOf('ajaxSubmit(') == -1);
         });

   jQuery(".quickDateRangePicker", context).each(function (intIndex)
         {
            var selector = jQuery(this);
            var selectorId = selector.attr("id");
            var beginDate = jQuery("#" + escapeId4JQ(selectorId.replace("_DateRangePicker","")));
            var endDate = jQuery("#" + escapeId4JQ(selectorId.replace("_DateRangePicker","_EndDate")));
            setDateFromEnum(beginDate,endDate,selector.val());
            beginDate.change(clearRangePicker);
            endDate.change(clearRangePicker);
            beginDate.each(clearRangePicker);
            selector.change(setDateFromEnumOnChange);
         });

   $jQ("[id*='BeginDate_']").each(setMultiRowMinEndDate);
   $jQ("[id*='BeginDate_']").live("change", setMultiRowMinEndDate);

   if(context != null) {
      $jQ("input", context).focus(function() {
            lastActiveInput = $jQ(this);
      });
   }

   /**
    * Binder for alfresco properties that are generated with ClassificatorSelectorAndTextGenerator.class
    * Binds all elements that have class="selectBoundWithText" with corresponding textAreas/inputs(assumed to have same id prefix and suffix specified with TARGET_SUFFIX)
    */
   $jQ(".selectBoundWithText", context).each(function (intIndex)
   {
      var TARGET_SUFFIX = "select_target"; // corresponding textAreas/input
      var selectId = $(this).id;
      var textAreaId = selectId.substring(0, selectId.lastIndexOf(':') + 1) + TARGET_SUFFIX;
      var existingValue = $jQ("#" + escapeId4JQ(textAreaId)).text();
      var initialValue = $jQ('#' + escapeId4JQ(selectId) + ' :selected').text();
      if (initialValue != "" && existingValue == "" && initialValue != getTranslation("select_default_label"))
      {
         var targetElem = $jQ("#" + escapeId4JQ(textAreaId));
         targetElem.val(initialValue);
      }
      $jQ(this).change(function() {
         appendSelection($jQ(this), textAreaId);
      });
   });

   /**
    * Add onChange functionality to jQuery change event (we can't use onChange attribute because of jQuery bug in IE)
    */
   $jQ("[class*=selectWithOnchangeEvent]", context).each(function (intIndex, selectElement)
   {
      var classString = selectElement.className;
      var currElId = selectElement.id;
      var onChangeJavascript = classString.substring(classString.lastIndexOf('¤¤¤¤') + 4, classString.lastIndexOf(';') + 1);
      if(onChangeJavascript != ""){
            $jQ(this).change(function(){
               //assume onChangeJavascript contains valid function body
               eval("(function(currElId) {" + onChangeJavascript + "}) ('" + selectElement.id + "');");
            });
      }
   });

   propSheetValidateOnDocumentReady();
   propSheetValidateRegisterOnDocumentReady();

   // this method should be called after critical activities have been done in handleHtmlLoaded as it displays alerts and possibly submits page
   confirmWorkflow('workflow-confirmation-messages', 'workflow-after-confirmation-link');
   confirmWorkflow("workflow-delegation-confirmation-messages", "workflow-after-delegation-confirmation-link");

   // trigger keyup event (for validation & textarea resize) on paste. Can't use live() because of IE
   $jQ("textarea, input[type='text']", context).bind("paste", function(){
      var input = $jQ(this);// pasted value not jet assigned
      setTimeout(function() {
         input.keyup();
      }, 100);
   });

   /**
    * Forward click event to autocomplete input.
    * (We wrap autocomplete inputs to fix IE bug related to input with background image and text shadowing)
    */
   $jQ(".suggest-wrapper", context).click(function (e) {
      $jQ(this).children("textarea").focus();
   });

   $jQ(".modalwrap select option", context).tooltip();
   initSelectTooltips((selects==undefined) ? $jQ("select") : selects);

   var forms = $jQ(document.forms);
   if(forms.length > 2){
      forms.each(function(){
         var form = this;
         if(form.id != "rshStorageForm" && form.id != "searchForm" && form.id != "ConfigAdmin-console-title" && form.id != "node-browser-titlebar") {
            alert("unexpected form.id='"+form.id+"' (found "+forms.length+" forms)");
         }
      });
   }

   $jQ(".readonly", context).attr('readonly', 'readonly');
}

function checkFileLock(filePath, callback) {
   // When page is submitted, user sees an hourglass cursor
   $jQ(".submit-protection-layer").show().focus();

   var uri = getContextPath() + '/ajax/invoke/AjaxBean.isFileLocked';
   $jQ.ajax({
     type: 'POST',
     url: uri,
     data: 'path=' + filePath, // path is already escaped, so disable jquery escaping by giving it a string directly
     mode: 'queue',
     success: function (responseText) {
       $jQ(".submit-protection-layer").hide();
       var status = { code: -1, data: "" };
       if (responseText.length == 0) { // If we get an empty response, then open read-only (other conditions prevent from editing - incoming letter, finished, some running workflow)
          status.code = 0;
       } else if (responseText.indexOf("NOT_LOCKED") > -1) {
          status.code = 1;
       } else if (responseText.indexOf("DOCUMENT_DELETED") > -1) {
          status.code = 2;
       } else if (responseText.indexOf("FILE_DELETED") > -1) {
          status.code = 3;
       } else {
          status.data = responseText;
       }

       if (callback && typeof(callback) === "function") {
          callback(filePath, status);
       }
     },
     error: ajaxError,
     datatype: 'html'
   });
}

function lockFileManually(filePath, callback) {
   var uri = getContextPath() + '/ajax/invoke/AjaxBean.lockFileManually';
   $jQ.ajax({
     type: 'POST',
     url: uri,
     data: 'path=' + filePath, // path is already escaped, so disable jquery escaping by giving it a string directly
     mode: 'queue',
     success: function (responseText) {
       $jQ(".submit-protection-layer").hide();
       var edit = false;
       if (responseText.indexOf("LOCKING_SUCCESSFUL") > -1) {
          edit = true;
       }

       if (callback && typeof(callback) === "function") {
          callback(filePath, !edit, edit);
       }
     },
     error: ajaxError,
     datatype: 'html'
   });
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

function closeSignSuccess() {
	 $jQ('#signApplet').hide();
	 $jQ('#signWait').show();
	 return oamSubmitForm('dialog','dialog:dialog-body:closeSignSuccess',null,[[]]);
	}

function performSigningPluginOperation(operation, hashHex, certId, path) {
   try {
      // plugin works when it's the first child of body; doesn't work when it's somewhere in the middle
      $jQ('body').prepend('<div id="pluginLocation"></div>');

      loadSigningPlugin('est');
      var plugin = new IdCardPluginHandler('est');

      if (operation == 'PREPARE') {
         var selectedCertificate = plugin.getCertificate();
         var certHex = selectedCertificate.cert;
         var certId = selectedCertificate.id;

         if (certHex) {
            processCert(certHex, certId);
         } else {
            throw new IdCardException(1601, 'Sertifikaadi lugemine ebaõnnestus');
         }

      } else if (operation == 'FINALIZE') {
         var signedHashHex = plugin.sign(certId, hashHex);
         if (signedHashHex) {
            signDocument(signedHashHex);
         } else {
            throw new IdCardException(1602, 'Allkirjastamine ebaõnnestus');
         }
      }

   } catch(ex) {
      if (ex instanceof IdCardException) {
         $jQ('#signWait').html(ex.message + ' (vea kood ' + ex.returnCode + ')');
      } else {
         $jQ('#signWait').html('Viga: ' + (ex.message != undefined ? ex.message : ex));
      }
   }
}



function signDigidoc4j() {
	try {
		if (window.location.protocol != "https:") {
			$jQ('#signWait').html('Veebis allkirjastamise käivitamine on võimalik vaid https aadressilt');
			return;
		}
		window.hwcrypto.getCertificate({lang: 'ee'}).then(function(certificate) {
	        return fetchDigidoc4jHash(certificate);
	    }, function(reason) {
			if (reason == 'Error: user_cancel') {
				$jQ('#signWait').html('Allkirjastamine katkestati');
			} else if (reason == 'Error: no_certificates') {
				$jQ('#signWait').html('Sertifikaate ei leitud');
			} else {
               $jQ('#signWait').html('Tehniline viga! veakood: (' + reason + ')');
			}
	    });
	   
	} catch(ex) {
		$jQ('#signWait').html('Viga: ' + (ex.message != undefined ? ex.message : ex));
	}
}

function fetchDigidoc4jHash(cert) {
	var uri = getContextPath()
			+ "/ajax/invoke/WorkflowBlockBean.getDigidoc4jHash";
	$jQ.ajax({
				type : 'POST',
				mode : 'queue',
				url : uri,
				data : $jQ.param({
					'certInHex' : cert.hex
				}),
				dataType : 'html',
				success : function(hash, status, xhr) {
					window.hwcrypto.sign(cert, {type: 'SHA-256', hex: hash},{lang: 'ee'}).then(function(signature) {
						//return oamSubmitForm('dialog','dialog:dialog-body:finishDigidoc4jSigning',null,[['signInHex', signature.hex]]);
						return finishDigidoc4jSign(signature.hex);
				}, function(reason) {
					if (reason == 'Error: user_cancel') {
						$jQ('#signWait').html('Allkirjastamine katkestati');
					} else if (reason == 'Error: pin_blocked') {
						$jQ('#signWait').html('Allkirjastamise PIN on blokeeritud');
					} else if (reason == 'Error: invalid_argument') {
						$jQ('#signWait').html('Vale allkirjastamise PIN');
					} else {
						$jQ('#signWait').html('Tehniline viga! veakood: (' + reason + ')');
					}
				});
			   }
			});
}

function finishDigidoc4jSign(signInHex) {
	var uri = getContextPath()
			+ "/ajax/invoke/WorkflowBlockBean.finishDigidoc4jSigning";
	$jQ.ajax({
				type : 'POST',
				mode : 'queue',
				url : uri,
				data : $jQ.param({
					'signInHex' : signInHex
				}),
				dataType : 'html',
				success : function(responseOutput, status, xhr) {
					closeSignSuccess();
				}
			});
}

function getMobileIdSignature() {
   var uri = getContextPath() + "/ajax/invoke/WorkflowBlockBean.getMobileIdSignature";
   var mobileIdChallengeId = $jQ('#mobileIdChallengeId').text();
   $jQ.ajax({
      type: 'POST',
      mode: 'queue',
      url: uri,
      data: $jQ.param({'mobileIdChallengeId' : mobileIdChallengeId }),
      dataType: 'html',
      success: function( responseText, status, xhr ) {
         if (responseText == 'FINISH') {
            $jQ('#mobileIdChallengeMessage').html('<p>Vastus saadud...</p>');
            $jQ('#' + escapeId4JQ('dialog:dialog-body:mobileIdChallengeModal_submit_btn')).click();
         } else if (responseText == 'REPEAT') {
            window.setTimeout(getMobileIdSignature, 2000);
         } else if (responseText.indexOf('ERROR') == 0){
            $jQ('#mobileIdChallengeMessage').html('<p>' + responseText.substring(5) + '</p>');
         }
      }
   });
}

function sendToSapManually(){
   return showModal('entrySapNumber_popup');
}

function confirmWorkflow(selectClass, confirmationLinkClass){
   var confirmationMessagesSelect = $jQ("[class='" + selectClass + "']").get(0);
   if(confirmationMessagesSelect == undefined){
      return false;
   }
   for (i = confirmationMessagesSelect.children.length - 1; i >= 0; i--) {
      if (!confirm(confirmationMessagesSelect.children[i].value)){
         return false;
      }
   }
   $jQ("[class='" + confirmationLinkClass + "']").eq(0).click();
}

function clearFormHiddenParams(currFormName, newTargetVal) {
   var f = document.forms[currFormName];
   $jQ("#hiddenInputsContainer input[type='hidden']", $jQ(f)).each(function(){
      var jqInput=$jQ(this);
      var before = jqInput.val();// FIXME DLSeadist debug
      jqInput.val("");
   });
   if(newTargetVal){// FIXME DLSeadist debug - testiks sisse jäätud..kui üllatust ei tule kuskilt, siis kustutan lõpus ära
      alert("suprize! newTargetVal="+newTargetVal+" ( old val was '"+f.target+"')");
   }
   f.target = newTargetVal ? newTargetVal : '';
}
function clearRangePicker(){
   var dateId = jQuery(this).attr("id");
   var isEndDate = dateId.indexOf("_EndDate") > -1;
   var startDateSelector = "#" + (isEndDate ? escapeId4JQ(dateId.replace("_EndDate","")) : escapeId4JQ(dateId));
   var endDateSelector = "#" + (isEndDate ? escapeId4JQ(dateId) : escapeId4JQ(dateId + "_EndDate"));
   var startDate = jQuery(startDateSelector);
   var endDate = jQuery(endDateSelector);
   
   startDate.datepicker("option","minDate",null);
   startDate.datepicker("option","maxDate",null);
   endDate.datepicker("option","maxDate",null);
   setMinDateOption(startDate, endDate);
   var selector = jQuery("#" + escapeId4JQ(dateId + "_DateRangePicker"));
   selector.val("");
}
function setMultiRowMinEndDate() {
   var beginDate = $jQ(this);
   var fieldId = beginDate.attr("id");
   var endDateId = fieldId.replace(/BeginDate(?=[^BeginDate]*$)/, "EndDate"); // replace last occurrence
   var endDate = $jQ("#" + escapeId4JQ(endDateId));
   setMinDateOption(beginDate, endDate);
}
function setMinDateOption(beginDate, endDate) {
   var selectedStartDate = beginDate.val() ? beginDate.val() : null;
   endDate.datepicker("option","minDate",selectedStartDate);
}
function setDateFromEnumOnChange(){
   var selector = jQuery(this);
   var selectorId = selector.attr("id");
   var beginDate = jQuery("#" + escapeId4JQ(selectorId.replace("_DateRangePicker","")));
   var endDate = jQuery("#" + escapeId4JQ(selectorId.replace("_DateRangePicker","_EndDate")));
   setDateFromEnum(beginDate,endDate,selector.val());
}
function getEstonianWeekday(date){
   var weekday = date.getDay()-1;
   if(weekday<0) {
      return 6;
   }
   return weekday;
}
function setDateFromEnum(beginDate,endDate,selectedEnum){
   if(selectedEnum=="") return;
   var startDate = new Date();
   var finishDate = new Date();
   if(selectedEnum == "YESTERDAY"){
      startDate.setDate(startDate.getDate()-1);
      finishDate.setDate(finishDate.getDate()-1);
   } else if(selectedEnum == "CURRENT_WEEK"){
      startDate.setDate(startDate.getDate() - getEstonianWeekday(startDate));
   } else if(selectedEnum == "PREV_WEEK"){
      startDate.setDate(startDate.getDate() - getEstonianWeekday(startDate) -7);
      finishDate= new Date(startDate.getFullYear(),startDate.getMonth(),startDate.getDate() + 6,0,0,0,0);
   } else if(selectedEnum == "FROM_PREV_WEEK"){
      startDate.setDate(startDate.getDate() - getEstonianWeekday(startDate) -7);
   } else if(selectedEnum == "CURRENT_MONTH"){
      startDate = new Date(startDate.getFullYear(),startDate.getMonth(),1,0,0,0,0);
   } else if(selectedEnum == "PREV_MONTH"){
      startDate = new Date(startDate.getFullYear(),startDate.getMonth()-1,1,0,0,0,0);
      finishDate= new Date(startDate.getFullYear(),startDate.getMonth() + 1,1,0,0,0,0);
      finishDate.setDate(finishDate.getDate()-1);
   } else if(selectedEnum == "FROM_PREV_MONTH"){
      startDate = new Date(startDate.getFullYear(),startDate.getMonth()-1,1,0,0,0,0);
   } else if(selectedEnum == "CURRENT_YEAR"){
      startDate = new Date(startDate.getFullYear(),0,1,0,0,0,0);
   }
   beginDate.datepicker("option","defaultDate",startDate);
   beginDate.datepicker("setDate",startDate);
   endDate.datepicker("option","minDate",startDate);
   endDate.datepicker("option","defaultDate",finishDate);
   endDate.datepicker("setDate",finishDate);
}

function clickNextLink(currElId){
   $jQ("#" + escapeId4JQ(currElId)).nextAll("a").eq(0).click();
}

function endsWith(str, suffix) {
   return str && suffix && str.indexOf(suffix, str.length - suffix.length) !== -1;
}
function help(url) {
   var settings=
      "toolbar=no,location=no,directories=no,"+
      "status=no,menubar=no,scrollbars=yes,"+
      "resizable=yes,width=800px,height=600px";

   var win = window.open(url,"Abiinfo",settings);

   if (window.focus) {
      win.focus();
   }
   return false;
}

//http://remysharp.com/2010/07/21/throttling-function-calls/
function throttle(fn, delay) {
   var timer = null;
   return function() {
      var context = this, args = arguments;
      clearTimeout(timer);
      timer = setTimeout(function() {
         fn.apply(context, args);
      }, delay);
   };
}