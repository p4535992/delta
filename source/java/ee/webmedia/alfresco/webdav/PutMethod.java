/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package ee.webmedia.alfresco.webdav;

import static ee.webmedia.alfresco.common.web.BeanHelper.getPolicyBehaviourFilter;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import ee.webmedia.alfresco.utils.UserUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.webdav.LockInfo;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVMethod;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import org.springframework.dao.ConcurrencyFailureException;


/**
 * Implements the WebDAV PUT method
 *
 * @author Gavin Cornwell
 */
public class PutMethod extends WebDAVMethod
{
    // Request parameters
    private String m_strContentType = null;
    private boolean m_expectHeaderPresent = false;
    // Indicates if a zero byte node was created by a LOCK call.
    // Try to delete the node if the PUT fails
    private boolean noContent = false;
    private boolean created = false;
    private FileInfo contentNodeInfo;
    private long fileSize;

    /**
     * Default constructor
     */
    public PutMethod()
    {
    }

    /**
     * Parse the request headers
     *
     * @exception org.alfresco.repo.webdav.WebDAVServerException
     */
    protected void parseRequestHeaders() throws WebDAVServerException
    {
        m_strContentType = m_request.getHeader(WebDAV.HEADER_CONTENT_TYPE);
        String strExpect = m_request.getHeader(WebDAV.HEADER_EXPECT);

        if (strExpect != null && strExpect.equals(WebDAV.HEADER_EXPECT_CONTENT))
        {
            m_expectHeaderPresent = true;
        }

        // Parse Lock tokens and ETags, if any

        parseIfHeader();
    }

    /**
     * Clears the aspect added by a LOCK request for a new file, so
     * that the Timer started by the LOCK request will not remove the
     * node now that the PUT request has been received. This is needed
     * for large content.
     *
     * @exception org.alfresco.repo.webdav.WebDAVServerException
     */
    protected void parseRequestBody() throws WebDAVServerException
    {
        // Nothing is done with the body by this method. The body contains
        // the content it will be dealt with later.

        // This method is called ONCE just before the FIRST call to executeImpl,
        // which is in a retrying transaction so may be called many times.

        // Although this method is called just before the first executeImpl,
        // it is possible that the Thread could be interrupted before the first call
        // or between calls. However the chances are low and the consequence
        // (leaving a zero byte file) is minor.

        noContent = getTransactionService().getRetryingTransactionHelper().doInTransaction(
                new RetryingTransactionHelper.RetryingTransactionCallback<Boolean>()
                {
                    public Boolean execute() throws Throwable
                    {
                        FileInfo contentNodeInfo = null;
                        try
                        {
                            contentNodeInfo = getNodeForPath(getRootNodeRef(), getPath());
                            checkNode(contentNodeInfo);
                            final NodeRef nodeRef = contentNodeInfo.getNodeRef();
                            if (getNodeService().hasAspect(contentNodeInfo.getNodeRef(), ContentModel.ASPECT_WEBDAV_NO_CONTENT))
                            {
                                getNodeService().removeAspect(nodeRef, ContentModel.ASPECT_WEBDAV_NO_CONTENT);
                                if (logger.isDebugEnabled())
                                {
                                    String path = getPath();
                                    logger.debug("Put Timer DISABLE " + path);
                                }
                                return Boolean.TRUE;
                            }
                        }
                        catch (FileNotFoundException e)
                        {
                            // Does not exist, so there will be no aspect.
                        }
                        return Boolean.FALSE;
                    }
                }, false, true);
    }

