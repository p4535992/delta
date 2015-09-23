// Custom JavaScript required for mDelta

// See: http://www.adequatelygood.com/JavaScript-Module-Pattern-In-Depth.html
var mDelta = (function($) {
   var api = {};

   var debugEnabled = true;

   // Public methods

   api.init = function() {
      initTriggers();
      initPagers();
      initSubstitutionLinks();
   }

   api.log = function() {
      return log(arguments);
   }

   // Private methods

   log = function() {
      window.console && window.console.log && window.console.log.apply(console, arguments); // TODO retain caller line number
   }

   debug = function() {
      debugEnabled && log.apply(this, arguments);
   }

   notify = function() {
      alert.apply(this, arguments);
   }

   initTriggers = function() {
      debug("Init triggers");

      var triggers = $('.trigger');
      triggers.each(function(e) {
         var trigger = $(this), targetId = $(this).attr('data-target');
         var target = targetId ? $("#" + targetId) : $($(this).attr('href'));
         var independent = trigger.hasClass("independent");

         trigger.on('click', function(e) {
            e.preventDefault();

            if (target.is(':visible')) {
               target.slideUp(100);
               trigger.removeClass('active');

            } else {
               var url = ajaxUrl(trigger.attr("href"));
               var callback = function() {
                  // Collapse other expanders
                  if (!independent) {
                     triggers.removeClass('active');
                     $('.expander:visible').slideUp(100);
                  }

                  target.slideDown(100);
                  trigger.addClass('active');
                  updateWindowLocation("#" + targetId);
               }

               // Check if we need to fetch the data (no content and data-target attribute is set and valid url is provided with href)
               if (target.children().length > 0 || !targetId || !url) {
                  // Element already has content fetched, process callback
                  callback();
               } else {
                  updateExpandableContent(trigger, target, url, callback);
               }
            }
         });

         // Check if we need to activate a trigger
         if (targetId === activeTrigger() && !target.is(':visible')) {
            trigger.click();
         }
      });
      debug("Init triggers complete");
   }

   // TODO - http://benalman.com/projects/jquery-hashchange-plugin/
   updateWindowLocation = function(target) {
      // Support only in-page references
      if (target.lastIndexOf("#", 0) !== 0) {
         throw "Target must start with a '#'"
      }

      var baseUrl = document.URL;
      var hashIndex = baseUrl.indexOf("#");
      if (hashIndex > -1) {
         baseUrl = baseUrl.substring(0, hashIndex);
      }

      window.location = (baseUrl + "#!" + target.substring(1));
   }
   
   updateExpandableContent = function(trigger, target, dataUrl, callback) {
      $.get(dataUrl, function(data) {
         var content = $(data);
         $(trigger).text(content.find('.trigger').text());
         $(target).html($(content[2]).html()); // TODO find a nicer way to retrieve a top level element
      }).done(typeof callback == "function" && callback());
   }

   initPagers = function() {
      debug("Init pagers");
      
      var links = $('.pagerlink');
      links.each(function(e) {
         var link = $(this), list = $(link.attr('data-list'));

         link.on('click', function(e) {
            e.preventDefault();
            var target = link.attr("href");
            
            // Show all?
            if (link.hasClass("all")) {
               list.children(".page").removeClass("hidden");
               link.closest("ol").find(".pageractive").removeClass("pageractive");
               link.addClass("pageractive");
            } else {
               // Hide currently active page and pager number
               list.children(".page").filter(":visible").addClass("hidden");
               link.closest("ol").find(".pageractive").removeClass("pageractive");
               
               // Show selected page and pager number
               $(target).removeClass("hidden");
               link.addClass("pageractive");
            }

            updateWindowLocation(target);
         });
      });
      
      // Resolve currently active page (if any)
      var activePage = activeTrigger();
      $(".pagerlink[href='#" + activePage + "']").click();
      
      debug("Init pagers complete");
   }

   ajaxUrl = function(url) {
      if (!url) {
         return null;
      }
      return url + "?ajax";
   }
   
   activeTrigger = function() {
      var index = document.URL.indexOf("#!");
      return index < 0 ? "" : document.URL.substring(index + 2, document.URL.length);
   }
   
   initSubstitutionLinks = function() {
      $('.substitutionLink').click(function() {
         var userName = $(this).siblings('input').first().attr('value');
         $.ajax({
            url: getContextPath() + '/m/ajax/substitute',
            data: { 'userName' : userName },
            method: 'POST',
            mode: 'queue',
            success: function() {
               location.replace(getContextPath() + '/m');
            },
            error: function() {
               location.replace(getContextPath() + '/m');
            }
         });
      });
   }

   return api;

}(jQuery));

