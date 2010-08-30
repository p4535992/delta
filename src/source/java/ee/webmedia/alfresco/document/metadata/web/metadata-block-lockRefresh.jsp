<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript" id="metaLockRefreshScript" >
$jQ(document).ready(function(){
   var inEditMode = '</f:verbatim><h:outputText id="textInEditMode" value="#{MetadataBlockBean.inEditMode}" /><f:verbatim>';
   if(inEditMode){
      var clientLockRefreshFrequency = '</f:verbatim><h:outputText id="textClientLockRef" value="#{MetadataBlockBean.clientLockRefreshFrequency}" /><f:verbatim>';
      setTimeout(requestForLockRefresh, clientLockRefreshFrequency/3);
   }
});

function requestForLockRefresh() {
   var uri = getContextPath() + '/ajax/invoke/MetadataBlockBean.refreshLockClientHandler';
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
   if (!xml) {
      return;
   }
   xml = xml.documentElement;
   var success = xml.getAttribute('success');
   var nextReqInMs = xml.getAttribute('nextReqInMs');
   setTimeout(requestForLockRefresh, nextReqInMs); // start new request based on sugessted timeout
   if(!success){
      var errMsg = xml.getAttribute('errMsg');
      alert("failed to refresh lock on document: "+errMsg);
   }
}
function requestForLockRefreshFailure() {
   $jQ.log("Refreshing lock in server side failed");
}
</script>
</f:verbatim>
