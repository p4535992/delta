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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVMethod;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.dom4j.io.XMLWriter;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * Implements the WebDAV LOCK method
 * 
 * @author gavinc
 */
public class LockMethod extends WebDAVMethod {
    private String m_strLockToken = null;
    // timeout duration in seconds
    private static final int m_timeoutDuration = 180;
    private int m_requestTimeoutDuration;

    /**
     * Default constructor
     */
    public LockMethod() {
    }

    /**
     * Check if the lock token is valid
     * 
     * @return boolean
     */
    protected final boolean hasLockToken(NodeRef fileRef, String userName) {
        boolean hasToken = false;
        if (m_strLockToken != null) {
            // If lock token is present, check if the lock still belongs to this user but is expired. 
            // If so, mark the lock as nonexistent so it can be recreated, not refreshed. 
            // This can happen, if Office doesn't send update request for lock update. 
            LockStatus lockStatus = BeanHelper.getDocLockService().getLockStatus(fileRef);
            String lockOwner = (String) BeanHelper.getNodeService().getProperty(fileRef, ContentModel.PROP_LOCK_OWNER);
            if (!LockStatus.LOCK_EXPIRED.equals(lockStatus) || !StringUtils.equals(userName, lockOwner)) {
                hasToken = true;
            }
        }
        return hasToken;
    }

    /**
     * Return the lock token of an existing lock
     * 
     * @return String
     */
    protected final String getLockToken() {
        return m_strLockToken;
    }

    /**
     * Return the lock timeout, in minutes
     * 
     * @return int
     */
    protected final int getLockTimeout() {
        return m_timeoutDuration;
    }

    /**
     * Parse the request headers
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestHeaders() throws WebDAVServerException {
        // Get the lock token, if any

        m_strLockToken = parseIfHeader();

        m_userAgent = m_request.getHeader(WebDAV.HEADER_USER_AGENT);

        // Get the lock timeout value

        String strTimeout = m_request.getHeader(WebDAV.HEADER_TIMEOUT);

        // If the timeout header starts with anything other than Second
        // leave the timeout as the default

        if (strTimeout != null && strTimeout.startsWith(WebDAV.SECOND)) {
            try {
                // Some clients send header as Second-180 Seconds so we need to
                // look for the space

                int idx = strTimeout.indexOf(" ");

                if (idx != -1) {
                    // Get the bit after Second- and before the space

                    strTimeout = strTimeout.substring(WebDAV.SECOND.length(), idx);
                } else {
                    // The string must be in the correct format

                    strTimeout = strTimeout.substring(WebDAV.SECOND.length());
                }
                m_requestTimeoutDuration = Integer.parseInt(strTimeout);
            } catch (Exception e) {
                // Warn about the parse failure and leave the timeout as the
                // default

                logger.warn("Failed to parse Timeout header: " + strTimeout);
            }
        }

        // DEBUG

        if (logger.isDebugEnabled()) {
            logger.debug("Lock lockToken=" + getLockToken() + ", request timeout=" + m_requestTimeoutDuration
                    + ", user-agent=" + m_request.getHeader(WebDAV.HEADER_USER_AGENT));
        }
    }

    /**
     * Parse the request body
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestBody() throws WebDAVServerException {
        // NOTE: There is a body for lock requests which contain the
        // type of lock to apply and the lock owner but we will
        // ignore these settings so don't bother reading the body
    }

    @Override
    protected void executeImpl() throws WebDAVServerException, Exception {
        String path = getPath();
        // Get the active user
        String userName = UserUtil.getUsernameAndSession(getDAVHelper().getAuthenticationService().getCurrentUserName(), FacesContext.getCurrentInstance());

        if (logger.isDebugEnabled()) {
            logger.debug("Locking node: \n" +
                    "   user: " + userName + "\n" +
                    "   path: " + path);
        }

        FileInfo lockNodeInfo = null;
        try {
            // Check if the path exists
            lockNodeInfo = getDAVHelper().getNodeForPath(getRootNodeRef(), getPath(), m_request.getServletPath());
        } catch (FileNotFoundException e) {
            // create not allowed
            throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN);
        }
        NodeRef fileRef = lockNodeInfo.getNodeRef();
        WebDAVCustomHelper.checkDocumentFileWritePermission(fileRef);
        Map<QName, Serializable> originalProps = getNodeService().getProperties(fileRef);

        int responseStatus = HttpServletResponse.SC_OK;
        try {
            // Check if this is a new lock or a refresh
            if (hasLockToken(fileRef, userName)) {
                // Refresh an existing lock
                refreshLock(fileRef, userName);
            } else {
                // Create a new lock
                createLock(fileRef, userName);
            }
            // Set modifier and modified properties to original, so that locking doesn't appear to change the file
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(ContentModel.PROP_MODIFIER, originalProps.get(ContentModel.PROP_MODIFIER));
            props.put(ContentModel.PROP_MODIFIED, originalProps.get(ContentModel.PROP_MODIFIED));
            getBehaviourFilter().disableBehaviour(ContentModel.ASPECT_AUDITABLE);
            getNodeService().addProperties(fileRef, props);
        } catch (WebDAVServerException e) {
            responseStatus = e.getHttpStatusCode();
            logger.error("Failed to refresh or create WebDAV lock", e);
        } finally {
            m_response.setHeader(WebDAV.HEADER_LOCK_TOKEN, "<" + WebDAV.makeLockToken(lockNodeInfo.getNodeRef(), userName) + ">");
            m_response.setHeader(WebDAV.HEADER_CONTENT_TYPE, WebDAV.XML_CONTENT_TYPE);
            // We either created a new lock or refreshed an existing lock, send back the lock details
            generateResponse(fileRef, userName, responseStatus);
        }
    }

    protected BehaviourFilter getBehaviourFilter() {
        BehaviourFilter behaviourFilter = (BehaviourFilter) getServiceRegistry().getService(QName.createQName("", "policyBehaviourFilter"));
        return behaviourFilter;
    }

    /**
     * Create a new lock
     * 
     * @param lockNode NodeRef
     * @param userName String
     * @exception WebDAVServerException
     */
    private final void createLock(NodeRef lockNode, String userName) throws WebDAVServerException {
        LockService lockService = getLockService();

        // Check the lock status of the node
        LockStatus lockSts = lockService.getLockStatus(lockNode);

        // ALF-3681 fix. WebDrive 10 client doesn't send If header when locked resource is updated so check the node by lockOwner.
        boolean webDrive10Fix = false;
        if (m_userAgent != null && m_userAgent.equals(WebDAV.AGENT_MICROSOFT_DATA_ACCESS_INTERNET_PUBLISHING_PROVIDER_DAV))
        {
            String currentUser = getAuthenticationService().getCurrentUserName();
            String lockOwner = (String) getNodeService().getProperty(lockNode, ContentModel.PROP_LOCK_OWNER);
            // OK to write - lock is owned by current user.
            webDrive10Fix = lockOwner != null && lockOwner.equals(currentUser);
        }

        // DEBUG
        if (logger.isDebugEnabled()) {
            logger.debug("Create lock status=" + lockSts);
        }

        if (lockSts == LockStatus.LOCKED && !webDrive10Fix || isLockingDisabled(lockNode)) {
            // Indicate that the resource is already locked
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED);
        }
        if (lockSts == LockStatus.LOCK_OWNER) {
            logger.warn("user " + userName + " is already lock owner");
        }

