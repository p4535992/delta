// Modified functionality should be ported to mDelta.js

$(document).ready(function() {
	"use strict";
	$('[data-confirm]').each(function() {
		var trigger = $(this),
			target = $(this).attr('href'),
			text = $('<h2>').html($(this).attr('data-text')),
			alert = $('<div>').attr('id', 'alert').append(text),
			buttongroup = $('<div>').attr('class', 'buttongroup').appendTo(alert),
			yes = $('<a>').attr('class', 'continue').attr('href', target).html($(this).attr('data-accept')).appendTo(buttongroup),
			no = $('<a>').attr('class', 'alt').html($(this).attr('data-decline')).appendTo(buttongroup),
			screen = $('<div>').attr('id', 'screen'),
			transitionEnd = 'transitionend webkitTransitionEnd oTransitionEnd otransitionend';
		
		trigger.on('click', function(e) {
			e.preventDefault();
			screen.remove();
			alert.appendTo(screen).css('top', $(window).height()/ 4);
			screen.appendTo('body').css('height', $(window).height()).addClass('active');
			
			alert.find('a')
				.on('click', function(e) {
					if($(this) == no) e.preventDefault;
					screen.removeClass('active').on(transitionEnd, function() { screen.remove(); });
				});
		});
	});

	// Auto-growing textarea
	$('textarea').autosize({ append: "\n" });
	
	// tabs
	$('.tabs').each(function() {
		var parent = $(this),
			triggers = $(this).find('.tablinks a'),
			tabs = parent.find('section');
		
		triggers.each(function() {
			if($(this).hasClass('active') && $($(this).attr('href')).length > 0) $($(this).attr('href')).addClass('active');
			
			$(this).on('click', function(e) {
				e.preventDefault();
				
				if($($(this).attr('href')).length > 0) {
					triggers.removeClass('active');
					tabs.removeClass('active');
					$(this).addClass('active');
					$($(this).attr('href')).addClass('active');
				}
			})
		})
	})

	
});
