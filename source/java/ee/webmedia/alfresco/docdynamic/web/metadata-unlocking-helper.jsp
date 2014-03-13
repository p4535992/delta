<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<f:verbatim>
<script type="text/javascript">
var excludedElementsSpecific = new Array("addFile","addInactiveFile","log-details-link");
function addAdditionalBlockElementsToExcludedList() {
   var blocks = new Array("filelistList","inactiveFilelistList","assocsList","logList","documentWorkflowList");
   for(var i = 0; i < blocks.length; i++) {
      addSubelementsWithClassToExcludedList(blocks[i],".header"); // sortable headers
      addSubelementsWithClassToExcludedList(blocks[i],".icon-link"); // iconlinks in blocks
   }
}
function addSubelementsWithClassToExcludedList(element,className) {
   var elem = document.getElementById(element);
   if(elem == null) {
      return;
   }
   var childElements = $jQ(elem).find(className);
   disable(childElements);
}
function generateIdIfNecessary(element) {
   if(!$jQ(element).attr("id")) {
      $jQ(element).attr("id", $jQ(element).attr("class") + Math.random().toString(36).substr(2));
   }
}
function disableUnlockOnPager() {
   var numberSelects = $jQ(".page-controls");
   for(var i = 0; numberSelects != null && i < numberSelects.length; i++) {
      var selectItem = numberSelects[i].getElementsByTagName("select")[0];
      if(selectItem != null) {
         disableUnlockOnSelectElement(selectItem.id);
      }
   }
   var navigators = $jQ(".pager-wrapper");
   for(var i = 0; navigators != null && i < navigators.length; i++) {
      var anchors = navigators[i].getElementsByTagName("a");
      disable(anchors);
   }
}
function disableUnlockOnSelectElement(elementId) {
   var element = document.getElementById(elementId);
   if(element == null) {
      return;
   }
   var options = element.getElementsByTagName("option");
   if(options != null) {
      for(var i = 0; options != null && i < options.length; i++) {
         $jQ(options[i]).on('mouseup', function() {
            finishButtonClicked = true;
         });
      }
   }
}
function disable(elements) {
   for(var i = 0; elements != null && i < elements.length; i++) {
      generateIdIfNecessary(elements[i]);
      excludedElementsSpecific.push(elements[i].id);
   }
}
</script>
</f:verbatim>