package ee.webmedia.alfresco.document.lock.service;

import java.util.List;

import org.alfresco.repo.webdav.LockInfo;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.lock.model.Lock;

public interface DocLockService extends LockService {
    String BEAN_NAME = "DocLockService";

    /**
     * Create a new lock
     * 
     * @param lockNode NodeRef
     * @return true false if lock can't be obtained, true otherwise
     */
    LockStatus setLockIfFree(NodeRef lockNode);

    /**
     * @param nodeRef
     */
    void unlockIfOwner(NodeRef nodeRef);

    int getLockTimeout();

    String getLockOwnerIfLockedByOther(NodeRef nodeRef);

    String getLockOwnerIfLocked(NodeRef nodeRef);

    void lockFile(NodeRef lockedFileNodeRef);

    void lockFile(NodeRef fileNodeRef, LockInfo lockInfo);

    void unlockFile(NodeRef lockedFileNodeRef);

    void unlockFiles(List<Pair<NodeRef, Boolean>> lockedFileRefsWithGeneratedProp, NodeRef parentRef);

    boolean isGeneratedFileDocumentLocked(NodeRef fileRef);

    List<Lock> getDocumentAndFileLocks();

    boolean isLockByOther(NodeRef nodeRef);
}
