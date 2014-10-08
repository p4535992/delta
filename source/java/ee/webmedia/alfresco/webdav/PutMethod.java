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

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVMethod;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * Implements the WebDAV PUT method
 * 
 * @author Gavin Cornwell
 */
public class PutMethod extends WebDAVMethod {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PutMethod.class);

    // Request parameters
    private String m_strContentType = null;

    /**
     * Parse the request headers
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestHeaders() throws WebDAVServerException {
        m_strContentType = m_request.getHeader(WebDAV.HEADER_CONTENT_TYPE);

        // Get the lock token, if any
        parseIfHeader();
    }

    /**
     * Parse the request body
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestBody() throws WebDAVServerException {
        // Nothing to do in this method, the body contains
        // the content it will be dealt with later
    }

    /**
     * Exceute the WebDAV request
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void executeImpl() throws WebDAVServerException, Exception {
        FileFolderService fileFolderService = getFileFolderService();

        // Get the status for the request path
        FileInfo contentNodeInfo = null;
        boolean created = false;
        try {
            contentNodeInfo = getDAVHelper().getNodeForPath(getRootNodeRef(), getPath(), getServletPath());
            // make sure that we are not trying to use a folder
            if (contentNodeInfo.isFolder()) {
                throw new WebDAVServerException(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (FileNotFoundException e) {
            // create not allowed
            throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN);
        }
        NodeRef fileRef = contentNodeInfo.getNodeRef();
        WebDAVCustomHelper.checkDocumentFileWritePermission(fileRef);

        // Require the file to be locked for current user
        LockStatus lockStatus = getDocLockService().getLockStatus(fileRef);
        if (!LockStatus.LOCK_OWNER.equals(lockStatus)) {
            log.info("Not saving " + fileRef + ". LockStatus is " + lockStatus.name() + ", lock owner " + getDocLockService().getLockOwnerIfLockedByOther(fileRef));
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED);
        }

        if (m_request.getContentLength() <= 0) {
            StringBuilder s = new StringBuilder("Client is trying to save zero-length content, ignoring and returning success; request headers:");
            for (Enumeration<?> e = m_request.getHeaderNames(); e.hasMoreElements();) {
                String headerName = (String) e.nextElement();
                s.append("\n  ").append(headerName).append(": ").append(m_request.getHeader(headerName));
            }
            log.warn(s.toString());
            // Set the response status, depending if the node existed or not
            m_response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK);

            m_response.setContentType("text/plain");
            m_response.setCharacterEncoding("UTF-8");
            PrintWriter writer = m_response.getWriter();
            try {
                writer.println("You are trying to save zero-length content, we are ignoring it and returning a successful result.");
                writer.println("This is probably a weird behaviour of the WebDAV client [described here http://java.net/jira/browse/JERSEY-154]:");
                writer.print("Some HTTP clients are sending empty bodies in PUTs. e. g. Microsoft's 'WebDAV-Mini-Redirector' does this: It first sends a PUT with Content-Length=0 ");
                writer.print("and an empty (zero bytes) body, and if that returns 200 OK it sends another PUT with 'correct' concent-length and full body; seems to be somekind of ");
                writer.println("safety or performance optimization.");
                writer.flush();
            } finally {
                writer.close();
            }
            return;
        }

        // Update the version if the node is unlocked
        boolean createdNewVersion = ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().updateVersion(fileRef, contentNodeInfo.getName(), true);

        // Access the content
        ContentWriter writer = fileFolderService.getWriter(fileRef);

        // Get the input stream from the request data
        InputStream is = m_request.getInputStream();

        // Do not allow to change mimeType or locale, use the same values as were set during file creation
        ContentData contentData = contentNodeInfo.getContentData();
        if (contentData == null) {
            log.warn("ContentData for node is null: " + fileRef);

            // set content properties
            String mimetype = getMimetypeService().guessMimetype(contentNodeInfo.getName());
            writer.setMimetype(mimetype);

            // Get the input stream from the request data
            is = is.markSupported() ? is : new BufferedInputStream(is);

            ContentCharsetFinder charsetFinder = getMimetypeService().getContentCharsetFinder();
            Charset encoding = charsetFinder.getCharset(is, mimetype);
            writer.setEncoding(encoding.name());

        } else {
            String mimetype = contentData.getMimetype();
            writer.setMimetype(mimetype);
            writer.setEncoding(contentData.getEncoding());
            if (m_strContentType != null && !mimetype.equalsIgnoreCase(m_strContentType)) {
                log.info("Client sent different mimetype '" + m_strContentType + "' when updating file with original mimetype '" + mimetype + "', ignoring");
            }
        }

        // Write the new data to the content node
        log.info("Writing data to " + writer.getContentUrl());
        writer.putContent(is);

        if (writer.getSize() <= 0) {
            throw new RuntimeException("Saving zero-length content is not allowed" + ", is=" + is);
        }

        // add the user and date information to the custom aspect properties
        ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().updateVersionModifiedAspect(fileRef);

        // Update document search info
        NodeRef document = getNodeService().getPrimaryParent(fileRef).getParentRef();
        ((WebDAVCustomHelper) getDAVHelper()).getDocumentService().updateSearchableFiles(document);

        // Throw exception when user tries to save mandatory fields as blank
        try {
            // Update Document meta data and generated files
            BeanHelper.getDocumentDynamicService().updateDocumentAndGeneratedFiles(fileRef, document, true);
        } catch (UnableToPerformException e) {
            if ("notification_document_saving_failed_due_to_blank_mandatory_fields".equals(e.getMessageKey())) {
                String userId = AuthenticationUtil.getRunAsUser();
                Object[] obj = e.getMessageValuesForHolders();
                Object regNr = obj[0];
                Object docName = obj[1];
                Object fileName = obj[2];
                Object emptyFileds = obj[3];
                BeanHelper.getNotificationService().addUserSpecificNotification(userId,
                        MessageUtil.getMessage("notification_document_saving_failed_due_to_blank_mandatory_fields", regNr, docName, fileName, emptyFileds));
            }
            throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN);
        }

        // Set the response status, depending if the node existed or not
        m_response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_NO_CONTENT);
        logger.debug("saved file " + fileRef + ", " + (createdNewVersion ? "created" : "didn't crerate") + " new version");
    }
}
