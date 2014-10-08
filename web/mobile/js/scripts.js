// Modified functionality should be ported to mDelta.js

$(document).ready(function() {
	"use strict";
	$('.readmore').each(function() {
		var lenght = 150,
			more = 'Näita rohkem',
			less = 'Näita vähem';
			
		if($(this).text().length > lenght) {
			var target = $(this),
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
				
			$(this).addClass('active').append(trigger);
			
		}
	});
	
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
	
	// Datepicker. http://amsul.ca/pickadate.js/date.htm
	$('.datepicker').pickadate({
		firstDay: 1,
		monthsFull: ['Jaanuar', 'Veebruar', 'Märts', 'Aprill', 'Mai', 'Juuni', 'Juuli', 'August', 'September', 'Oktoober', 'November', 'Detsember'],
		weekdaysShort: ['Püh', 'Esm', 'Tei', 'Kol', 'Nel', 'Ree', 'Lau'],
		today: 'Täna',
		clear: 'Kustuta',
		formatSubmit: 'dd.mm.yyyy'
	});
	
	// Timepicker. http://amsul.ca/pickadate.js/time.htm
	$('.timepicker').pickatime({
		clear: 'Kustuta',
		format: 'HH:i',
		interval: 15
	});
	
	// Autocomplete - http://loopj.com/jquery-tokeninput/
	$('.autocomplete')
		.tokenInput(
			[{
				'name': "Nipi Tiri",
				'id': '38001017000'
			}, {
				'name': "Kati Karu",
				'id': '48001018000'
			}, {
				'name': "Mati Karu",
				'id': '38001016000'
			}, {
				'name': "Aleksander Skafander",
				'id': '38001016666'
			}
			], {
				propertyToSearch: 'name',
				resultsFormatter: function(item) { return '<li><span class="name">' + item.name + '</span><span class="id">' + item.id + '</li>' }, 
				tokenFormatter: function(item) { return '<li><span class="name">' + item.name +  '</span></li>'},
				
				hintText: false,
				noResultsText: 'Vasted puuduvad',
				searchingText: 'Otsimine...',
				deleteText: '&times;'
			});
			
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
