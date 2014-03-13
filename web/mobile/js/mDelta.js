// Custom JavaScript required for mDelta

// See: http://www.adequatelygood.com/JavaScript-Module-Pattern-In-Depth.html
var mDelta = (function($) {
   var api = {};

   var debugEnabled = true;

   // Public methods

   api.init = function() {
      initTriggers();
      initPagers();
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
      return index < 0 ? "" : document.URL.substring(index + 2, document.URL.length);;
   }

   return api;

}(jQuery));

$(document).ready(function() {
   mDelta.init();
});

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

function isEmptyValue(inputValue) {
   return inputValue == null || (!(inputValue instanceof Array) && inputValue.replace(/^\\s+|\\s+$/g, '').length == 0);
}