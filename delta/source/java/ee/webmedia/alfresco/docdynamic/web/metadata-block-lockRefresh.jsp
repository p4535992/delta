<%@page import="ee.webmedia.alfresco.common.web.BeanHelper"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript" id="metaLockRefreshScript" >
$jQ(document).ready(function(){
   var lockingAllowed = <%= BeanHelper.getDocumentLockHelperBean().isLockingAllowed() %>;
   if(lockingAllowed){
      var clientLockRefreshFrequency = <%= BeanHelper.getDocumentLockHelperBean().getLockExpiryPeriod() %>;
      setTimeout(requestForLockRefresh, clientLockRefreshFrequency/3); // We need to lock sooner for the first time (add file dialog etc.)
   }
});

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
</script>
</f:verbatim>
