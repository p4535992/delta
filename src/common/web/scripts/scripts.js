
/**
 * @return input string where ":" and "." are escaped, so that the result could be used by jQuery
 */
function escapeId4JQ(idToEscape)
{
   return idToEscape.replace(/:/g, "\\:").replace(/\./g, "\\.");
}

/**
 * append selection of source element to the element with id ending with "toItemIdSuffix" and beginning with the same id prefix as selectBox
 * @author Ats Uiboupin
 */
function appendSelection(source, targetId)
{
   var appendSepparator = ', ';
   var targetElem = $jQ("#" + escapeId4JQ(targetId));
   var lable = $jQ('#' + escapeId4JQ(source.attr("id")) + ' :selected').text(); // using label not value!
   var lastToItemValue = targetElem.val();
   if (lastToItemValue.length != 0)
   {
      lastToItemValue = lastToItemValue + appendSepparator;
   }
   targetElem.val(lastToItemValue + lable);
};


$jQ(document).ready(function()
{
   /**
    * Binder for alfresco properties that are generated with ClassificatorSelectorAndTextGenerator.class
    * Binds all elements that have class="selectBoundWithText" with corresponding textAreas/inputs(assumed to have same id prefix and suffix specified with TARGET_SUFFIX) 
    * @author Ats Uiboupin
    */
   $jQ(".selectBoundWithText").each(function (intIndex)
   {
      var TARGET_SUFFIX = "select_target"; // corresponding textAreas/input
      var selectId = $(this).id;
      var textAreaId = selectId.substring(0, selectId.lastIndexOf(':') + 1) + TARGET_SUFFIX;
//      var initialValue = $jQ(this).val();
      var initialValue = $jQ('#' + escapeId4JQ(selectId) + ' :selected').text()
      if (initialValue != "")
      {
         var targetElem = $jQ("#" + escapeId4JQ(textAreaId));
         targetElem.val(initialValue);
      }
      $jQ(this).bind("change", function()
      {
         appendSelection($jQ(this), textAreaId)
      });
   });
});

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
   var formId = document.forms[0].name;
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

function showModal(target, size){
   target = escapeId4JQ(target);
	if ($jQ("#overlay").length == 0) {
		$jQ("#" + target).before("<div id='overlay'></div>");
	}
	if (openModalContent != null){
		$jQ("#" + openModalContent).hide();
	}
	openModalContent = target;

	$jQ("#overlay").css("display","block");
	$jQ("#" + target).css("display","block");
	//TODO fadeIn does not work
	$jQ("#" + target).fadeIn(1150);

	if(size == 'big'){
		$jQ("#" + target).addClass("modalpopup-large");
	} else {
		$jQ("#" + target).removeClass("modalpopup-large");
	}

	$jQ("#" + target).css("margin-top","-" + $jQ("#" + target).height() / 2 + "px");

	return false;
}

$jQ(document).ready(function(){
	$jQ(".modalwrap").click(function(){ positionDialog() });
	$jQ(".modalwrap").find(":radio, :checkbox, select").change(function(){ positionDialog() });
   $jQ(".modalwrap select option").tooltip();
});

function positionDialog(){
	if( $jQ(".modalwrap").css("position") == "absolute" ){
		return false;
	} else {
		$jQ(".modalwrap").animate({ 
			marginTop: "-" + $jQ(".modalwrap").height() / 2 + "px"
		}, "normal" );
	}
}

function hideModal(){
	if (openModalContent != null){
		$jQ("#" + openModalContent).fadeOut(150, function(){
	      $jQ("#overlay").remove();
	   });
	}
	$jQ("#" + openModalContent).removeClass("modalpopup-large");

	return false;
}