$(document).ready(function() {
   mDelta.init();
   
   // Datepicker. http://amsul.ca/pickadate.js/date.htm
   $('.datepicker').pickadate({
      firstDay: 1,
      monthsFull: ['Jaanuar', 'Veebruar', 'Märts', 'Aprill', 'Mai', 'Juuni', 'Juuli', 'August', 'September', 'Oktoober', 'November', 'Detsember'],
      weekdaysShort: ['Püh', 'Esm', 'Tei', 'Kol', 'Nel', 'Ree', 'Lau'],
      today: 'Täna',
      clear: 'Kustuta',
      formatSubmit: 'dd.mm.yyyy',
      hiddenSuffix: '',
      hiddenName: true,
      onClose: function() {$('.datepicker').blur()}
   });

   // Timepicker. http://amsul.ca/pickadate.js/time.htm
   $('.timepicker').pickatime({
      clear: 'Kustuta',
      format: 'HH:i',
      interval: 15
   });
   
   $('.readmore').each(function(i, elem) {
      addShowMoreListener($(elem));
   });

});

// Autocomplete - http://loopj.com/jquery-tokeninput/
function setupSuggester(suggester, url) {
   suggester.tokenInput(
      getContextPath() + url, {
      method: 'POST',
      propertyToSearch: 'name',
      resultsFormatter: function(item) {
         if(!item) {return '';}
         var showId = item.userItemFilterType && (item.userItemFilterType == 8 || item.userItemFilterType == 4 || item.userItemFilterType == 2);
         return '<li><span class="name">' + item.name + (showId ? '</span></li>' : ('</span><span class="id">' + item.userId + '</span></li>'));
      },
      tokenFormatter: function(item) {
         if(!item) {return '';}
         var isGroup = item.userItemFilterType && (item.userItemFilterType == 8 || item.userItemFilterType == 2);
         var type = isGroup ? "group" : "";
         return '<li><span class="name" itemtype='+type+'>' + item.name + '</span></li>'; 
      },
      minChars: 3,
      onAdd: function(item) {
         var val = suggester.attr('value');
         if(val.length > 0) {
            val +="¤¤";
         }
         val += item.userId;
         suggester.attr('value', val);
         suggester.val(val);
      },
      onDelete: function(item) {
         removeFromList('¤¤', item.userId, suggester);
         suggester.val(suggester.attr('value'));
      },
      prePopulate: suggester.hasClass('prePopulate') ? [{id: suggester.parent().find('#initialUserId').attr('value'), name: suggester.parent().find('#initialUserName').attr('value')}] : null,
      searchDelay: 0,
      tokenLimit: suggester.hasClass('singleEntry') ? 1 : null,
      hintText: false,
      noResultsText: 'Vasted puuduvad',
      searchingText: 'Otsimine...',
      deleteText: '&times;',
      appendToElement: $(".autocomplete").parent()
   });
}

