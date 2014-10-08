<<<<<<< HEAD
//
// Helper functions for common components
// Kevin Roast 12-04-2005
//

// Menu component functions
var _lastMenu = null;
var _curtain = null;

function setupCurtain() {
   _curtain = document.createElement("div");
   _curtain.id = "menuCurtain";
   _curtain.style.width = "100%";
   _curtain.style.height = "100%";
   _curtain.style.position = "absolute";
   _curtain.style.top = "0px";
   _curtain.style.left = "0px";
   _curtain.style.zIndex = "9999";
   _curtain.style.backgroundColor = "transparent"; // IE7
   _curtain.onclick = _hideLastMenu;
}

// toggle temporary menu children visibility
function _toggleTempMenu(e, menuId) {
	$jQ("#" + escapeId4JQ(menuId)).toggle();
	return false;
}

// toggle a dynamic menu dropping down
function _toggleMenu(e, menuId)
{
   // hide any open menu
   if (_lastMenu != null && _lastMenu != menuId)
   {
      document.getElementById(_lastMenu).style.display = 'none';
      _lastMenu = null;
   }
   
   // toggle visibility of the specified element id
   if (document.getElementById(menuId).style.display == 'none')
   {
      var menu = document.getElementById(menuId);
      var origTop = menu.style.top;
      var winHeight, top;
      menu.style.top = '-300px';
      menu.style.visibility = 'hidden';
      menu.style.display = 'block';
       
      if (typeof(window.innerWidth) == 'number') {
         // Non-IE
         winHeight = window.innerHeight;
         top = 20;
      } else if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
         // IE 6+ in 'standards compliant mode'
         winHeight = document.documentElement.clientHeight;
         top = 20;
      }
      
      menu.style.top = origTop;
      
      menu.style.visibility = 'visible';
      _lastMenu = menuId;
      
      // set global onclick handler to hide menu
   	e.cancelBubble = true;
   	if (e.stopPropagation)
   	{
   	   e.stopPropagation();
   	}
   	
   	  var primaryMenu = menu.parentNode.parentNode.parentNode;
   	  if(primaryMenu.id == "menu") {
   		setupCurtain();
	   	primaryMenu.insertBefore(_curtain, primaryMenu.firstChild);
   	  } else {
   		  document.onclick = _hideLastMenu;
   	  }
   }
   else
   {
      document.getElementById(menuId).style.display = 'none';
      document.onclick = null;
      _hideLastMenu();
   }
}

// Hide the last opened menu
function _hideLastMenu()
{
   if (_lastMenu != null)
   {
      document.getElementById(_lastMenu).style.display = 'none';
      _lastMenu = null;
      document.onclick = null;
   }

   if(_curtain != null) {
	   _curtain.parentNode.removeChild(_curtain);
	   _curtain = null;
   }
}

function _togglePersistentMenu(e, menuId)
{
   var menu = document.getElementById(menuId);
   
   var targ;
   if (!e) var e = window.event;
   if (e.target) targ = e.target;
   else if (e.srcElement) targ = e.srcElement;
   if (targ.nodeType == 3) // defeat Safari bug
      targ = targ.parentNode;
   var liElement = targ.parentNode;
   var expanded = "expanded";
   var clName = liElement.className;
   var startIdx = clName.indexOf(expanded);
   if(startIdx == -1)
   {
      liElement.className = clName + " " + expanded;
   }
   else
   {
      liElement.className = clName.substring(0, startIdx) + clName.substring(startIdx + expanded.length);
      if(clName.indexOf("dropdown") == -1) {
    	  liElement.className = liElement.className + " dropdown";
      }
   }
   
   if (menu.style.display == 'none')
   {
      menu.style.display = 'block';
   }
   else
   {
      menu.style.display = 'none';
   }
   return false;
=======
//
// Helper functions for common components
// Kevin Roast 12-04-2005
//

// Menu component functions
var _lastMenu = null;
var _curtain = null;

function setupCurtain() {
   _curtain = document.createElement("div");
   _curtain.id = "menuCurtain";
   _curtain.style.width = "100%";
   _curtain.style.height = "100%";
   _curtain.style.position = "absolute";
   _curtain.style.top = "0px";
   _curtain.style.left = "0px";
   _curtain.style.zIndex = "9999";
   _curtain.style.backgroundColor = "transparent"; // IE7
   _curtain.onclick = _hideLastMenu;
}

// toggle temporary menu children visibility
function _toggleTempMenu(e, menuId) {
	$jQ("#" + escapeId4JQ(menuId)).toggle();
	return false;
}

// toggle a dynamic menu dropping down
function _toggleMenu(e, menuId)
{
   // hide any open menu
   if (_lastMenu != null && _lastMenu != menuId)
   {
      document.getElementById(_lastMenu).style.display = 'none';
      _lastMenu = null;
   }
   
   // toggle visibility of the specified element id
   if (document.getElementById(menuId).style.display == 'none')
   {
      var menu = document.getElementById(menuId);
      var origTop = menu.style.top;
      var winHeight, top;
      menu.style.top = '-300px';
      menu.style.visibility = 'hidden';
      menu.style.display = 'block';
       
      if (typeof(window.innerWidth) == 'number') {
         // Non-IE
         winHeight = window.innerHeight;
         top = 20;
      } else if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
         // IE 6+ in 'standards compliant mode'
         winHeight = document.documentElement.clientHeight;
         top = 20;
      }
      
      menu.style.top = origTop;
      
      menu.style.visibility = 'visible';
      _lastMenu = menuId;
      
      // set global onclick handler to hide menu
   	e.cancelBubble = true;
   	if (e.stopPropagation)
   	{
   	   e.stopPropagation();
   	}
   	
   	  var primaryMenu = menu.parentNode.parentNode.parentNode;
   	  if(primaryMenu.id == "menu") {
   		setupCurtain();
	   	primaryMenu.insertBefore(_curtain, primaryMenu.firstChild);
   	  } else {
   		  document.onclick = _hideLastMenu;
   	  }
   }
   else
   {
      document.getElementById(menuId).style.display = 'none';
      document.onclick = null;
      _hideLastMenu();
   }
}

// Hide the last opened menu
function _hideLastMenu()
{
   if (_lastMenu != null)
   {
      document.getElementById(_lastMenu).style.display = 'none';
      _lastMenu = null;
      document.onclick = null;
   }

   if(_curtain != null) {
	   _curtain.parentNode.removeChild(_curtain);
	   _curtain = null;
   }
}

function _togglePersistentMenu(e, menuId)
{
   var menu = document.getElementById(menuId);
   
   var targ;
   if (!e) var e = window.event;
   if (e.target) targ = e.target;
   else if (e.srcElement) targ = e.srcElement;
   if (targ.nodeType == 3) // defeat Safari bug
      targ = targ.parentNode;
   var liElement = targ.parentNode;
   var expanded = "expanded";
   var clName = liElement.className;
   var startIdx = clName.indexOf(expanded);
   if(startIdx == -1)
   {
      liElement.className = clName + " " + expanded;
   }
   else
   {
      liElement.className = clName.substring(0, startIdx) + clName.substring(startIdx + expanded.length);
      if(clName.indexOf("dropdown") == -1) {
    	  liElement.className = liElement.className + " dropdown";
      }
   }
   
   if (menu.style.display == 'none')
   {
      menu.style.display = 'block';
   }
   else
   {
      menu.style.display = 'none';
   }
   return false;
>>>>>>> develop-5.1
}