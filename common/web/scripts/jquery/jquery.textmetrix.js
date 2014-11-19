<<<<<<< HEAD
// original source: http://stackoverflow.com/questions/118241/calculate-text-width-with-javascript
(function(jQuery) {

 jQuery.textMetrics = function(el) {

  var h = 0, w = 0;

  var div = document.createElement('div');
  document.body.appendChild(div);
  jQuery(div).css({
   position: 'absolute',
   left: -1000,
   top: -1000,
   display: 'none'
  });

  jQuery(div).html(jQuery(el).html());
  var styles = ['font-size','font-style', 'font-weight', 'font-family','line-height', 'text-transform', 'letter-spacing'];
  jQuery(styles).each(function() {
   var s = this.toString();
   jQuery(div).css({
    s: jQuery(el).css(s)
   });
  });

  h = jQuery(div).outerHeight();
  w = jQuery(div).outerWidth();

  jQuery(div).remove();

  var ret = {
   height: h,
   width: w
  };

  return ret;
 }

})(jQuery);
=======
// original source: http://stackoverflow.com/questions/118241/calculate-text-width-with-javascript
(function(jQuery) {

 jQuery.textMetrics = function(el) {

  var h = 0, w = 0;

  var div = document.createElement('div');
  document.body.appendChild(div);
  jQuery(div).css({
   position: 'absolute',
   left: -1000,
   top: -1000,
   display: 'none'
  });

  jQuery(div).html(jQuery(el).html());
  var styles = ['font-size','font-style', 'font-weight', 'font-family','line-height', 'text-transform', 'letter-spacing'];
  jQuery(styles).each(function() {
   var s = this.toString();
   jQuery(div).css({
    s: jQuery(el).css(s)
   });
  });

  h = jQuery(div).outerHeight();
  w = jQuery(div).outerWidth();

  jQuery(div).remove();

  var ret = {
   height: h,
   width: w
  };

  return ret;
 }

})(jQuery);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
