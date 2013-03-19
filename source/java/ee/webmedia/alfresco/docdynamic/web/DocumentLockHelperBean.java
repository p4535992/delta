package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.lock.service.DocLockService;
import ee.webmedia.alfresco.document.sendout.web.DocumentSendOutDialog;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * Helper for locking documents
 * 
 * @author Kaarel Jõgeva
 */
public class DocumentLockHelperBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentLockHelperBean.class);

    public static final String BEAN_NAME = "DocumentLockHelperBean";

    /** timeOut in seconds how often lock should be refreshed to avoid expiring */
    private Integer lockRefreshTimeout;

    public boolean isLockable(NodeRef docRef) {
        try {
            getDocLockService().checkForLock(docRef);
            return true;
        } catch (NodeLockedException e) {
            handleLockedNode("document_validation_alreadyLocked", docRef);
        }
        return false;
    }

    /**
     * @return how often (in seconds) clients should call {@link #refreshLockClientHandler()} to refresh lock
     */
    public int getLockExpiryPeriod() {
        if (lockRefreshTimeout == null) {
            lockRefreshTimeout = getDocLockService().getLockTimeout();
        }
        return lockRefreshTimeout;
    }

    /**
     * @param mustLock4Edit
     * @return true if current user holds the lock after execution of this function
     * @throws UnableToPerformException when node is already locked by another user
     */
    public boolean lockOrUnlockIfNeeded(boolean mustLock4Edit) throws NodeLockedException {
        // XXX ALAR: It would be correct to lock all nodes in document/caseFile dialog stack which are opened in edit mode.
        // But currently we only lock the top most node.
        final Node docNode = getDocumentDialogHelperBean().getNode();
        if (docNode == null) {
            return false;
        }
        final DocLockService lockService = getDocLockService();
        final NodeRef docRef = docNode.getNodeRef();
        synchronized (docNode) { // to avoid extending lock after unlock(save/cancel)
            if (!getNodeService().exists(docRef)) {
                throw new UnableToPerformException("document_delete_success"); // XXX: Alar
            }
            if (mustLock4Edit) {
                if (lockService.setLockIfFree(docRef) == LockStatus.LOCK_OWNER) {
                    return true;
                }
                LOG.debug("Lock can't be created: document_validation_alreadyLocked");

                // Node cannot be locked
                throw new NodeLockedException(docRef);
            }
            lockService.unlockIfOwner(docRef);
        }
        return false;
    }

    /**
     * Returns true if required conditions are met for locking.
     * a) document is in edit mode
     * OR
     * b) current document is opened in send out dialog
     * 
     * @return true if we can lock, false otherwise.
     */
    public boolean isLockingAllowed() {
        return isLockingAllowed(getDocumentDialogHelperBean().isInEditMode());
    }

    public boolean isLockingAllowed(boolean inEditMode) {
        DocumentSendOutDialog sendOut = null;
        DocumentDialogHelperBean documentDialogHelperBean = getDocumentDialogHelperBean();
        final Node docNode = documentDialogHelperBean.getNode();
        if (docNode != null && (sendOut = BeanHelper.getDocumentSendOutDialog()) != null && sendOut.getModel() != null) {
            inEditMode |= docNode.getNodeRef().equals(sendOut.getModel().getNodeRef());
        }
        return inEditMode;
    }

    public void checkAssocDocumentLocks(Node dynamicDocumentNode, String customMessage) {
        DocLockService docLockService = BeanHelper.getDocLockService();
        for (NodeRef assocNodeRef : BeanHelper.getDocumentDynamicService().getAssociatedDocRefs(dynamicDocumentNode)) {
            if (docLockService.getLockStatus(assocNodeRef) == LockStatus.LOCKED) {
                NodeLockedException nodeLockedException = new NodeLockedException(assocNodeRef);
                if (customMessage != null) {
                    nodeLockedException.setCustomMessageId(customMessage);
                }
                throw nodeLockedException;
            }
        }
    }

    /**
     * AJAX: Extend lock on document (or create one)
     */
    public void refreshLockClientHandler() throws IOException {
        boolean lockSuccessfullyRefreshed = false;
        String errMsg = null;
        final Node node = getDocumentDialogHelperBean().getNode();
        if (node == null) {
            errMsg = "Form is reset";
        } else {
            synchronized (node) { // to avoid extending lock after unlock(save/cancel)
                boolean lockingAllowed = isLockingAllowed(getDocumentDialogHelperBean().isInEditMode());
                if (lockingAllowed) {
                    try {
                        lockSuccessfullyRefreshed = lockOrUnlockIfNeeded(lockingAllowed);
                    } catch (UnableToPerformException e) {
                        // lockSuccessfullyRefreshed stays false
                        errMsg = MessageUtil.getMessage(e);
                    } catch (NodeLockedException e) {
                        // lockSuccessfullyRefreshed stays false
                        errMsg = "Dokument on lukustatud kellegi teise poolt";
                    }
                } else {
                    errMsg = "Can't refresh lock - page not in editMode";
                    LOG.warn(errMsg);
                }
            }
        }
        FacesContext context = FacesContext.getCurrentInstance();
        ResponseWriter out = context.getResponseWriter();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>");
        xml.append("<refresh-lock success=\"" + lockSuccessfullyRefreshed + "\"");
        xml.append(" nextReqInMs=\"" + getLockExpiryPeriod() * 1000 + "\"");
        if (errMsg != null) {
            xml.append(" errMsg=\"" + errMsg + "\"");
        }
        xml.append(" />");
        out.write(xml.toString());
        LOG.debug("returning XML: " + xml.toString());
    }

    public void handleLockedNode(String messageId) {
        handleLockedNode(messageId, getDocumentDialogHelperBean().getNodeRef());
    }

    public void handleLockedNode(String messageId, NodeLockedException e) {
        if (StringUtils.isNotBlank(e.getCustomMessageId())) {
            messageId = e.getCustomMessageId();
        }
        handleLockedNode(messageId, e.getNodeRef());
    }

    public void handleLockedNode(String messageId, NodeRef nodeRef) {
        handleLockedNode(messageId, nodeRef, new Object[0]);
    }

    public void handleLockedNode(String messageId, NodeRef nodeRef, Object... valueHolders) {
        Pair<String, Object[]> messageKeyAndValueHolders = getErrorMessageKeyAndValueHolders(messageId, nodeRef, valueHolders);
        MessageUtil.addErrorMessage(messageKeyAndValueHolders.getFirst(), messageKeyAndValueHolders.getSecond());
    }

    public Pair<String, Object[]> getErrorMessageKeyAndValueHolders(String messageId, NodeRef nodeRef, Object... valueHolders) {
        NodeRef lockedFile = (NodeRef) getNodeService().getProperty(nodeRef, FileModel.Props.LOCKED_FILE_NODEREF);
        if (lockedFile != null && BeanHelper.getFileService().isFileGenerated(lockedFile) || ContentModel.TYPE_CONTENT.equals(getNodeService().getType(nodeRef))) {
            messageId += "_file";
            valueHolders = ArrayUtils.add(valueHolders, getNodeService().getProperty(lockedFile != null ? lockedFile : nodeRef, FileModel.Props.DISPLAY_NAME));
        }

        valueHolders = ArrayUtils.add(valueHolders,
                BeanHelper.getUserService().getUserFullName(
                        StringUtils.substringBefore((String) BeanHelper.getNodeService().getProperty(nodeRef, ContentModel.PROP_LOCK_OWNER), "_")));
        Pair<String, Object[]> messageKeyAndValueHolders = Pair.newInstance(messageId, valueHolders);
        return messageKeyAndValueHolders;
    }

    public boolean isLockReleased(NodeRef nodeRef) {
        LockStatus lockStatus = getDocLockService().getLockStatus(nodeRef, AuthenticationUtil.getRunAsUser());
        return !LockStatus.LOCK_OWNER.equals(lockStatus);
    }
}
