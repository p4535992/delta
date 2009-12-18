package ee.webmedia.alfresco.webdav;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.versions.model.VersionsModel;

public class UnlockMethod extends org.alfresco.repo.webdav.UnlockMethod {
    
    @Override
    protected void executeImpl() throws WebDAVServerException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Lock node; path=" + getPath() + ", token=" + getLockToken());
        }

        FileInfo lockNodeInfo = null;
        try
        {
            lockNodeInfo = getDAVHelper().getNodeForPath(getRootNodeRef(), getPath(), getServletPath());
        }
        catch (FileNotFoundException e)
        {
            throw new WebDAVServerException(HttpServletResponse.SC_NOT_FOUND);
        }

        // Parse the lock token
        String[] lockInfo = WebDAV.parseLockToken(getLockToken());
        if (lockInfo == null)
        {
            // Bad lock token
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        // Get the lock status for the node
        LockService lockService = getDAVHelper().getLockService();
        // String nodeId = lockInfo[0];
        // String userName = lockInfo[1];

        LockStatus lockSts = lockService.getLockStatus(lockNodeInfo.getNodeRef());
        if (lockSts == LockStatus.LOCK_OWNER)
        {
            // Unlock the node
            lockService.unlock(lockNodeInfo.getNodeRef());

            // Indicate that the unlock was successful
            m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            // DEBUG
            if (logger.isDebugEnabled())
            {
                logger.debug("Unlock token=" + getLockToken() + " Successful");
            }
            
            updateVersionLockableAspect(lockNodeInfo.getNodeRef());
        }
        else if (lockSts == LockStatus.NO_LOCK)
        {
            // DEBUG
            if (logger.isDebugEnabled())
                logger.debug("Unlock token=" + getLockToken() + " Not locked");

            // Node is not locked
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }
        else if (lockSts == LockStatus.LOCKED)
        {
            // DEBUG
            if (logger.isDebugEnabled())
                logger.debug("Unlock token=" + getLockToken() + " Not lock owner");

            // Node is locked but not by this user
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }
        else if (lockSts == LockStatus.LOCK_EXPIRED)
        {
            // DEBUG
            if (logger.isDebugEnabled())
                logger.debug("Unlock token=" + getLockToken() + " Lock expired");

            // Return a success status
            m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    private void updateVersionLockableAspect(NodeRef lockNode) {
        if (getDAVHelper().getNodeService().hasAspect(lockNode, VersionsModel.Aspects.VERSION_LOCKABLE) == true) {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting VERSION_LOCKABLE aspect's lock to false on nodeRef = " + lockNode);
            }
            getDAVHelper().getNodeService().setProperty(lockNode, VersionsModel.Props.VersionLockable.LOCKED, false);
        }
    }
}
