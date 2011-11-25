package ee.webmedia.alfresco.webdav;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class UnlockMethod extends org.alfresco.repo.webdav.UnlockMethod {

    @Override
    protected void executeImpl() throws WebDAVServerException {
        if (logger.isDebugEnabled()) {
            logger.debug("Lock node; path=" + getPath() + ", token=" + getLockToken());
        }

        FileInfo lockNodeInfo = null;
        try {
            lockNodeInfo = getDAVHelper().getNodeForPath(getRootNodeRef(), getPath(), getServletPath());
        } catch (FileNotFoundException e) {
            throw new WebDAVServerException(HttpServletResponse.SC_NOT_FOUND);
        }

        // Parse the lock token
        String[] lockInfo = WebDAV.parseLockToken(getLockToken());
        if (lockInfo == null) {
            // Bad lock token
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        Map<QName, Serializable> originalProps = getNodeService().getProperties(lockNodeInfo.getNodeRef());

        // Get the lock status for the node
        LockService lockService = getDAVHelper().getLockService();
        // String nodeId = lockInfo[0];
        // String userName = lockInfo[1];

        LockStatus lockSts = lockService.getLockStatus(lockNodeInfo.getNodeRef());
        if (lockSts == LockStatus.LOCK_OWNER) {
            // Unlock the node
            lockService.unlock(lockNodeInfo.getNodeRef());

            // Unlock the document also if we are dealing with generated file.
            if (BeanHelper.getFileService().isFileGenerated(lockNodeInfo.getNodeRef())) {
                NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(lockNodeInfo.getNodeRef(), DocumentCommonModel.Types.DOCUMENT);
                if (docRef != null) {
                    lockService.unlock(docRef);
                }
            }

            // Indicate that the unlock was successful
            m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            // DEBUG
            if (logger.isDebugEnabled()) {
                logger.debug("Unlock token=" + getLockToken() + " Successful");
            }

            ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().setVersionLockableAspect(lockNodeInfo.getNodeRef(), false);

            // Set modifier and modified properties to original, so that locking doesn't appear to change the file
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(ContentModel.PROP_MODIFIER, originalProps.get(ContentModel.PROP_MODIFIER));
            props.put(ContentModel.PROP_MODIFIED, originalProps.get(ContentModel.PROP_MODIFIED));
            getBehaviourFilter().disableBehaviour(ContentModel.ASPECT_AUDITABLE);
            getNodeService().addProperties(lockNodeInfo.getNodeRef(), props);
        } else if (lockSts == LockStatus.NO_LOCK) {
            // DEBUG
            if (logger.isDebugEnabled()) {
                logger.debug("Unlock token=" + getLockToken() + " Not locked");
            }

            // Node is not locked
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        } else if (lockSts == LockStatus.LOCKED) {
            // DEBUG
            if (logger.isDebugEnabled()) {
                logger.debug("Unlock token=" + getLockToken() + " Not lock owner");
            }

            // Node is locked but not by this user
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        } else if (lockSts == LockStatus.LOCK_EXPIRED) {
            // DEBUG
            if (logger.isDebugEnabled()) {
                logger.debug("Unlock token=" + getLockToken() + " Lock expired");
            }

            // Return a success status
            m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    protected BehaviourFilter getBehaviourFilter() {
        BehaviourFilter behaviourFilter = (BehaviourFilter) getServiceRegistry().getService(QName.createQName("", "policyBehaviourFilter"));
        return behaviourFilter;
    }

}
