package ee.webmedia.alfresco.document.lock.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.lock.LockServiceImpl;
import org.alfresco.repo.lock.mem.LockState;
import org.alfresco.repo.lock.mem.LockStore;
import org.alfresco.repo.webdav.LockInfo;
import org.alfresco.repo.webdav.LockInfoImpl;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.lock.model.Lock;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.user.service.UserService;

public class DocLockServiceImpl extends LockServiceImpl implements DocLockService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocLockServiceImpl.class);
    /** timeOut in seconds how long lock is kept after creation(refreshing) before expiring */
    private final int lockTimeout = 180;

    private FileService _fileService;
    private DocumentSearchService _documentSearchService;
    private UserService userService;
    private DocumentDynamicService _documentDynamicService;

    @Override
    public List<Lock> getDocumentAndFileLocks() {
        ArrayList<Lock> locks = new ArrayList<>();
        Date now = new Date();

        // Persistent locks
        fetchLockDetails(locks, getDocumentSearchService().searchActiveLocks(), now);
        // Ephemeral locks
        fetchLockDetails(locks, lockStore.getNodes(), now);

        return locks;
    }

    private void fetchLockDetails(List<Lock> locks, Collection<NodeRef> lockedNodeRefs, Date now) {
        for (NodeRef nodeRef : lockedNodeRefs) {
            Lock lock = getLock(nodeRef, now);
            if (lock != null) {
                locks.add(lock);
            }
        }
    }

    private Lock getLock(NodeRef nodeRef, Date expiry) {
        Assert.notNull(nodeRef, "NodeRef id mandatory");
        Assert.notNull(expiry, "Expiry date is mandatory");

        // Filter additionally by time as well, since Lucene only supports dates in query
        Date repoExpiry = getLockExpiry(nodeRef);
        String owner = getLockOwner(nodeRef);

        if (repoExpiry == null || owner == null || expiry.after(repoExpiry) || getDocumentDynamicService().isDraft(nodeRef)) {
            return null;
        }

        Lock lock = new Lock(nodeRef);
        NodeRef docNodeRef = null;
        QName type = nodeService.getType(nodeRef);
        if (DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            docNodeRef = nodeRef;
        } else if (ContentModel.TYPE_CONTENT.equals(type)) { // a file
            docNodeRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
            lock.setFileName((String) nodeService.getProperty(nodeRef, FileModel.Props.DISPLAY_NAME));
            lock.setFileUrl(getFileService().generateURL(nodeRef));
        } else {
            return null;
        }

        lock.setDocNodeRef(docNodeRef);
        lock.setDocName((String) nodeService.getProperty(docNodeRef, DocumentCommonModel.Props.DOC_NAME));
        lock.setDocRegDate((Date) nodeService.getProperty(docNodeRef, DocumentCommonModel.Props.REG_DATE_TIME));
        lock.setDocRegNr((String) nodeService.getProperty(docNodeRef, DocumentCommonModel.Props.REG_NUMBER));
        String lockOwner = StringUtils.substringBefore(getLockOwnerIfLocked(nodeRef), "_");
        lock.setLockedBy(userService.getUserFullNameAndId(lockOwner));

        return lock;
    }

    @Override
    public String getLockOwnerIfLockedByOther(NodeRef nodeRef) {
        Assert.notNull(nodeRef, "NodeRef cannot be null!");
        if (isLockByOther(nodeRef)) {
            return StringUtils.substringBefore(getLockOwner(nodeRef), "_");
        }
        return null;
    }

    private String getLockOwner(NodeRef nodeRef) {
        String owner;

        final LockState lockState = lockStore.get(nodeRef);
        if (lockState != null) {
            owner = lockState.getOwner();
        } else {
            owner = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_LOCK_OWNER);
        }

        return owner;
    }

    private Date getLockExpiry(NodeRef nodeRef) {
        Date expiry;

        final LockState lockState = lockStore.get(nodeRef);
        if (lockState != null) {
            expiry = lockState.getExpires();
        } else {
            expiry = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_EXPIRY_DATE);
        }

        return expiry;
    }

    @Override
    public String getLockOwnerIfLocked(NodeRef nodeRef) {
        Assert.notNull(nodeRef, "NodeRef cannot be null!");
        String currentLockOwner = getLockOwner(nodeRef);
        if (StringUtils.isNotBlank(currentLockOwner)) {
            Date expiryDate = getLockExpiry(nodeRef);
            if (expiryDate != null && expiryDate.after(new Date())) {
                return currentLockOwner;
            }
        }

        return null;
    }

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
        LockState ephemeralLockState = lockStore.get(nodeRef);
        if (ephemeralLockState != null || nodeService.hasAspect(nodeRef, ContentModel.ASPECT_LOCKABLE)) {
            // Get the current lock owner
            String repoUsername = getLockOwner(nodeRef);
            String currentLockOwner = repoUsername == null ? null : getUserNameAndSession(repoUsername);
            String nodeOwner = ownableService.getOwner(nodeRef);
            if (currentLockOwner != null) {
                Date expiryDate = getLockExpiry(nodeRef);
                if (expiryDate != null && expiryDate.before(new Date())) {
                    // Indicate that the lock has expired
                    result = LockStatus.LOCK_EXPIRED;
                } else {
                    String userNameAndSession = getUserNameAndSession(userName);
                    if (currentLockOwner.equals(userNameAndSession)) {
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

    @Override
    public void lockFile(NodeRef fileNodeRef) {
        final LockInfoImpl lockInfo = new LockInfoImpl();
        lockInfo.setTimeoutSeconds(WebDAV.TIMEOUT_180_SECONDS);
        lockFile(fileNodeRef, lockInfo);
    }

    @Override
    public void lockFile(NodeRef fileNodeRef, LockInfo lockInfo) {
        BeanHelper.getWebDAVLockService().lock(fileNodeRef, lockInfo);

        // If the file is generated, lock the document also
        if (!getFileService().isFileGenerated(fileNodeRef)) {
            return;
        }

        NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(fileNodeRef, DocumentCommonModel.Types.DOCUMENT);
        if (docRef != null) {
            if (getLockStatus(docRef) != LockStatus.LOCKED) {
                BeanHelper.getWebDAVLockService().lock(docRef, lockInfo);
            }
            nodeService.setProperty(docRef, FileModel.Props.LOCKED_FILE_NODEREF, fileNodeRef);
        }
    }

    @Override
    public void unlockFile(NodeRef lockedFileNodeRef) {
        unlockIfOwner(lockedFileNodeRef);

        // If the file is generated, unlock the document also
        if (!getFileService().isFileGenerated(lockedFileNodeRef)) {
            return;
        }

        NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(lockedFileNodeRef, DocumentCommonModel.Types.DOCUMENT);
        if (docRef != null) {
            unlock(docRef);
        }
    }

    @Override
    public void unlockFiles(List<Pair<NodeRef, Boolean>> lockedFileRefsWithGeneratedProp, NodeRef parentRef) {
        boolean generatedFile = false;
        for (Pair<NodeRef, Boolean> fileRefAndGeneratedProp : lockedFileRefsWithGeneratedProp) {
            unlockIfOwner(fileRefAndGeneratedProp.getFirst());
            generatedFile |= fileRefAndGeneratedProp.getSecond();

        }
        if (!generatedFile) {
            return;
        }

        // If the file is generated, unlock the document also
        if (parentRef != null) {
            unlock(parentRef);
        }
    }

    @Override
    public boolean isGeneratedFileDocumentLocked(NodeRef fileRef) {
        if (!getFileService().isFileGenerated(fileRef)) {
            return false;
        }

        NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(fileRef, DocumentCommonModel.Types.DOCUMENT);
        if (docRef != null) {
            return (getLockStatus(docRef) == LockStatus.LOCKED);
        }

        return false;
    }

    private LockStatus debugLock(NodeRef lockNode, String msgPrefix) {
        LockStatus lockSts = getLockStatus(lockNode);
        if (log.isDebugEnabled()) {
            String msg = msgPrefix + ": existing lock: status=" + lockSts;
            if (lockSts != LockStatus.NO_LOCK) {
                String lockOwnerUserName = getLockOwner(lockNode);
                Date locExpireDate = getLockExpiry(lockNode);
                msg += "; owner '" + lockOwnerUserName + "'";
                msg += "; lockType=" + super.getLockType(lockNode);
                msg += "; expires=" + locExpireDate;
            }
            log.debug(msg);
        }
        return lockSts;
    }

    @Override
    public boolean isLockByOther(NodeRef nodeRef) {
        return isLockByOther(nodeRef, getUserName(), null);
    }

    private boolean isLockByOther(NodeRef nodeRef, String userName, String lockOwner) {
        boolean isLockOwnedByOther = true;
        String currentLockOwner = getLockOwner(nodeRef);
        if (StringUtils.isNotBlank(currentLockOwner)) {
            Date expiryDate = getLockExpiry(nodeRef);
            if (expiryDate != null && expiryDate.before(new Date())) {
                isLockOwnedByOther = false; // LockStatus.LOCK_EXPIRED;
                log.debug("existing lock has expired");
            } else {
                userName = getUserNameAndSession(userName);
                if (currentLockOwner.equals(userName)) {
                    log.debug("user '" + userName + "' owns the lock");
                    isLockOwnedByOther = false; // LockStatus.LOCK_OWNER;
                } else {
                    log.debug("user '" + userName + "' doesn't own the lock - lock owned by '" + currentLockOwner + "'");
                    isLockOwnedByOther = true; // LockStatus.LOCKED;
                }
            }
        } else {
            isLockOwnedByOther = false;
        }
        return isLockOwnedByOther;
    }

    @Override
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

    // START: getters / setters
    @Override
    public int getLockTimeout() {
        return lockTimeout;
    }

    public DocumentSearchService getDocumentSearchService() {
        if (_documentSearchService == null) {
            _documentSearchService = BeanHelper.getDocumentSearchService();
        }
        return _documentSearchService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public DocumentDynamicService getDocumentDynamicService() {
        if (_documentDynamicService == null) {
            _documentDynamicService = BeanHelper.getDocumentDynamicService();
        }
        return _documentDynamicService;
    }

    private FileService getFileService() {
        if (_fileService == null) {
            _fileService = BeanHelper.getFileService();
        }
        return _fileService;
    }

    @Override
    public void setLockStore(LockStore lockStore) {
        this.lockStore = lockStore;
    }

    // END: getters / setters

}