    /**
     * Execute the WebDAV request
     *
     * @exception org.alfresco.repo.webdav.WebDAVServerException
     */
    protected void executeImpl() throws WebDAVServerException, Exception
    {
        if (logger.isDebugEnabled())
        {
            String path = getPath();
            String userName = getDAVHelper().getAuthenticationService().getCurrentUserName();
            logger.debug("Put node: \n" +
                    "     user: " + userName + "\n" +
                    "     path: " + path + "\n" +
                    "noContent: " + noContent);
        }

        FileFolderService fileFolderService = getFileFolderService();

        // Get the status for the request path
        LockInfo nodeLockInfo = null;
        try
        {
            contentNodeInfo = getNodeForPath(getRootNodeRef(), getPath());
            // make sure that we are not trying to use a folder
            if (contentNodeInfo.isFolder())
            {
                throw new WebDAVServerException(HttpServletResponse.SC_BAD_REQUEST);
            }

            WebDAVCustomHelper.checkDocumentFileWritePermission(contentNodeInfo.getNodeRef());

            nodeLockInfo = checkNode(contentNodeInfo);

            // 'Unhide' nodes hidden by us and behave as though we created them
            //            NodeRef contentNodeRef = contentNodeInfo.getNodeRef();
            //            if (fileFolderService.isHidden(contentNodeRef) && !getDAVHelper().isRenameShuffle(getPath()))
            //            {
            //                fileFolderService.setHidden(contentNodeRef, false);
            //                created = true;
            //            }
        }
        catch (FileNotFoundException e)
        {
            // the file doesn't exist - create it
            String[] paths = getDAVHelper().splitPath(getPath());
            try
            {
                FileInfo parentNodeInfo = getNodeForPath(getRootNodeRef(), paths[0]);
                // create file
                contentNodeInfo = getDAVHelper().createFile(parentNodeInfo, paths[1]);
                created = true;

            }
            catch (FileNotFoundException ee)
            {
                // bad path
                throw new WebDAVServerException(HttpServletResponse.SC_CONFLICT);
            }
            catch (FileExistsException ee)
            {
                // ALF-7079 fix, retry: it looks like concurrent access (file not found but file exists)
                throw new ConcurrencyFailureException("Concurrent access was detected.",  ee);
            }
        }

        String userName = UserUtil.getUsernameAndSession(getDAVHelper().getAuthenticationService().getCurrentUserName(), FacesContext.getCurrentInstance());
        LockInfo lockInfo = getDAVLockService().getLockInfo(contentNodeInfo.getNodeRef());

        if (lockInfo != null)
        {
            if (lockInfo.isLocked() && !lockInfo.getOwner().equals(userName))
            {
                if (logger.isDebugEnabled())
                {
                    String path = getPath();
                    String owner = lockInfo.getOwner();
                    logger.debug("Node locked: path=["+path+"], owner=["+owner+"], current user=["+userName+"]");
                }
                // Indicate that the resource is locked
                throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED);
            }
        }
        // ALF-16808: We disable the versionable aspect if we are overwriting
        // empty content because it's probably part of a compound operation to
        // create a new single version
        boolean disabledVersioning = false;

        try
        {
            // Disable versioning if we are overwriting an empty file with content
            NodeRef nodeRef = contentNodeInfo.getNodeRef();
            ContentData contentData = (ContentData)getNodeService().getProperty(nodeRef, ContentModel.PROP_CONTENT);
            if ((contentData == null || contentData.getSize() == 0) && getNodeService().hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE))
            {
                getPolicyBehaviourFilter().disableBehaviour(nodeRef, ContentModel.ASPECT_VERSIONABLE);
                disabledVersioning = true;
            }
            // ALF-16756: To avoid firing inbound rules too early (while a node is still locked) apply the no content aspect
            if (nodeLockInfo != null && nodeLockInfo.isExclusive() && !(ContentData.hasContent(contentData) && contentData.getSize() > 0))
            {
                getNodeService().addAspect(contentNodeInfo.getNodeRef(), ContentModel.ASPECT_NO_CONTENT, null);
            }

            // Update the version if the node is unlocked
            boolean createdNewVersion = ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().updateVersion(nodeRef, contentNodeInfo.getName(), true);
            // Access the content
            ContentWriter writer = fileFolderService.getWriter(contentNodeInfo.getNodeRef());

            // set content properties
            String mimetype = getMimetypeService().guessMimetype(contentNodeInfo.getName());
            writer.setMimetype(mimetype);

            // Get the input stream from the request data
            InputStream is = m_request.getInputStream();

            ContentCharsetFinder charsetFinder = getMimetypeService().getContentCharsetFinder();
            Charset encoding = charsetFinder.getCharset(is, mimetype);
            writer.setEncoding(encoding.name());

