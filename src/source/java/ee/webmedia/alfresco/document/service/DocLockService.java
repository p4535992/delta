package ee.webmedia.alfresco.document.service;

import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeRef;

public interface DocLockService extends LockService {
    String BEAN_NAME = "DocLockService";

    /**
     * Create a new lock
     * 
     * @param lockNode NodeRef
     * @return true false if lock can't be obtained, true otherwise
     */
    LockStatus createLockIfFree(NodeRef lockNode);

    /**
     * @param nodeRef
     */
    void unlockIfOwner(NodeRef nodeRef);

    int getLockTimeout();

}
