<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>

<script src='<c:url value="/scripts/jquery/jquery.ui.widget.min.js" />'></script>
<script src='<c:url value="/scripts/jquery/fileupload/jquery.fileupload.js" />'></script>
<script src='<c:url value="/mobile/js/files.js" />'></script>

<form:form modelAttribute="inProgressTasksForm" method="POST" >

	<c:if test="${ inProgressTasksForm.signingFlowView == 'GET_PHONE_NUMBER'}">
		<fmt:message key="workflow.task.sign.phone.number" var="modalTitle" />
		<tag:modal title="${ modalTitle }">
				<p class="valuerow" >
					<fmt:message key="workflow.task.sign.phone.number" var="inputTitle" />
					<span class="desc">${inputTitle}:</span>
					<span class="value">
						<input id="phoneNumber" name="phoneNumber" type="text" value="${ inProgressTasksForm.phoneNumber }" />
						<script type="text/javascript">
							$(document).ready(function(){
							   var disableBtnFunction = function() {
							      var disabled = isEmptyInputOr('phoneNumber', '+372');
							      var buttonToDisable = $('#signMobileBtn');
							      if (disabled) {
							         buttonToDisable.attr('disabled','disabled');
							      } else {
							         buttonToDisable.removeAttr('disabled');
							      }
							   };
							   disableBtnFunction();
							   $('#phoneNumber').bind('keyup', disableBtnFunction);
							   $('#phoneNumber').bind('change', disableBtnFunction);
							});
						</script>
					</span>
				</p>
                <p class="valuerow" >
                    <span class="desc"></span>
                    <span class="value" style="float:right;">
                        <fmt:message key="workflow.task.sign.default.signing.number" var="inputTitle" />
                        <form:checkbox id="defaultSigningNumber" path="defaultSigningNumber" />
                        <span>${inputTitle}</span>
                    </span>
                </p>
				<div class="buttongroup">
					<button type="submit" name="actions['mobileNumberInserted']" id="signMobileBtn"><fmt:message key="workflow.task.sign.button.title" /></button>
					<button type="submit" name="actions['signingCancelled']"><fmt:message key="workflow.task.sign.cancel.button.title" /></button>
				</div>
		</tag:modal>
	</c:if>
	
	<c:if test="${ inProgressTasksForm.signingFlowView == 'POLL_SIGNATURE' }">
		<fmt:message key="workflow.task.sign.waiting.title" var="modalTitle" />
		<tag:modal title="${ modalTitle }">
		<p style="text-align: center;">
		<p>Sõnumit saadetakse, palun oodake...</p>
		<p>Kontrollkood:</p>
		<p style="padding-top: 10px; font-size: 28px; vertical-align: middle;">${ inProgressTasksForm.mobileIdChallengeId }</p>
		<div class="buttongroup">
			<fmt:message key="workflow.task.sign.cancel.button.title"
				var="buttonLabel" />
			<input type="submit" name="actions['signingCancelled']"
				value="${ buttonLabel }" />
			<input
				id="finishMobileIdSigning"
				type="submit" name="actions['finishMobileIdSigning']"
				value="${ buttonLabel }" style="display: none;" />
		</div>
		</tag:modal>
		<c:url value="/m/compound-workflow/get-signature" var="uri" />
		<script type="text/javascript">
         $(document).ready(function() {
            var signingFlowId = $('#signingFlowId').val();
            window.setTimeout(function getSignature() {
               getMobileIdSignature(signingFlowId, "ajax/get-signature");
            }, 2000);
         });
      </script>
	</c:if>

	<input name="compoundWorkflowRef" value="${ inProgressTasksForm.compoundWorkflowRef }" type="hidden"  />
	<input name="containerRef" value="${ inProgressTasksForm.containerRef }" type="hidden" />
	<input id="signingFlowId" name="signingFlowId" value="${ inProgressTasksForm.signingFlowId }" type="hidden" />
	<input id="mobileIdChallengeId" name="mobileIdChallengeId" value="${ inProgressTasksForm.mobileIdChallengeId }" type="hidden" />
	<c:if test="${not empty inProgressTasksForm.inProgressTasks }">
		<c:forEach items="${inProgressTasksForm.inProgressTasks}" var="taskEntry" varStatus="status">
			<c:set value="${ taskEntry.value }" var="task" />
			<c:set value="${ task.type.localName }" var="taskType" />
            <c:set value="${ task.commentLabel }" var="commentLabel" />
            <tag:expanderBlock blockId="workflow-in-progress-tasks-${ status.index }" titleId="site.workflow.inProgressTasks.${ taskType }" expanded="true" independent="true">			
	            <div id="task-${task.nodeRef.id}">
					<tag:valueRow labelId="workflow.task.prop.resolution" value="${task.resolution }" />
					<fmt:formatDate value="${task.dueDate}" pattern="dd.MM.yyyy HH:mm" var="dueDateStr" />
					<tag:valueRow labelId="workflow.task.prop.dueDate" value="${ dueDateStr }" />
					<tag:formrow labelId="${ commentLabel }">
						<tag:textarea name="inProgressTasks['${taskEntry.key}'].comment" id="${task.comment}" value="${task.comment}" />
					</tag:formrow>
                    <c:if test="${ taskType == 'opinionTask' }" >
                       <tag:formrow labelId="workflow.task.prop.file">
                       <input id="fileupload-${task.nodeRef.id}" type="file" name="files[]" multiple/>
                       </tag:formrow>
                       <script>addFileUpload('${task.nodeRef.id}', '${task.nodeRef}', '<c:url value="/uploadFileServlet" />');</script>
                       <table id="taskFiles-${task.nodeRef.id}" class="task-files">
                          <tbody>
                             <c:forEach items="${task.files}" var="file" varStatus="status">
                                <tr class="fileRow">
                                   <td>${file.name}</td>
                                   <td class="actions">
                                      <a class="remove" data-file-ref="${file.nodeRef}" data-task-ref="${task.nodeRef}" data-delete-url="${file.deleteUrl}" onclick="deleteFile($(this))" ></a>
                                   </td>
                                </tr>
                             </c:forEach>
                          </tbody>
                       </table>
                    </c:if>
					<c:if test="${ taskType == 'reviewTask' }" >
						<tag:formrow labelId="workflow.task.review.tmp.outcome">					
							<form:select path="inProgressTasks['${taskEntry.key}'].reviewTaskOutcome" items="${ reviewTaskOutcomes }" />
						</tag:formrow>
					</c:if>
					<input name="inProgressTasks['${taskEntry.key}'].signTogether" value="${task.signTogether}" type="hidden" />
					<input name="inProgressTasks['${taskEntry.key}'].nodeRef" value="${task.nodeRef}" type="hidden" />
                    <input name="inProgressTasks['${taskEntry.key}'].typeStr" value="${task.typeStr}" type="hidden" />
					
					<div class="buttongroup">				
						<c:forEach items="${taskOutcomeButtons[task.nodeRef] }" var="outcomeBtn" varStatus="status" >
							<fmt:message key="${outcomeBtn.second}" var="buttonLabel" />
							<button type="submit" name="inProgressTasks['${taskEntry.key}'].actions['${ outcomeBtn.first }']" value="${ buttonLabel }"><c:out value="${ buttonLabel }" /></button>
						</c:forEach>
					</div>
	            </div>
            </tag:expanderBlock>
		</c:forEach>
	</c:if>
</form:form>


