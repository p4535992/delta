package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSignatureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog.handleWorkflowChangedException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SignatureChallenge;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.web.SignatureBlockBean;
import ee.webmedia.alfresco.user.service.Cas20ProxyReceivingRedirectingTicketValidationFilter;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.mobile.alfresco.workflow.SigningFlowView;
import ee.webmedia.mobile.alfresco.workflow.model.InProgressTasksForm;

public class SigningFlowContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SigningFlowContainer.class);

    private List<NodeRef> signingQueue;
    protected final SignatureTask signatureTask;
    private final NodeRef compoundWorkflowRef;
    private final NodeRef containerRef;
    /** Has non-null value if current signing is signing multiple (> 1) documents together */
    private NodeRef mainDocumentRef;
    private Map<NodeRef, String> originalStatuses;
    private Map<NodeRef, List<File>> signingFiles;
    private String phoneNumber;
    private String signature;
    protected MessageData signatureError;
    private SigningFlowView signingFlowView;
    protected InProgressTasksForm inProgressTasksForm;
    private final boolean signTogether;
    private boolean defaultTelephoneForSigning;

    public static final String EE_COUNTRY_CODE = "+372";
    public static final String LAST_USED_MOBILE_ID_NUMBER = "lastUsedMobileIdNumber";

    public SigningFlowContainer(SignatureTask signatureTask, boolean signTogether, InProgressTasksForm inProgressTasksForm, NodeRef compoundWorkflowRef, NodeRef containerRef) {
        this(signatureTask, signTogether, compoundWorkflowRef, containerRef);
        Assert.isTrue(inProgressTasksForm != null);
        this.inProgressTasksForm = inProgressTasksForm;
    }

    public SigningFlowContainer(SignatureTask signatureTask, boolean signTogether, NodeRef compoundWorkflowRef, NodeRef containerRef) {
        signingQueue = new ArrayList<>();
        this.signatureTask = signatureTask;
        this.compoundWorkflowRef = compoundWorkflowRef;
        this.containerRef = containerRef;
        this.signTogether = signTogether;
    }

    public boolean prepareSigning() {
        long step0 = System.currentTimeMillis();
        List<File> activeFiles = new ArrayList<File>();
        Map<NodeRef, List<File>> signingFilesMap = new HashMap<NodeRef, List<File>>();
        collectSigningFiles(activeFiles, signingFilesMap);
        if (!checkSigningFiles(activeFiles, false)) {
            return false;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("found " + activeFiles.size() + "active files for signing:\n");
            for (File activeFile : activeFiles) {
                LOG.debug("nodeRef=" + activeFile.getNodeRef() + "; displayName=" + activeFile.getDisplayName() + "\n");
            }
        }

        try {
            long step1 = System.currentTimeMillis();
            mainDocumentRef = null;
            originalStatuses = new HashMap<NodeRef, String>();
            boolean signSeparately = true;
            boolean existingBdoc = false;
            List<NodeRef> signingDocumentRefs = null;
            if (compoundWorkflowRef == null) {
                addSigningDocument(containerRef);
                addDocumentStatus(containerRef);
            } else {
                signingDocumentRefs = getWorkflowService().getCompoundWorkflowSigningDocumentRefs(compoundWorkflowRef);

                if (isSignTogether() && signingDocumentRefs.size() > 1) {
                    mainDocumentRef = getWorkflowService().getCompoundWorkflowMainDocumentRef(compoundWorkflowRef);
                    if (mainDocumentRef == null || !getNodeService().exists(mainDocumentRef)) {
                        throw new UnableToPerformException("compoundWorkflow_main_document_missing");
                    }
                    existingBdoc = getDocumentService().checkExistingBdoc(mainDocumentRef, compoundWorkflowRef) != null;
                    signSeparately = false;
                    addSigningDocument(mainDocumentRef);
                    Map<NodeRef, List<File>> tmpFileMap = new HashMap<NodeRef, List<File>>();
                    List<File> tmpFiles = new ArrayList<File>();
                    tmpFileMap.put(mainDocumentRef, tmpFiles);
                    for (Map.Entry<NodeRef, List<File>> entry : signingFilesMap.entrySet()) {
                        List<File> documentFiles = entry.getValue();
                        addDocumentStatus(entry.getKey());
                        if (documentFiles != null) {
                            tmpFiles.addAll(documentFiles);
                        }
                    }
                    signingFilesMap = tmpFileMap;

                } else {
                    getSigningQueue().addAll(signingDocumentRefs);
                    addDocumentStatuses(signingQueue);
                }
            }
            signingFiles = signingFilesMap;

            getDocumentService().prepareDocumentSigning(signSeparately ? signingQueue : signingDocumentRefs, !existingBdoc, signSeparately);
            long step2 = System.currentTimeMillis();
            doAfterPrepareSigning();
            long step3 = System.currentTimeMillis();
            if (LOG.isInfoEnabled()) {
                LOG.info("prepareDocumentSigning took total time " + (step3 - step0) + " ms\n    load file list - " + (step1 - step0)
                        + " ms\n    service call - " + (step2 - step1) + " ms\n    reload file list - " + (step3 - step2) + " ms");
            }

        } catch (UnableToPerformException e) {
            handleUnableToPerformException(e);
            return false;
        } catch (NodeLockedException e) {
            handleNodeLockedException(e);
            return false;
        }
        signingFlowView = SigningFlowView.GET_PHONE_NUMBER;
        return true;
    }

    public void resolveUserPhoneNr(HttpSession session) {
        String phoneNumber = null;
        String signInPhoneNumber = (String) session.getAttribute(Cas20ProxyReceivingRedirectingTicketValidationFilter.PHONE_NUMBER);
        String lastUsedPhoneNumber = (String) session.getAttribute(LAST_USED_MOBILE_ID_NUMBER);
        if (StringUtils.isNotBlank(signInPhoneNumber)) {
            phoneNumber = signInPhoneNumber;
        } else if (StringUtils.isNotBlank(lastUsedPhoneNumber)) {
            phoneNumber = lastUsedPhoneNumber;
        } else {
            phoneNumber = BeanHelper.getUserService().getDefaultTelephoneForSigning(AuthenticationUtil.getFullyAuthenticatedUser());
            setDefaultTelephoneForSigning(StringUtils.isNotBlank(phoneNumber));
        }
        setPhoneNumber(StringUtils.isNotBlank(phoneNumber) ? phoneNumber : EE_COUNTRY_CODE);
    }

    public boolean startMobileIdSigning() {
        try {
            // Strip all whitespace
            phoneNumber = StringUtils.stripToEmpty(phoneNumber);
            if (phoneNumber.startsWith("372")) {
                phoneNumber = "+" + phoneNumber;
            }
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = EE_COUNTRY_CODE + phoneNumber;
            }
            if (defaultTelephoneForSigning) {
                BeanHelper.getUserService().setCurrentUserProperty(ContentModel.DEFAULT_TELEPHONE_FOR_SIGNING, phoneNumber);
            }
            phoneNumber = phoneNumber.replaceAll("\\s+", "");
            long step0 = System.currentTimeMillis();
            if (!collectAndCheckSigningFiles()) {
                return false;
            }
            SignatureChallenge signatureChallenge = getDocumentService().prepareDocumentChallenge(getSigningDocument(0), phoneNumber,
                    AuthenticationUtil.getFullyAuthenticatedUser(), getMainDocumentRef() != null ? compoundWorkflowRef : null);
            long step1 = System.currentTimeMillis();
            setSignatureChallenge(signatureChallenge);
            signature = null;
            signatureError = null;
            signingFlowView = SigningFlowView.POLL_SIGNATURE;
            if (LOG.isInfoEnabled()) {
                LOG.info("startMobileIdSigning took total time " + (step1 - step0) + " ms\n    service call - " + (step1 - step0) + " ms");
            }
        } catch (UnableToPerformException e) {
            handleUnableToPerformException(e);
            return false;
        } catch (SignatureException e) {
            handleSignatureException(e);
            return false;
        }
        return true;
    }

    public String getMobileIdSignature(String requestParamChallengeId) {
        signature = null;
        try {
            if (!checkSignatureData(requestParamChallengeId)) {
                return handleInvalidSigantureState();
            }
            signature = getSignatureService().getMobileIdSignature(getSignatureChallenge());
            if (signature == null) {
                return "REPEAT";
            }
            return "FINISH";
        } catch (UnableToPerformException e) {
            signatureError = e;
            return "FINISH";
        }
    }

    public boolean finishMobileIdSigning() {
        if (signatureError == null) {
            boolean signingFinished = signDocumentImpl(signature);
            if (!isSigningQueueEmpty()) {
                signingFlowView = SigningFlowView.GET_PHONE_NUMBER;
            }
            return signingFinished;
        }
        handleSignatureError();
        return false;
    }

    public boolean signDocumentImpl(String signatureHex) {
        try {
            long step0 = System.currentTimeMillis();
            if (!collectAndCheckSigningFiles()) {
                return false;
            }
            boolean finishTask = isFinishTaskStep();
            SignatureTask signatureTaskToFinish = signatureTask;
            if (signatureTaskToFinish.getParent() == null) {
                signatureTaskToFinish = (SignatureTask) BeanHelper.getWorkflowService().getTaskWithParents(signatureTask.getNodeRef());
                if (!signatureTaskToFinish.isStatus(Status.IN_PROGRESS)) {
                    handleInvalidSigantureState();
                    return false;
                }
                signatureTaskToFinish.setComment(signatureTask.getComment());
                signatureTaskToFinish.setSignatureChallenge(signatureTask.getSignatureChallenge());
                signatureTaskToFinish.setSignatureDigest(signatureTask.getSignatureDigest());
            }
            getDocumentService().finishDocumentSigning(signatureTaskToFinish, signatureHex, getSigningDocument(0), mainDocumentRef == null, finishTask, originalStatuses);
            signingQueue.remove(0);
            long step1 = System.currentTimeMillis();
            long step2 = System.currentTimeMillis();
            if (LOG.isInfoEnabled()) {
                LOG.info("finishDocumentSigning took total time " + (step2 - step0) + " ms\n    service call - " + (step1 - step0) + " ms\n    reload document - "
                        + (step2 - step1) + " ms");
            }
            if (finishTask) {
                handleTaskFinishedSuccess();
            }
            return true;
        } catch (UnableToPerformException e) {
            handleUnableToPerformException(e);
        } catch (WorkflowChangedException e) {
            handleWorkflowChanged(e);
        } catch (SignatureRuntimeException e) {
            handleSignatureException(e);
        } catch (FileExistsException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to create ddoc, file with same name already exists, parentRef = " + containerRef, e);
            }
            handleFileExistsException();
        }
        // this code must be reached ONLY in case of error
        return false;
    }

    private boolean checkSignatureData(String requestParamChallengeId) {
        if (!hasSignatureChallenge()) {
            return false;
        }
        if (StringUtils.isBlank(requestParamChallengeId) || !requestParamChallengeId.equals(getSignatureChallenge().getChallengeId())) {
            return false;
        }
        return true;
    }

    protected void handleFileExistsException() {
        Utils.addErrorMessage(MessageUtil.getMessage("ddoc_file_exists"));
    }

    protected void handleWorkflowChanged(WorkflowChangedException e) {
        handleWorkflowChangedException(e, "Finishing signature task failed", "workflow_task_save_failed", LOG);
    }

    protected void handleTaskFinishedSuccess() {
        MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
    }

    protected void handleSignatureError() {
        MessageUtil.addStatusMessage(signatureError);
    }

    public static String handleInvalidSigantureState() {
        return "ERROR" + MessageUtil.getMessage("task_finish_error_signature_data_changed");
    }

    protected void handleSignatureException(Exception e) {
        SignatureBlockBean.addSignatureError(e);
    }

    protected void handleNodeLockedException(NodeLockedException e) {
        e.setCustomMessageId(null);
        BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_registerDoc_error_docLocked", e);
    }

    protected void handleUnableToPerformException(MessageData e) {
        MessageUtil.addStatusMessage(e);
    }

    protected void handleZeroByteFileError() {
        MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "task_files_zero_byte_file");
    }

    protected void handleFileRequiredError() {
        MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "task_files_required");
    }

    // FIXME: this method content should be in WorkflowBlockBean, but for logging reasons it is easier to leave it here
    protected void doAfterPrepareSigning() {
        if (compoundWorkflowRef == null) {
            BeanHelper.getFileBlockBean().restore();
        } else {
            BeanHelper.getCompoundWorkflowAssocListDialog().restored();
        }
    }

    protected boolean isSignTogether() {
        return signTogether;
    }

    private void addDocumentStatuses(List<NodeRef> docRefs) {
        if (docRefs != null) {
            for (NodeRef docRef : docRefs) {
                addDocumentStatus(docRef);
            }
        }
    }

    public boolean needsSignatureInput(NodeRef nodeRef) {
        List<File> files = signingFiles.get(nodeRef);
        return files != null && !files.isEmpty();
    }

    private boolean hasZeroByteFile(List<File> files) {
        for (File file : files) {
            if (file.getSize() == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean collectAndCheckSigningFiles() {
        List<File> activeFiles = new ArrayList<File>();
        collectSigningFiles(activeFiles, new HashMap<NodeRef, List<File>>());
        return checkSigningFiles(activeFiles, true);
    }

    private void collectSigningFiles(List<File> activeFiles, Map<NodeRef, List<File>> signingFiles) {
        if (compoundWorkflowRef == null) {
            List<File> documentFiles = getFileService().getAllActiveFiles(containerRef);
            if (documentFiles != null) {
                activeFiles.addAll(documentFiles);
            }
            signingFiles.put(containerRef, activeFiles);
        } else {
            signingFiles.putAll(getWorkflowService().getCompoundWorkflowSigningFiles(compoundWorkflowRef));
            for (Map.Entry<NodeRef, List<File>> entry : signingFiles.entrySet()) {
                List<File> documentFiles = entry.getValue();
                if (documentFiles != null) {
                    activeFiles.addAll(documentFiles);
                }
            }
        }
    }

    private boolean checkSigningFiles(List<File> activeFiles, boolean checkReference) {
        if (activeFiles == null || activeFiles.isEmpty()) {
            handleFileRequiredError();
            return false;
        }
        if (checkReference) {
            boolean hasNotReferenceFile = false;
            for (File file : activeFiles) {
                if (file.getGeneratedFileRef() == null) {
                    hasNotReferenceFile = true;
                    break;
                }
            }
            if (!hasNotReferenceFile) {
                handleFileRequiredError();
                return false;
            }
        }
        if (hasZeroByteFile(activeFiles)) {
            handleZeroByteFileError();
            return false;
        }
        return true;
    }

    private void addDocumentStatus(NodeRef docRef) {
        originalStatuses.put(docRef, (String) getNodeService().getProperty(docRef, DocumentCommonModel.Props.DOC_STATUS));
    }

    public List<NodeRef> getSigningQueue() {
        return signingQueue;
    }

    public void setSigningQueue(List<NodeRef> signingQueue) {
        this.signingQueue = signingQueue;
    }

    public boolean isSigningQueueEmpty() {
        return signingQueue == null || signingQueue.isEmpty();
    }

    public NodeRef getSigningDocument(int i) {
        return signingQueue.get(i);
    }

    public void addSigningDocument(NodeRef nodeRef) {
        signingQueue.add(nodeRef);
    }

    public SignatureTask getSignatureTask() {
        return signatureTask;
    }

    public void setSignatureDigest(SignatureDigest signatureDigest) {
        signatureTask.setSignatureDigest(signatureDigest);
    }

    public void setSignatureChallenge(SignatureChallenge signatureChallenge) {
        signatureTask.setSignatureChallenge(signatureChallenge);
    }

    public SignatureChallenge getSignatureChallenge() {
        return signatureTask.getSignatureChallenge();
    }

    public boolean hasSignatureChallenge() {
        return signatureTask != null && signatureTask.getSignatureChallenge() != null;
    }

    public Object getMainDocumentRef() {
        return mainDocumentRef;
    }

    public Map<NodeRef, String> getOriginalStatuses() {
        return originalStatuses;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public SigningFlowView getSigningFlowView() {
        return signingFlowView;
    }

    public String getChallengeId() {
        return signatureTask != null && signatureTask.getSignatureChallenge() != null ? signatureTask.getSignatureChallenge().getChallengeId() : null;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isFinishTaskStep() {
        return signingQueue != null && signingQueue.size() == 1;
    }

    public boolean isDefaultTelephoneForSigning() {
        return defaultTelephoneForSigning;
    }

    public void setDefaultTelephoneForSigning(boolean defaultTelephoneForSigning) {
        this.defaultTelephoneForSigning = defaultTelephoneForSigning;
    }

}
