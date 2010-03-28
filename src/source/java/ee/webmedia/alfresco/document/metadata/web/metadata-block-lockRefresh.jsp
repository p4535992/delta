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
   YAHOO.util.Connect.asyncRequest("GET", getContextPath() + '/ajax/invoke/MetadataBlockBean.refreshLockClientHandler', 
         { 
            success: requestForLockRefreshSuccess
            ,failure: requestForLockRefreshFailure
         }, 
         null);
}
function requestForLockRefreshSuccess(ajaxResponse) {
   var xml = ajaxResponse.responseXML.documentElement;
   var success = xml.getAttribute('success');
   var nextReqInMs = xml.getAttribute('nextReqInMs');
   setTimeout(requestForLockRefresh, nextReqInMs); // start new request based on sugessted timeout
   if(!success){
      var errMsg = xml.getAttribute('errMsg');
      alert("failed to refresh lock on document: "+errMsg);
   }
}
function requestForLockRefreshFailure(ajaxResponse) {
	// alert("response: "+ajaxResponse.responseText);
}
</script>
</f:verbatim>
