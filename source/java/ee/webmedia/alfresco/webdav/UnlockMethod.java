package ee.webmedia.alfresco.webdav;

import java.util.Set;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.UserUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.webdav.LockInfo;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.lock.UnableToReleaseLockException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;


public class UnlockMethod extends org.alfresco.repo.webdav.UnlockMethod {

    @Override
    protected void attemptUnlock() throws WebDAVServerException {
        if (logger.isDebugEnabled()) {
            logger.debug("Lock node; path=" + getPath() + ", token=" + getLockToken());
        }

        FileInfo lockNodeInfo = null;
        try
        {
            lockNodeInfo = getNodeForPath(getRootNodeRef(), getPath());
        }
        catch (FileNotFoundException e)
        {
            throw new WebDAVServerException(HttpServletResponse.SC_NOT_FOUND);
        }

        // Parse the lock token
        String[] lockInfoFromRequest = WebDAV.parseLockToken(getLockToken());
        if (lockInfoFromRequest == null)
        {
            // Bad lock token
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        NodeRef nodeRef = lockNodeInfo.getNodeRef();
        LockInfo lockInfo = getDAVLockService().getLockInfo(nodeRef);

        if (lockInfo == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Unlock token=" + getLockToken() + " Not locked - no info in lock store.");
            }
            // Node is not locked
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }


        if (!lockInfo.isLocked())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Unlock token=" + getLockToken() + " Not locked");
            }
            // Node is not locked
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }
        else if (lockInfo.isExpired())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Unlock token=" + getLockToken() + " Lock expired");
            }
            // Return a success status
            m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            removeNoContentAspect(nodeRef);
        }
        else if (lockInfo.isExclusive())
        {
            String currentUser = UserUtil.getUsernameAndSession(getDAVHelper().getAuthenticationService().getCurrentUserName(), FacesContext.getCurrentInstance());
            if (currentUser.equals(lockInfo.getOwner()))
            {
            	try
                {
                    BeanHelper.getDocLockService().unlockFile(nodeRef);
                }
                catch (UnableToReleaseLockException e)
                {
                    throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED, e);
                }

                // Indicate that the unlock was successful
                m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                removeNoContentAspect(nodeRef);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Unlock token=" + getLockToken() + " Successful");
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Unlock token=" + getLockToken() + " Not lock owner");
                }
                // Node is not locked
                throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
            }

            getBehaviourFilter().disableBehaviour(ContentModel.ASPECT_AUDITABLE);
            ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().setVersionLockableAspect(nodeRef, false);
        }
        else if (lockInfo.isShared())
        {
            Set<String> sharedLocks = lockInfo.getSharedLockTokens();
            if (sharedLocks.contains(m_strLockToken))
            {
                sharedLocks.remove(m_strLockToken);

                // Indicate that the unlock was successful
                m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                removeNoContentAspect(nodeRef);

                // DEBUG
                if (logger.isDebugEnabled())
                {
                    logger.debug("Unlock token=" + getLockToken() + " Successful");
                }
            }
        }
        else
        {
            throw new IllegalStateException("Invalid LockInfo state: " + lockInfo);
        }
    }

	protected BehaviourFilter getBehaviourFilter() {
        BehaviourFilter behaviourFilter = (BehaviourFilter) getServiceRegistry().getService(QName.createQName("", "policyBehaviourFilter"));
        return behaviourFilter;
    }

}
