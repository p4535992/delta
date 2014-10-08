package ee.webmedia.mobile.alfresco.workflow;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog;
import ee.webmedia.alfresco.workflow.web.SigningFlowContainer;
import ee.webmedia.mobile.alfresco.workflow.model.InProgressTasksForm;

public class MobileSigningFlowContainer extends SigningFlowContainer {

    private static final long serialVersionUID = 1L;
    private CompundWorkflowDetailsController controller;
    private RedirectAttributes redirectAttributes;

    public MobileSigningFlowContainer(SignatureTask signatureTask, InProgressTasksForm inProgressTasksForm, NodeRef compoundWorkflowRef, NodeRef containerRef) {
        super(signatureTask, inProgressTasksForm, compoundWorkflowRef, containerRef);
        setPhoneNumber(getUserPhoneNumber());
    }

    private String getUserPhoneNumber() {
        String userName = AuthenticationUtil.getFullyAuthenticatedUser();
        BeanHelper.getUserService().getUserMobilePhone(userName);
        return null;
    }

    @Override
    protected boolean isSignTogether() {
        return inProgressTasksForm.getTask(signatureTask.getNodeRef()).isSignTogether();
    }

    @Override
    protected void handleFileExistsException() {
        controller.addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.ddoc.file.exists");
    }

    @Override
    protected void handleSignatureException(Exception e) {
        LOG.warn(e.getMessage(), e);
        String additionalInfo = "";
        if (e.getCause() != null && StringUtils.isNotEmpty(e.getCause().getMessage())) {
            additionalInfo = ": " + e.getCause().getMessage();
        }
        Pair<MessageSeverity, String> message = controller.addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.ddoc.signature.failed");
        message.setSecond(message.getSecond() + additionalInfo);
    }

    @Override
    protected void handleWorkflowChanged(WorkflowChangedException e) {
        CompoundWorkflowDialog.logWorkflowChangedException(e, "Finishing signature task failed", LOG);
        controller.addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
    }

    @Override
    protected void handleTaskFinishedSuccess() {
        controller.addRedirectInfoMsg(redirectAttributes, "workflow.task.finish.success.defaultMsg");
    }

    @Override
    protected void handleSignatureError() {
        handleUnableToPerformException(signatureError);
    }

    @Override
    protected void handleNodeLockedException(NodeLockedException e) {
        e.setCustomMessageId(null);
        Pair<String, Object[]> messageKeyAndValueHolders = DocumentLockHelperBean.getErrorMessageKeyAndValueHolders("workflow.task.finish.error.document.registerDoc.docLocked",
                e.getNodeRef());
        controller.addRedirectErrorMsg(redirectAttributes, messageKeyAndValueHolders.getFirst(), messageKeyAndValueHolders.getSecond());
    }

    @Override
    protected void handleUnableToPerformException(MessageData e) {
        controller.addRedirectErrorMsg(redirectAttributes, e.getMessageKey(), e.getMessageValuesForHolders());
    }

    @Override
    protected void handleZeroByteFileError() {
        controller.addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.task.files.zero.byte.file");
    }

    @Override
    protected void handleFileRequiredError() {
        controller.addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.task.files.required");
    }

    @Override
    protected void doAfterPrepareSigning() {
        // no action needed
    }

    public boolean prepareSigning(CompundWorkflowDetailsController controller, RedirectAttributes redirectAttributes) {
        setMessageHandler(controller, redirectAttributes);
        return super.prepareSigning();
    }

    private void setMessageHandler(CompundWorkflowDetailsController controller, RedirectAttributes redirectAttributes) {
        this.controller = controller;
        this.redirectAttributes = redirectAttributes;
    }

    public boolean startMobileIdSigning(CompundWorkflowDetailsController controller, RedirectAttributes redirectAttributes) {
        setMessageHandler(controller, redirectAttributes);
        return super.startMobileIdSigning();
    }

    public boolean finishMobileIdSigning(CompundWorkflowDetailsController controller, RedirectAttributes redirectAttributes) {
        setMessageHandler(controller, redirectAttributes);
        return super.finishMobileIdSigning();
    }
}