function removeFromList(separator, valueToRemove, $element) {
   var separatorLength = separator.length;
   var val = $element.attr('value');
   if(!val) {
      return;
   }
   val = val.replace(valueToRemove, '');
   val = val.replace(separator + separator, separator);
   if(val.length > 0 && val.substr(val.length - separatorLength) == separator) {
      val = val.substr(0, val.length - separatorLength);
   }
   if(val.length >= separatorLength && val.substr(0, separatorLength) == separator) {
      val = val.substr(separatorLength, val.length);
   }
   $element.attr('value', val);
}

var contextPath = null;

function getContextPath() {
   if (contextPath == null) {
      var path = window.location.pathname;
      var idx = path.indexOf("/", 1);
      if (idx != -1) {
         contextPath = path.substring(0, idx);
      } else {
         contextPath = "";
      }
   }
   return contextPath;
}

function getMobileIdSignature(signingFlowId, uri) {
   var mobileIdChallengeId = $('#mobileIdChallengeId').val();
   $.ajax({
      type: 'POST',
      mode: 'queue',
      url: uri,
      data: JSON.stringify( {mobileIdChallengeId : mobileIdChallengeId, signingFlowId : signingFlowId }),
      dataType: 'json',
      contentType: 'application/json',
      success: function( responseText, status, xhr ) {
         if (responseText == 'FINISH') {
            $('#mobileIdChallengeMessage').html('<p>Vastus saadud...</p>');
            $('#finishMobileIdSigning').click();
         } else if (responseText == 'REPEAT') {
            window.setTimeout(function (){ getMobileIdSignature(signingFlowId, uri);}, 2000);
         } else if (responseText.indexOf('ERROR') == 0){
            $('#mobileIdChallengeMessage').html('<p>' + responseText.substring(5) + '</p>');            
         }
      },
   });
}

function isEmptyInput(inputId) {
   var inputValue = document.getElementById(inputId).value;
   return isEmptyValue(inputValue);
}

function isEmptyInputOr(inputId, valueToCheck) {
   var inputValue = document.getElementById(inputId).value;
   return isEmptyValue(inputValue) || inputValue == valueToCheck;
}

function isEmptyValue(inputValue) {
   return inputValue == null || (!(inputValue instanceof Array) && inputValue.replace(/^\\s+|\\s+$/g, '').length == 0);
}

function addShowMoreListener(element) {
   var lenght = 150,
   more = 'Näita rohkem',
   less = 'Näita vähem';

   if(element.text().length > lenght) {
      var target = element,
      trigger = $('<a>').addClass('more').html(more);

      trigger
         .on('click', function(e) {
            if(target.hasClass('active')) {
               trigger.html(less);
               target.removeClass('active');
            } else {
               trigger.html(more);
               target.addClass('active');
            }
         });
      element.addClass('active').append(trigger);
   }
}

function addFancyBox($element) {
   $element.fancybox({
      padding: 0,
      openEffect: "none",
      closeEffect: "none",
      scrolling: "visible",
      fitToView: false,
      minWidth: 250,
      autoSize: true,
      helpers: {
         overlay: { closeClick: false } // prevents closing when clicking OUTSIDE fancybox
      },
      tpl: {
         wrap: '<div class="fancybox-wrap" tabIndex="-1"><div class="fancybox-skin"><div class="fancybox-outer"><div class="fancybox-inner"></div></div></div></div>',
         iframe: '<iframe id="fancybox-frame{rnd}" name="fancybox-frame{rnd}" class="fancybox-iframe" frameborder="0" vspace="0" hspace="0"' + (window.navigator.userAgent.indexOf("MSIE ") > 0 ? ' allowtransparency="true"' : '') + '></iframe>',
         error: '<p class="fancybox-error">Sisu pole võimalik laadida.</p>',
         closeBtn: '<a title="Sulge" class="fancybox-item fancybox-close" href="javascript:;"></a>'
      }
   });
}

var translations = [];

function addTranslation(key, translation) {
   translations[key] = translation;
}

function translate(key) {
   var translation = translations[key];
   if (translation == null) {
      translation = key;
   }
   return translation;
}