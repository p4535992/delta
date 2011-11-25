package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.sendout.web.DocumentSendOutDialog;
import ee.webmedia.alfresco.document.service.DocLockService;
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
        final Node docNode = getDocumentDialogHelperBean().getNode();
        if (docNode == null) {
            return false;
        }
        final DocLockService lockService = getDocLockService();
        final NodeRef docRef = docNode.getNodeRef();
        synchronized (docNode) { // to avoid extending lock after unlock(save/cancel)
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
        final Node docNode = getDocumentDialogHelperBean().getNode();
        if (docNode != null && (sendOut = BeanHelper.getDocumentSendOutDialog()) != null && sendOut.getModel() != null) {
            inEditMode |= docNode.getNodeRef().equals(sendOut.getModel().getNodeRef());
        }
        return inEditMode;
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
                    lockSuccessfullyRefreshed = lockOrUnlockIfNeeded(lockingAllowed);
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

    public void handleLockedNode(String messageId, NodeRef nodeRef) {
        MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), messageId,
                BeanHelper.getUserService().getUserFullName((String) BeanHelper.getNodeService().getProperty(nodeRef, ContentModel.PROP_LOCK_OWNER)));
    }
}
