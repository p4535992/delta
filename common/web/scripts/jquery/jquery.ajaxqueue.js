<<<<<<< HEAD
// Based on http://www.onemoretake.com/2009/10/11/ajaxqueue-and-jquery-1-3/

(function(jQuery) {

   var ajax = jQuery.ajax, ajaxRunning = [];

   jQuery.ajaxDestroy = function() {
      ajax = function() {
         jQuery.log('AJAX request dissapeared into black hole');
      };
   };

   jQuery.ajaxPause = function(port) {
      if (!port) {
         port = "";
      }

      var queue = jQuery([ ajax ]).queue("ajax" + port);
      queue.unshift(function() {
         jQuery.log('AJAX queue pause begin');
      });
      jQuery([ ajax ]).queue("ajax" + port, queue);

      if (jQuery([ ajax ]).queue("ajax" + port).length == 1 && !ajaxRunning[port]) {
         ajaxRunning[port] = true;
         jQuery([ ajax ]).dequeue("ajax" + port);
      }
   };

   jQuery.ajaxResume = function(port) {
      if (!port) {
         port = "";
      }

      jQuery.log('AJAX queue pause end');
      
      if (jQuery([ ajax ]).queue("ajax" + port).length > 0) {
         jQuery([ ajax ]).dequeue("ajax" + port);
      } else {
         ajaxRunning[port] = false;
      }
   }

   jQuery.ajax = function(settings) {
      // create settings for compatibility with ajaxSetup
      settings = jQuery.extend(settings, jQuery.extend({}, jQuery.ajaxSettings, settings));

      var port = settings.port;
      if (!port) {
         port = "";
      }

      switch (settings.mode) {
      case "queue":
         var _old = settings.complete;
         settings.complete = function() {
            if (_old) {
               _old.apply(this, arguments);
            }
            if (jQuery([ ajax ]).queue("ajax" + port).length > 0) {
               jQuery([ ajax ]).dequeue("ajax" + port);
            } else {
               ajaxRunning[port] = false;
            }
         };

         jQuery([ ajax ]).queue("ajax" + port, function() {
            ajax(settings);
         });

         if (jQuery([ ajax ]).queue("ajax" + port).length == 1 && !ajaxRunning[port]) {
            ajaxRunning[port] = true;
            jQuery([ ajax ]).dequeue("ajax" + port);
         }

         return;
      }
      return ajax.apply(this, arguments);
   };

})(jQuery);
=======
// Based on http://www.onemoretake.com/2009/10/11/ajaxqueue-and-jquery-1-3/

(function(jQuery) {

   var ajax = jQuery.ajax, ajaxRunning = [];

   jQuery.ajaxDestroy = function() {
      ajax = function() {
         jQuery.log('AJAX request dissapeared into black hole');
      };
   };

   jQuery.ajaxPause = function(port) {
      if (!port) {
         port = "";
      }

      var queue = jQuery([ ajax ]).queue("ajax" + port);
      queue.unshift(function() {
         jQuery.log('AJAX queue pause begin');
      });
      jQuery([ ajax ]).queue("ajax" + port, queue);

      if (jQuery([ ajax ]).queue("ajax" + port).length == 1 && !ajaxRunning[port]) {
         ajaxRunning[port] = true;
         jQuery([ ajax ]).dequeue("ajax" + port);
      }
   };

   jQuery.ajaxResume = function(port) {
      if (!port) {
         port = "";
      }

      jQuery.log('AJAX queue pause end');
      
      if (jQuery([ ajax ]).queue("ajax" + port).length > 0) {
         jQuery([ ajax ]).dequeue("ajax" + port);
      } else {
         ajaxRunning[port] = false;
      }
   }

   jQuery.ajax = function(settings) {
      // create settings for compatibility with ajaxSetup
      settings = jQuery.extend(settings, jQuery.extend({}, jQuery.ajaxSettings, settings));

      var port = settings.port;
      if (!port) {
         port = "";
      }

      switch (settings.mode) {
      case "queue":
         var _old = settings.complete;
         settings.complete = function() {
            if (_old) {
               _old.apply(this, arguments);
            }
            if (jQuery([ ajax ]).queue("ajax" + port).length > 0) {
               jQuery([ ajax ]).dequeue("ajax" + port);
            } else {
               ajaxRunning[port] = false;
            }
         };

         jQuery([ ajax ]).queue("ajax" + port, function() {
            ajax(settings);
         });

         if (jQuery([ ajax ]).queue("ajax" + port).length == 1 && !ajaxRunning[port]) {
            ajaxRunning[port] = true;
            jQuery([ ajax ]).dequeue("ajax" + port);
         }

         return;
      }
      return ajax.apply(this, arguments);
   };

})(jQuery);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