        // Lock the node
        lockService.lock(lockNode, LockType.WRITE_LOCK, getLockTimeout());
        BeanHelper.getDocLockService().lockGeneratedFileDocument(lockNode);

        ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().addVersionLockableAspect(lockNode);
        ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().setVersionLockableAspect(lockNode, false);
    }

    private boolean isLockingDisabled(NodeRef lockNode) {
        FileService fileService = BeanHelper.getFileService();
        return (fileService.isFileGeneratedFromTemplate(lockNode) || fileService.isFileAssociatedWithDocMetadata(lockNode))
                && BeanHelper.getDocLockService().isGeneratedFileDocumentLocked(lockNode);
    }

    /**
     * Refresh an existing lock
     * 
     * @param lockNode NodeRef
     * @param userName String
     * @exception WebDAVServerException
     */
    private final void refreshLock(NodeRef lockNode, String userName) throws WebDAVServerException {
        LockService lockService = getLockService();

        // Check the lock status of the node
        LockStatus lockSts = lockService.getLockStatus(lockNode);

        // DEBUG
        if (logger.isDebugEnabled()) {
            logger.debug("Refresh lock status=" + lockSts);
        }

        if (lockSts != LockStatus.LOCK_OWNER) {
            // Indicate that the resource is already locked
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED);
        }

        // Update the expiry for the lock
        lockService.lock(lockNode, LockType.WRITE_LOCK, getLockTimeout());
        BeanHelper.getDocLockService().lockGeneratedFileDocument(lockNode);
    }

    /**
     * Generates the XML lock discovery response body
     */
    private void generateResponse(NodeRef lockNode, String userName, int responseStatus) throws Exception {
        // Send the XML back to the client
        m_response.setStatus(responseStatus);

        XMLWriter xml = createXMLWriter();

        xml.startDocument();

        String nsdec = generateNamespaceDeclarations(null);
        xml.startElement(WebDAV.DAV_NS, WebDAV.XML_MULTI_STATUS + nsdec, WebDAV.XML_NS_MULTI_STATUS + nsdec,
                getDAVHelper().getNullAttributes());

        // Output the lock details
        generateLockDiscoveryXML(xml, lockNode);

        // Close off the XML
        xml.endElement(WebDAV.DAV_NS, WebDAV.XML_MULTI_STATUS, WebDAV.XML_NS_MULTI_STATUS);

        xml.flush();
    }
}
