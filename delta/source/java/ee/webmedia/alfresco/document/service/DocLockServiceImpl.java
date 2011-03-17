package ee.webmedia.alfresco.document.service;

import java.util.Date;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.lock.LockServiceImpl;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.NodeRef;

public class DocLockServiceImpl extends LockServiceImpl implements DocLockService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocLockServiceImpl.class);
    /** timeOut in seconds how long lock is kept after creation(refreshing) before expiring */
    private int lockTimeout = 180;

    /**
     * Gets the lock statuc for a node and a user name. <br>
     * <br>
     * <b>
     * NB! Unlike implementation in superclass, this class returns LOCKED(not LOCK_OWNER) <br>
     * if given nodeRef is locked by someone else, but nodeRef itself is owned by given user
     * </b>
     * 
     * @param nodeRef the node reference
     * @param userName the user name
     * @return the lock status
     */
    @Override
    public LockStatus getLockStatus(NodeRef nodeRef, String userName) {
        LockStatus result = LockStatus.NO_LOCK;
        if (this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_LOCKABLE)) {
            // Get the current lock owner
            String currentLockOwner = (String) this.nodeService.getProperty(nodeRef, ContentModel.PROP_LOCK_OWNER);
            String nodeOwner = ownableService.getOwner(nodeRef);
            if (currentLockOwner != null) {
                Date expiryDate = (Date) this.nodeService.getProperty(nodeRef, ContentModel.PROP_EXPIRY_DATE);
                if (expiryDate != null && expiryDate.before(new Date())) {
                    // Indicate that the lock has expired
                    result = LockStatus.LOCK_EXPIRED;
                } else {
                    if (currentLockOwner.equals(userName)) {
                        result = LockStatus.LOCK_OWNER;
                    } else if ((nodeOwner != null) && nodeOwner.equals(userName)) {
                        // this else-if block could be omitted, but is left here to point to the only difference with parent method
                        result = LockStatus.LOCKED; // superclass returns different value from here: LockStatus.LOCK_OWNER
                    } else {
                        result = LockStatus.LOCKED;
                    }
                }
            }
        }
        return result;
    }
    
    @Override
    public void unlockIfOwner(NodeRef nodeRef) {
        if (nodeService.exists(nodeRef)) {
            String msg = "after unlocking";
            if (isLockByOther(nodeRef)) {
                msg = "Unable to unlock - Not lock owner";
            } else {
                unlock(nodeRef);
            }
            if (log.isDebugEnabled()) {
                debugLock(nodeRef, msg);
            }
        }
    }

    @Override
    public LockStatus setLockIfFree(NodeRef lockNode) {
        // Check the lock status of the node
        LockStatus lockSts = debugLock(lockNode, "before creating/refreshing lock");
        if (lockSts == LockStatus.LOCKED) {// lock owned by other user
            log.warn("nodeRef is locked by some other user");
        } else { // could be locked: LockStatus: LOCK_OWNER | NO_LOCK | LOCK_EXPIRED
            if (lockSts == LockStatus.LOCK_OWNER) {
                log.debug("Current user already has the lock for nodeRef, refreshing lock");
            }
            super.lock(lockNode, LockType.WRITE_LOCK, getLockTimeout());
            lockSts = debugLock(lockNode, "after locking/extending lock");
            if (lockSts != LockStatus.LOCK_OWNER) {
                throw new RuntimeException("Failed to get lock");
            }
        }
        return lockSts;
    }

    private LockStatus debugLock(NodeRef lockNode, String msgPrefix) {
        LockStatus lockSts = getLockStatus(lockNode);
        if (log.isDebugEnabled()) {
            String msg = msgPrefix + ": existing lock: status=" + lockSts;
            if (lockSts != LockStatus.NO_LOCK) {
                String lockOwnerUserName = (String) nodeService.getProperty(lockNode, ContentModel.PROP_LOCK_OWNER);
                Date locExpireDate = (Date) this.nodeService.getProperty(lockNode, ContentModel.PROP_EXPIRY_DATE);
                msg += "; owner '" + lockOwnerUserName + "'";
                msg += "; lockType=" + super.getLockType(lockNode);
                msg += "; expires=" + locExpireDate;
            }
            log.debug(msg);
        }
        return lockSts;
    }

    private boolean isLockByOther(NodeRef nodeRef) {
        return isLockByOther(nodeRef, getUserName());
    }

    private boolean isLockByOther(NodeRef nodeRef, String userName) {
        boolean isLockOwnedByOther = true;
        if (this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_LOCKABLE)) {
            String currentLockOwner = (String) this.nodeService.getProperty(nodeRef, ContentModel.PROP_LOCK_OWNER);
            if (currentLockOwner != null) {
                Date expiryDate = (Date) this.nodeService.getProperty(nodeRef, ContentModel.PROP_EXPIRY_DATE);
                if (expiryDate != null && expiryDate.before(new Date())) {
                    isLockOwnedByOther = false; // LockStatus.LOCK_EXPIRED;
                    log.debug("existing lock has expired");
                } else {
                    if (currentLockOwner.equals(userName)) {
                        log.debug("user '" + userName + "' owns the lock");
                        isLockOwnedByOther = false; // LockStatus.LOCK_OWNER;
                    } else {
                        log.debug("user '" + userName + "' doesn't own the lock - lock owned by '" + currentLockOwner + "'");
                        isLockOwnedByOther = true; // LockStatus.LOCKED;
                    }
                }
            }
        } else {
            isLockOwnedByOther = false;
        }
        return isLockOwnedByOther;
    }

    // START: getters / setters
    @Override
    public int getLockTimeout() {
        return lockTimeout;
    }
    // END: getters / setters

}
