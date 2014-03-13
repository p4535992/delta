<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil"%>

<script>
	function redirectLinkedReviewTask(url){
	   if(confirm('<%= MessageUtil.getMessageAndEscapeJS("task_original_object_open_confirm")%>')){
	      window.location = url;
	   }
       return false;
	}
</script>