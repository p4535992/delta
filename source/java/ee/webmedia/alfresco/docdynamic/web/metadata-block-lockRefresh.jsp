<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript" id="metaLockRefreshScript" >
<<<<<<< HEAD
=======
var finishButtonClicked = false;
var excludedElementsIds = new Array("dialog:finish-button","dialog:finish-button-2","dialog:cancel-button","dialog:cancel-button-2","dialog:documentRegisterButton","dialog:documentRegisterButton-2");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
$jQ(document).ready(function(){
   var lockingAllowed = <%= BeanHelper.getDocumentLockHelperBean().isLockingAllowed() %>;
   if(lockingAllowed){
      var clientLockRefreshFrequency = <%= BeanHelper.getDocumentLockHelperBean().getLockExpiryPeriod() %>;
      setTimeout(requestForLockRefresh, clientLockRefreshFrequency/3); // We need to lock sooner for the first time (add file dialog etc.)
<<<<<<< HEAD
   }
});

=======
      disableUnlockOnExcludedElements();
      disableUnlockOnExtraButtons();
   }
});
function disableUnlockOnExcludedElements() {
   if(typeof excludedElementsSpecific != 'undefined') { // excludedElementsSpecific is defined in metadata-unlocking-helper.jsp.
	  excludedElementsIds = $jQ.merge(excludedElementsIds, excludedElementsSpecific);
   }
   for(var i = 0; i < excludedElementsIds.length; i++) {
      var element = document.getElementById(excludedElementsIds[i]);
      disableUnlockOnElement(element);
   }
}
function disableUnlockOnExtraButtons() {
   var buttonClasses = $jQ(".actions-menu");
   for(var i = 0; buttonClasses != null && i < buttonClasses.length; i++) {
      var anchors = buttonClasses[i].getElementsByTagName("a");
      if(anchors != null){
         disableUnlockOnElements(anchors);
      }
   }
}
function disableUnlockOnElements(collection){
   for(var i = 0; i < collection.length; i++) {
      disableUnlockOnElement(collection[i]);
   }
}
function disableUnlockOnElement(element) {
   if(element != null){
      $jQ(element).on('mouseup', function() {
         finishButtonClicked = true;
      });
   }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
function requestForLockRefresh() {
   var uri = getContextPath() + '/ajax/invoke/DocumentLockHelperBean.refreshLockClientHandler';
   $jQ.ajax({
      type: 'POST',
      url: uri,
      mode: 'queue',
      success: requestForLockRefreshSuccess,
      error: requestForLockRefreshFailure,
      dataType: 'xml'
   });
}
function requestForLockRefreshSuccess(xml) {
   if (!xml) { // check that response is not empty
      return;
   }
   xml = xml.documentElement;
   var success = xml.getAttribute('success');
   var nextReqInMs = xml.getAttribute('nextReqInMs');
   setTimeout(requestForLockRefresh, nextReqInMs/2); // start new request based on suggested timeout (take page loading and request times into account, divide by two)
   if(!success){
      var errMsg = xml.getAttribute('errMsg');
      alert("Dokumendi lukustamise uuendamine ebaõnnestus: " + errMsg);
   }
}
function requestForLockRefreshFailure() {
   $jQ.log("Refreshing lock in server side failed");
}
<<<<<<< HEAD
=======
$jQ(window).on('beforeunload', function(){
   if(finishButtonClicked) {
	  return;
   }
   var uri = getContextPath() + '/ajax/invoke/DocumentLockHelperBean.unlockNode';
   $jQ.ajax({
      type: 'POST',
      url: uri,
      async: false
   });
});
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
</script>
</f:verbatim>