            // Write the new data to the content node
            writer.putContent(is);
            // Ask for the document metadata to be extracted
//            Action extract = getActionService().createAction(ContentMetadataExtracter.EXECUTOR_NAME);
//            if(extract != null)
//            {
//                extract.setExecuteAsynchronously(false);
//                getActionService().executeAction(extract, contentNodeInfo.getNodeRef());
//            }

            // If the mime-type determined by the repository is different
            // from the original specified in the request, update it.
            if (m_strContentType == null || !m_strContentType.equals(writer.getMimetype()))
            {
                String oldMimeType = m_strContentType;
                m_strContentType = writer.getMimetype();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Mimetype originally specified as " + oldMimeType +
                            ", now guessed to be " + m_strContentType);
                }
            }

            // add the user and date information to the custom aspect properties
            ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().updateVersionModifiedAspect(nodeRef);
            // Update document search info
            NodeRef document = getNodeService().getPrimaryParent(nodeRef).getParentRef();
            ((WebDAVCustomHelper) getDAVHelper()).getDocumentService().updateSearchableFiles(document);

            // Throw exception when user tries to save mandatory fields as blank
            try {
                // Update Document meta data and generated files
                BeanHelper.getDocumentDynamicService().updateDocumentAndGeneratedFiles(nodeRef, document, true);
            } catch (UnableToPerformException e) {
                if ("notification_document_saving_failed_due_to_blank_mandatory_fields".equals(e.getMessageKey())) {
                    String userId = AuthenticationUtil.getRunAsUser();
                    Object[] obj = e.getMessageValuesForHolders();
                    Object regNr = obj[0];
                    Object docName = obj[1];
                    Object fileName = obj[2];
                    Object emptyFields = obj[3];
                    BeanHelper.getNotificationService().addUserSpecificNotification(userId,
                            MessageUtil.getMessage("notification_document_saving_failed_due_to_blank_mandatory_fields", regNr, docName, fileName, emptyFields));
                }
                throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN);
            }

            // Record the uploaded file's size
            fileSize = writer.getSize();

            // Set the response status, depending if the node existed or not
            m_response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_NO_CONTENT);
            logger.debug("saved file " + nodeRef + ", " + (createdNewVersion ? "created" : "didn't crerate") + " new version");
        }
        catch (AccessDeniedException e)
        {
            throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN, e);
        }
        catch (Throwable e)
        {
            // check if the node was marked with noContent aspect previously by lock method AND
            // we are about to give up
            if (noContent && RetryingTransactionHelper.extractRetryCause(e) == null)
            {
                // remove the 0 bytes content if save operation failed or was cancelled
                final NodeRef nodeRef = contentNodeInfo.getNodeRef();
                getTransactionService().getRetryingTransactionHelper().doInTransaction(
                        new RetryingTransactionHelper.RetryingTransactionCallback<String>()
                        {
                            public String execute() throws Throwable
                            {
                                getNodeService().deleteNode(nodeRef);
                                if (logger.isDebugEnabled())
                                {
                                    logger.debug("Put failed. DELETE  " + getPath());
                                }
                                return null;
                            }
                        }, false, false);
            }
            throw new WebDAVServerException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
        finally
        {
            if (disabledVersioning)
            {
                getPolicyBehaviourFilter().enableBehaviour(contentNodeInfo.getNodeRef(), ContentModel.ASPECT_VERSIONABLE);
            }
        }

    }

    /**
     * Can be used after a successful {@link #execute()} invocation to
     * check whether the resource was new (created) or over-writing existing
     * content.
     *
     * @return true if the content was newly created, false if existing.
     */
    protected boolean isCreated()
    {
        return created;
    }

    /**
     * Retrieve the mimetype of the content sent for the PUT request. The initial
     * value specified in the request may be updated after the file contents have
     * been uploaded if the repository has determined a different mimetype for the content.
     *
     * @return content-type
     */
    public String getContentType()
    {
        return m_strContentType;
    }

    /**
     * The FileInfo for the uploaded file, or null if not yet uploaded.
     *
     * @return FileInfo
     */
    public FileInfo getContentNodeInfo()
    {
        return contentNodeInfo;
    }

    /**
     * Returns the size of the uploaded file, zero if not yet uploaded.
     *
     * @return the fileSize
     */
    public long getFileSize()
    {
        return fileSize;
    }
}
