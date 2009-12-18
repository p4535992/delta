//
// Helper functions for common components
// Kevin Roast 12-04-2005
//

// Menu component functions
var _lastMenu = null;

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
         //Non-IE
         winHeight = window.innerHeight;
         top = 20;
      } else if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
         //IE 6+ in 'standards compliant mode'
         winHeight = document.documentElement.clientHeight;
         top = 20;
      }
      
      if ((winHeight - e.clientY) < menu.offsetHeight) 
      {
         menu.style.top = '-' + (menu.offsetHeight + top) + 'px';
      }
      else
      {
         menu.style.top = origTop;
      }
      
      menu.style.visibility = 'visible';
      _lastMenu = menuId;
      
      // set global onclick handler to hide menu
   	e.cancelBubble = true;
   	if (e.stopPropagation)
   	{
   	   e.stopPropagation();
   	}
      document.onclick = _hideLastMenu;
   }
   else
   {
      document.getElementById(menuId).style.display = 'none';
      document.onclick = null;
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
   
   var clName = liElement.className;
   if(clName.indexOf("expanded") == -1)
   {
      liElement.className = clName + " expanded";
   }
   else
   {
      liElement.className = "dropdown";
   }
   
   if (menu.style.display == 'none')
   {
      menu.style.display = 'block';
   }
   else
   {
      menu.style.display = 'none';
   }
}